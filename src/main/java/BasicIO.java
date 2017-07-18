import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;

/**
 * @author Clownvin
 * @author AWildBeard
 */

public class BasicIO
{

    // Do not instantiate
    private BasicIO() {}

    public static boolean isEOLChar(int b) { return b == 0x0A || b == 0x0B || b == 0x0C || b == 0x0D; }

    public static char[] readLine(InputStream stream)
    {
        char[] chars = new char[1];
        int index = 0;

        try
        {
            // Remove extra EOL chars (sometimes there can be multiples)
            while (isEOLChar((chars[index++] = (char) stream.read())))
                index--;

            // Make room for while loop
            chars = Arrays.copyOf(chars, index + 1);

            // While chars[index] is not an EOL character, read the next char in and increase the size
            // of the char array by 1
            while (! isEOLChar((chars[index++] = (char) stream.read())))
                chars = Arrays.copyOf(chars, index + 1);
        } catch (IOException e)
        {
            e.printStackTrace(System.out);
            System.out.println(e.getMessage());
            return chars;
        }

        return chars;
    }

    public static String readURL(String stringURL)
    {
        BufferedReader reader = null;
        StringBuffer buffer = new StringBuffer();

        try
        {
            URL url = new URL(stringURL);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));

            char[] chars = new char[1024];
            int readLength;

            while ((readLength = reader.read(chars)) != -1)
                buffer.append(chars, 0, readLength);

            reader.close();
        } catch (IOException e)
        {
            System.out.println(e.getMessage());
        }

        return buffer.toString();
    }
}
