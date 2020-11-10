package dokek.strose.edu.wifi_direct;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Peer extends AsyncTask {

    private final static String TAG = "Peer";
    private String hostAdd;
    boolean received=false;
    DatagramSocket socket;

    public Peer(InetAddress hostAddress) {
        this.hostAdd = hostAddress.getHostAddress();

    }


    @Override
    protected Object doInBackground(Object[] objects) {

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

    @Override
    protected void onPostExecute(Object o) {

        if(received){

        }
        super.onPostExecute(o);
    }
}
