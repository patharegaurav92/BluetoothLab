package edu.student.android.bluetoothlab;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import static android.R.attr.data;
import static edu.student.android.bluetoothlab.R.id.send;
import static edu.student.android.bluetoothlab.R.id.turnOffWifi;
import static edu.student.android.bluetoothlab.R.id.turnOnWifi;

public class MainActivity extends AppCompatActivity {
    private Button connectBluetooth,sendMessage,sendAnImage,sendARecording,connectWifi;
    private TextView status,wifiStatus;
    private boolean isWifiP2pEnabled = false;
    private String device_name;
    private static final int REQUEST_CONNECT_DEVICE= 1;
    private static final int REQUEST_CONNECT_DEVICE_WIFI= 105;
    private BluetoothAdapter myBluetoothAdapter;
    private static final String TAG = "MainActivity";
    private String whichConnection=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_ui);

        BluetoothManager manager =(BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        myBluetoothAdapter = manager.getAdapter();
        connectWifi = (Button)findViewById(R.id.wifi_connect);
        connectBluetooth = (Button) findViewById(R.id.bluetooth_connect);
        status = (TextView) findViewById(R.id.textView);
        wifiStatus =(TextView) findViewById(R.id.textView2);
        sendMessage = (Button) findViewById(R.id.sendMessage) ;
        sendMessage.setVisibility(View.GONE);
        sendAnImage = (Button) findViewById(R.id.sendAnImage);
        sendARecording =(Button) findViewById(R.id.sendARecording);

        if(Data.isWifiEnabled){
            sendMessage.setVisibility(View.VISIBLE);
            wifiStatus.setTextColor(Color.parseColor("#19aa23"));
            wifiStatus.setText("Connected" );
            sendARecording.setVisibility(View.VISIBLE);
            sendAnImage.setVisibility(View.VISIBLE);
        }else if(Data.isBluetoothEnabled){
            sendMessage.setVisibility(View.VISIBLE);
            status.setTextColor(Color.parseColor("#19aa23"));
            status.setText("Connected to: "+SocketAndDevice.device.getName());
            sendARecording.setVisibility(View.VISIBLE);
            sendAnImage.setVisibility(View.VISIBLE);
        }

        sendARecording.setVisibility(View.GONE);
        sendAnImage.setVisibility(View.GONE);
        if(myBluetoothAdapter.isEnabled()){
            status.setTextColor(Color.parseColor("#19aa23"));
            status.setText("Bluetooth Connected");
        }else{
            status.setTextColor(Color.parseColor("#f44141"));
            status.setText("Bluetooth Disconnected");
        }

        connectWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this,WifiDirectActivity.class);
                startActivityForResult(i,REQUEST_CONNECT_DEVICE_WIFI);
            }
        });
        connectBluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this,BluetoothChat.class);
                startActivityForResult(i,REQUEST_CONNECT_DEVICE);
            }
        });
        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Data.isBluetoothEnabled) {
                    Intent i = new Intent(MainActivity.this, SendAMessage.class);
                    startActivity(i);
                }else{
                    Intent i = new Intent(MainActivity.this, SendAMessageWifi.class);
                    startActivity(i);
                }
            }
        });
        sendAnImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Data.isBluetoothEnabled) {
                    Intent i = new Intent(MainActivity.this,SendAnImage.class);

                    startActivity(i);
                }else{
                    Intent i = new Intent(MainActivity.this,SendAnImageWifi.class);

                    startActivity(i);
                }

            }
        });
        sendARecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Data.isBluetoothEnabled) {
                Intent i = new Intent(MainActivity.this,SendARecording.class);
                startActivity(i);
                }else{
                    Intent i = new Intent(MainActivity.this,SendARecordingWifi.class);

                    startActivity(i);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                Data.isWifiEnabled = false;
                Data.isBluetoothEnabled = true;
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    sendMessage.setVisibility(View.VISIBLE);

                    sendARecording.setVisibility(View.VISIBLE);
                    sendAnImage.setVisibility(View.VISIBLE);
                    status.setTextColor(Color.parseColor("#19aa23"));
                    status.setText("Connected to: "+SocketAndDevice.device.getName());
                    whichConnection = "bluetooth";

                }
                break;
            case REQUEST_CONNECT_DEVICE_WIFI:
                Data.isWifiEnabled = true;
                Data.isBluetoothEnabled = false;
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    whichConnection = "wifi";
                    sendMessage.setVisibility(View.VISIBLE);

                    sendARecording.setVisibility(View.VISIBLE);
                    sendAnImage.setVisibility(View.VISIBLE);
                    wifiStatus.setTextColor(Color.parseColor("#19aa23"));
                     //String state = data.getExtras().get("stateofclient").toString();
                        wifiStatus.setText("Connected" );


                }
                break;
        }
    }

}
