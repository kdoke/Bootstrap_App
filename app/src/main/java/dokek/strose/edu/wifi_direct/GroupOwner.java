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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class GroupOwner extends AsyncTask {
    private static final String TAG = "GroupOwner";
    ServerSocket serverSocket;
    Socket socket;
    private String send = "";
    private String receive = "";
    InputStream inputStream;
    OutputStream outputStream;
    boolean received = false;

    @Override
    protected Object doInBackground(Object[] objects) {

        String s = "This is an alert of the Emergency Broadcasting System. Please seek shelter until flood has expired!!";
        for(int i=0; i<10; i++) {
            send += s; }

        try {
            // open a socket and wait for the client to connect
           serverSocket = new ServerSocket(8881);
           socket = serverSocket.accept();
            Log.i(TAG, "Waiting for client to connect .....");

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
            Log.i(TAG, "Receive String Hello From Client: " + receive.length());
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
