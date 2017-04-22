package edu.student.android.bluetoothlab;

import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import static android.os.Build.VERSION.SDK_INT;
import static edu.student.android.bluetoothlab.Data.socket;

public class SendARecordingWifi extends AppCompatActivity {
    private static final String TAG="SendARecordingWifi";
    private MediaRecorder myRecorder;
    private MediaPlayer myPlayer;
    private String outputFile = null;
    private String senderFile = null;
    private Button startBtn;
    private Button stopBtn;
    private Button playBtn;
    private Button stopPlayBtn;
    private Button sendRecording;
    private Button playRecording;
    private TextView text;
    private boolean permissionToRecordAccepted = false;
    private boolean permissionToWriteAccepted = false;
    private String [] permissions = {"android.permission.RECORD_AUDIO", "android.permission.WRITE_EXTERNAL_STORAGE"};
    private ConnectedThread mConnectedThread;
    private int mState;
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;// now connected to a remote device
    public static final int STATE_SENT= 4;
    private int sentState;
    byte[] result=new byte[0];
    private  byte[] byteArray;
    private Socket socket;
    public int port = 8008;
    private static final int SOCKET_TIMEOUT = 60000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_arecording_wifi);
        int requestCode = 200;
        if (SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, requestCode);
        }
        if (SDK_INT > 8) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
            text = (TextView) findViewById(R.id.textView2);
            // store it to sd card
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            outputFile = Environment.getExternalStorageDirectory().
                    getAbsolutePath() + "/Recording_" + timeStamp + ".3gpp";
            senderFile = Environment.getExternalStorageDirectory().
                    getAbsolutePath() + "/SenderRecording_" + timeStamp + ".3gpp";
            myRecorder = new MediaRecorder();
            myRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            myRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            myRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
            myRecorder.setOutputFile(outputFile);
            startBtn = (Button) findViewById(R.id.startRecording1);
            socket = Data.socket;
            startBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    start(v);
                }
            });
            stopBtn = (Button) findViewById(R.id.stopRecording1);
            stopBtn.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    stop(v);
                }
            });

            playBtn = (Button) findViewById(R.id.play1);
            playBtn.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    play(v);
                }
            });

            stopPlayBtn = (Button) findViewById(R.id.stopPlay1);
            stopPlayBtn.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    stopPlay(v);
                }
            });
            sendRecording = (Button) findViewById(R.id.send_rec1);
            sendRecording.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (byteArray.length > 0) {
                        startBtn.setEnabled(true);
                        write(byteArray);
                    }
                }
            });
            playRecording = (Button) findViewById(R.id.senderPlayRecording1);
            playRecording.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        myPlayer = new MediaPlayer();
                        myPlayer.setDataSource(senderFile);
                        myPlayer.prepare();
                        myPlayer.start();
                        text.setText("Recording Point: Playing Senders Message");


                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            });
            mConnectedThread = new ConnectedThread(Data.socket);
            mConnectedThread.start();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 200:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                permissionToWriteAccepted  = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) SendARecordingWifi.super.finish();
        if (!permissionToWriteAccepted ) SendARecordingWifi.super.finish();

    }
    public void start(View view){
        try {
            myRecorder.prepare();
            myRecorder.start();
        } catch (IllegalStateException e) {
            // start:it is called before prepare()
            // prepare: it is called after start() or before setOutputFormat()
            e.printStackTrace();
        } catch (IOException e) {
            // prepare() fails
            e.printStackTrace();
        }

        text.setText("Recording Point: Recording");
        startBtn.setEnabled(false);
        stopBtn.setEnabled(true);

        Toast.makeText(getApplicationContext(), "Start recording...",
                Toast.LENGTH_SHORT).show();
    }

    public void stop(View view){
        try {
            myRecorder.stop();
            myRecorder.release();
            myRecorder  = null;

            stopBtn.setEnabled(false);
            playBtn.setEnabled(true);
            text.setText("Recording Point: Stop recording");
            File file1 = new File(outputFile);
            Uri uri = Uri.fromFile(file1);
            byteArray = readBytes(uri);
            Log.v(TAG,"Byte Array"+byteArray.length);
            Toast.makeText(getApplicationContext(), "Stop recording...",
                    Toast.LENGTH_SHORT).show();
        } catch (IllegalStateException e) {
            //  it is called before start()
            e.printStackTrace();
        } catch (RuntimeException e) {
            // no valid audio/video data has been received
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void play(View view) {
        try{
            myPlayer = new MediaPlayer();
            myPlayer.setDataSource(outputFile);
            myPlayer.prepare();
            myPlayer.start();

            playBtn.setEnabled(false);
            stopPlayBtn.setEnabled(true);
            text.setText("Recording Point: Playing");

            Toast.makeText(getApplicationContext(), "Start play the recording...",
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void stopPlay(View view) {
        try {
            if (myPlayer != null) {
                myPlayer.stop();
                myPlayer.release();
                myPlayer = null;
                playBtn.setEnabled(true);
                stopPlayBtn.setEnabled(false);
                text.setText("Recording Point: Stop playing");

                Toast.makeText(getApplicationContext(), "Stop playing the recording...",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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

    public byte[] AudioToByte(String path){
        File file = new File(path);
//init array with file length
        byte[] bytesArray = new byte[(int) file.length()];

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            fis.read(bytesArray); //read file into bytes[]
            fis.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return bytesArray;
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

                        Log.v(TAG,"Reading data");
                        if(lengthOfReadBuffer != lengthOfFile)
                            out.write(buffer,0,length);
                        else{
                            out.write(buffer,0,length);
                            break;
                        }
                    }

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


                Log.v(TAG,"The message :"+bytes.length);
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
                        File f = new File(senderFile);
                        toFile(result,f);
                        Toast.makeText(SendARecordingWifi.this, "File recieved", Toast.LENGTH_SHORT).show();
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

    private void toFile(byte[] result, File dest) {
        try {
            FileOutputStream fos= new FileOutputStream(dest);
            fos.write(result);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    byte[] concatenateByteArrays(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}
