package gurux.dlms.android;

import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import gurux.common.GXCommon;
import gurux.common.GXSync;
import gurux.common.GXSynchronousMediaBase;
import gurux.common.IGXMedia2;
import gurux.common.IGXMediaListener;
import gurux.common.MediaStateEventArgs;
import gurux.common.PropertyChangedEventArgs;
import gurux.common.ReceiveParameters;
import gurux.common.TraceEventArgs;
import gurux.common.enums.MediaState;
import gurux.common.enums.TraceLevel;
import gurux.common.enums.TraceTypes;
import gurux.io.BaudRate;
import gurux.io.Parity;
import gurux.io.StopBits;
import gurux.serial.GXPort;
import gurux.serial.GXProperties;
import gurux.serial.GXPropertiesFragment;
import gurux.serial.IGXSerialListener;
import gurux.serial.enums.AvailableMediaSettings;

public class GXBlutooth  implements IGXMedia2, AutoCloseable {

    private int receiveDelay;
    private int asyncWaitTime;
    private Context mContext;
    private GXSynchronousMediaBase mSyncBase;
    private List<IGXMediaListener> mMediaListeners;
    private List<IGXSerialListener> mPortListeners;
    private GXPort mPort;
    private Activity mActivity;
    private BaudRate mBaudRate;
    private StopBits mStopBits;
    private Parity mParity;
    private int mDataBits;
    private int mWriteTimeout;
    private int mReadTimeout;
    private long mBytesSend;
    private int mSynchronous;
    private TraceLevel mTrace;
    private static List<GXPort> mPorts;
    private Object mEop;
    private int mConfigurableSettings;
    private BluetoothAdapter mBluetoothAdapter;

    private InputStream mmInStream;
    private OutputStream mmOutStream;
    private BluetoothSocket mmSocket;
    private BluetoothDevice mmDevice;

    public GXBlutooth(Context context){

        this.mBaudRate = BaudRate.BAUD_RATE_9600;
        this.mDataBits = 8;
        this.mStopBits = StopBits.ONE;
        this.mParity = Parity.NONE;
        this.mWriteTimeout = 5000;
        this.mReadTimeout = 5000;
        this.mBytesSend = 0L;
        this.mSynchronous = 0;
        this.mTrace = TraceLevel.OFF;
        this.mMediaListeners = new ArrayList();
        this.mPortListeners = new ArrayList();
        this.init(context);
    }

    public GXBlutooth(Activity activity) {
        this.mBaudRate = BaudRate.BAUD_RATE_9600;
        this.mDataBits = 8;
        this.mStopBits = StopBits.ONE;
        this.mParity = Parity.NONE;
        this.mWriteTimeout = 5000;
        this.mReadTimeout = 5000;
        this.mBytesSend = 0L;
        this.mSynchronous = 0;
        this.mTrace = TraceLevel.OFF;
        this.mMediaListeners = new ArrayList();
        this.mPortListeners = new ArrayList();
        this.init(activity);
        this.mActivity = activity;
    }

    public GXBlutooth(Context context, String port, BaudRate baudRate, int dataBits, Parity parity, StopBits stopBits) {
        this.mBaudRate = BaudRate.BAUD_RATE_9600;
        this.mDataBits = 8;
        this.mStopBits = StopBits.ONE;
        this.mParity = Parity.NONE;
        this.mWriteTimeout = 5000;
        this.mReadTimeout = 5000;
        this.mBytesSend = 0L;
        this.mSynchronous = 0;
        this.mTrace = TraceLevel.OFF;
        this.mMediaListeners = new ArrayList();
        this.mPortListeners = new ArrayList();
        this.init(context);
        GXPort[] var7 = this.getPorts();
        int var8 = var7.length;

        for(int var9 = 0; var9 < var8; ++var9) {
            GXPort it = var7[var9];
            if (port.compareToIgnoreCase(it.getPort()) == 0) {
                this.setPort(it);
                break;
            }
        }

        this.setBaudRate(baudRate);
        this.setDataBits(dataBits);
        this.setParity(parity);
        this.setStopBits(stopBits);
    }

