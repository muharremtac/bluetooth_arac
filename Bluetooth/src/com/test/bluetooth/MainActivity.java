package com.test.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnItemClickListener {

	ArrayAdapter<String> listAdapter;
	ListView listView;
	TextView accelerometerText;
	BluetoothAdapter btAdapter;
	Set<BluetoothDevice> devicesArray;
	ArrayList<String> pairedDevices;
	ArrayList<BluetoothDevice> devices;
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	protected static final int SUCCESS_CONNECT = 0;
	protected static final int MESSAGE_READ = 1;
	IntentFilter filter;
	BroadcastReceiver receiver;
	String tag = "debugging";
	
	private BluetoothSocket mmSocket;
    private BluetoothDevice mmDevice;
    
	private SensorManager sensorManager;
	private Sensor accelerometer;
	private static final byte MOTOR_ID_1=0x1;
	
	Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			Log.i(tag, "in handler");
			super.handleMessage(msg);
				ConnectedThread connectedThread = new ConnectedThread((BluetoothSocket)msg.obj);
				Toast.makeText(getApplicationContext(), "CONNECT", 0).show();
				String s = msg.what+"";
				if("50".equals(s)){
					connectedThread.write("c".getBytes());
				}else if("60".equals(s)){
					connectedThread.write("a".getBytes());
				}else if("70".equals(s)){
					connectedThread.write("b".getBytes());
				}else{
					connectedThread.write(s.getBytes());
				}
		}
	};
 
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        init();
        if(btAdapter==null){
        	Toast.makeText(getApplicationContext(), "No bluetooth detected", 0).show();
        	finish();
        }
        else{
        	if(!btAdapter.isEnabled()){
        		turnOnBT();
        	}
        	getPairedDevices();
        	startDiscovery();
        }
    }

	private void startDiscovery() {
		btAdapter.cancelDiscovery();
		btAdapter.startDiscovery();
		
	}

	private void turnOnBT() {
		Intent intent =new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivityForResult(intent, 1);
	}

	private void getPairedDevices() {
		devicesArray = btAdapter.getBondedDevices();
		if(devicesArray.size()>0){
			for(BluetoothDevice device:devicesArray){
				pairedDevices.add(device.getName());
			}
		}
	}
	
	private void init() {
		listView=(ListView)findViewById(R.id.listView);
		accelerometerText=(TextView)findViewById(R.id.accelerometerText);
		listView.setOnItemClickListener(this);
		listAdapter= new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,0);
		listView.setAdapter(listAdapter);
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		pairedDevices = new ArrayList<String>();
		filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		devices = new ArrayList<BluetoothDevice>();
		receiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				// TODO Auto-generated method stub
				String action = intent.getAction();
				
				if(BluetoothDevice.ACTION_FOUND.equals(action)){
					BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					devices.add(device);
					String s = "";
					for(int a = 0; a < pairedDevices.size(); a++){
						if(device.getName().equals(pairedDevices.get(a))){
							s = "(Paired)";
							break;
						}
					}
			
					listAdapter.add(device.getName()+" "+s+" "+"\n"+device.getAddress());
				}
				
				else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){

				}
				else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){

				}
				else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
					if(btAdapter.getState() == btAdapter.STATE_OFF){
						turnOnBT();
					}
				}
		  
			}
		};
		
		registerReceiver(receiver, filter);
		 filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		registerReceiver(receiver, filter);
		 filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(receiver, filter);
		 filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(receiver, filter);
		
        if(sensorManager==null){
        	sensorManager=(SensorManager) getSystemService(SENSOR_SERVICE);
        }
        if(accelerometer==null){
        	accelerometer=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

	}
			
	@Override
	protected void onPause() {
		unregisterReceiver(receiver);
		sensorManager.unregisterListener(sensorEventListener);
		super.onPause();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode == RESULT_CANCELED){
			Toast.makeText(getApplicationContext(), "Bluetooth must be enabled to continue", Toast.LENGTH_SHORT).show();
			finish();
		}
	}
		
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
	
		if(btAdapter.isDiscovering()){
			btAdapter.cancelDiscovery();
		}
		if(listAdapter.getItem(arg2).contains("Paired")){
	
			BluetoothDevice selectedDevice = devices.get(arg2);
			ConnectThread connect = new ConnectThread(selectedDevice);
			connect.start();
			Log.i(tag, "in click listener");
		}
		else{
			Toast.makeText(getApplicationContext(), "device is not paired", 0).show();
		}
	}
		
	private class ConnectThread extends Thread {
	    public ConnectThread(BluetoothDevice device) {
	        BluetoothSocket tmp = null;
	        mmDevice = device;
	        Log.i(tag, "construct");
	        try {
	            tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
	        } catch (IOException e) { 
	        	Log.i(tag, "get socket failed");
	        	
	        }
	        mmSocket = tmp;
	    }
	 
	    public void run() {
	        btAdapter.cancelDiscovery();
	        Log.i(tag, "connect - run");
	        try {
	            mmSocket.connect();
	            Log.i(tag, "connect - succeeded");
	        } catch (IOException connectException) {	Log.i(tag, "connect failed");
	            try {
	                mmSocket.close();
	            } catch (IOException closeException) { }
	            return;
	        }
	        sensorManager.registerListener(sensorEventListener, accelerometer,SensorManager.SENSOR_DELAY_GAME);
	    }

	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}

	private class ConnectedThread extends Thread {
	    private final InputStream mmInStream;
	    private final OutputStream mmOutStream;
	 
	    public ConnectedThread(BluetoothSocket socket) {
	        mmSocket = socket;
	        InputStream tmpIn = null;
	        OutputStream tmpOut = null;
	 
	        try {
	            tmpIn = socket.getInputStream();
	            tmpOut = socket.getOutputStream();
	        } catch (IOException e) { }
	 
	        mmInStream = tmpIn;
	        mmOutStream = tmpOut;
	    }
	 
	    public void run() {
	        byte[] buffer;
	        int bytes;
	
	        while (true) {
	            try {
	            	buffer = new byte[1024];
	                bytes = mmInStream.read(buffer);
	                mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
	               
	            } catch (IOException e) {
	                break;
	            }
	        }
	    }
	 
	    public void write(byte[] bytes) {
	        try {
	            mmOutStream.write(bytes);
	        } catch (IOException e) { }
	    }
	 
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}

	private final SensorEventListener sensorEventListener = new SensorEventListener() {
		
		
		public void onSensorChanged(SensorEvent event) {
			synchronized (this) {
	            if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER)
	
	            {
	               int xAcceleration=(int) (-event.values[0] * 10);
	               int yAcceleration=(int) (-event.values[1] * 10);
	               int zAcceleration=(int) (-event.values[2] * 10);
	            	
	               accelerometerText.setText("X: "+ xAcceleration + " Y: "+ yAcceleration+ " Z: " + zAcceleration);
	               if(xAcceleration<=-20){
	            	   mHandler.obtainMessage(1, mmSocket).sendToTarget();
	               }else if(xAcceleration>=30){
	            	   mHandler.obtainMessage(5, mmSocket).sendToTarget();
	               }else if(xAcceleration>=20){
	            	   mHandler.obtainMessage(4, mmSocket).sendToTarget();
	               }else if(xAcceleration<=10){
	            	   mHandler.obtainMessage(0, mmSocket).sendToTarget();
	               }else if(yAcceleration<=-20){
	            	   mHandler.obtainMessage(50, mmSocket).sendToTarget();
	               }else if(yAcceleration<=10){
	            	   mHandler.obtainMessage(60, mmSocket).sendToTarget();
	               }else if(yAcceleration>=20){
	            	   mHandler.obtainMessage(70, mmSocket).sendToTarget();
	               }
	            }
	        }
		}
		
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			
		}
	};
			
}
