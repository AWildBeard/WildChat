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

import java.io.Serializable;

// Data holder for the user of the program
public class Client implements Serializable
{
    private String oauth = null, nick = null;

    public Client()
    {
        oauth = null;
        nick = null;
    }

    public Client(String oauth, String nick)
    {
        setOauth(oauth);
        setNick(nick);
    }

    // Accessors
    public String getNick()
    {
        return this.nick;
    }

    public void setNick(String nick)
    {
        this.nick = nick.toLowerCase().trim();
    }

    public String getOauth()
    {
        return this.oauth;
    }

    // Mutators
    public void setOauth(String oauth)
    {
        //  Must be a length of 30
        if ((oauth = oauth.trim()).length() != 30)
        {
            throw new IllegalOauthKey(
                    "OAUTH token is the wrong length. " + "Need: 30 Received: " + oauth.length());
        }

        this.oauth = oauth;
    }

    public boolean isReady()
    {
        return (oauth != null && nick != null);
    }
}
