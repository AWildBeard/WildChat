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

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Scanner;

public class TwitchChatter
{
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
            System.out.println("Received>>> " + socketRunner.getData());
        });

        System.out.println("Attempting to start connection...");
        th.start();

        DataOutputStream os = null;
        boolean initialized = false;
        String command;

        while (true)
        {
            command = kb.nextLine();
            if (!initialized)
            {
                os = socketRunner.getOutputStream();
                initialized = true;
            }

            try
            {
                os.write(command.getBytes());
            }
            catch (IOException e)
            {
                System.out.println(e.getMessage());
                System.out.println("Failed to send command: " + command);
            }

        }

    }
}
