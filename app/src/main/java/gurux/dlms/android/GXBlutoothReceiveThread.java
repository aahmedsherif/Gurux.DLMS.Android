package gurux.dlms.android;

import android.bluetooth.BluetoothSocket;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Array;

import gurux.common.GXSynchronousMediaBase;
import gurux.common.ReceiveEventArgs;
import gurux.common.TraceEventArgs;
import gurux.common.enums.TraceLevel;
import gurux.common.enums.TraceTypes;

public class GXBlutoothReceiveThread  extends Thread {
    private static final int WAIT_TIME = 200;
    private BluetoothSocket mConnection;
    private InputStream mInput;
    private final GXBlutooth mParentMedia;
    private long mBytesReceived = 0L;

    GXBlutoothReceiveThread(GXBlutooth parent, BluetoothSocket conn, InputStream input) {
        this.mConnection = conn;
        this.mInput = input;
        this.mParentMedia = parent;


    }

    final long getBytesReceived() {
        return this.mBytesReceived;
    }

    final void resetBytesReceived() {
        this.mBytesReceived = 0L;
    }

    private void handleReceivedData(byte[] buffer, int len) {
        this.mBytesReceived += (long)len;
        int totalCount = 0;
        if (this.mParentMedia.getIsSynchronous()) {
            TraceEventArgs arg = null;
            synchronized(this.mParentMedia.getSyncBase().getSync()) {
                this.mParentMedia.getSyncBase().appendData(buffer, 0, len);
                if (this.mParentMedia.getEop() != null) {
                    if (this.mParentMedia.getEop() instanceof Array) {
                        Object[] var6 = (Object[])((Object[])this.mParentMedia.getEop());
                        int var7 = var6.length;

                        for(int var8 = 0; var8 < var7; ++var8) {
                            Object eop = var6[var8];
                            totalCount = GXSynchronousMediaBase.indexOf(buffer, GXSynchronousMediaBase.getAsByteArray(eop), 0, len);
                            if (totalCount != -1) {
                                break;
                            }
                        }
                    } else {
                        totalCount = GXSynchronousMediaBase.indexOf(buffer, GXSynchronousMediaBase.getAsByteArray(this.mParentMedia.getEop()), 0, len);
                    }
                }

                if (totalCount != -1) {
                    if (this.mParentMedia.getTrace() == TraceLevel.VERBOSE) {
                        arg = new TraceEventArgs(TraceTypes.RECEIVED, buffer, 0, totalCount + 1);
                    }

                    this.mParentMedia.getSyncBase().setReceived();
                }
            }

            if (arg != null) {
                this.mParentMedia.notifyTrace(arg);
            }
        } else {
            this.mParentMedia.getSyncBase().resetReceivedSize();
            byte[] data = new byte[len];
            System.arraycopy(buffer, 0, data, 0, len);
            if (this.mParentMedia.getTrace() == TraceLevel.VERBOSE) {
                this.mParentMedia.notifyTrace(new TraceEventArgs(TraceTypes.RECEIVED, data));
            }

            ReceiveEventArgs arg = new ReceiveEventArgs(data, this.mParentMedia.getPort().getPort());
            this.mParentMedia.notifyReceived(arg);
        }

    }

    public final void run() {


        byte[] buff = new byte[this.mConnection.getMaxReceivePacketSize()];

        while(!Thread.currentThread().isInterrupted()) {
            try {

                if(!this.mConnection.isConnected()){
                    break;
                }

                if(this.mConnection.getInputStream().available() > 0) {


                    int len = this.mConnection.getInputStream().read(buff);
                    if (len == 0 && Thread.currentThread().isInterrupted()) {
                        break;
                    }


                    if (len > 0) {
                        if (this.mParentMedia.getReceiveDelay() > 0) {
                            long start = System.currentTimeMillis();
                            int elapsedTime = 0;
                            ByteArrayOutputStream tmp = new ByteArrayOutputStream();
                            tmp.write(buff, 0, len);

                            while ( this.mConnection.getInputStream().available() > 0 && (len = this.mConnection.getInputStream().read(buff)) > 0) {

                                if (len > 0) {
                                    tmp.write(buff, 0, len);
                                }

                                elapsedTime = (int) (System.currentTimeMillis() - start);
                                if (this.mParentMedia.getReceiveDelay() - elapsedTime < 1) {
                                    break;
                                }
                            }

                            buff = tmp.toByteArray();
                            len = buff.length;
                        }

                        this.handleReceivedData(buff, len);
                    }
                }
            } catch (Exception var7) {
                if (!Thread.currentThread().isInterrupted()) {
                    this.mParentMedia.notifyError(new RuntimeException(var7.getMessage()));
                }
            }
        }

    }
}
