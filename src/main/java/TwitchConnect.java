/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import static logUtils.Logger.log;

public class TwitchConnect implements Runnable
{
    // Basically a first in first out array list that is also thread safe :D
    private static LinkedBlockingQueue<String> messages = new LinkedBlockingQueue<>();
    private static PipedOutputStream pipedOutputStream = new PipedOutputStream();
    private static PipedInputStream pipedInputStream = new PipedInputStream();
    // The client using the program. Contains the user name and OAUTH token.
    private final Client client;
    private final Thread messageProcessor = new Thread(() ->
    {
        // Some thread safety?
        while (!getMessageReceiverRunning())
            ;

        DataInputStream inputStream = null;
        ExecutorService executor =
                Executors.newCachedThreadPool();
        try
        {
            pipedInputStream.connect(pipedOutputStream);
            inputStream = new DataInputStream(pipedInputStream);
        } catch (IOException e)
        {
            log(e.getMessage());
        }

        if (inputStream != null)
        {
            while (!Thread.currentThread().isInterrupted())
            {
                String data = null;
                try
                {
                    data = inputStream.readUTF();
                } catch (IOException e)
                {
                    log(e.getMessage());
                }
                executor.execute(new DataProcessor(data));
            }
        }
    });
    private DataInputStream is;
    private DataOutputStream os;
    private String initialChannel = null;
    private boolean acceptingMessages = true, messageReceiverRunning = false;
    private final Thread messageSender = new Thread(() ->
    {
        // Some thread safety?
        while (!getMessageReceiverRunning())
            ;

        log("messageSender running");
        while (!Thread.currentThread().isInterrupted())
        {
            try
            {
                // Don't spam twitch. It doesn't like it.
                Thread.sleep(777);
            } catch (InterruptedException y)
            {
                log(y.getMessage());
            }

            if (messages.size() < 1)
                continue;

            if (messages.peek() == null)
                continue;

            try
            {
                if (getAcceptingMessages())
                {
                    log("messageSender sending: " + messages.peek());
                    os.write(messages.poll().getBytes());
                    os.flush();
                }
            } catch (IOException y)
            {
                log(y.getMessage());
            }
        }
    });

    public TwitchConnect(Client client)
    {
        this.client = client;
    }

    public TwitchConnect(Client client, String initialChannel)
    {
        this.client = client;
        this.initialChannel = initialChannel;
    }

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
        log("Starting messageProcessor service");
        messageProcessor.start();

        DataOutputStream outputStream = new DataOutputStream(pipedOutputStream);

        log("messageReceiver service running");
        setMessageReceiverRunning(true);
        while (!Thread.currentThread().isInterrupted())
        {
            // Don't add the received data directly to the StringProperty.
            // Check it for relevance before adding.
            String tmpData = String.valueOf(BasicIO.readLine(is));

            if (tmpData.substring(0, 4).equals("PING"))
            {
                sendMessage("PONG " + tmpData.substring(5));
            } else if (tmpData.length() > 51 && tmpData.substring(0, 52).equals(
                    ":tmi.twitch.tv NOTICE * :Login authentication failed"))
            {
                log("Bad credentials received");
                setAcceptingMessages(false);
                try
                {
                    outputStream.writeUTF("EEE: Incorrect login information!");
                } catch (IOException e)
                {
                    log(e.getMessage());
                }
                messages.clear();
            } else
            {
                try
                {
                    outputStream.writeUTF(tmpData);
                } catch (IOException e)
                {
                    log(e.getMessage());
                }
            }
        }

        if (Thread.currentThread().isInterrupted())
        {
            messageSender.interrupt();
            messageProcessor.interrupt();
        }
    }

    private void connect()
    {
        try
        {
            log("Connecting to twitch IRC services");
            Socket socket = new Socket(TwitchConnectionInfo.getIrcChatTwitchTv(), TwitchConnectionInfo.getPort());
            WildChat.connected = true;
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
            log("Connection started");
        } catch (IOException e)
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
        if (initialChannel != null)
        {
            sendMessage("JOIN " + initialChannel);
            Platform.runLater(() ->
            {
                WildChat.session.setChannel(initialChannel);
                WildChat.displayMessage("Joining channel " + initialChannel + "...");
            });
        }
    }

    private synchronized boolean getAcceptingMessages()
    {
        return this.acceptingMessages;
    }

    private synchronized void setAcceptingMessages(boolean acceptingMessages)
    {
        this.acceptingMessages = acceptingMessages;
    }

    private synchronized boolean getMessageReceiverRunning()
    {
        return this.messageReceiverRunning;
    }

    private synchronized void setMessageReceiverRunning(boolean messageReceiverRunning)
    {
        this.messageReceiverRunning = messageReceiverRunning;
    }

    // Send a message to the Twitch IRC
    public synchronized void sendMessage(String command)
    {

        if (acceptingMessages)
        {
            try
            {
                messages.put(command + "\r\n");
            } catch (InterruptedException e)
            {
                System.out.println(e.getMessage());
            }
        }
    }
}
