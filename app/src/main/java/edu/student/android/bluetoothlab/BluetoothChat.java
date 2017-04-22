package edu.student.android.bluetoothlab;
import android.app.Activity;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import static edu.student.android.bluetoothlab.SocketAndDevice.socket;

/**
 * Created by Gaurav on 13-04-2017.
 */

public class BluetoothChat extends AppCompatActivity  {
    public static final UUID MY_UUID=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String TAG ="BluetoothChat";
    private static final int REQUEST_ENABLE_BT = 1;
    private Button onBtn;
    private Button offBtn;
    private Button listBtn;
    private Button findBtn;
    private TextView text;
    private BluetoothAdapter myBluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;
    private HashSet<BluetoothDevice> devicesFoundArray;
    private ListView myListView;
    private ArrayAdapter<String> listAdapter;
    private static final String NAME = "BluetoothChat";
    private AcceptThread mSecureAcceptThread;
    private ConnectThread mConnectThread;
    private int bt_enabled;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    private int mState;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.e(TAG,"onCreate");
        // take an instance of BluetoothAdapter - Bluetooth radio
        BluetoothManager manager =(BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        myBluetoothAdapter = manager.getAdapter();
        devicesFoundArray = new HashSet<BluetoothDevice>();
        if(myBluetoothAdapter == null) {
            onBtn.setEnabled(false);
            offBtn.setEnabled(false);
            listBtn.setEnabled(false);
            findBtn.setEnabled(false);
            text.setText("Status: not supported");

            Toast.makeText(getApplicationContext(),"Your device does not support Bluetooth",
                    Toast.LENGTH_LONG).show();
        } else {
            text = (TextView) findViewById(R.id.text);
            onBtn = (Button)findViewById(R.id.turnOn);
            onBtn.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    Log.e(TAG,"on button");
                    on();
                }
            });

            offBtn = (Button)findViewById(R.id.turnOff);
            offBtn.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    off(v);
                }
            });

            listBtn = (Button)findViewById(R.id.paired);
            listBtn.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    list(v);
                }
            });

            findBtn = (Button)findViewById(R.id.search);
            findBtn.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    find(v);
                }
            });

            myListView = (ListView)findViewById(R.id.listView1);

            // create the arrayAdapter that contains the BTDevices, and set it to the ListView
            listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
            myListView.setAdapter(listAdapter);
            myListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String info = ((TextView) view).getText().toString();
                    String address = info.substring(info.length() - 17);

                    BluetoothDevice selectedDevice = myBluetoothAdapter.getRemoteDevice(address);
                    Toast.makeText(BluetoothChat.this,address, Toast.LENGTH_SHORT).show();

                    connect(selectedDevice);

                }
            });
            registerReceiver(bReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            if(myBluetoothAdapter.isEnabled()){
                text.setText("Status: Enabled");
                start();
            }
            else{
                on();
            }
        }
    }
    public synchronized void connect(BluetoothDevice device){
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        Log.v(TAG,"connect Method");
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
    }
    public synchronized void start(){

            mSecureAcceptThread = new AcceptThread();
            mSecureAcceptThread.start();
    }

    public synchronized void connected(BluetoothSocket socket,BluetoothDevice device){
        Log.d(TAG, "connected");

            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        if(mSecureAcceptThread !=null){
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

       /*mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();*/
        Intent go = new Intent(BluetoothChat.this,MainActivity.class);
        SocketAndDevice.device = device;
        SocketAndDevice.socket = socket;
        setResult(Activity.RESULT_OK,go);

        Log.v(TAG,"GO BACK");
        finish();
    }

    public void on(){
        if (!myBluetoothAdapter.isEnabled()) {
            Log.e(TAG,"Bluetooth is not enabled");
            Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOnIntent, REQUEST_ENABLE_BT);

            Toast.makeText(getApplicationContext(),"Bluetooth turned on" ,
                    Toast.LENGTH_LONG).show();
            Log.e(TAG,"Bluetooth is enabled");

        }
        else{
            Log.e(TAG,"Bluetooth is enabled");
            Toast.makeText(getApplicationContext(),"Bluetooth is already on",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        if(requestCode == REQUEST_ENABLE_BT){
            if(myBluetoothAdapter.isEnabled()) {
                text.setText("Status: Enabled");
                start();
            } else {
                text.setText("Status: Disabled");
            }
        }
    }

    public void list(View view){
        listAdapter.clear();
        Log.e(TAG,"List Method");
        registerReceiver(bReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        pairedDevices = myBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size()>0) {
            for (BluetoothDevice device : pairedDevices) {
                Log.e(TAG, "found paired of devices " + device.getName() + "\n" + device.getAddress());
                listAdapter.add(device.getName() + "(Paired) \n" + device.getAddress());
                listAdapter.notifyDataSetChanged();
            }
        }else {
            String noDevice ="No Device found".toString();
            listAdapter.add(noDevice);
        }

    }

    BroadcastReceiver bReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG,"OnReceive Method");
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.e(TAG,"Search Devices found");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                devicesFoundArray.add(device);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    Log.e(TAG, "devices " + device.getName() + "\n" + device.getAddress());
                    listAdapter.add(device.getName() + "\n" + device.getAddress());
                    listAdapter.notifyDataSetChanged();
                }

            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                if(listAdapter.getCount() == 0){
                    String noDevice ="No Device found".toString();
                    listAdapter.add(noDevice);
                }
            }
        }
    };



    public void find(View view) {
        Log.e(TAG,"find method");
        if (myBluetoothAdapter.isDiscovering()) {
            Log.e(TAG,"Bluetooth is still discovering");
            // the button is pressed when it discovers, so cancel the discovery
            myBluetoothAdapter.cancelDiscovery();
            Log.e(TAG,"discovery cancelled");
        }
        else {
            Toast.makeText(this, "Searching", Toast.LENGTH_SHORT).show();
            listAdapter.clear();
            checkBTPermission();
            myBluetoothAdapter.startDiscovery();

            registerReceiver(bReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            if(!myBluetoothAdapter.isDiscovering()){
                checkBTPermission();
                myBluetoothAdapter.startDiscovery();
                registerReceiver(bReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
                Log.e(TAG,"Listener set");
            }
        }
    }

    private void checkBTPermission() {

        int permissionCheck = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck+= this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");

        }
           if(permissionCheck!=0){
               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
               this.requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},1001);
           }
       }else{
            Log.e("LOG","Helloo");
        }
    }

    public void off(View view){
        Log.e(TAG,"off method");
        myBluetoothAdapter.disable();
        text.setText("Status: Disconnected");

        Toast.makeText(getApplicationContext(),"Bluetooth turned off",
                Toast.LENGTH_LONG).show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bReceiver);
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            Log.v(TAG,"in ConnectThread Constructor");
            BluetoothSocket tmp = null;
            mmDevice = device;
            mState = STATE_CONNECTING;

            try {
                Log.v(TAG,"Creating..");
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                Log.v(TAG,"Socket created");
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            myBluetoothAdapter.cancelDiscovery();

            try {
                Log.v(TAG,"Run Method");
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                Log.v(TAG,"Waiting for connection");
                mmSocket.connect();
                Log.v(TAG,"Connection done");
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    Log.e(TAG, "Closing socket");
                    mmSocket.close();
                    Log.e(TAG,  "socket Closed");
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }
            //Toast.makeText(BluetoothChat.this,mmDevice.getName()+" is connected", Toast.LENGTH_SHORT).show();
            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connected(mmSocket, mmDevice);
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
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                Log.v(TAG,"Socket Listening");
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = myBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
                Log.v(TAG,"Socket recieved");
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
            mState = STATE_LISTEN;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (mState!=STATE_CONNECTED) {
                try {
                    Log.v(TAG,"Accepting..");
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    Log.v(TAG,"Connection Accepted");
                    connected(socket, socket.getRemoteDevice());
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
                Log.v(TAG,"Socket closed1");
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }




}
