package com.example.r2d2.bluetoothclient;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class MainActivity extends AppCompatActivity implements Handler.Callback, AdapterView.OnItemClickListener {

    //Debugging variables
    String LOG_TAG = "Main";
    int foundCount = 0;

    //Lists
    private Set<BluetoothDevice> btDevicesArray;     //set is used because no duplicate paired devices are allowed, used
                                                        //when populating the pairedDevices arraylist
    private ArrayList<String> pairedDevices;     //the previously paired devices
    private ArrayList<String> btNames;          //the
    private ArrayList<BluetoothDevice> btDevices;
    ArrayAdapter<String> listAdapter;
    BluetoothSocket tmpSocket;

    //Constants
    protected static final int SUCCESS_CONNECT = 0;
    protected static final int MESSAGE_READ = 1;
    protected static final int CHK_BLUE = 2;

    //Layout items
    ListView listView;
    Button bDisconnect, bSearch;
    TextView tvStatus;

    BluetoothAdapter btAdapter;

    BroadcastReceiver receiver;         //receiver
    IntentFilter filter;
    Handler handler = new Handler();

    public static final UUID CLIENT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");     //UUID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(btAdapter!=null)
        {

            Log.i(LOG_TAG, "Bluetooth Capabilities Found");
            init();
        }else
        {
            Toast.makeText(getApplicationContext(), "No Bluetooth Found...", Toast.LENGTH_SHORT).show();
            finish();       //exits application
        }
    }

    @Override
    public boolean handleMessage(Message msg) {

        switch (msg.what)
        {
            case CHK_BLUE:
                String chk = (String)msg.obj;
                Toast.makeText(getApplicationContext(), chk, Toast.LENGTH_SHORT).show();
                break;

            case SUCCESS_CONNECT:
                Log.i(LOG_TAG,"connected");
                Toast.makeText(getApplicationContext(), "CONNECTED", Toast.LENGTH_SHORT).show();
                tmpSocket = (BluetoothSocket)msg.obj;



                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[])msg.obj;
                String message = new String(readBuf);
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                break;
        }

        return false;


    }

    //initialization
    private void init()
    {
        listView = (ListView)findViewById(R.id.listView);
        listView.setOnItemClickListener(this);              //sets an on item click listner where the onItemClick method
                                                                //is found in this context
        tvStatus = (TextView)findViewById(R.id.tvStatus);

        bDisconnect = (Button)findViewById(R.id.bDisconnect);
        bDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Disconnect Bluetooth
            }
        });

        bSearch = (Button)findViewById(R.id.bSearch);
        bSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Start searching process
                pairedDevices = new ArrayList<String>();
                getPairedDevices();
                startDiscovery();
            }
        });

        btDevices = new ArrayList<>();
        listAdapter = new ArrayAdapter<String>(this, R.layout.main_layout,0);     //array adapter to adapt the arraylist for display
        listView.setAdapter(listAdapter);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch(action)
                {
                    case BluetoothDevice.ACTION_FOUND:
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);       //pulls device form calling intent

                        foundCount++;
                        Log.i(LOG_TAG,"Device Found. Count: " + foundCount);

                        String s = "";  //extra message for paired devices
                        if(isPaired(device.getName()))
                        {
                            s +="(Paired)";     //flags the device as a paired device
                        }

                        listAdapter.add(device.getName() + " " + s + "\n" + device.getAddress());
                         break;

                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                        tvStatus.setText("Searching ... Please Wait");
                        break;

                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        tvStatus.setText("Search Complete");
                        break;

                    case BluetoothAdapter.ACTION_STATE_CHANGED:
                        if(btAdapter.getState() == btAdapter.STATE_OFF)
                        {
                            turnOnBT();
                        }
                }
            }
        };

        //filters
        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(receiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver,filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(receiver,filter);

    }

    private void turnOnBT()
    {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK)
        {
            btAdapter.enable();
        }else if(resultCode == RESULT_CANCELED)
        {
            Toast.makeText(getApplicationContext(), "Bluetooth must be enabled to continue...", Toast.LENGTH_SHORT).show();
        }
    }

    private void getPairedDevices()
    {
        btDevicesArray = btAdapter.getBondedDevices();
        if(btDevicesArray.size()>0)
        {
            for(BluetoothDevice device : btDevicesArray)
            {
                pairedDevices.add(device.getName());
            }
        }
        return;
    }

    private boolean isPaired(String name)
    {
        for(int i=0;i<pairedDevices.size();i++)
        {
            if(name.equals(pairedDevices.get(i)))
            {
                return true;
            }
        }
       return false;
    }

    private void startDiscovery()
    {
        btAdapter.cancelDiscovery();     //cancels previous discovery, if there is one
        btAdapter.startDiscovery();      //starts new discovery
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(btAdapter.isDiscovering())
        {
            btAdapter.cancelDiscovery();
        }

    }

    //Connection Thread AKA the "Pipeline"
    public class ConnectThread extends Thread
    {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;
        String chk_blue = "Check bluetooth";

        public ConnectThread(BluetoothDevice device)
        {
            this.device = device;
            BluetoothSocket tmp = null;     //temperory socket, later assigned to the socket

            Log.i(LOG_TAG,"In Connection Thread");

            try
            {
                tmp = device.createRfcommSocketToServiceRecord(CLIENT_UUID);
            }catch (IOException e)
            {
                Log.i(LOG_TAG, "get socket failed");
            }

            socket = tmp;
        }

        public void run()
        {
            btAdapter.cancelDiscovery();
            Log.i(LOG_TAG,"connect - run");

            try
            {
                socket.connect();
                Log.i(LOG_TAG,"connect - succeeded");
            }catch (IOException conctException)
            {
                Log.i(LOG_TAG,"connect - failed");

                //could not connect, close socket and exit thread
                try
                {
                    socket.close();
                    handler.obtainMessage(CHK_BLUE,chk_blue).sendToTarget();
                }catch (IOException closeException)
                {}
                return; //exits thread
            }

            handler.obtainMessage(SUCCESS_CONNECT, socket).sendToTarget();
        }

        //cancels a connection that is in progress
        public void cancel()
        {
            try
            {
                socket.close();
            }catch (Exception e)
            {
                Log.i(LOG_TAG, "Cannot Cancel connection because socket cannot be closed");
            }
        }
    }

    public class ConnectedThread extends Thread
    {
        private final InputStream inputStream;
        private  final OutputStream outputStream;
        private final BluetoothSocket socket;

        public ConnectedThread(BluetoothSocket socket)
        {
            this.socket=socket;

            InputStream tmpIS= null;
            OutputStream tmpOS= null;

            try
            {
                tmpIS = socket.getInputStream();
                tmpOS = socket.getOutputStream();
            }catch (IOException e) {}

            inputStream = tmpIS;
            outputStream =tmpOS;
        }

        public void run()
        {
            byte[] buffer;      //buffer for the stream
            int bytes;          //number of bytes in the buffer

            while(true)
            {
                try
                {
                    buffer = new byte[1024];
                    bytes = inputStream.read(buffer);       //number of bytes in the buffer

                    handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();  //sends Message to the handler to read the input bytes
                }catch(IOException e)
                {
                    break;
                }
            }
        }

        //called so sent data
        public  void write(byte[] bytes) throws IOException
        {
            outputStream.write(bytes);
        }

        //called to cancel connection
        public void cancel()
        {
            try
            {
                socket.close();
            }catch (IOException e)
            {
                Log.i(LOG_TAG,"Could not disconnect. Socket could not be closed.");
            }
        }
    }
}
