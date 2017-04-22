package edu.student.android.bluetoothlab;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static android.os.Build.VERSION.SDK_INT;
import static edu.student.android.bluetoothlab.R.id.statusOfImage;

public class SendAnImageWifi extends AppCompatActivity {
    private ImageView imageView;
    private Button sendButton,attachButton;
    private TextView statusOfImage;
    private final int REQUEST_CODE = 102;
    private  byte[] byteArray;
    private final static String TAG = "SendAnImageWifi";
    private ConnectedThread mConnectedThread;
    private int mState;
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;// now connected to a remote device
    public static final int STATE_SENT= 4;
    private byte[] readB =null;
    private int sentState;
    byte[] result=new byte[0];
    private Socket socket;
    public int port = 8008;
    private static final int SOCKET_TIMEOUT = 60000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_an_image_wifi);
        Log.v(TAG, "onCreate");
        if (SDK_INT > 8)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
            //your codes here
            socket = Data.socket;
            setUpUiButtonsAndListeners();
            mConnectedThread = new ConnectedThread(socket);
            mConnectedThread.start();

        }


    }
    public void setUpUiButtonsAndListeners(){
        imageView = (ImageView) findViewById(R.id.imageView5);
        sendButton = (Button) findViewById(R.id.send1);
        attachButton = (Button) findViewById(R.id.attach1);
        statusOfImage = (TextView) findViewById(R.id.statusOfImage1);
        statusOfImage.setVisibility(View.GONE);

        attachButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, "Starting Intent");
                //startActivityForResult(new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI), REQUEST_CODE);
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, REQUEST_CODE);
            }
        });
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.v(TAG, "Send Button");
                if (byteArray.length > 0) {
                    write(byteArray);
                }

            }
        });

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            switch (requestCode) {

                case REQUEST_CODE:
                    if (resultCode == Activity.RESULT_OK) {
                        Log.v(TAG, "Result OK");

                        Log.v(TAG,"Bluetooth Connection- SendAnImage - onActivityResult");
                        final Uri imageUri = data.getData();
                        final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                        imageView.setImageBitmap(selectedImage);
                        byteArray = readBytes(imageUri);
                        Log.v(TAG,"No of Bytes: "+byteArray.length);


                        break;
                    } else if (resultCode == Activity.RESULT_CANCELED) {
                        Log.e(TAG, "Selecting picture cancelled");
                    }
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in onActivityResult : " + e.getMessage());
        }
    }
    public byte[] readBytes(Uri uri) throws IOException {
        // this dynamically extends to take the bytes you read
        InputStream inputStream = getContentResolver().openInputStream(uri);
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        // this is storage overwritten on each iteration with bytes
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        // we need to know how may bytes were read to write them to the byteBuffer
        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        // and then we can return your byte array.
        return byteBuffer.toByteArray();
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
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        // private byte[] mmBuffer; // mmBuffer store for the stream

        public  ConnectedThread(Socket socket) {
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
            mState = STATE_CONNECTED;
        }

        public void run() {
            // byte[] buffer = new byte[1024];
            byte[] b=null; /// bytes returned from read()
            byte[] buffer = new byte[1024];
            int numBytes;

            Log.v(TAG,"run - ConnectThread");
            // Keep listening to the InputStream until an exception occurs.
            while (mState ==  STATE_CONNECTED) {
                Log.v(TAG,"run- while - ConnectThread");
                try {

                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    int length = 0;
                    //int bytes;
                    byte[] firstBuffer= new byte[1024]; // for first 4 bytes
                    length = mmInStream.read(firstBuffer); //read the bytes , check how much read if error
                    //bytes = length;
                    byte[] first4Bytes = Arrays.copyOfRange(firstBuffer, 0 , 4); // copy the first 4 bytes to another byte[]
                    Log.v(TAG,"length of first4Bytes :"+first4Bytes);
                    ByteBuffer bb = ByteBuffer.wrap(first4Bytes); // convert to bytebuffer
                    int lengthOfFile = bb.getInt(); // then to int, this is the length of the file
                    int lengthOfBuffer = length; // length of the buffer
                    firstBuffer = Arrays.copyOfRange(firstBuffer,4,lengthOfBuffer); // copy  the contents after the byte(length of file) till the end to itself
                    Log.v(TAG,"length of first Buffer"+firstBuffer.length);
                    out.write(firstBuffer,0,firstBuffer.length); // write from the first buffer to out
                   /* ByteBuffer bb1 = ByteBuffer.wrap(firstBuffer);*/
                    int lengthOfReadBuffer = firstBuffer.length; // get the length of this buffer to keep count of the bytes read after file transfer
                    int j=0;
                    Log.v(TAG,"Length of ReadBuffer: "+lengthOfReadBuffer);
                    Log.v(TAG,"Length of File: "+lengthOfFile);
                    while((length = mmInStream.read(buffer))>=0 ){
                        lengthOfReadBuffer = lengthOfReadBuffer +length;
                        Log.v(TAG,"Length of ReadBuffer: "+lengthOfReadBuffer);
                        if(j==0) { j++;
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    statusOfImage.setVisibility(View.VISIBLE);
                                    statusOfImage.setText("Downloading in Progress");
                                }
                            });
                        }
                        Log.v(TAG,"Reading data");
                        if(lengthOfReadBuffer != lengthOfFile)
                            out.write(buffer,0,length);
                        else{
                            out.write(buffer,0,length);
                            break;
                        }
                    }
                    runOnUiThread(new Runnable() {
                        public void run() {
                            statusOfImage.setText("Downloading Completed");
                            //Toast.makeText(SendAnImage.this, "Downloading Completed", Toast.LENGTH_LONG).show();
                        }
                    });
                    b = out.toByteArray();
                    Log.v(TAG, "run- while - ConnectThread | Bytes array: " + b.length);
                    Message readMsg = mHandler.obtainMessage(
                            Constants.MESSAGE_READ, -1, -1,
                            b);
                    readMsg.sendToTarget();


                    // Log.v(TAG, "run- while - ConnectThread | Total No of Bytes: " + numBytes);



                    sentState = STATE_SENT;
                    // Log.v(TAG,"run- while - ConnectThread | Total No of Bytes: "+numBytes);
                    // Send the obtained bytes to the UI activity.
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            Log.v(TAG,"write - ConnectThread");

            try {
                int byteLength = bytes.length;
                byte[] byte1 = ByteBuffer.allocate(4).putInt(byteLength).array();
                byte[] sendbytes = concatenateByteArrays(byte1,bytes);
                Log.v(TAG,"SIZE OF BYTE [] : "+sendbytes.length);
                mmOutStream.write(sendbytes);
                /*byte[] first4Bytes1 = Arrays.copyOfRange(sendbytes, 0 , 4);
                ByteBuffer bb = ByteBuffer.wrap(first4Bytes1);
                int value1 = bb.getInt();
                Log.v(TAG,"The Value :"+value1);*/

                Log.v(TAG,"The message :"+sendbytes.length);
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
    private void byteToImage() {

        Bitmap bitmap = BitmapFactory.decodeByteArray(result, 0,
                result.length);
        imageView.setImageBitmap(bitmap);

    }
    byte[] concatenateByteArrays(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    //  Log.v(TAG,"The length of writeBuf:"+);
                    // construct a string from the buffer
                    /*String writeMessage = new String(writeBuf);*/
                    Log.v(TAG,"write "+writeBuf);
                    //mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;

                    result = concatenateByteArrays(result,readBuf);

                    Log.v(TAG,"The length is "+result.length);

                    /*byte[] readBuf = new byte[readB.length + readBuf1.length];
                    System.arraycopy(readB.length, 0, readBuf, 0, readB.length);
                    System.arraycopy(readBuf1.length, 0, readBuf, readB.length, readBuf1.length);*/
                    // construct a string from the valid bytes in the buffer
                   /* String readMessage = new String(readBuf, 0, msg.arg1);*/
                    if(sentState == STATE_SENT){
                        Log.v(TAG,"STATE SENT");
                        byteToImage();
                    }
                    //mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    break;
                /*case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name

                    Toast.makeText(SendAMessage.this, "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();

                    break;
                case Constants.MESSAGE_TOAST:

                    Toast.makeText(SendAMessage.this, msg.getData().getString(Constants.TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;*/
            }

        }

    };
}
