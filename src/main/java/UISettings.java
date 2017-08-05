import java.io.Serializable;

public class UISettings implements Serializable
{
    private  String textFill = "#e5e6ea", backgroundColor = "#25262b",
        highlightColor = "#1e90ff", UIAccentColor = "#383c4a", highlightTextColor = "#000000";

    private  double messageFontSize = 14.0, uiFont = 14.0;

    public UISettings() {}

    // mutators
    public void setTextFill(String textFill)
    {
        this.textFill = textFill;
    }

    public void setBackgroundColor(String backgroundColor)
    {
        this.backgroundColor = backgroundColor;
    }

    public void setHighlightColor(String highlightColor)
    {
        this.highlightColor = highlightColor;
    }

    public void setUIAccentColor(String UIAccentColor)
    {
        this.UIAccentColor = UIAccentColor;
    }

    public void setHighlightTextColor(String highlightTextColor)
    {
        this.highlightTextColor = highlightTextColor;
    }

    public void setMessageFontSize(double messageFontSize)
    {
        this.messageFontSize = messageFontSize;
    }

    public void setUiFont(double uiFont)
    {
        this.uiFont = uiFont;
    }

    // accessors
    public String getTextFill()
    {
        return textFill;
    }

    public String getBackgroundColor()
    {
        return backgroundColor;
    }

    public String getHighlightColor()
    {
        return highlightColor;
    }

    public String getUIAccentColor()
    {
        return UIAccentColor;
    }

    public String getHighlightTextColor()
    {
        return highlightTextColor;
    }

    public double getMessageFontSize()
    {
        return messageFontSize;
    }

    public double getUiFont()
    {
        return uiFont;
    }
}
