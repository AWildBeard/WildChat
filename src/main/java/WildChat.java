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

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class WildChat extends Application
{
    private static String[] launchArgs;

    private TwitchConnect socketRunner;

    private Thread th = new Thread(socketRunner);

    private boolean credentialError = false;

    public WildChat()
    {
        File dotDir = new File("$HOME/.WildChat");
        File credentialsFile = new File(dotDir.getPath() + dotDir.getName() + "/credentials");
        if (! credentialsFile.canRead() && ! credentialsFile.canWrite())
        {
            if (dotDir.mkdir() || dotDir.exists())
            {
                try
                {
                    if (! credentialsFile.createNewFile())
                        credentialError = true;
                } catch (IOException e)
                {
                    credentialError = true;
                }
            }
            else
                credentialError = true;
        }
        else
        {
        }
    }

    public void start(Stage primaryStage)
    {
        if (credentialError)
        {
            VBox content = new VBox();
            Text message = new Text("Failed to read/ write/ create credential file");
            Button closeButton = new Button("Okay");

            content.getChildren().addAll(message, closeButton);

            closeButton.setOnAction(e -> System.exit(1));

            content.setAlignment(Pos.CENTER);
            content.setSpacing(10.0);

            Scene root = new Scene(content, 400, 300);

            primaryStage.setScene(root);
            primaryStage.show();
        }
    }
    public static void main(String[] args)
    {
        launchArgs = args;
        launch(args);
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

            // Exactly part, join, or msg
            if (part || join || msg)
            {
                // The exact index of the userName end delimeter
                if (msg)
                {
                    StringBuilder sb = new StringBuilder();
                    char[] rawData = data.toCharArray();
                    int categoryStart;
                    int endOfCategoryLocation = 0;

                    System.out.print("> ");

                    // Test for badges
                    categoryStart = data.indexOf("badges=") + 7; // Always 7. Length of badges declaration
                    endOfCategoryLocation = data.indexOf(';', categoryStart);

                    if (! (categoryStart == endOfCategoryLocation))
                    { // Message has badges data
                        for (int count = categoryStart; count <= endOfCategoryLocation; count++)
                        {
                            // End of badges
                            if (count == endOfCategoryLocation)
                                break;

                            // Found badge name end and beginning of badge version
                            if (rawData[count] == '/')
                            {
                                // End of badge name found
                                System.out.print(sb.toString() + "/");
                                count++; // Skip the /

                                while (rawData[count] != ',' && rawData[count] != ';')
                                    System.out.print(rawData[count++]);

                                System.out.print(" ");
                                sb = new StringBuilder(); // Clear the StringBuilder
                            }
                            else
                                sb.append(rawData[count]);
                        }
                    }

                    // Test for color
                    categoryStart = data.indexOf("color=") + 6; // Always 6. Length of color declaration
                    endOfCategoryLocation = data.indexOf(';', categoryStart);

                    if (! (categoryStart == endOfCategoryLocation))
                    { // Message has color data
                        sb = new StringBuilder(); // Clear the StringBuilder
                        for (int count = categoryStart ; count <= endOfCategoryLocation ; count++)
                        {
                            if (count == endOfCategoryLocation) // The end of the color field
                            {
                                System.out.print(sb.toString() + " ");
                                break;
                            }

                            else
                                sb.append(rawData[count]);
                        }
                    }

                    // Grab the username
                    categoryStart = data.indexOf("display-name=") + 13;
                    endOfCategoryLocation = data.indexOf(';', categoryStart);
                    String username = data.substring(categoryStart, endOfCategoryLocation);

                    // Test for emotes
                    categoryStart = data.indexOf("emotes=", endOfCategoryLocation) + 7; // 7 emotes= length
                    endOfCategoryLocation = data.indexOf(';', categoryStart);

                    if (! (categoryStart == endOfCategoryLocation))
                    { // Message has emote data
                        System.out.print("[ignoring emotes] ");
                    }

                    // Grab the message
                    categoryStart = (data.indexOf(':', data.indexOf("PRIVMSG")));
                    String message = data.substring(categoryStart + 1);

                    System.out.println(username + ": " + message);
                    return;
                    // END TODO
                }

                int nameStart = (data.indexOf(':'));
                int endOfNameLocation = (data.indexOf('!', nameStart));
                String username = data.substring(nameStart + 1, endOfNameLocation);

                if (part)
                    System.out.println("> " + username + " left the channel");

                else
                    System.out.println("> " + username + " joined the channel");

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
