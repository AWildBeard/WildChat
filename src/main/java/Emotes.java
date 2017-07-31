import javafx.scene.image.Image;

import java.util.ArrayList;

public class Emotes
{
    private static ArrayList<Image> downloadedEmotes = new ArrayList<>();

    private static ArrayList<String> emoteIDAtIndex = new ArrayList<>();

    // Prevent instantiation
    private Emotes() {}

    public static void cacheEmote(Image emote, String emoteID)
    {
        if (! emoteIDAtIndex.contains(emoteID))
        {
            downloadedEmotes.add(emote);
            emoteIDAtIndex.add(emoteID);
        }
    }

    public static boolean hasEmote(String emoteID) { return emoteID != null && emoteIDAtIndex.contains(emoteID); }

    public static Image getEmote(String emoteID) { return downloadedEmotes.get(emoteIDAtIndex.indexOf(emoteID)); }
}
