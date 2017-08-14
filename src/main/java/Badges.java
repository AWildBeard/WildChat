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

import javafx.scene.image.Image;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Badges
{
    private static ArrayList<Image> downloadedBadges = new ArrayList<>();

    private static ArrayList<String> badgeNamesAtIndex = new ArrayList<>();

    // Prevent instantiation
    private Badges() {}

    public static ArrayList<String> getValidBadges()
    {
        return new ArrayList<>(Arrays.asList(
            "admin", "bits", "broadcaster", "global_mod", "moderator", "subscriber", "staff", "premium", "turbo"));
    }

    public static Map<String, String> getBadgesWithURLs()
    {
        Map<String, String> badgesWithURLs = new HashMap<>();

        // admin
        badgesWithURLs.put("admin/1", "http://static-cdn.jtvnw.net/badges/v1/9ef7e029-4cdf-4d4d-a0d5-e2b3fb2583fe/1");

        // bits
        badgesWithURLs.put("bits/1", "http://static-cdn.jtvnw.net/badges/v1/73b5c3fb-24f9-4a82-a852-2f475b59411c/1");
        badgesWithURLs.put("bits/100", "http://static-cdn.jtvnw.net/badges/v1/09d93036-e7ce-431c-9a9e-7044297133f2/1");
        badgesWithURLs.put("bits/1000", "http://static-cdn.jtvnw.net/badges/v1/0d85a29e-79ad-4c63-a285-3acd2c66f2ba/1");
        badgesWithURLs.put("bits/10000", "http://static-cdn.jtvnw.net/badges/v1/68af213b-a771-4124-b6e3-9bb6d98aa732/1");
        badgesWithURLs.put("bits/100000", "http://static-cdn.jtvnw.net/badges/v1/96f0540f-aa63-49e1-a8b3-259ece3bd098/1");
        badgesWithURLs.put("bits/1000000", "http://static-cdn.jtvnw.net/badges/v1/494d1c8e-c3b2-4d88-8528-baff57c9bd3f/1");
        badgesWithURLs.put("bits/200000", "http://static-cdn.jtvnw.net/badges/v1/4a0b90c4-e4ef-407f-84fe-36b14aebdbb6/1");
        badgesWithURLs.put("bits/25000", "http://static-cdn.jtvnw.net/badges/v1/64ca5920-c663-4bd8-bfb1-751b4caea2dd/1");
        badgesWithURLs.put("bits/300000", "http://static-cdn.jtvnw.net/badges/v1/ac13372d-2e94-41d1-ae11-ecd677f69bb6/1");
        badgesWithURLs.put("bits/400000", "http://static-cdn.jtvnw.net/badges/v1/a8f393af-76e6-4aa2-9dd0-7dcc1c34f036/1");
        badgesWithURLs.put("bits/5000", "http://static-cdn.jtvnw.net/badges/v1/57cd97fc-3e9e-4c6d-9d41-60147137234e/1");
        badgesWithURLs.put("bits/50000", "http://static-cdn.jtvnw.net/badges/v1/62310ba7-9916-4235-9eba-40110d67f85d/1");
        badgesWithURLs.put("bits/500000", "http://static-cdn.jtvnw.net/badges/v1/f6932b57-6a6e-4062-a770-dfbd9f4302e5/1");
        badgesWithURLs.put("bits/600000", "http://static-cdn.jtvnw.net/badges/v1/4d908059-f91c-4aef-9acb-634434f4c32e/1");
        badgesWithURLs.put("bits/700000", "http://static-cdn.jtvnw.net/badges/v1/a1d2a824-f216-4b9f-9642-3de8ed370957/1");
        badgesWithURLs.put("bits/75000", "http://static-cdn.jtvnw.net/badges/v1/ce491fa4-b24f-4f3b-b6ff-44b080202792/1");
        badgesWithURLs.put("bits/800000", "http://static-cdn.jtvnw.net/badges/v1/5ec2ee3e-5633-4c2a-8e77-77473fe409e6/1");
        badgesWithURLs.put("bits/900000", "http://static-cdn.jtvnw.net/badges/v1/088c58c6-7c38-45ba-8f73-63ef24189b84/1");

        // broadcaster
        badgesWithURLs.put("broadcaster/1", "http://static-cdn.jtvnw.net/badges/v1/5527c58c-fb7d-422d-b71b-f309dcb85cc1/1");

        // global_mod
        badgesWithURLs.put("global_mod/1", "http://static-cdn.jtvnw.net/badges/v1/9384c43e-4ce7-4e94-b2a1-b93656896eba/1");

        // moderator
        badgesWithURLs.put("moderator/1", "http://static-cdn.jtvnw.net/badges/v1/3267646d-33f0-4b17-b3df-f923a41db1d0/1");

        // subscriber
        badgesWithURLs.put("subscriber/0", "http://static-cdn.jtvnw.net/badges/v1/5d9f2208-5dd8-11e7-8513-2ff4adfae661/1");
        badgesWithURLs.put("subscriber/1", "http://static-cdn.jtvnw.net/badges/v1/5d9f2208-5dd8-11e7-8513-2ff4adfae661/1");

        // staff
        badgesWithURLs.put("staff/1", "http://static-cdn.jtvnw.net/badges/v1/d97c37bd-a6f5-4c38-8f57-4e4bef88af34/1");

        // premium
        badgesWithURLs.put("premium/1", "http://static-cdn.jtvnw.net/badges/v1/a1dd5073-19c3-4911-8cb4-c464a7bc1510/1");

        // turbo
        badgesWithURLs.put("turbo/1", "http://static-cdn.jtvnw.net/badges/v1/bd444ec6-8f34-4bf9-91f4-af1e3428d80f/1");

        return badgesWithURLs;
    }

    public static String getValue(String key) { return getBadgesWithURLs().get(key); }

    public static void cacheBadge(Image badge, String badgeName)
    {
        if (badge != null && badgeName != null)
        {
            downloadedBadges.add(badge);
            badgeNamesAtIndex.add(badgeName);
        }
    }

    public static boolean hasBadge(String badgeName) { return badgeName != null && badgeNamesAtIndex.contains(badgeName);}

    public static Image getBadge(String badgeName)
    {
        if (badgeName != null)
            return downloadedBadges.get(badgeNamesAtIndex.indexOf(badgeName));

        throw new IllegalArgumentException("You did not give me a badgeName!");
    }
}
