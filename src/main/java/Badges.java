import java.util.ArrayList;
import java.util.Arrays;

public class Badges
{
    // Prevent instantiation
    private Badges(){}

    public static ArrayList<String> getValidBadges()
    {
        return new ArrayList<>(Arrays.asList(
            "admin", "bits", "broadcaster", "global_mod", "moderator", "subscriber", "staff", "premium"));
    }
}
