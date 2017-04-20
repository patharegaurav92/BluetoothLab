package edu.student.android.bluetoothlab;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.nfc.Tag;
import android.os.Bundle;
import android.provider.Settings;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

import static edu.student.android.bluetoothlab.R.string.on;


public class WifiDirectActivity extends AppCompatActivity {
    private WifiP2pDevice device;
    public static final String TAG = "WifiDirectActivity";
     List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager manager;
    private BroadcastReceiver receiver = null;
    private WifiP2pManager.Channel channel;
    private boolean isWifiP2pEnabled = false;
    private Button turnOnWifi,turnOffWifi,discoverpeers,cancelConnect;
    private TextView status;
    private ListView peerListView;
    ProgressDialog progressDialog = null;
    WifiP2pManager.PeerListListener myPeerListListener;
    private WiFiPeerListAdapter wifiPeerListAdapter;

    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }
    @Override
    protected void onResume() {
        super.onResume();
        if(isWifiP2pEnabled){
            status.setText("Status:- Enabled");
            turnOnWifi.setVisibility(View.GONE);
            turnOffWifi.setVisibility(View.VISIBLE);
        }else{
            status.setText("Status:- Disabled");
            turnOnWifi.setVisibility(View.VISIBLE);
            turnOffWifi.setVisibility(View.GONE);
        }
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    public void resetData() {
        //removing all the listviews.
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_direct);
        turnOnWifi = (Button) findViewById(R.id.turnOnWifi);
        turnOffWifi =(Button) findViewById(R.id.turnOffWifi);
        discoverpeers= (Button) findViewById(R.id.discover_peers);
        cancelConnect = (Button) findViewById(R.id.cancel_connection);
        status = (TextView) findViewById(R.id.text1);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        peerListView = (ListView) findViewById(R.id.peerListView);


        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        turnOnWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (manager != null && channel != null && !isWifiP2pEnabled) {
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                } else {
                    Log.e(TAG, "channel or manager is null");
                }
            }
        });
        turnOffWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (manager != null && channel != null && isWifiP2pEnabled) {
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                } else {
                    Log.e(TAG, "channel or manager is null");
                }
            }
        });
        discoverpeers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                discover();

            }
        });
        wifiPeerListAdapter = new WiFiPeerListAdapter(this,R.layout.row_devices,peers);
        peerListView.setAdapter(wifiPeerListAdapter);
        peerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                device = wifiPeerListAdapter.getItem(position);
                Log.v(TAG,device.deviceName);
                if(device.status == WifiP2pDevice.AVAILABLE) {
                    connect(device);

                }else if(device.status == WifiP2pDevice.CONNECTED || device.status == WifiP2pDevice.INVITED){
                    boolean isGroupOwner = device.isGroupOwner();
                    Toast.makeText(WifiDirectActivity.this, "isGroupOwner "+device.deviceName+" " +isGroupOwner, Toast.LENGTH_SHORT).show();

                    /*Toast.makeText(WifiDirectActivity.this, "Already Connected", Toast.LENGTH_SHORT).show();
                    SocketAndDevice.wifiP2pDevice=device;
                    Intent go = new Intent(WifiDirectActivity.this,MainActivity.class);
                    if(isGroupOwner){
                        go.putExtra("stateofclient","sender");
                    }
                    else{
                        go.putExtra("stateofclient","receiver");
                    }
                    setResult(Activity.RESULT_OK,go);
                    finish();*/

                }
            }
        });
        cancelConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manager.cancelConnect(channel, new ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(WifiDirectActivity.this, "Connection Cancelled", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reason) {

                    }
                });
            }
        });


    }
    public synchronized void connect(final WifiP2pDevice device){
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;

        manager.connect(channel, config, new ActionListener() {
            @Override
            public void onSuccess() {
                SocketAndDevice.wifiP2pDevice = device;
                 boolean isGroupOwner = device.isGroupOwner();
                Toast.makeText(WifiDirectActivity.this, "isGroupOwner "+device.deviceName+" " +isGroupOwner, Toast.LENGTH_SHORT).show();
                Toast.makeText(WifiDirectActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                Intent go = new Intent(WifiDirectActivity.this,MainActivity.class);
                if(isGroupOwner){
                    go.putExtra("stateofclient","sender");
                }
                else{
                    go.putExtra("stateofclient","receiver");
                }
                setResult(Activity.RESULT_OK,go);
                finish();
            }

            @Override
            public void onFailure(int reason) {

            }
        });
    }



    private void discover() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = ProgressDialog.show(this, "Press back to cancel", "finding peers", true,
                true, new DialogInterface.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {

                    }
                });
        manager.discoverPeers(channel, new ActionListener() {

            @Override
            public void onSuccess() {
                Toast.makeText(WifiDirectActivity.this, "Discovery Initiated",
                        Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onFailure(int reasonCode) {
                Toast.makeText(WifiDirectActivity.this, "Discovery Failed : " + reasonCode,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

        public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

            private WifiP2pManager mManager;
            private WifiP2pManager.Channel mChannel;
            private WifiDirectActivity mActivity;

            public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                               WifiDirectActivity activity) {
                super();
                this.mManager = manager;
                this.mChannel = channel;
                this.mActivity = activity;
            }


            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                    // UI update to indicate wifi p2p status.
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    // Wifi Direct mode is enabled
                    Log.v(TAG,"Wifi Direct Mode activated");
                    mActivity.setIsWifiP2pEnabled(true);
                        status.setText("Status:- Enabled");
                    turnOnWifi.setVisibility(View.GONE);
                    turnOffWifi.setVisibility(View.VISIBLE);

                } else {
                    mActivity.setIsWifiP2pEnabled(false);
                    status.setText("Status:- Disabled");
                    turnOnWifi.setVisibility(View.VISIBLE);
                    turnOffWifi.setVisibility(View.GONE);
                    mActivity.resetData();

                }
                Log.d(TAG, "P2P state changed - " + state);
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                // Call WifiP2pManager.requestPeers() to get a list of current peers
                checkPeers();
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                Log.v(TAG,"Some connections changes");
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                // Respond to this device's wifi state changing
            }
        }
    }

    private void checkPeers() {
        if(manager!=null){
            manager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
                @Override
                public void onPeersAvailable(WifiP2pDeviceList peerList) {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    //Toast.makeText(WifiDirectActivity.this, peerList.toString(), Toast.LENGTH_SHORT).show();
                    peers.clear();
                    peers.addAll(peerList.getDeviceList());
                    wifiPeerListAdapter.notifyDataSetChanged();
                    if (peers.size() == 0) {
                        Log.d(TAG, "No devices found");
                        return;
                    }
                }
            });
        }
    }
    public class WiFiPeerListAdapter extends ArrayAdapter<WifiP2pDevice> {
        private List<WifiP2pDevice> items;

        /**
         * @param context
         * @param textViewResourceId
         * @param objects
         */
        public WiFiPeerListAdapter(Context context, int textViewResourceId,
                                   List<WifiP2pDevice> objects) {
            super(context, textViewResourceId, objects);
            items = objects;

        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getApplication().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.row_devices, null);
            }
            WifiP2pDevice device = items.get(position);
            if (device != null) {
                TextView top = (TextView) v.findViewById(R.id.device_name);
                TextView bottom = (TextView) v.findViewById(R.id.device_details);
                if (top != null) {
                    top.setText(device.deviceName);
                }
                if (bottom != null) {
                    bottom.setText(getDeviceStatus(device.status));
                    /*if(device.status==WifiP2pDevice.CONNECTED){
                        SocketAndDevice.wifiP2pDevice = device;
                        //Toast.makeText(WifiDirectActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                        Intent go = new Intent(WifiDirectActivity.this,MainActivity.class);
                        go.putExtra("stateofclient",2);
                        setResult(Activity.RESULT_OK,go);
                        finish();
                    }*/
                }
            }

            return v;

        }
    }
    private static String getDeviceStatus(int deviceStatus) {
        Log.d(TAG, "Peer status :" + deviceStatus);
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";

        }
    }

}
