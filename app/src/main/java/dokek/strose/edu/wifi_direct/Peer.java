package dokek.strose.edu.wifi_direct;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Peer extends AsyncTask {

    private final static String TAG = "Peer";
    boolean received=false;
    private String hostAdd;
    private String receive = "";
    private String send = "";
    private Socket socket;
    InputStream inputStream;
    OutputStream outputStream;


    public Peer(InetAddress hostAddress) {
      this.hostAdd = hostAddress.getHostAddress();
    }


    @Override
    protected Object doInBackground(Object[] objects) {

        String s = "This is an alert of the Emergency Broadcasting System. Please seek shelter until flood has expired!!";
        for(int i=0; i<10; i++) {
            send += s; }

        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(hostAdd, 8881));
            outputStream = socket.getOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(send);

            inputStream = socket.getInputStream();
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
            try {
                receive = (String) objectInputStream.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            Log.i(TAG, "Receive String Hello From Group Owner: " + receive.length());
            received=true;


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
