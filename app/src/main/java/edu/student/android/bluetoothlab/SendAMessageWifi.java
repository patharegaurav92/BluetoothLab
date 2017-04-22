package edu.student.android.bluetoothlab;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import static edu.student.android.bluetoothlab.SocketAndDevice.socket;

public class SendAMessageWifi extends AppCompatActivity {
    private static final String TAG="SendAMessageWifi";
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    private ArrayAdapter<String> mConversationArrayAdapter;
    private String mConnectedDeviceName;
    private ConnectedThread mConnectedThread;
    private StringBuffer mOutStringBuffer;
    private Socket socket;
    public int mState;
    public int port = 8008;
    private static final int SOCKET_TIMEOUT = 60000;
    public static final int STATE_CONNECTED = 3;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_a_message_wifi);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mConversationView = (ListView) findViewById(R.id.in1);
        mOutEditText = (EditText) findViewById(R.id.edit_text_out1);
        mSendButton = (Button) findViewById(R.id.button_send1);
        socket = Data.socket;
        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
            //your codes here
            setupChat();

        }

    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");
        mConnectedDeviceName = SocketAndDevice.wifiP2pDevice.deviceName;
        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.message);
        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                TextView textView = (TextView) findViewById(R.id.edit_text_out1);
                String message = textView.getText().toString();
                sendMessage(message);

            }
        });

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

    }
    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };
    private void sendMessage(String message) {


            if (message.length() > 0) {
                // Get the message bytes and tell the BluetoothChatService to write
                byte[] send = message.getBytes();
                write(send);

                // Reset out string buffer to zero and clear the edit text field
                mOutStringBuffer.setLength(0);
                mOutEditText.setText(mOutStringBuffer);
            }

    }

    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }


    private class ConnectedThread extends Thread {
        private final Socket mmSocket;
        private InputStream mmInStream;
        private OutputStream mmOutStream;
        // private byte[] mmBuffer; // mmBuffer store for the stream

        public  ConnectedThread(Socket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            Log.v(TAG,"The socket is "+socket);
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
            mState = STATE_CONNECTED;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int numBytes; // bytes returned from read()
            if(mState == STATE_CONNECTED) {
               // Keep listening to the InputStream until an exception occurs.
                while (true) {
                    try{
                        // Read from the InputStream.
                        numBytes = mmInStream.read(buffer);
                        Log.v(TAG,"The number of Bytes: "+ numBytes);
                        // Send the obtained bytes to the UI activity.
                        Message readMsg = mHandler.obtainMessage(
                                Constants.MESSAGE_READ, numBytes, -1,
                                buffer);
                        readMsg.sendToTarget();

                    } catch (IOException e) {
                        Log.d(TAG, "Input stream was disconnected", e);
                        break;
                    }
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            Log.v(TAG,"write - Socket is "+socket);
            try {
                mmOutStream.write(bytes);
                Log.v(TAG,"The length of message :"+bytes.length);
                // Share the sent message with the UI activity.
                Message writtenMsg = mHandler.obtainMessage(
                        Constants.MESSAGE_WRITE, -1, -1, bytes);
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
                Message writeErrorMsg =
                        mHandler.obtainMessage(Constants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast",
                        "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                mHandler.sendMessage(writeErrorMsg);
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

    @Override
    protected void onPause() {
        super.onPause();
        mConversationArrayAdapter.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mConversationArrayAdapter.clear();
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    //  Log.v(TAG,"The length of writeBuf:"+);
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    Log.v(TAG,"Msg written: "+writeMessage);
                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Log.v(TAG,"Msg: "+readMessage);
                    mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    Toast.makeText(SendAMessageWifi.this, "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();

                    break;
                case Constants.MESSAGE_TOAST:

                    Toast.makeText(SendAMessageWifi.this, msg.getData().getString(Constants.TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }

        }

    };

}
