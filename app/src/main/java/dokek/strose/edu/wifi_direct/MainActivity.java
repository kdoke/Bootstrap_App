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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
    Button btnSend;
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

    static final int MESSAGE_READ = 1;
    boolean bStarted = true;

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_READ:
                    byte[] readBuff = (byte[]) msg.obj;
                    String tempMsg = new String(readBuff, 0, msg.arg1);
                    read_msg_box.setText(tempMsg);
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
        // discoverPeers();
        startTimer();


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
        // btnSend = (Button) findViewById(R.id.sendButton);
        listView = (ListView) findViewById(R.id.peerListView);
        read_msg_box = (TextView) findViewById(R.id.readMsg);
        connectionStatus = (TextView) findViewById(R.id.connectionStatus);
        //writeMsg = (EditText) findViewById(R.id.writeMsg);
        enableWifi();

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
                WriteFile(s);
            }


            @Override
            public void onFailure(int reason) {
                connectionStatus.setText("Discovery Starting Failed");
                Log.d("btnDiscover", "Discovery Failed");
            }
        });
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
                    WriteFile(s);

                }

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, deviceNameArray);
                listView.setAdapter(adapter);
            }
            if (peers.size() == 0) {
                Toast.makeText(getApplicationContext(), "No Device Found", Toast.LENGTH_SHORT).show();
                Log.d("Peers:", "No peers found");
                String s = "No_Peers_Found" + "," + String.valueOf(System.currentTimeMillis()) + "," + myDeviceName + "," + "null" + "\n";
                Log.d(TAG, "onSuccess: discovery: " + s);
                WriteFile(s);
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
                connectionStatus.setText("Host");
                ServerClass serverClass = new ServerClass();
                serverClass.execute();

                Log.d("ServerClassThread: ", "Server Thread Started");
                Toast.makeText(getApplicationContext(), "Server Thread Started", Toast.LENGTH_SHORT).show();
            } else if (wifiP2pInfo.groupFormed) {
                ClientClass clientClass = new ClientClass(groupOwnerAddress);
                clientClass.execute();
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

            //  while (bStarted) {

            try{
                serverSocket = new ServerSocket(8881);
                socket = serverSocket.accept();

                //send data
             /*   OutputStream outputStream = socket.getOutputStream();
                ObjectOutputStream objectOutputStream = new ObjectOutputStream((outputStream));

                List<Incident> MyIncidents = new ArrayList<>();
                Incident incident1 = new Incident(1, "Fire on Route 9", "Twitter");
                Incident incident2 = new Incident(2, "Tree down", "Facebook");
                Incident incident3 = new Incident(3, "Flood", "Twitter");
                MyIncidents.add(incident1);
                MyIncidents.add(incident2);
                MyIncidents.add(incident3);

                if (MyIncidents.size() != 0) {
                    objectOutputStream.writeObject(MyIncidents);
                    objectOutputStream.flush();
                } else {
                    System.out.println("My List is empty");
                }

                // Receive data
                InputStream inputStream = socket.getInputStream();
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

                try{
                    PeerIncidents = (List<Incident>) objectInputStream.readObject();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                for (Incident item : PeerIncidents) {
                    System.out.println(item.getDescription());
                }


                objectInputStream.close();
                objectOutputStream.close();
                System.out.println("Closing Sockets");*/
                serverSocket.close();
                //socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //  }// while started

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
                reconnect(hostAdd, 8881);

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
            Incident incident1 = new Incident(1, "Fire on Route 9", "Twitter");
            Incident incident2 = new Incident(2, "Tree down", "Facebook");
            Incident incident3 = new Incident(3, "Flood", "Twitter");
            MyIncidents.add(incident1);
            MyIncidents.add(incident2);
            MyIncidents.add(incident3);

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
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Received [" + PeerIncidents.size() + "] messages from: " + socket);
            System.out.println("Messages from other ids:");
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
