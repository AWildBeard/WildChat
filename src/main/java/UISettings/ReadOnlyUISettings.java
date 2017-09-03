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

package UISettings;

public class ReadOnlyUISettings
{
    private static UISettings settings = null;
    private static boolean init = false;

    private ReadOnlyUISettings() {}

    public static String getMessagePrefix()
    {
        return settings.getMessagePrefix();
    }

    public static String getTextFill()
    {
        return settings.getTextFill();
    }

    public static String getActionColor()
    {
        return settings.getActionColor();
    }

    public static String getBackgroundColor()
    {
        return settings.getBackgroundColor();
    }

    public static String getHighlightColor()
    {
        return settings.getHighlightColor();
    }

    public static String getUIAccentColor()
    {
        return settings.getUIAccentColor();
    }

    public static String getHighlightTextColor()
    {
        return settings.getHighlightTextColor();
    }

    public static String getWhisperTextColor()
    {
        return settings.getWhisperTextColor();
    }

    public static double getMessageFontSize()
    {
        return settings.getMessageFontSize();
    }

    public static double getUiFont()
    {
        return settings.getUiFont();
    }

    public static int getWindowWidth()
    {
        return settings.getWindowWidth();
    }

    public static int getWindowHeight()
    {
        return settings.getWindowHeight();
    }

    public static boolean isInitialized()
    {
        return init;
    }

    public static void setSettings(UISettings uiSettings)
    {
        settings = uiSettings;
        init = true;
    }
}
