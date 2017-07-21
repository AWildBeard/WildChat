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

    /* Data to hold the 'Data' from the Twitch IRC. Is implemented as a StringProperty so other threads can listen
     * for changes to the data so they know when to grab it. Normally this would be a problem if the same message is
     * received twice from twitch. But twitch PROHIBITS sending the same message twice in a row in within 30 seconds
     * so this is not really a problem. It also works really really well as it IS thread safe.
     */
    private StringProperty data = new SimpleStringProperty();

    // Basically a first in first out array list that is also thread safe :D
    private static LinkedBlockingQueue<String> messages = new LinkedBlockingQueue<>();

    // Holds the direct contact from the Twitch IRC. Done this so we can test what Twitch has sent us instead
    // of triggering the StringProperty and then testing.
    private String tmpData;

    private Socket socket;

    // The client using the program. Contains the user name and OAUTH token.
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
                // Don't spam twitch. It doesn't like it.
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

        // Enable TwitchAPI options
        twitchAPIOps();

        // Start message Sender Thread
        messageSender.start();

        while (true)
        {
            // Don't add the received data directly to the StringProperty. Check it for relevance before adding.
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

    private void twitchAPIOps()
    {
        // None of this actually happens until the messenger services is started
        sendMessage("CAP REQ :twitch.tv/membership");
        sendMessage("CAP REQ :twitch.tv/tags");
        sendMessage("CAP REQ :twitch.tv/commands");
    }

    // Send a message to the Twitch IRC
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

    // Get the data property so we can listen for changes.
    public StringProperty getDataProperty()
    {
        return data;
    }

    // Get the current data from Twitch IRC stored in this class. Safely.
    public String getData()
    {
        return data.getValueSafe();
    }
}
