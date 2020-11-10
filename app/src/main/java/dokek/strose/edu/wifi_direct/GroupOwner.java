package dokek.strose.edu.wifi_direct;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class GroupOwner extends AsyncTask {
    private static final String TAG = "GroupOwner";
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

    @Override
    protected void onPostExecute(Object o) {

        if(received){

        }
        super.onPostExecute(o);
    }
}
