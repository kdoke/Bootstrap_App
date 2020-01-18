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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
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
        initalWork();
        exqListener();
        removePersistentGroups();
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

        btnExecute.setOnClickListener(new View.OnClickListener() {
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
        });


       /* btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg=writeMsg.getText().toString();
                //sendReceive.write(msg.getBytes());
                Log.d("btnSend", "send message");
            }
        });*/

    }

    private void initalWork() {
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
                String s = "Connected" + "," + String.valueOf(System.currentTimeMillis()) + "," + myDeviceName + "," + "null" + "\n";
                Log.d(TAG, "connected: " + s);
                WriteFile(s);
                connectionStatus.setText("GroupOwner");

                mFlag = "server";
                connected = true;

                Log.d("ServerClassThread: ", "Server Thread Started");
                Toast.makeText(getApplicationContext(), "Server Thread Started", Toast.LENGTH_SHORT).show();
            } else if (wifiP2pInfo.groupFormed) {
                mFlag="client";
                connected = true;
                mGroupOwnerAddress = groupOwnerAddress;
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
        ServerSocket serverSocket;
        Socket socket;
        InputStream inputStream;
        OutputStream outputStream;
        List<Incident> MyIncidents;
        List<Incident> PeerIncidents;

        @Override
        protected Object doInBackground(Object[] objects) {



            try{
                serverSocket = new ServerSocket(8881);
                socket = serverSocket.accept();

               /* List<Incident> MyIncidents = new ArrayList<>();
                Incident incident1 = new Incident(1111, "Fire on Route 9", "Twitter");
                Incident incident2 = new Incident(2222, "Tree down", "Facebook");
                Incident incident3 = new Incident(3333, "Flood", "Twitter");

                MyIncidents.add(incident1);
                MyIncidents.add(incident2);
                MyIncidents.add(incident3);*/

                List<Incident> MyIncidents = new ArrayList<>();
                Incident myIncident41 = new Incident(3111,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-07-22 09:05:38", "'Rain'" , "'311'", "'Alerts'");
                Incident myIncident42 = new Incident(3112,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-07-22 09:06:38", "'Rain'" , "'312'", "'Alerts'");
                Incident myIncident43 = new Incident(3113,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-07-22 09:07:38", "'Rain'" , "'313'", "'Alerts'");
                Incident myIncident44 = new Incident(3114,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-07-22 09:08:38", "'Rain'" , "'314'", "'Alerts'");
                Incident myIncident45 = new Incident(3115,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-07-22 09:09:38", "'Rain'" , "'315'", "'Alerts'");
                Incident myIncident46 = new Incident(3116,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-07-22 09:10:38", "'Rain'" , "'316'", "'Alerts'");
                Incident myIncident47 = new Incident(3117,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-07-22 09:11:38", "'Rain'" , "'317'", "'Alerts'");
                Incident myIncident48 = new Incident(3118,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-07-22 09:12:38", "'Rain'" , "'318'", "'Alerts'");
                Incident myIncident49 = new Incident(3119,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-07-22 09:13:38", "'Rain'" , "'319'", "'Alerts'");
                Incident myIncident50 = new Incident(3120,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-07-22 09:14:38", "'Rain'" , "'320'", "'Alerts'");
                Incident myIncident51 = new Incident(3121,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-07-22 09:15:38", "'Rain'" , "'321'", "'Alerts'");
                Incident myIncident52 = new Incident(3122,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-07-22 09:16:38", "'Rain'" , "'322'", "'Alerts'");
                Incident myIncident53 = new Incident(3123,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-07-22 09:17:38", "'Rain'" , "'323'", "'Alerts'");
                Incident myIncident54 = new Incident(3124,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-07-22 09:18:38", "'Rain'" , "'324'", "'Alerts'");
                Incident myIncident55 = new Incident(3125,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-07-22 09:19:38", "'Rain'" , "'325'", "'Alerts'");
                Incident myIncident56 = new Incident(3126,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-07-22 09:20:38", "'Rain'" , "'326'", "'Alerts'");
                Incident myIncident57 = new Incident(3127,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-07-22 09:21:38", "'Rain'" , "'327'", "'Alerts'");
                Incident myIncident58 = new Incident(3128,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-07-22 09:22:38", "'Rain'" , "'328'", "'Alerts'");
                Incident myIncident59 = new Incident(3129,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-07-22 09:23:38", "'Rain'" , "'329'", "'Alerts'");
                Incident myIncident60 = new Incident(3130,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-07-22 09:24:38", "'Rain'" , "'330'", "'Alerts'");



                Incident myIncident61 = new Incident(4111,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-06-22 09:05:38", "'mmmm'" , "'411'", "'Alerts'");
                Incident myIncident62 = new Incident(4112,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-06-22 09:06:38", "'mmmm'" , "'412'", "'Alerts'");
                Incident myIncident63 = new Incident(4113,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-06-22 09:07:38", "'mmmm'" , "'413'", "'Alerts'");
                Incident myIncident64 = new Incident(4114,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-06-22 09:08:38", "'mmmm'" , "'414'", "'Alerts'");
                Incident myIncident65 = new Incident(4115,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-06-22 09:09:38", "'mmmm'" , "'415'", "'Alerts'");
                Incident myIncident66 = new Incident(4116,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-06-22 09:10:38", "'mmmm'" , "'416'", "'Alerts'");
                Incident myIncident67 = new Incident(4117,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-06-22 09:11:38", "'mmmm'" , "'417'", "'Alerts'");
                Incident myIncident68 = new Incident(4118,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-06-22 09:12:38", "'mmmm'" , "'418'", "'Alerts'");
                Incident myIncident69 = new Incident(4119,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-06-22 09:13:38", "'mmmm'" , "'419'", "'Alerts'");
                Incident myIncident70 = new Incident(4120,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-06-22 09:14:38", "'mmmm'" , "'420'", "'Alerts'");
                Incident myIncident71 = new Incident(4121,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-06-22 09:15:38", "'mmmm'" , "'421'", "'Alerts'");
                Incident myIncident72 = new Incident(4122,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-06-22 09:16:38", "'mmmm'" , "'422'", "'Alerts'");
                Incident myIncident73 = new Incident(4123,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-06-22 09:17:38", "'mmmm'" , "'423'", "'Alerts'");
                Incident myIncident74 = new Incident(4124,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-06-22 09:18:38", "'mmmm'" , "'424'", "'Alerts'");
                Incident myIncident75 = new Incident(4125,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-06-22 09:19:38", "'mmmm'" , "'425'", "'Alerts'");
                Incident myIncident76 = new Incident(4126,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-06-22 09:20:38", "'mmmm'" , "'426'", "'Alerts'");
                Incident myIncident77 = new Incident(4127,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-06-22 09:21:38", "'mmmm'" , "'427'", "'Alerts'");
                Incident myIncident78 = new Incident(4128,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-06-22 09:22:38", "'mmmm'" , "'428'", "'Alerts'");
                Incident myIncident79 = new Incident(4129,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-06-22 09:23:38", "'mmmm'" , "'429'", "'Alerts'");
                Incident myIncident80 = new Incident(4130,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-06-22 09:24:38", "'mmmm'" , "'430'", "'Alerts'");


                Incident myIncident91 = new Incident(5111,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-05-22 09:05:38", "'pppp'" , "'511'", "'Alerts'");
                Incident myIncident92 = new Incident(5112,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-05-22 09:06:38", "'pppp'" , "'512'", "'Alerts'");
                Incident myIncident93 = new Incident(5113,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-05-22 09:07:38", "'pppp'" , "'513'", "'Alerts'");
                Incident myIncident94 = new Incident(5114,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-05-22 09:08:38", "'pppp'" , "'514'", "'Alerts'");
                Incident myIncident95 = new Incident(5115,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-05-22 09:09:38", "'pppp'" , "'515'", "'Alerts'");
                Incident myIncident96 = new Incident(5116,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-05-22 09:10:38", "'pppp'" , "'516'", "'Alerts'");
                Incident myIncident97 = new Incident(5117,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-05-22 09:11:38", "'pppp'" , "'517'", "'Alerts'");
                Incident myIncident98 = new Incident(5118,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-05-22 09:12:38", "'pppp'" , "'518'", "'Alerts'");
                Incident myIncident99 = new Incident(5119,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-05-22 09:13:38", "'pppp'" , "'519'", "'Alerts'");
                Incident myIncident100 = new Incident(5120,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-05-22 09:14:38", "'pppp'" , "'520'", "'Alerts'");
                Incident myIncident101 = new Incident(5121,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-05-22 09:05:38", "'pppp'" , "'521'", "'Alerts'");
                Incident myIncident102 = new Incident(5122,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-05-22 09:06:38", "'pppp'" , "'522'", "'Alerts'");
                Incident myIncident103 = new Incident(5123,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-05-22 09:07:38", "'pppp'" , "'523'", "'Alerts'");
                Incident myIncident104 = new Incident(5124,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-05-22 09:08:38", "'pppp'" , "'524'", "'Alerts'");
                Incident myIncident105 = new Incident(5125,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-05-22 09:09:38", "'pppp'" , "'525'", "'Alerts'");
                Incident myIncident106 = new Incident(5126,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-05-22 09:10:38", "'pppp'" , "'526'", "'Alerts'");
                Incident myIncident107 = new Incident(5127,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-05-22 09:11:38", "'pppp'" , "'527'", "'Alerts'");
                Incident myIncident108 = new Incident(5128,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-05-22 09:12:38", "'pppp'" , "'528'", "'Alerts'");
                Incident myIncident109 = new Incident(5129,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-05-22 09:13:38", "'pppp'" , "'529'", "'Alerts'");
                Incident myIncident110 = new Incident(5130,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-05-22 09:14:38", "'pppp'" , "'530'", "'Alerts'");


                Incident myIncident111 = new Incident(6111,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-04-22 09:05:38", "'nnnn'" , "'611'", "'Alerts'");
                Incident myIncident112 = new Incident(6112,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-04-22 09:06:38", "'nnnn'" , "'612'", "'Alerts'");
                Incident myIncident113 = new Incident(6113,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-04-22 09:07:38", "'nnnn'" , "'613'", "'Alerts'");
                Incident myIncident114 = new Incident(6114,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-04-22 09:08:38", "'nnnn'" , "'614'", "'Alerts'");
                Incident myIncident115 = new Incident(6115,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-04-22 09:09:38", "'nnnn'" , "'615'", "'Alerts'");
                Incident myIncident116 = new Incident(6116,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-04-22 09:10:38", "'nnnn'" , "'616'", "'Alerts'");
                Incident myIncident117 = new Incident(6117,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-04-22 09:11:38", "'nnnn'" , "'617'", "'Alerts'");
                Incident myIncident118 = new Incident(6118,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-04-22 09:12:38", "'nnnn'" , "'618'", "'Alerts'");
                Incident myIncident119 = new Incident(6119,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-04-22 09:13:38", "'nnnn'" , "'619'", "'Alerts'");
                Incident myIncident120 = new Incident(6120,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-04-22 09:14:38", "'nnnn'" , "'620'", "'Alerts'");
                Incident myIncident121 = new Incident(6121,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-04-22 09:05:38", "'nnnn'" , "'621'", "'Alerts'");
                Incident myIncident122 = new Incident(6122,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-04-22 09:06:38", "'nnnn'" , "'622'", "'Alerts'");
                Incident myIncident123 = new Incident(6123,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-04-22 09:07:38", "'nnnn'" , "'623'", "'Alerts'");
                Incident myIncident124 = new Incident(6124,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-04-22 09:08:38", "'nnnn'" , "'624'", "'Alerts'");
                Incident myIncident125 = new Incident(6125,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-04-22 09:09:38", "'nnnn'" , "'625'", "'Alerts'");
                Incident myIncident126 = new Incident(6126,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-04-22 09:10:38", "'nnnn'" , "'626'", "'Alerts'");
                Incident myIncident127 = new Incident(6127,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-04-22 09:11:38", "'nnnn'" , "'627'", "'Alerts'");
                Incident myIncident128 = new Incident(6128,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-04-22 09:12:38", "'nnnn'" , "'628'", "'Alerts'");
                Incident myIncident129 = new Incident(6129,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-04-22 09:13:38", "'nnnn'" , "'629'", "'Alerts'");
                Incident myIncident130 = new Incident(6130,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                        "2019-04-22 09:14:38", "'nnnn'" , "'630'", "'Alerts'");

                MyIncidents.add(myIncident41);
                MyIncidents.add(myIncident42);
                MyIncidents.add(myIncident43);
                MyIncidents.add(myIncident44);
                MyIncidents.add(myIncident45);
                MyIncidents.add(myIncident46);
                MyIncidents.add(myIncident47);
                MyIncidents.add(myIncident48);
                MyIncidents.add(myIncident49);
                MyIncidents.add(myIncident50);
                MyIncidents.add(myIncident51);
                MyIncidents.add(myIncident52);
                MyIncidents.add(myIncident53);
                MyIncidents.add(myIncident54);
                MyIncidents.add(myIncident55);
                MyIncidents.add(myIncident56);
                MyIncidents.add(myIncident57);
                MyIncidents.add(myIncident58);
                MyIncidents.add(myIncident59);
                
                MyIncidents.add(myIncident60);
                MyIncidents.add(myIncident61);
                MyIncidents.add(myIncident62);
                MyIncidents.add(myIncident63);
                MyIncidents.add(myIncident64);
                MyIncidents.add(myIncident65);
                MyIncidents.add(myIncident66);
                MyIncidents.add(myIncident67);
                MyIncidents.add(myIncident68);
                MyIncidents.add(myIncident69);
                MyIncidents.add(myIncident70);
                MyIncidents.add(myIncident71);
                MyIncidents.add(myIncident72);
                MyIncidents.add(myIncident73);
                MyIncidents.add(myIncident74);
                MyIncidents.add(myIncident75);
                MyIncidents.add(myIncident76);
                MyIncidents.add(myIncident77);
                MyIncidents.add(myIncident78);
                MyIncidents.add(myIncident79);
                MyIncidents.add(myIncident80);

                MyIncidents.add(myIncident91);
                MyIncidents.add(myIncident92);
                MyIncidents.add(myIncident93);
                MyIncidents.add(myIncident94);
                MyIncidents.add(myIncident95);
                MyIncidents.add(myIncident96);
                MyIncidents.add(myIncident97);
                MyIncidents.add(myIncident98);
                MyIncidents.add(myIncident99);
                MyIncidents.add(myIncident100);
                MyIncidents.add(myIncident101);
                MyIncidents.add(myIncident102);
                MyIncidents.add(myIncident103);
                MyIncidents.add(myIncident104);
                MyIncidents.add(myIncident105);
                MyIncidents.add(myIncident106);
                MyIncidents.add(myIncident107);
                MyIncidents.add(myIncident108);
                MyIncidents.add(myIncident109);
                MyIncidents.add(myIncident110);

                MyIncidents.add(myIncident111);
                MyIncidents.add(myIncident112);
                MyIncidents.add(myIncident113);
                MyIncidents.add(myIncident114);
                MyIncidents.add(myIncident115);
                MyIncidents.add(myIncident116);
                MyIncidents.add(myIncident117);
                MyIncidents.add(myIncident118);
                MyIncidents.add(myIncident119);
                MyIncidents.add(myIncident120);
                MyIncidents.add(myIncident121);
                MyIncidents.add(myIncident122);
                MyIncidents.add(myIncident123);
                MyIncidents.add(myIncident124);
                MyIncidents.add(myIncident125);
                MyIncidents.add(myIncident126);
                MyIncidents.add(myIncident127);
                MyIncidents.add(myIncident128);
                MyIncidents.add(myIncident129);
                MyIncidents.add(myIncident130);


                // send data
                OutputStream outputStream = socket.getOutputStream();
                ObjectOutputStream objectOutputStream = new ObjectOutputStream((outputStream));

                if (MyIncidents.size() != 0) {
                    System.out.println("Sending messages to client");
                    try{
                        objectOutputStream.writeObject(MyIncidents);
                        objectOutputStream.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try{
                        objectOutputStream.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("My List is empty");
                }

                // Receive data
                InputStream inputStream = socket.getInputStream();
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

                try{
                    PeerIncidents = (List<Incident>) objectInputStream.readObject();
                    handler.obtainMessage(MESSAGE_READ).sendToTarget();

                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }


                if(!PeerIncidents.isEmpty()){
                System.out.println("Received [" + PeerIncidents.size() + "] messages from: " + socket);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutput out = null;

                    out=new ObjectOutputStream(bos);
                    out.writeObject(PeerIncidents);
                    byte[] yourBytes = bos.toByteArray();
                    long mBytes = yourBytes.length;
                    Log.i(TAG, "doInBackground: Bytes received: " + mBytes);

                }


                objectInputStream.close();
                objectOutputStream.close();
                System.out.println("Closing Sockets");
                serverSocket.close();
                //socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }


            return null;
        }

    }

    public class ClientClass extends AsyncTask {
        private final static String TAG = "Peer";
        private Socket socket;
        private String hostAdd;
        ObjectInputStream objectInputStream;
        ObjectOutputStream objectOutputStream;
        Context context;
        List<Incident> MyIncidents;
        List<Incident> PeerIncidents;


        public ClientClass(InetAddress hostAddress) {
            this.hostAdd = hostAddress.getHostAddress();
            socket = new Socket();
        }


        @Override
        protected String doInBackground(Object[] object) {
            connect(hostAdd, 8881);
            writeIncidents();
            readIncidents();
            disconnect();
            return null;
        }

        public void connect(String hostAdd, int port) {
            socket = new Socket();
            try{
                socket.connect(new InetSocketAddress(hostAdd, port), 500);
            } catch (IOException e) {
                e.printStackTrace();
                try{
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
               // reconnect(hostAdd, 8881);

            }


            OutputStream outputStream = null;
            try{
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try{
                objectOutputStream = new ObjectOutputStream(outputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }

            InputStream inputStream = null;
            try{
                inputStream = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try{
                objectInputStream = new ObjectInputStream(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void writeIncidents() {

            MyIncidents = new ArrayList<>();

            List<Incident> MyIncidents = new ArrayList<>();

              /* List<Incident> MyIncidents = new ArrayList<>();
                Incident incident1 = new Incident(1111, "Fire on Route 9", "Twitter");
                Incident incident2 = new Incident(2222, "Tree down", "Facebook");
                Incident incident3 = new Incident(3333, "Flood", "Twitter");

                MyIncidents.add(incident1);
                MyIncidents.add(incident2);
                MyIncidents.add(incident3);*/



            Incident myIncident1 = new Incident(1111,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-09-22 09:05:38", "'Fire'" , "'111'", "'Alerts'");
            Incident myIncident2 = new Incident(1112,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-09-22 09:06:38", "'Fire'" , "'112'", "'Alerts'");
            Incident myIncident3 = new Incident(1113,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-09-22 09:07:38", "'Fire'" , "'113'", "'Alerts'");
            Incident myIncident4 = new Incident(1114,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-09-22 09:08:38", "'Fire'" , "'114'", "'Alerts'");
            Incident myIncident5 = new Incident(1115,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-09-22 09:09:38", "'Fire'" , "'115'", "'Alerts'");
            Incident myIncident6 = new Incident(1116,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-09-22 09:10:38", "'Fire'" , "'116'", "'Alerts'");
            Incident myIncident7 = new Incident(1117,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-09-22 09:11:38", "'Fire'" , "'117'", "'Alerts'");
            Incident myIncident8 = new Incident(1118,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-09-22 09:12:38", "'Fire'" , "'118'", "'Alerts'");
            Incident myIncident9 = new Incident(1119,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-09-22 09:13:38", "'Fire'" , "'119'", "'Alerts'");
            Incident myIncident10 = new Incident(1120,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-09-22 09:14:38", "'Fire'" , "'120'", "'Alerts'");
            Incident myIncident11 = new Incident(1121,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-09-22 09:05:38", "'Fire'" , "'121'", "'Alerts'");
            Incident myIncident12 = new Incident(1122,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-09-22 09:06:38", "'Fire'" , "'122'", "'Alerts'");
            Incident myIncident13 = new Incident(1123,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-09-22 09:07:38", "'Fire'" , "'123'", "'Alerts'");
            Incident myIncident14 = new Incident(1124,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-09-22 09:08:38", "'Fire'" , "'124'", "'Alerts'");
            Incident myIncident15 = new Incident(1125,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-09-22 09:09:38", "'Fire'" , "'125'", "'Alerts'");
            Incident myIncident16 = new Incident(1126,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-09-22 09:10:38", "'Fire'" , "'126'", "'Alerts'");
            Incident myIncident17 = new Incident(1127,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-09-22 09:11:38", "'Fire'" , "'127'", "'Alerts'");
            Incident myIncident18 = new Incident(1128,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-09-22 09:12:38", "'Fire'" , "'128'", "'Alerts'");
            Incident myIncident19 = new Incident(1129,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-09-22 09:13:38", "'Fire'" , "'129'", "'Alerts'");
            Incident myIncident20 = new Incident(1130,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-09-22 09:14:38", "'Fire'" , "'130'", "'Alerts'");



            Incident myIncident21 = new Incident(2111,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-08-22 09:05:38", "'Snow'" , "'211'", "'Alerts'");
            Incident myIncident22 = new Incident(2112,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-08-22 09:06:38", "'Snow'" , "'212'", "'Alerts'");
            Incident myIncident23 = new Incident(2113,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-08-22 09:07:38", "'Snow'" , "'213'", "'Alerts'");
            Incident myIncident24 = new Incident(2114,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-08-22 09:08:38", "'Snow'" , "'214'", "'Alerts'");
            Incident myIncident25 = new Incident(2115,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-08-22 09:09:38", "'Snow'" , "'215'", "'Alerts'");
            Incident myIncident26 = new Incident(2116,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-08-22 09:10:38", "'Snow'" , "'216'", "'Alerts'");
            Incident myIncident27 = new Incident(2117,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-08-22 09:11:38", "'Snow'" , "'217'", "'Alerts'");
            Incident myIncident28 = new Incident(2118,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-08-22 09:12:38", "'Snow'" , "'218'", "'Alerts'");
            Incident myIncident29 = new Incident(2119,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-08-22 09:13:38", "'Snow'" , "'219'", "'Alerts'");
            Incident myIncident30 = new Incident(2120,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-08-22 09:14:38", "'Snow'" , "'220'", "'Alerts'");
            Incident myIncident31 = new Incident(2121,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-08-22 09:05:38", "'Snow'" , "'221'", "'Alerts'");
            Incident myIncident32 = new Incident(2122,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-08-22 09:06:38", "'Snow'" , "'222'", "'Alerts'");
            Incident myIncident33 = new Incident(2123,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-08-22 09:07:38", "'Snow'" , "'223'", "'Alerts'");
            Incident myIncident34 = new Incident(2124,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-08-22 09:08:38", "'Snow'" , "'224'", "'Alerts'");
            Incident myIncident35 = new Incident(2125,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-08-22 09:09:38", "'Snow'" , "'225'", "'Alerts'");
            Incident myIncident36 = new Incident(2126,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-08-22 09:10:38", "'Snow'" , "'226'", "'Alerts'");
            Incident myIncident37 = new Incident(2127,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-08-22 09:11:38", "'Snow'" , "'227'", "'Alerts'");
            Incident myIncident38 = new Incident(2128,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-08-22 09:12:38", "'Snow'" , "'228'", "'Alerts'");
            Incident myIncident39 = new Incident(2129,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-08-22 09:13:38", "'Snow'" , "'229'", "'Alerts'");
            Incident myIncident40 = new Incident(2130,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-08-22 09:14:38", "'Snow'" , "'230'", "'Alerts'");

            Incident myIncident41 = new Incident(3111,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-07-22 09:05:38", "'Rain'" , "'311'", "'Alerts'");
            Incident myIncident42 = new Incident(3112,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-07-22 09:06:38", "'Rain'" , "'312'", "'Alerts'");
            Incident myIncident43 = new Incident(3113,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-07-22 09:07:38", "'Rain'" , "'313'", "'Alerts'");
            Incident myIncident44 = new Incident(3114,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-07-22 09:08:38", "'Rain'" , "'314'", "'Alerts'");
            Incident myIncident45 = new Incident(3115,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-07-22 09:09:38", "'Rain'" , "'315'", "'Alerts'");
            Incident myIncident46 = new Incident(3116,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-07-22 09:10:38", "'Rain'" , "'316'", "'Alerts'");
            Incident myIncident47 = new Incident(3117,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-07-22 09:11:38", "'Rain'" , "'317'", "'Alerts'");
            Incident myIncident48 = new Incident(3118,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-07-22 09:12:38", "'Rain'" , "'318'", "'Alerts'");
            Incident myIncident49 = new Incident(3119,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-07-22 09:13:38", "'Rain'" , "'319'", "'Alerts'");
            Incident myIncident50 = new Incident(3120,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-07-22 09:14:38", "'Rain'" , "'320'", "'Alerts'");
            Incident myIncident51 = new Incident(3121,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-07-22 09:15:38", "'Rain'" , "'321'", "'Alerts'");
            Incident myIncident52 = new Incident(3122,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-07-22 09:16:38", "'Rain'" , "'322'", "'Alerts'");
            Incident myIncident53 = new Incident(3123,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-07-22 09:17:38", "'Rain'" , "'323'", "'Alerts'");
            Incident myIncident54 = new Incident(3124,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-07-22 09:18:38", "'Rain'" , "'324'", "'Alerts'");
            Incident myIncident55 = new Incident(3125,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-07-22 09:19:38", "'Rain'" , "'325'", "'Alerts'");
            Incident myIncident56 = new Incident(3126,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-07-22 09:20:38", "'Rain'" , "'326'", "'Alerts'");
            Incident myIncident57 = new Incident(3127,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-07-22 09:21:38", "'Rain'" , "'327'", "'Alerts'");
            Incident myIncident58 = new Incident(3128,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-07-22 09:22:38", "'Rain'" , "'328'", "'Alerts'");
            Incident myIncident59 = new Incident(3129,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-07-22 09:23:38", "'Rain'" , "'329'", "'Alerts'");
            Incident myIncident60 = new Incident(3130,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-07-22 09:24:38", "'Rain'" , "'330'", "'Alerts'");

            Incident myIncident61 = new Incident(4111,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-06-22 09:05:38", "'mmmm'" , "'411'", "'Alerts'");
            Incident myIncident62 = new Incident(4112,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-06-22 09:06:38", "'mmmm'" , "'412'", "'Alerts'");
            Incident myIncident63 = new Incident(4113,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-06-22 09:07:38", "'mmmm'" , "'413'", "'Alerts'");
            Incident myIncident64 = new Incident(4114,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-06-22 09:08:38", "'mmmm'" , "'414'", "'Alerts'");
            Incident myIncident65 = new Incident(4115,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-06-22 09:09:38", "'mmmm'" , "'415'", "'Alerts'");
            Incident myIncident66 = new Incident(4116,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-06-22 09:10:38", "'mmmm'" , "'416'", "'Alerts'");
            Incident myIncident67 = new Incident(4117,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-06-22 09:11:38", "'mmmm'" , "'417'", "'Alerts'");
            Incident myIncident68 = new Incident(4118,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-06-22 09:12:38", "'mmmm'" , "'418'", "'Alerts'");
            Incident myIncident69 = new Incident(4119,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-06-22 09:13:38", "'mmmm'" , "'419'", "'Alerts'");
            Incident myIncident70 = new Incident(4120,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-06-22 09:14:38", "'mmmm'" , "'420'", "'Alerts'");
            Incident myIncident71 = new Incident(4121,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-06-22 09:15:38", "'mmmm'" , "'421'", "'Alerts'");
            Incident myIncident72 = new Incident(4122,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-06-22 09:16:38", "'mmmm'" , "'422'", "'Alerts'");
            Incident myIncident73 = new Incident(4123,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-06-22 09:17:38", "'mmmm'" , "'423'", "'Alerts'");
            Incident myIncident74 = new Incident(4124,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-06-22 09:18:38", "'mmmm'" , "'424'", "'Alerts'");
            Incident myIncident75 = new Incident(4125,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-06-22 09:19:38", "'mmmm'" , "'425'", "'Alerts'");
            Incident myIncident76 = new Incident(4126,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-06-22 09:20:38", "'mmmm'" , "'426'", "'Alerts'");
            Incident myIncident77 = new Incident(4127,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-06-22 09:21:38", "'mmmm'" , "'427'", "'Alerts'");
            Incident myIncident78 = new Incident(4128,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-06-22 09:22:38", "'mmmm'" , "'428'", "'Alerts'");
            Incident myIncident79 = new Incident(4129,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-06-22 09:23:38", "'mmmm'" , "'429'", "'Alerts'");
            Incident myIncident80 = new Incident(4130,"Fire started on route 87. All traffic is being diverted to alternate routes.","URL", "Twitter", 1, 1,
                    "2019-06-22 09:24:38", "'mmmm'" , "'430'", "'Alerts'");


            MyIncidents.add(myIncident1);
            MyIncidents.add(myIncident2);
            MyIncidents.add(myIncident3);
            MyIncidents.add(myIncident4);
            MyIncidents.add(myIncident5);
            MyIncidents.add(myIncident6);
            MyIncidents.add(myIncident7);
            MyIncidents.add(myIncident8);
            MyIncidents.add(myIncident9);
            MyIncidents.add(myIncident10);
            MyIncidents.add(myIncident11);
            MyIncidents.add(myIncident12);
            MyIncidents.add(myIncident13);
            MyIncidents.add(myIncident14);
            MyIncidents.add(myIncident15);
            MyIncidents.add(myIncident16);
            MyIncidents.add(myIncident17);
            MyIncidents.add(myIncident18);
            MyIncidents.add(myIncident19);
            MyIncidents.add(myIncident20);

            MyIncidents.add(myIncident21);
            MyIncidents.add(myIncident22);
            MyIncidents.add(myIncident23);
            MyIncidents.add(myIncident24);
            MyIncidents.add(myIncident25);
            MyIncidents.add(myIncident26);
            MyIncidents.add(myIncident27);
            MyIncidents.add(myIncident28);
            MyIncidents.add(myIncident29);
            MyIncidents.add(myIncident30);
            MyIncidents.add(myIncident31);
            MyIncidents.add(myIncident32);
            MyIncidents.add(myIncident33);
            MyIncidents.add(myIncident34);
            MyIncidents.add(myIncident35);
            MyIncidents.add(myIncident36);
            MyIncidents.add(myIncident37);
            MyIncidents.add(myIncident38);
            MyIncidents.add(myIncident39);
            MyIncidents.add(myIncident40);

            MyIncidents.add(myIncident41);
            MyIncidents.add(myIncident42);
            MyIncidents.add(myIncident43);
            MyIncidents.add(myIncident44);
            MyIncidents.add(myIncident45);
            MyIncidents.add(myIncident46);
            MyIncidents.add(myIncident47);
            MyIncidents.add(myIncident48);
            MyIncidents.add(myIncident49);
            MyIncidents.add(myIncident50);
            MyIncidents.add(myIncident51);
            MyIncidents.add(myIncident52);
            MyIncidents.add(myIncident53);
            MyIncidents.add(myIncident54);
            MyIncidents.add(myIncident55);
            MyIncidents.add(myIncident56);
            MyIncidents.add(myIncident57);
            MyIncidents.add(myIncident58);
            MyIncidents.add(myIncident59);
            MyIncidents.add(myIncident60);

            MyIncidents.add(myIncident61);
            MyIncidents.add(myIncident62);
            MyIncidents.add(myIncident63);
            MyIncidents.add(myIncident64);
            MyIncidents.add(myIncident65);
            MyIncidents.add(myIncident66);
            MyIncidents.add(myIncident67);
            MyIncidents.add(myIncident68);
            MyIncidents.add(myIncident69);
            MyIncidents.add(myIncident70);
            MyIncidents.add(myIncident71);
            MyIncidents.add(myIncident72);
            MyIncidents.add(myIncident73);
            MyIncidents.add(myIncident74);
            MyIncidents.add(myIncident75);
            MyIncidents.add(myIncident76);
            MyIncidents.add(myIncident77);
            MyIncidents.add(myIncident78);
            MyIncidents.add(myIncident79);
            MyIncidents.add(myIncident80);


           /* MyIncidents.addAll(databaseHelper.getAllIncidents().values());
            MyIncidents.add(new Incident(-1));*/
            Log.i(TAG, String.valueOf(MyIncidents.size()));


            if (MyIncidents.size() != 0) {
                System.out.println("Sending messages to groupowner");
                try{
                    objectOutputStream.writeObject(MyIncidents);
                    objectOutputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try{
                    objectOutputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("My List is empty");
            }


        }

        public void readIncidents() {
            try{
                PeerIncidents = (List<Incident>) objectInputStream.readObject();
                handler.obtainMessage(MESSAGE_READ).sendToTarget();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(!PeerIncidents.isEmpty()){
                System.out.println("Received [" + PeerIncidents.size() + "] messages from: " + socket);
                }
           /* for (Incident item : PeerIncidents) {
                System.out.println(item.getIncidentId());
            }*/
            //insertIncidents(PeerIncidents);
        }

        public void reconnect(String hostAdd, int port) {
            int max_connections = 5;
            int reconnections = 0;

            try{

                Thread.sleep(1000);
                Log.d(TAG, "reconnect: sleeping ......");
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }

            if (reconnections < max_connections) {
                reconnections++;
                connect(hostAdd, 8881);
                Log.i(TAG, "reconnect: " + reconnections + String.valueOf(System.currentTimeMillis()));
            } else {
                System.exit(0);
                return;
            }
        }

        public void disconnect() {
            System.out.println("Closing Sockets");

            try{
                objectInputStream.close();
                objectOutputStream.close();
                socket.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }
}
