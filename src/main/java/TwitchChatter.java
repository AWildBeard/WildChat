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
    private static ArrayList<RemoteClient> users = new ArrayList<>();

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

            // The exact index of the userName end delimeter
            int nameStart = (data.indexOf(':'));
            int nameSeperator = (data.indexOf('!', nameStart));

            // Exactly part, join, or msg
            if (part || join || msg)
            {
                String username = data.substring(nameStart + 1, nameSeperator);

                if (part)
                    System.out.println("> " + username + " left the channel");

                if (join)
                    System.out.println("> " + username + " joined the channel");

                if (msg)
                {
                    boolean knownUser = false;

                    // Find the user in our array
                    for (RemoteClient rClient : users)
                        if (username.equals(rClient.getUserName()))
                            knownUser = true;

                    if (! knownUser)
                    {
                        // Required fields
                        ArrayList<String> badges = new ArrayList<>();
                        ArrayList<Integer> badgeVers = new ArrayList<>();
                        String color = "NOCOLOR";
                        // Already know username
                        // TODO: implement
                        boolean mod, subscriber, turbo;
                        String userType;
                        String userID;

                        // Utility variables
                        StringBuilder sb = new StringBuilder();
                        int endOfValueLocation = data.indexOf(';');
                        char[] rawData = data.toCharArray();

                        // Badges
                        int badgesStart = 8; // Always 8 because all PRIVMSG start with @badges
                        for (int count = badgesStart; count <= endOfValueLocation; count++)
                        {
                            // End of badges
                            if (count == endOfValueLocation)
                                break;

                            // Found badge name end and beginning of badge version
                            if (rawData[count] == '/')
                            {
                                // End of badge name found
                                badges.add(sb.toString());
                                badgeVers.add(Integer.parseInt(String.valueOf(rawData[++count])));
                                // Skip over the ,
                                count++;
                            }

                            else
                                sb.append(rawData[count]);
                        }

                        // Color
                        // Reset the stringBuilder
                        sb = new StringBuilder();

                        //                                ;color=
                        int colorStart = endOfValueLocation + 7;
                        endOfValueLocation = data.indexOf(';', colorStart - 1);

                        for (int count = colorStart ; count <= endOfValueLocation ; count++)
                        {
                            if (count == endOfValueLocation)
                                if (sb.length() <= 6)
                                {
                                    color = "NOCOLOR";
                                    break;
                                }
                                else
                                {
                                    color = sb.toString();
                                    break;
                                }

                            else
                                sb.append(rawData[count]);
                        }

                        users.add(new RemoteClient(badges, badgeVers, color, username));
                    }

                    // Get the client;
                    RemoteClient rClient = null;

                    for (RemoteClient remoteClient : users)
                        if (remoteClient.getUserName().equals(username))
                            rClient = remoteClient;

                    int messageStart = (data.indexOf(':', nameSeperator));
                    String message = data.substring(messageStart + 1);

                    System.out.print("> ");

                    for (String badge : rClient.getBadges())
                        System.out.print(badge + " ");

                    System.out.println(username + ": " + message);
                    // END TODO
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
