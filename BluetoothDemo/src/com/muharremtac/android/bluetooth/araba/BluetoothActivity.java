/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.muharremtac.android.bluetooth.araba;

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

/**
 * This is the main Activity that displays the current chat session.
 */
public class BluetoothActivity extends Activity {

	private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
	
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private ListView mConversationView;
    private Button voiceCommandButton;

    private String mConnectedDeviceName = null;
    private ArrayAdapter<String> mConversationArrayAdapter;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothService mCarService = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    private final void setStatus(int resId) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(resId);
    }

    private final void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(subTitle);
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            if (mCarService == null) setupCar();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (mCarService != null) {
            if (mCarService.getState() == BluetoothService.STATE_NONE) {
              mCarService.start();
            }
        }
    }
 
    private void setupCar() {
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mConversationView = (ListView) findViewById(R.id.listView);
        mConversationView.setAdapter(mConversationArrayAdapter);

        voiceCommandButton = (Button) findViewById(R.id.voiceCommandButton);
        voiceCommandButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	 startVoiceRecognitionActivity();
            }
        });
        
        mCarService = new BluetoothService(this, mHandler);

    }

    @Override
    public synchronized void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCarService != null) mCarService.stop();
    }

    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }
    
  private void sendData(byte[] send){
	  if (mCarService.getState() != BluetoothService.STATE_CONNECTED) {
          Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
          return;
      }
	  
	  
	  if(send.length>0)
		  mCarService.write(send);
  }
 
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BluetoothService.STATE_CONNECTED:
                	setStatus(getString(R.string.title_connected_to));
                    voiceCommandButton.setVisibility(View.VISIBLE);
                    setStatus(mConnectedDeviceName);
                    mConversationArrayAdapter.clear();
                    break;
                case BluetoothService.STATE_CONNECTING:
                	setStatus(R.string.title_connecting);
                    break;
                case BluetoothService.STATE_LISTEN:
                case BluetoothService.STATE_NONE:
                	setStatus(R.string.title_not_connected);
                    break;
                } 
                break;
           
            case MESSAGE_READ:
                String s = msg.what+"";
				if("".equals(s)){
					sendData("0".getBytes());
				}
				if("50".equals(s)){
					sendData("c".getBytes());
				}else if("60".equals(s)){
					sendData("a".getBytes());
				}else if("70".equals(s)){
					sendData("b".getBytes());
				}else if("100".equals(s)){
					sendData("d".getBytes());
				}else{
					sendData(s.getBytes());
				}
                
                break;
            case MESSAGE_DEVICE_NAME:
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    
    private void startVoiceRecognitionActivity() {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass().getPackage().getName());
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT,  getString(R.string.konusma_algilama));
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR");
		intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
		startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
	}
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            if (resultCode == Activity.RESULT_OK) {
                String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                mCarService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            if (resultCode == Activity.RESULT_OK) {
                setupCar();
            } else {
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {

			ArrayList<String> matchList = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			
			for (String string : matchList) {
				if("ileri".equals(string)){
					sendData("3".getBytes());
				}
				if("ileri dur".equals(string)){
					sendData("3".getBytes());
					new Thread(new Runnable() {
					    public void run() {
					        try {
					            Thread.sleep(1000);
								sendData("d".getBytes());
					        } catch (InterruptedException e) {
					            e.printStackTrace();
					        }
					    }
					}).start();
				}
				if("geri".equals(string)){
					sendData("1".getBytes());
				}
				if("geri dur".equals(string)){
					sendData("1".getBytes());
					new Thread(new Runnable() {
					    public void run() {
					        try {
					            Thread.sleep(1000);
								sendData("d".getBytes());
					        } catch (InterruptedException e) {
					            e.printStackTrace();
					        }
					    }
					}).start();
				}
				if("dur".equals(string) || "duy".equals(string)){
					sendData("d".getBytes());
				}
				if("sola".equals(string)){
					sendData("b".getBytes());
				}
				if("sola ileri".equals(string)){
					sendData("b".getBytes());
					sendData("3".getBytes());
				}
				if("sağa".equals(string)){
					sendData("c".getBytes());
				}
				if("sağa ileri".equals(string)){
					sendData("c".getBytes());
					sendData("3".getBytes());
				}
			}
		 }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.scan:
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
        }
        return false;
    }

}