import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class TwitchConnect implements Runnable
{

    // oauth:7592bdpxcsgjgyyrqmgi2k1xm22xo5

    private DataInputStream is;

    private DataOutputStream os;

    private StringProperty data = new SimpleStringProperty("");

    private String tmpData;

    private Client client;

    public TwitchConnect(Client client)
    {
        this.client = client;
    }

    public void run()
    {
        // Connect
        try
        {
            Socket socket = new Socket(TwitchConnectionInfo.getHost(), TwitchConnectionInfo.getPort());
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
        }
        catch (IOException e)
        {
            System.out.println(e.getMessage());
            System.out.println("Error connecting and/ or getting inputStream");
        }

        // Login
        try
        {
            os.write(("PASS oauth:" + client.getOauth() + "\n").getBytes());
            os.write(("NICK " + client.getNick() + "\n").getBytes());
        }
        catch (IOException e)
        {
            System.out.println(e.getMessage());
            System.out.println("Error logging into the twitch IRC service");
        }

        while (true)
        {
            tmpData = String.valueOf(BasicIO.readLine(is));

            if (tmpData.substring(0, 4).equals("PING"))
            {
                try
                {
                    os.write(("PONG" + tmpData.substring(5)).getBytes());
                }
                catch (IOException e)
                {
                    System.out.println(e.getMessage());
                    System.out.println("Failed on keepAlive");
                }
            }
            else
            {
                data.setValue(tmpData);
            }
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

    public DataOutputStream getOutputStream()
    {
        return os;
    }
}
