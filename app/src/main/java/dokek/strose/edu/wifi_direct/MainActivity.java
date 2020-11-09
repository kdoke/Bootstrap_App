package dokek.strose.edu.wifi_direct;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.net.wifi.WifiManager;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {
    public static String TAG = "MainActivity";
    Button btnOnOff;
    Button btnDiscover;
    Button btnExecute;
    ListView listView;
    TextView read_msg_box;
    TextView connectionStatus;
    EditText writeMsg;

    WifiManager wifiManager;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;

    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;

    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;
    ServerClass mServerClass;
    ClientClass mClientClass;
    String mFlag = "";
    InetAddress mGroupOwnerAddress;
    boolean connected = false;


    static final int MESSAGE_READ = 1;
    boolean bStarted = true;

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_READ:
                   // byte[] readBuff = (byte[]) msg.obj;
                   // String tempMsg = new String(readBuff, 0, msg.arg1);
                    read_msg_box.setText("Incidents Received");
                    break;
            }
            return true;
        }
    });
  // comment
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        initialWork();
        exqListener();
      //  removePersistentGroups();
        removeGroup();



    }


    private void exqListener() {
        btnDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        connectionStatus.setText("Discovery Started");
                        Log.d("btnDiscover", "starting discovery");
                    }


                    @Override
                    public void onFailure(int reason) {
                        connectionStatus.setText("Discovery Starting Failed");
                        Log.d("btnDiscover", "Discovery Failed");
                    }
                });
            }
        });

        // Used to click on a device name in a list and connect
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final WifiP2pDevice device = deviceArray[i];
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(), "Connected to " + device.deviceName, Toast.LENGTH_SHORT).show();
                        Log.d("Connected:", "Connected to: " + device.deviceName);
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(getApplicationContext(), "Not Connected ", Toast.LENGTH_SHORT).show();
                        Log.d("Connected: ", "Not Connected to selected device");
                    }
                });

            }
        });

       /* btnExecute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(connected){
                    if (mFlag.equals("server")){
                        mServerClass = new ServerClass();
                        mServerClass.execute();
                    }else{
                        mClientClass = new ClientClass(mGroupOwnerAddress);
                        mClientClass.execute();
                    }
                }else {
                    Toast.makeText(MainActivity.this, "Not connected yet", Toast.LENGTH_SHORT).show();
                }

            }
        });*/


       /* btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg=writeMsg.getText().toString();
                //sendReceive.write(msg.getBytes());
                Log.d("btnSend", "send message");
            }
        });*/

    }

    private void initialWork() {
        btnOnOff = (Button) findViewById(R.id.onOff);
        btnDiscover = (Button) findViewById(R.id.discoverPeers);
        btnExecute = (Button) findViewById(R.id.sendButton);
        listView = (ListView) findViewById(R.id.peerListView);
        read_msg_box = (TextView) findViewById(R.id.readMsg);
        connectionStatus = (TextView) findViewById(R.id.connectionStatus);
        //writeMsg = (EditText) findViewById(R.id.writeMsg);
        //enableWifi();

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);

        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);

        mIntentFilter = new IntentFilter();
        // indicates a change in the peer-to-peer status
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        // indicates a change in the list of available peers
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        // indicates the state of connectivity has changed
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        // indicates that this device's details have changed
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    public void discoverPeers() {
        final String myDeviceName = getMacAddr();
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                connectionStatus.setText("Discovery Started");
                Log.d("btnDiscover", "starting discovery");
                String s = "Discover" + "," + String.valueOf(System.currentTimeMillis()) + "," + myDeviceName + "," + "null" + "\n";
                Log.d(TAG, "onSuccess: discovery: " + s);
                //WriteFile(s);
            }


            @Override
            public void onFailure(int reason) {
                connectionStatus.setText("Discovery Starting Failed");
                Log.d("btnDiscover", "Discovery Failed");
            }
        });
    }

    private void removePersistentGroups() {
        try {
            Method[] methods = WifiP2pManager.class.getMethods();
            for (int i = 0; i < methods.length; i++) {
                if (methods[i].getName().equals("deletePersistentGroup")) {
                    // Remove any persistent group
                    for (int netid = 0; netid < 32; netid++) {
                        methods[i].invoke(mManager, mChannel, netid, null);
                    }
                }
            }
            Log.i(TAG, "Persistent groups removed");
        } catch(Exception e) {
            Log.e(TAG, "Failure removing persistent groups: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void connect() {
        WifiP2pConfig config = new WifiP2pConfig();
        final String device = "38:80:df:94:02:15"; // host address
        config.deviceAddress = device;
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(getApplicationContext(), "Connected to " + device, Toast.LENGTH_SHORT).show();
                Log.d("Connected:", "Connected to: " + device);
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(getApplicationContext(), "Not Connected ", Toast.LENGTH_SHORT).show();
                Log.d("Connected: ", "Not Connected to selected device " + reason);
            }
        });

    }

    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            final String myDeviceName = getMacAddr();
            if (!peerList.getDeviceList().equals(peers)) {
                peers.clear();
                peers.addAll(peerList.getDeviceList());
                deviceNameArray = new String[peerList.getDeviceList().size()];
                deviceArray = new WifiP2pDevice[peerList.getDeviceList().size()];

                int index = 0;

                for (WifiP2pDevice device : peerList.getDeviceList()) {
                    deviceNameArray[index] = device.deviceName;
                    deviceArray[index] = device;
                    index++;
                    Log.d("device: ", "device " + device.deviceName + " address: " + device.deviceAddress);
                    String s = "Peers_Found" + "," + String.valueOf(System.currentTimeMillis()) + "," + myDeviceName + "," + "null" + "\n";
                    Log.d(TAG, "onSuccess: peers found: " + s);
                    // if (!myDeviceName.equalsIgnoreCase("38:80:df:94:02:15"));
                    //{connect();}
                   // WriteFile(s);

                }

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, deviceNameArray);
                listView.setAdapter(adapter);
            }
            if (peers.size() == 0) {
                Toast.makeText(getApplicationContext(), "No Device Found", Toast.LENGTH_SHORT).show();
                Log.d("Peers:", "No peers found");
                String s = "No_Peers_Found" + "," + String.valueOf(System.currentTimeMillis()) + "," + myDeviceName + "," + "null" + "\n";
                Log.d(TAG, "onSuccess: discovery: " + s);
                //WriteFile(s);
                return;
            }
        }
    };

    // To get information about host and client and sending and receiving
    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(final WifiP2pInfo wifiP2pInfo) {
            final InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;
            final String myDeviceName = getMacAddr();
            if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
                mServerClass = new ServerClass();
                mServerClass.execute();
                String s = "Connected" + "," + String.valueOf(System.currentTimeMillis()) + "," + myDeviceName + "," + "null" + "\n";
                Log.d(TAG, "connected: " + s);
                WriteFile(s);
                connectionStatus.setText("GroupOwner");


                Log.d("ServerClassThread: ", "Server Thread Started");
                Toast.makeText(getApplicationContext(), "Server Thread Started", Toast.LENGTH_SHORT).show();
            } else if (wifiP2pInfo.groupFormed) {
                mGroupOwnerAddress = groupOwnerAddress;
                mClientClass = new ClientClass(mGroupOwnerAddress);
                mClientClass.execute();
                Log.d("groupOwnerAddress: ", "address: " + groupOwnerAddress);
                connectionStatus.setText("Client");
                Log.d("ClientClassThread: ", "Client Thread Started");
                Toast.makeText(getApplicationContext(), "Client Thread Started", Toast.LENGTH_SHORT).show();
            }
        }
    };

    public void WriteFile(String s) {
        try{
            FileOutputStream fileout = openFileOutput("mytextfile.txt", MODE_APPEND);
            OutputStreamWriter outputWriter = new OutputStreamWriter(fileout);
            outputWriter.write(s);
            outputWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void enableWifi() {

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            Log.d(TAG, "enableWifi: Not Enabled");
            wifiManager.setWifiEnabled(true);
        } else {
            Log.d(TAG, "enableWifi: Enabled");
        }

    }

    private void removeGroup() {

        mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup group) {
                if (group != null) {
                    mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "onSuccess: group removed");
                        }

                        @Override
                        public void onFailure(int reason) {
                            Log.d(TAG, "onFailure: can't remove group " + reason);

                        }
                    });
                }
            }
        });

    }

    public static String getMacAddr() {
        try{
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:", b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
        }
        return "02:00:00:00:00:00";
    }

    private Timer timer;
    private TimerTask timerTask;

    public void startTimer() {
        timer = new Timer();
        timerTask = new TimerTask() {
            public void run() {
                Log.d(TAG, "run: Timer Started");
                discoverPeers();
            }
        };
        timer.schedule(timerTask, 1000, 20000); //
    }

    public void stoptimertask() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stoptimertask();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        stoptimertask();
        removeGroup();
        removePersistentGroups();

    }

    public class ServerClass extends AsyncTask {
        private DatagramSocket socket;
        boolean received = false;

        @Override
        protected Object doInBackground(Object[] objects) {

            String send = "";
            String s = "This is an alert of the Emergency Broadcasting System. Please seek shelter until flood has expired!!";
            for(int i=0; i<500; i++) {
                send += s; }

            try {
                // open a socket and wait for the client to connect
                socket = new DatagramSocket(8881);
                byte[] receiveData = new byte[55000];
                byte[] sendData;
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                Log.i(TAG, "Waiting for client to connect .....");
                socket.receive(receivePacket);
                String str = new String(receivePacket.getData(),0,receivePacket.getLength());
                Log.i(TAG, "Receive String Hello From Client: " + str.length());
                received=true;

            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


            return null;
        }

    }

    public class ClientClass extends AsyncTask {
        private final static String TAG = "Peer";
        private String hostAdd;
        boolean received=false;
        DatagramSocket socket;

        public ClientClass(InetAddress hostAddress) {
            this.hostAdd = hostAddress.getHostAddress();

        }


        @Override
        protected String doInBackground(Object[] object) {
            try {

                DatagramSocket socket = new DatagramSocket();
                InetAddress IPAddress = InetAddress.getByName(hostAdd);
                byte[] receiveData = new byte[50000];
                byte[] sendData;
                String sentence = "Hello from UDP client";
                sendData = sentence.getBytes();
                Log.i(TAG, "Sending " + sentence);

                DatagramPacket sendingPacket = new DatagramPacket(sendData,sendData.length,IPAddress, 8881);
                socket.send(sendingPacket);
                Log.i(TAG, "Sent Hello to server");


            } catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;

        }


    }
}
