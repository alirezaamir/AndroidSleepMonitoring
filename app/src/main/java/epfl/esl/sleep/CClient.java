package epfl.esl.sleep;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class CClient
        implements Runnable
{
    private Socket socket;
    private String ServerIP = "192.168.1.8";
    private int ServerPort = 5500;

    private String TAG = CClient.class.getSimpleName();

    public void run()
    {
        try
        {
            socket = new Socket(ServerIP, ServerPort);
            Log.v(TAG, "TCP/IP connection success");

        }  catch(Exception e)
        {
            Log.v(TAG, "TCP/IP connection failed: "+ e.getLocalizedMessage());
        }
    }

    public void Send(String s)
    {
        try
        {
            PrintWriter outToServer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
            outToServer.print(s + "\n");
            outToServer.flush();
            outToServer.close();
        } catch (UnknownHostException e) {
            Log.v(TAG, "TCP/IP send: "+ e.toString());
        } catch (IOException e) {
            Log.v(TAG, "TCP/IP send: "+ e.toString());
        } catch (Exception e) {
            Log.v(TAG, "TCP/IP send: "+ e.toString());
        }

    }
}