    public GXBlutooth(Activity activity, String port, BaudRate baudRate, int dataBits, Parity parity, StopBits stopBits) {
        this.mBaudRate = BaudRate.BAUD_RATE_9600;
        this.mDataBits = 8;
        this.mStopBits = StopBits.ONE;
        this.mParity = Parity.NONE;
        this.mWriteTimeout = 5000;
        this.mReadTimeout = 5000;
        this.mBytesSend = 0L;
        this.mSynchronous = 0;
        this.mTrace = TraceLevel.OFF;
        this.mMediaListeners = new ArrayList();
        this.mPortListeners = new ArrayList();
        this.init(activity);
        this.mActivity = activity;
        GXPort[] var7 = this.getPorts();
        int var8 = var7.length;

        for(int var9 = 0; var9 < var8; ++var9) {
            GXPort it = var7[var9];
            if (port.compareToIgnoreCase(it.getPort()) == 0) {
                this.setPort(it);
                break;
            }
        }

        this.setBaudRate(baudRate);
        this.setDataBits(dataBits);
        this.setParity(parity);
        this.setStopBits(stopBits);
    }

    private void init(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context");
        } else {
            this.mContext = context;
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                Toast.makeText(this.mContext,"Bluetooth not supported",Toast.LENGTH_SHORT).show();

                // throw new Exception("Bluetooth not supported");
            }
            this.mSyncBase = new GXSynchronousMediaBase(200);
            this.setConfigurableSettings(AvailableMediaSettings.ALL.getValue());
        }
    }

    @Override
    public int getReceiveDelay() {
        return receiveDelay;
    }

    @Override
    public void setReceiveDelay(int value) {
        receiveDelay = value;
    }

    @Override
    public int getAsyncWaitTime() {
        return asyncWaitTime;
    }

    @Override
    public void setAsyncWaitTime(int value) {
        asyncWaitTime = value;
    }

    @Override
    public Object getAsyncWaitHandle() {
        return null;
    }

    @Override
    public void addListener(IGXMediaListener listener) {
        this.mMediaListeners.add(listener);
        if (listener instanceof IGXSerialListener) {
            this.mPortListeners.add((IGXSerialListener)listener);
        }

    }

    @Override
    public void removeListener(IGXMediaListener listener) {
        this.mMediaListeners.remove(listener);
        if (listener instanceof IGXSerialListener) {
            this.mPortListeners.remove((IGXSerialListener)listener);
        }
    }

    @Override
    public void copy(Object target) {
        GXBlutooth tmp = (GXBlutooth)target;
        this.setPort(tmp.getPort());
        this.setBaudRate(tmp.getBaudRate());
        this.setStopBits(tmp.getStopBits());
        this.setParity(tmp.getParity());
        this.setDataBits(tmp.getDataBits());
    }

    @Override
    public String getName() {
        return this.getPort() == null ? "" : this.getPort().getPort();
    }

    @Override
    public TraceLevel getTrace() {
        return this.mTrace;
    }

    @Override
    public void setTrace(TraceLevel value) {
        this.mTrace = value;
        this.mSyncBase.setTrace(value);
    }

    @Override
    public void open() throws Exception {
        final String TAG = GXBlutooth.class.getSimpleName();
        final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


        this.close();


        synchronized(this.mSyncBase.getSync()) {
            this.mSyncBase.resetLastPosition();
        }

        this.notifyMediaStateChange(MediaState.OPENING);
        if (this.mTrace.ordinal() >= TraceLevel.INFO.ordinal()) {
            String eopString = "None";
            if (this.getEop() instanceof byte[]) {
                eopString = GXCommon.bytesToHex((byte[])((byte[])this.getEop()));
            } else if (this.getEop() != null) {
                eopString = this.getEop().toString();
            }

            this.notifyTrace(new TraceEventArgs(TraceTypes.INFO, "Settings: Port: " + this.getPort() + " Baud Rate: " + this.getBaudRate() + " Data Bits: " + this.getDataBits() + " Parity: " + this.getParity().toString() + " Stop Bits: " + this.getStopBits().toString() + " Eop:" + eopString));
        }


        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();



        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                if(device.getName().contains("REDZ")){

                    //connect(device);

                    BluetoothAdapter mBluetoothAdapter;
                    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

                    BluetoothSocket tmp = null;
                    mmDevice = device;

                    try {
                        // Get a BluetoothSocket to connect with the given BluetoothDevice.
                        // MY_UUID is the app's UUID string, also used in the server code.
                        tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                    } catch (IOException e) {
                        Log.e(TAG, "Socket's create() method failed", e);
                    }
                    mmSocket = tmp;


                    if(mBluetoothAdapter.isDiscovering()) {
                        mBluetoothAdapter.cancelDiscovery();
                    }

                    try {
                        // Connect to the remote device through the socket. This call blocks
                        // until it succeeds or throws an exception.
                        mmSocket.connect();
                    } catch (IOException connectException) {
                        // Unable to connect; close the socket and return.
                        try {
                            mmSocket.close();
                        } catch (IOException closeException) {
                            Log.e(TAG, "Could not close the client socket", closeException);
                        }
                        return;
                    }



                    InputStream tmpIn = null;
                    OutputStream tmpOut = null;

                    // Get the input and output streams; using temp objects because
                    // member streams are final.
                    try {
                        tmpIn = mmSocket.getInputStream();
                    } catch (IOException e) {
                        Log.e(TAG, "Error occurred when creating input stream", e);
                    }
                    try {
                        tmpOut = mmSocket.getOutputStream();
                    } catch (IOException e) {
                        Log.e(TAG, "Error occurred when creating output stream", e);
                    }

                    mmInStream = tmpIn;
                    mmOutStream = tmpOut;

                }
            }
        }
    }

    private void connect(BluetoothDevice device) {
        ConnectThread connectThread = new ConnectThread(device);
        connectThread.start();
    }


    @Override
    public boolean isOpen() {

        return (mmInStream != null && mmOutStream != null);
    }

    @Override
    public void close() {



        try {

            if(mmSocket != null){
                mmSocket.close();
                mmSocket = null;
            }

            if(mmOutStream != null){
                mmOutStream.close();
                mmOutStream = null;
            }

            if(mmInStream != null){
                mmInStream.close();
                mmInStream = null;
            }

            this.notifyMediaStateChange(MediaState.CLOSING);
        } catch (IOException e) {
            String TAG = GXBlutooth.class.getSimpleName();
            Log.e(TAG, "Could not close the connect socket", e);
        } catch (RuntimeException var5) {
            this.notifyError(var5);
            throw var5;

        } finally {

            this.notifyMediaStateChange(MediaState.CLOSED);
            this.mBytesSend = 0L;
            this.mSyncBase.resetReceivedSize();
        }
    }

    private void notifyMediaStateChange(MediaState state) {
        IGXMediaListener listener;
        for(Iterator var2 = this.mMediaListeners.iterator(); var2.hasNext(); listener.onMediaStateChange(this, new MediaStateEventArgs(state))) {
            listener = (IGXMediaListener)var2.next();
            if (this.mTrace.ordinal() >= TraceLevel.ERROR.ordinal()) {
                listener.onTrace(this, new TraceEventArgs(TraceTypes.INFO, state));
            }
        }

    }

    final void notifyError(final RuntimeException ex) {
        if (this.mActivity != null) {
            this.mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    Iterator var1 = GXBlutooth.this.mMediaListeners.iterator();

                    while(var1.hasNext()) {
                        IGXMediaListener listener = (IGXMediaListener)var1.next();
                        listener.onError(this, ex);
                        if (GXBlutooth.this.mTrace.ordinal() >= TraceLevel.ERROR.ordinal()) {
                            listener.onTrace(this, new TraceEventArgs(TraceTypes.ERROR, ex));
                        }
                    }

                }
            });
        } else {
            Iterator var2 = this.mMediaListeners.iterator();

            while(var2.hasNext()) {
                IGXMediaListener listener = (IGXMediaListener)var2.next();
                listener.onError(this, ex);
                if (this.mTrace.ordinal() >= TraceLevel.ERROR.ordinal()) {
                    listener.onTrace(this, new TraceEventArgs(TraceTypes.ERROR, ex));
                }
            }
        }

    }

    @Override
    public void send(Object data, String receiver) throws Exception {
        this.send(data);
    }

    public final void send(Object data) throws Exception {
        String TAG = GXBlutooth.class.getSimpleName();

        if (this.mmOutStream == null)
            throw new RuntimeException("Blutooth connection is not open.");

        try {
            if (this.mTrace == TraceLevel.VERBOSE) {
                this.notifyTrace(new TraceEventArgs(TraceTypes.SENT, data));
            }

            this.mSyncBase.resetLastPosition();
            byte[] buff = GXSynchronousMediaBase.getAsByteArray(data);
            if (buff == null) {
                throw new IllegalArgumentException("Data send failed. Invalid data.");
            } else {

                mmOutStream.write(buff);


                this.mBytesSend += (long)buff.length;



              /*  Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        String TAG = GXBlutooth.class.getSimpleName();*/
                // Keep listening to the InputStream until an exception occurs.

                byte[] mmBuffer; // mmBuffer store for the stream
                mmBuffer = new byte[1024];
                int numBytes; // bytes returned from read()


                while (true) {
                    try {
                        // Read from the InputStream.
                        numBytes = mmInStream.read(mmBuffer);

                        // Send the obtained bytes to the UI activity.
                    } catch (IOException e) {
                        Log.d(TAG, "Input stream was disconnected", e);
                        break;
                    }
                }
                   /* }
                }, 50);*/



            }
            // Share the sent message with the UI activity.
        } catch (IOException e) {

            Log.e(TAG, "Error occurred when sending data", e);

            // Send a failure message back to the activity.
        }
    }

    final void notifyTrace(final TraceEventArgs arg) {
        if (this.mActivity != null) {
            this.mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    Iterator var1 = GXBlutooth.this.mMediaListeners.iterator();

                    while(var1.hasNext()) {
                        IGXMediaListener listener = (IGXMediaListener)var1.next();
                        listener.onTrace(this, arg);
                    }

                }
            });
        } else {
            Iterator var2 = this.mMediaListeners.iterator();

            while(var2.hasNext()) {
                IGXMediaListener listener = (IGXMediaListener)var2.next();
                listener.onTrace(this, arg);
            }
        }

    }
    @Override
    public String getMediaType() {
        return "Serial";
    }

    @Override
    public String getSettings() {
        StringBuilder sb = new StringBuilder();
        String nl = System.getProperty("line.separator");
        if (this.mPort != null) {
            sb.append("<Port>");
            sb.append(this.mPort.getPort());
            sb.append("</Port>");
            sb.append(nl);
        }

        if (this.mBaudRate != BaudRate.BAUD_RATE_9600) {
            sb.append("<BaudRate>");
            sb.append(String.valueOf(this.mBaudRate.getValue()));
            sb.append("</BaudRate>");
            sb.append(nl);
        }

        if (this.mStopBits != StopBits.ONE) {
            sb.append("<StopBits>");
            sb.append(String.valueOf(this.mStopBits.ordinal()));
            sb.append("</StopBits>");
            sb.append(nl);
        }

        if (this.mParity != Parity.NONE) {
            sb.append("<Parity>");
            sb.append(String.valueOf(this.mParity.ordinal()));
            sb.append("</Parity>");
            sb.append(nl);
        }

        if (this.mDataBits != 8) {
            sb.append("<DataBits>");
            sb.append(String.valueOf(this.mDataBits));
            sb.append("</DataBits>");
            sb.append(nl);
        }

        return sb.toString();
    }

    @Override
    public void setSettings(String value) {
        this.mPort = null;
        this.mBaudRate = BaudRate.BAUD_RATE_9600;
        this.mStopBits = StopBits.ONE;
        this.mParity = Parity.NONE;
        this.mDataBits = 8;
        if (value != null && !value.isEmpty()) {
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(new StringReader(value));

                while(true) {
                    while(true) {
                        int event;
                        do {
                            if ((event = parser.next()) == 3 || event == 1) {
                                return;
                            }
                        } while(event != 2);

                        String target = parser.getName();
                        boolean found = false;
                        if ("Port".equalsIgnoreCase(target)) {
                            String name = readText(parser);
                            GXPort[] var7 = this.getPorts();
                            int var8 = var7.length;

                            for(int var9 = 0; var9 < var8; ++var9) {
                                GXPort it = var7[var9];
                                if (name.equalsIgnoreCase(it.getPort())) {
                                    this.setPort(it);
                                    found = true;
                                    break;
                                }
                            }

                            if (!found) {
                                this.setPort((GXPort)null);
                            }
                        } else if ("BaudRate".equalsIgnoreCase(target)) {
                            this.setBaudRate(BaudRate.forValue(Integer.parseInt(readText(parser))));
                        } else if ("StopBits".equalsIgnoreCase(target)) {
                            this.setStopBits(StopBits.values()[Integer.parseInt(readText(parser))]);
                        } else if ("Parity".equalsIgnoreCase(target)) {
                            this.setParity(Parity.values()[Integer.parseInt(readText(parser))]);
                        } else if ("DataBits".equalsIgnoreCase(target)) {
                            this.setDataBits(Integer.parseInt(readText(parser)));
                        }
                    }
                }
            } catch (Exception var11) {
                throw new RuntimeException(var11.getMessage());
            }
        }
    }

    private static String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == 4) {
            result = parser.getText();
            parser.nextTag();
        }

        return result;
    }

    @Override
    public Object getSynchronous() {
        synchronized(this) {
            int[] tmp = new int[]{this.mSynchronous};
            GXSync obj = new GXSync(tmp);
            this.mSynchronous = tmp[0];
            return obj;
        }
    }

    @Override
    public boolean getIsSynchronous() {
        synchronized(this) {
            return this.mSynchronous != 0;
        }
    }

    @Override
    public <T> boolean receive(ReceiveParameters<T> args) {
        return this.mSyncBase.receive(args);
    }

    @Override
    public void resetSynchronousBuffer() {
        synchronized(this.mSyncBase.getSync()) {
            this.mSyncBase.resetReceivedSize();
        }
    }

    final GXSynchronousMediaBase getSyncBase() {
        return this.mSyncBase;
    }


    @Override
    public long getBytesSent() {
        return this.mBytesSend;
    }

    @Override
    public long getBytesReceived() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resetByteCounters() {
        this.mBytesSend = 0L;
        throw new UnsupportedOperationException();
    }

    @Override
    public void validate() {
        if (this.getPort() == null) {
            throw new RuntimeException("Invalid port name.");
        }
    }

    @Override
    public Object getEop() {
        return this.mEop;
    }

    @Override
    public void setEop(Object value) {
        this.mEop = value;
    }

    @Override
    public int getConfigurableSettings() {
        return this.mConfigurableSettings;
    }

    @Override
    public void setConfigurableSettings(int value) {
        this.mConfigurableSettings = value;
    }

    @Override
    public boolean properties(Activity activity) {
        //GXPropertiesBase.setSerial(this);
        Intent intent = new Intent(activity, GXProperties.class);
        activity.startActivity(intent);
        return true;
    }

    @Override
    public Fragment properties() {
        //GXPropertiesBase.setSerial(this);
        //return new GXPropertiesFragment();
        return new Fragment();
    }

    public final GXPort getPort() {
        return this.mPort;
    }

    public final void setPort(GXPort value) {
        boolean change = value != this.mPort;
        this.mPort = value;
        if (change) {
            this.notifyPropertyChanged("PortName");
            if (value != null && value.getVendorId() != 0 && value.getProductId() != 0) {
                //this.mChipset = getChipSet((String)null, value.getVendorId(), value.getProductId());
            }
        }

    }

    private void notifyPropertyChanged(final String info) {
        if (this.mActivity != null) {
            this.mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    Iterator var1 = GXBlutooth.this.mMediaListeners.iterator();

                    while(var1.hasNext()) {
                        IGXMediaListener listener = (IGXMediaListener)var1.next();
                        listener.onPropertyChanged(this, new PropertyChangedEventArgs(info));
                    }

                }
            });
        } else {
            Iterator var2 = this.mMediaListeners.iterator();

            while(var2.hasNext()) {
                IGXMediaListener listener = (IGXMediaListener)var2.next();
                listener.onPropertyChanged(this, new PropertyChangedEventArgs(info));
            }
        }

    }

    public final BaudRate getBaudRate() {
        return this.mBaudRate;
    }

    public final void setBaudRate(BaudRate value) {
        boolean change = this.getBaudRate() != value;
        if (change) {
            this.mBaudRate = value;
            this.notifyPropertyChanged("BaudRate");
        }

    }


    public final StopBits getStopBits() {
        return this.mStopBits;
    }

    public final void setStopBits(StopBits value) {
        boolean change = this.getStopBits() != value;
        if (change) {
            this.mStopBits = value;
            this.notifyPropertyChanged("StopBits");
        }

    }

    public final Parity getParity() {
        return this.mParity;
    }

    public final void setParity(Parity value) {
        boolean change = this.getParity() != value;
        if (change) {
            this.mParity = value;
            this.notifyPropertyChanged("Parity");
        }

    }

    public final int getDataBits() {
        return this.mDataBits;
    }

    public final void setDataBits(int value) {
        boolean change = this.getDataBits() != value;
        if (change) {
            this.mDataBits = value;
            this.notifyPropertyChanged("DataBits");
        }

    }

    public final int getWriteTimeout() {
        return this.mWriteTimeout;
    }

    public final void setWriteTimeout(int value) {
        boolean change = this.mWriteTimeout != value;
        if (change) {
            this.mWriteTimeout = value;
            this.notifyPropertyChanged("WriteTimeout");
        }

    }

    public final int getReadTimeout() {
        return this.mReadTimeout;
    }

    public final void setReadTimeout(int value) {
        boolean change = this.mReadTimeout != value;
        this.mReadTimeout = value;
        if (change) {
            this.notifyPropertyChanged("ReadTimeout");
        }

    }

    public GXPort[] getPorts() {
        Class var1 = GXPort.class;
        synchronized(GXPort.class) {
            if (mPorts == null) {
                //String name = "gurux.serial";
                //IntentFilter filter2 = new IntentFilter(name);
                //filter2.addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED");
                //filter2.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
                //this.mContext.registerReceiver(this.mUsbReciever, filter2);
                mPorts = new ArrayList();
               /* UsbManager manager = (UsbManager)this.mContext.getSystemService("usb");
                Map<String, UsbDevice> devices = manager.getDeviceList();
                Iterator var6 = devices.entrySet().iterator();

                while(var6.hasNext()) {
                    Map.Entry<String, UsbDevice> it = (Map.Entry)var6.next();
                    this.addPort(manager, (UsbDevice)it.getValue(), false);
                }*/

                GXPort port = new GXPort();
                port.setPort("");
                port.setVendorId(0);
                port.setProductId(0);
                port.setVendor("");
                port.setProduct("");
                port.setSerial("");
                port.setRawDescriptors(null);
                port.setManufacturer("");
                port.setChipset(null);


                Class var29 = GXPort.class;
                synchronized(GXPort.class) {
                    mPorts.add(port);
                }


            }
        }

        return (GXPort[])mPorts.toArray(new GXPort[mPorts.size()]);
    }




    class ConnectThread extends Thread {
        private  final String TAG = ConnectThread.class.getSimpleName();
        private  final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private BluetoothAdapter mBluetoothAdapter;
        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            if(mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            manageMyConnectedSocket(mmSocket);
            //          mDoneInterface.doneConnecting(mmDevice.getName());

/*
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                public void run() {
                    Toast.makeText(MainActivity.this.getApplicationContext(), "Connecting to " + mmDevice.getName(),
                            Toast.LENGTH_LONG).show();

                    mTextView.setText("Connecting to "+ mmDevice.getName());
                }
            });

 */
        }

        private void manageMyConnectedSocket(BluetoothSocket mmSocket) {
            ConnectedThread connectedThread = new ConnectedThread(mmSocket);
            connectedThread.start();

        }


        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    class ConnectedThread extends Thread {
        private  final String TAG = ConnectedThread.class.getSimpleName();
        //private final BluetoothSocket mmSocket;
        //private final InputStream mmInStream;
        //private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream


        public ConnectedThread(BluetoothSocket socket) {

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;

            //mDoneInterface.doneConnected(mmSocket.getRemoteDevice().getName());

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                public void run() {
                    Toast.makeText(GXBlutooth.this.mContext, "Connected to " + mmSocket.getRemoteDevice().getName(),
                            Toast.LENGTH_LONG).show();


                }
            });
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);
                    // Send the obtained bytes to the UI activity.
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);

                // Share the sent message with the UI activity.
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }


    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }
    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }


}
