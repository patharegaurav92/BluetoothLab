package edu.student.android.bluetoothlab;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import static android.R.attr.data;
import static edu.student.android.bluetoothlab.SocketAndDevice.socket;

public class WifiDirectActivity extends AppCompatActivity {
    public WifiP2pDevice device;
    public int port = 8008;
    private static final int SOCKET_TIMEOUT = 5000;
    private static final String TAG = "WifiDirectActivity";
    private Button turnOnWifi,turnOffWifi,discoverpeers,cancelConnect;
    private TextView status;
    private ProgressDialog progressDialog = null;
    private boolean isWifiP2pEnabled = false;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    WiFiDirectBroadcastReceiver receiver;
    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pInfo info;

    private WiFiPeerListAdapter wifiPeerListAdapter;
    private ListView peerListView;
    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_direct);

        turnOnWifi = (Button) findViewById(R.id.turnOnWifi);
        turnOffWifi =(Button) findViewById(R.id.turnOffWifi);
        discoverpeers= (Button) findViewById(R.id.discover_peers);
        cancelConnect = (Button) findViewById(R.id.cancel_connection);
        status = (TextView) findViewById(R.id.text1);
        peerListView = (ListView) findViewById(R.id.peerListView1);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        wifiPeerListAdapter = new WiFiPeerListAdapter(this,R.layout.row_devices,peers);
        peerListView.setAdapter(wifiPeerListAdapter);
        discoverpeers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                discoverPeers();
            }
        });
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


        peerListView.setOnItemClickListener(onItemClick);


    }
    AdapterView.OnItemClickListener onItemClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            device = wifiPeerListAdapter.getItem(position);
            Log.v(TAG,"Device: "+device.deviceName);
            if(device.status == WifiP2pDevice.AVAILABLE){
                connect(device);
            }else{
                SocketAndDevice.wifiP2pDevice = device;
            }
        }
    };

    private void connect(final WifiP2pDevice device) {
        SocketAndDevice.wifiP2pDevice = device;
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.v(TAG,"onSuccess()- Device Connected - "+device.deviceName);
                Toast.makeText(WifiDirectActivity.this, "Connected to "+device.deviceName, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {

            }
        });
    }


    private void discoverPeers() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = ProgressDialog.show(WifiDirectActivity.this, "Press Back To Cancel", "Finding Peers", true,
                true, new DialogInterface.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {

                    }
                });
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

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

    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
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
                }
                Log.d(TAG, "P2P state changed - " + state);
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                // Call WifiP2pManager.requestPeers() to get a list of current peers
                checkPeers();
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                // Respond to new connection or disconnections
                NetworkInfo networkInfo = (NetworkInfo) intent
                        .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo.isConnected()) {
                    checkPeers();
                    Iterator<WifiP2pDevice> i = peers.iterator();
                    while(i.hasNext()){
                        WifiP2pDevice dev = i.next();
                        if(dev.status == WifiP2pDevice.CONNECTED){
                            Toast.makeText(context, "One device is already connected", Toast.LENGTH_SHORT).show();
                            SocketAndDevice.wifiP2pDevice = dev;
                        }
                    }
                    requestConnectionInfo();
                }
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                // Respond to this device's wifi state changing
            }
        }
    }

    private void requestConnectionInfo() {
        manager.requestConnectionInfo(channel,new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(final WifiP2pInfo info) {

                Data.ownerAddress = info.groupOwnerAddress.getHostAddress();
                if(info.groupFormed && info.isGroupOwner){
                    Toast.makeText(WifiDirectActivity.this, "Owner", Toast.LENGTH_SHORT).show();
                    new AcceptAsync(getApplicationContext()).execute();

                }else if(info.groupFormed){
                    Toast.makeText(WifiDirectActivity.this, "Client", Toast.LENGTH_SHORT).show();
                    new ConnectAsync(getApplicationContext()).execute();
                }

            }
        });
    }


    private void checkPeers() {
        if(manager!=null){
            manager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
                @Override
                public void onPeersAvailable(WifiP2pDeviceList peerList) {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
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
                LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(
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

                }
            }
            if(device.status==WifiP2pDevice.CONNECTED){
                Log.v(TAG,"First time ------------");
                SocketAndDevice.wifiP2pDevice = device;

            }

            return v;

        }
    } //Adapter
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
    } //Status of device showed on each list
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

    public  class AcceptAsync extends AsyncTask<Void, Void, String> {

        private Context context;
        // private TextView statusText;

        /**
         * @param context
         *
         */
        public AcceptAsync(Context context) {
            this.context = context;
            //this.statusText = (TextView) statusText;
        }

        @Override
        protected String doInBackground(Void... params) {
            //Toast.makeText(MainActivity.this, "Group Owner", Toast.LENGTH_SHORT).show();
            //Device is Owner
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(port);
                serverSocket.setReuseAddress(true);
                Log.v(TAG,"Waiting to accept");
                Socket client = serverSocket.accept();
                Data.socket =client;
                ObjectInputStream objectInputStream = new ObjectInputStream(client.getInputStream());
                Object object = objectInputStream.readObject();
                if (object.getClass().equals(String.class) && ((String) object).equals("BROFIST")) {
                    Log.v(TAG,"Client Address " +client.getInetAddress().toString());
                    InetAddress inetaddress = client.getInetAddress();
                    Data.clientAddress = inetaddress.getHostAddress();
                    //Toast.makeText(MainActivity.this, "Connected to :"+Data.ownerAddress, Toast.LENGTH_SHORT).show();
                }
                return "Connected ---";
            } catch (SocketException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }


        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {
            Log.v(TAG,"onPostExecute");
            if (result != null) {
                Toast.makeText(context,"Owner Address: "+Data.ownerAddress+ " Client Address: "+Data.clientAddress, Toast.LENGTH_SHORT).show();
                Data.myDeviceHostAddress = Data.ownerAddress;
                Data.peerDeviceHostAddress = Data.clientAddress;
                Log.v(TAG,"Owner-"+Data.myDeviceHostAddress+" Client-"+Data.peerDeviceHostAddress +" Device-"+SocketAndDevice.wifiP2pDevice);
                Intent go = new Intent(WifiDirectActivity.this,MainActivity.class);
                setResult(Activity.RESULT_OK,go);

                Log.v(TAG,"GO BACK");
                finish();
            }

        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            Log.v(TAG,"onPreExecute");
        }

    }
    public  class ConnectAsync extends AsyncTask<Void, Void, String> {

        private Context context;
        // private TextView statusText;

        /**
         * @param context
         *
         */
        public ConnectAsync(Context context) {
            this.context = context;
            //this.statusText = (TextView) statusText;
        }

        @Override
        protected String doInBackground(Void... params) {
            //Toast.makeText(MainActivity.this, "Group Owner", Toast.LENGTH_SHORT).show();
            //Device is Client
            try {
                Socket socket = new Socket();
                socket.setReuseAddress(true);
                socket.connect((new InetSocketAddress(Data.ownerAddress, port)), SOCKET_TIMEOUT);
                OutputStream os = socket.getOutputStream();
                Data.socket =socket;
                ObjectOutputStream oos = new ObjectOutputStream(os);
                oos.writeObject(new String("BROFIST"));

                return "Connected --- ";
            } catch (SocketException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }


        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {
            Log.v(TAG,"onPostExecute");
            if (result != null) {
                WifiP2pDevice device;
                @SuppressLint("WifiManagerLeak")
                String inet = getLocalIpAddress();
                Log.v(TAG,"Device address is "+inet);
                Data.clientAddress = inet;
                Toast.makeText(context,"Owner Address: "+Data.clientAddress+ " Client Address: "+Data.ownerAddress, Toast.LENGTH_SHORT).show();
                Data.myDeviceHostAddress = Data.clientAddress;
                Data.peerDeviceHostAddress = Data.ownerAddress;

                Log.v(TAG,"Owner-"+Data.myDeviceHostAddress+" Client-"+Data.peerDeviceHostAddress+" Device-"+SocketAndDevice.wifiP2pDevice);
                Intent go = new Intent(WifiDirectActivity.this,MainActivity.class);
                setResult(Activity.RESULT_OK,go);

                Log.v(TAG,"GO BACK");
                finish();
            }

        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            Log.v(TAG,"onPreExecute");
        }

    }
    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
