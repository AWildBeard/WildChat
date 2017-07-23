import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

import static logUtils.Logger.*;

public class TwitchConnect implements Runnable
{
    private DataInputStream is;

    private DataOutputStream os;

    /* Data to hold the 'Data' from the Twitch IRC. Is implemented as a StringProperty so other classes can listen
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
        log("messageSender running");
        while (WildChat.connected)
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
                log("messageSender sending: " + messages.peek());
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
        log("Starting messageReceiver service");
        // Connect
        connect();

        // Login
        logIn();

        // Enable TwitchAPI options
        twitchAPIOps();

        // Start message Sender Thread
        log("Starting messageSender service");
        messageSender.start();

        log("messageReceiver service running");
        while (WildChat.connected)
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
            log("Connecting to twitch IRC services");
            socket = new Socket(TwitchConnectionInfo.getHost(), TwitchConnectionInfo.getPort());
            WildChat.connected = true;
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
            log("Connection started");
        }
        catch(IOException e)
        {
            WildChat.connected = false;
            log(e.getMessage());
            log("Failed to connect to twitch IRC services");
        }
    }

    private void logIn()
    {
        log("Sending client credentials");
        // None of this actually happens until the messageSender is started
        sendMessage("PASS oauth:" + client.getOauth());
        sendMessage("NICK " + client.getNick());
    }

    private void twitchAPIOps()
    {
        log("Requesting advanced operations from twitch IRC");
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
