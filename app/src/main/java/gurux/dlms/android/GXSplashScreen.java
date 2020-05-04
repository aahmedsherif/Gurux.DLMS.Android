//
// --------------------------------------------------------------------------
//  Gurux Ltd
//
//
//
// Filename:        $HeadURL$
//
// Version:         $Revision$,
//                  $Date$
//                  $Author$
//
// Copyright (c) Gurux Ltd
//
//---------------------------------------------------------------------------
//
//  DESCRIPTION
//
// This file is a part of Gurux Device Framework.
//
// Gurux Device Framework is Open Source software; you can redistribute it
// and/or modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; version 2 of the License.
// Gurux Device Framework is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
// See the GNU General Public License for more details.
//
// More information of Gurux products: http://www.gurux.org
//
// This code is licensed under the GNU General Public License v2.
// Full text may be retrieved at http://www.gnu.org/licenses/gpl-2.0.txt
//---------------------------------------------------------------------------

package gurux.dlms.android;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import gurux.dlms.GXDLMSConverter;
import gurux.dlms.manufacturersettings.GXManufacturerCollection;

/**
 * Show splashscreen and load necessary content.
 */
public class GXSplashScreen extends Activity implements IGXTaskCallback {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        /**
         * Showing splashscreen while downloading necessary data before launching the app.
         */
        GXTask m = new GXTask(this, Task.DOWNLOAD);
        m.execute();
    }

    @Override
    public void onExecute(final GXTask sender) {

//check for permission

       /* //Read OBIS codes.
        if (GXDLMSConverter.isFirstRun(this)) {
            GXDLMSConverter c = new GXDLMSConverter();
            c.update(this);
        }
        //Read Manufacturer settings.
        if (GXManufacturerCollection.isFirstRun(this) ||
                GXManufacturerCollection.isUpdatesAvailable(this)) {
            GXManufacturerCollection.updateManufactureSettings(this);
        }*/
    }

    /**
     * Start main activity and close splashscreen.
     */
    @Override
    public void onFinish(GXTask sender, Object result) {
        Intent i = new Intent(GXSplashScreen.this, MainActivity.class);
        startActivity(i);
        finish();
    }

    /**
     * Show occurred error.
     */
    @Override
    public void onError(GXTask sender, Exception e) {
        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        finish();
    }
}
