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
import java.io.Serializable;

public class UISettings implements Serializable
{
    private String textFill = "#e5e6ea", backgroundColor = "#25262b",
            highlightColor = "#1e90ff", UIAccentColor = "#383c4a",
            highlightTextColor = "#000000", actionColor = "#68f218",
            whisperTextColor = "#f7ea00", messagePrefix = "> ";

    private double messageFontSize = 14.0, uiFont = 14.0;

    private int windowWidth = 650, windowHeight = 400;

    public UISettings() {}

    public String getMessagePrefix()
    {
        return messagePrefix;
    }

    public void setMessagePrefix(String incomingMessagePrefix)
    {
        messagePrefix = incomingMessagePrefix;
    }

    public String getTextFill()
    {
        return textFill;
    }

    public void setTextFill(String incomingTextFill)
    {
        textFill = incomingTextFill;
    }

    public String getBackgroundColor()
    {
        return backgroundColor;
    }

    public void setBackgroundColor(String incommingBGColor)
    {
        backgroundColor = incommingBGColor;
    }

    public String getHighlightColor()
    {
        return highlightColor;
    }

    public void setHighlightColor(String incommingHLColor)
    {
        highlightColor = incommingHLColor;
    }

    public String getUIAccentColor()
    {
        return UIAccentColor;
    }

    public void setUIAccentColor(String incommingUIAccColor)
    {
        UIAccentColor = incommingUIAccColor;
    }

    public String getHighlightTextColor()
    {
        return highlightTextColor;
    }

    public void setHighlightTextColor(String incommingHighLiTxtColor)
    {
        highlightTextColor = incommingHighLiTxtColor;
    }

    public double getMessageFontSize()
    {
        return messageFontSize;
    }

    public void setMessageFontSize(double incommingMsgFntSize)
    {
        messageFontSize = incommingMsgFntSize;
    }

    public double getUiFont()
    {
        return uiFont;
    }

    public void setUiFont(double incommingUIFont)
    {
        uiFont = incommingUIFont;
    }

    public String getActionColor()
    {
        return actionColor;
    }

    public void setActionColor(String incommingActionColor)
    {
        actionColor = incommingActionColor;
    }

    public String getWhisperTextColor()
    {
        return whisperTextColor;
    }

    public void setWhisperTextColor(String incommingWhispTxtClr)
    {
        this.whisperTextColor = incommingWhispTxtClr;
    }

    public int getWindowWidth()
    {
        return windowWidth;
    }

    public void setWindowWidth(int incommingWindowWidth)
    {
        windowWidth = incommingWindowWidth;
    }

    public int getWindowHeight()
    {
        return windowHeight;
    }

    public void setWindowHeight(int incommingWindowHeight)
    {
        windowHeight = incommingWindowHeight;
    }
}
