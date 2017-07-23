// Data holder for the user of the program
public class Client
{
    private String oauth = null, nick = null;

    public Client(String oauth, String nick)
    {
        setOauth(oauth);
        setNick(nick);
    }

    public void setOauth(String oauth)
    {
        //  Must be a length of 30
        if ((oauth = oauth.trim()).length() < 30)
            throw new IllegalOauthKey("OAUTH token is the wrong length. Need: 30 Received: " + oauth.length());

        // Check for malformed oauth:
        if (oauth.substring(0, 6).equals("oauth:"))
            oauth = oauth.substring(6);

        this.oauth = oauth;
    }

    public void setNick(String nick)
    {
        this.nick = nick.toLowerCase().trim();
    }

    public String getNick() { return this.nick; }

    public String getOauth() { return this.oauth; }

    public boolean isReady() { return (oauth != null && nick != null);}
}
