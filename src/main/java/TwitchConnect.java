import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

public class TwitchConnect implements Runnable
{
    private DataInputStream is;

    private DataOutputStream os;

    private StringProperty data = new SimpleStringProperty();

    // Basically a first in first out array list that is also thread safe :D
    private static LinkedBlockingQueue<String> messages = new LinkedBlockingQueue<>();

    private String tmpData;

    private Socket socket;

    private final Client client;

    public TwitchConnect(Client client)
    {
        this.client = client;
    }

    private final Thread messageSender = new Thread(() ->
    {
        System.out.println("MessageSender running");
        while (true)
        {
            try
            {
                Thread.sleep(333);
            }
            catch (InterruptedException y)
            {
                System.out.println(y.getMessage());
            }

            if (messages.size() < 1)
                continue;

            if (messages.peek() == null)
                continue;

            try
            {
                System.out.println("Sending: " + messages.peek());
                os.write(messages.poll().getBytes());
                os.flush();
            }
            catch (IOException y)
            {
                System.out.println(y.getMessage());
            }
        }
    });

    public void run()
    {
        // Connect
        connect();

        // Login
        logIn();

        // Start message Sender Thread
        messageSender.start();

        while (true)
        {
            tmpData = String.valueOf(BasicIO.readLine(is));

            if (tmpData.substring(0, 4).equals("PING"))
            {
                sendMessage("PONG " + tmpData.substring(5));
            }
            else
            {
                data.setValue(tmpData);
            }
        }
    }

    private void connect()
    {
        try
        {
            System.out.println("Attempting to start connection");
            socket = new Socket(TwitchConnectionInfo.getHost(), TwitchConnectionInfo.getPort());
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
            System.out.println("Connection started");
        }
        catch(IOException e)
        {
            System.out.println(e.getMessage());
            System.out.println("Error connecting to twitch IRC service");
        }
    }

    private void logIn()
    {
        System.out.println("Attempting to log in");
        // None of this actually happens until the messageSender is started
        sendMessage("PASS oauth:" + client.getOauth());
        sendMessage("NICK " + client.getNick());
    }

    public synchronized void sendMessage(String command)
    {
        try
        {
            messages.put(command + "\r\n");
        }
        catch (InterruptedException e)
        {
            System.out.println(e.getMessage());
        }
    }

    public StringProperty getDataProperty()
    {
        return data;
    }

    public String getData()
    {
        return data.getValueSafe();
    }
}
