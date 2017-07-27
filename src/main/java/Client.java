import javafx.application.Platform;

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

    // Mutators
    public void setOauth(String oauth)
    {
        //  Must be a length of 30
        if ((oauth = oauth.trim()).length() != 30)
            throw new IllegalOauthKey("OAUTH token is the wrong length. Need: 30 Received: " + oauth.length());

        this.oauth = oauth;
    }

    public void setNick(String nick)
    {
        this.nick = nick.toLowerCase().trim();
    }

    // Accessors
    public String getNick() { return this.nick; }

    public String getOauth() { return this.oauth; }

    public boolean isReady() { return (oauth != null && nick != null);}
}
