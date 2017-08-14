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
import java.util.HashMap;
import java.util.Map;

public class Session
{
    private static String channel = null;
    private String clientColor = null, clientDisplayName = null;

    private ArrayList<String> badgeSignatures = null;

    private Map<String, String> emoteCodesAndIDs = new HashMap<>();

    boolean mapSet = false;

    public Session() { clearData(); }

    public static String getChannel() { return channel; }

    public ArrayList<String> getBadgeSignatures() { return badgeSignatures; }

    public String getClientColor() { return clientColor; }

    public String getClientDisplayName() { return clientDisplayName; }

    public Map<String, String> getEmoteCodesAndIDs() { return emoteCodesAndIDs; }

    public boolean isMapSet() { return mapSet; }

    public void setChannel(String newChannel) { channel = newChannel;}

    public void setBadgeSignatures(ArrayList<String> newBadgeSigs) { badgeSignatures = newBadgeSigs; }

    public void setClientColor(String newClientColor) { clientColor = newClientColor; }

    public void setClientDisplayName(String newClientDisplayName)
    { clientDisplayName = newClientDisplayName; }

    public void setEmoteCodesAndIDs(Map<String, String> emoteCodesAndIDs)
    {
        if (mapSet)
            return;

        this.emoteCodesAndIDs = emoteCodesAndIDs;
        mapSet = true;
    }

    public static void clearData()
    {
        channel = null;
    }
}
