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

import java.util.ArrayList;
import java.util.Scanner;

public class TwitchChatter
{
    private static ArrayList<String> users = new ArrayList<>();

    public static void main(String[] args)
    {
        Scanner kb = new Scanner(System.in);
        String nick, oauth;

        System.out.print("Please enter your username: ");
        nick = kb.next();
        System.out.print("Please enter your OAUTH key: ");
        oauth = kb.next();

        TwitchConnect socketRunner = new TwitchConnect(new Client(oauth, nick));
        Thread th = new Thread(socketRunner);

        socketRunner.getDataProperty().addListener(e ->
        {
            String data = socketRunner.getData();

            System.out.println("ACTUAL: " + data);

            boolean part = data.contains("PART"),
                join = data.contains("JOIN"),
                msg = data.contains("PRIVMSG");


            // Not join, part, or message
            if (! (part || join || msg))
            {
                // Analyze the header of the message
                String header;

                int headerStart = 1;
                int headerEnd = data.indexOf(':', headerStart); // Just past the first : looking for the last :

                // No header
                if (headerEnd == -1)
                    return;

                // Has a header
                else
                {
                    // Get the header
                    header = data.substring(headerStart, headerEnd);

                    // List of users
                    if (header.contains("353")) // Twitch code 353. Lists the users in the channel
                    // Not really sure if this is useful. As testing shows that the IRC will only
                    // list the client joining, not the other users on the IRC.
                    {
                        char[] usersBlob = data.substring(headerEnd + 1).toCharArray();
                        StringBuilder sb = new StringBuilder();

                        int start = 0;
                        for (int count = 0 ; count <= usersBlob.length - 1 ; count++)
                        {
                            if (usersBlob[count] == 32 || usersBlob[count] == '\r')
                            {
                                start++; // Skip the space between users
                                for (; start < count ; start++)
                                    sb.append(usersBlob[start]);

                                users.add(sb.toString());
                            }
                        }

                        System.out.print("> Users: ");

                        for (String user : users)
                            System.out.print(user);

                        System.out.print("\n");
                    }
                    else
                        return;
                }

            }

            // The exact index where the first channel name letter is
            int channelStart = (data.indexOf('#') + 1);

            // The exact endex of the userName end delimeter
            int nameSeperator = (data.indexOf('!'));

            // Get the name of the channel
            String channel = data.substring(channelStart);

            // Exactly part, join, or msg
            if (part || join || msg)
            {
                String username = data.substring(1, nameSeperator);

                if (part)
                    System.out.println("> " + username + " left channel: " + channel);

                if (join)
                    System.out.println("> " + username + " joined channel: " + channel);

                if (msg)
                {
                    int messageStart = (data.indexOf(':', nameSeperator));
                    String message = data.substring(messageStart + 1);

                    System.out.println("> " + username + ": " + message);
                }
            }
        });

        th.start();

        while (true)
        {
            String command = kb.nextLine();
            socketRunner.sendMessage(command);
        }
    }
}
