// Data holder for the user of the program
public class Client
{
    private String oauth, nick;

    public Client(String oauth, String nick)
    {
        setOauth(oauth);
        setNick(nick);
    }

    public void setOauth(String oauth)
    {
        // Check for malformed oauth:
        if (oauth.substring(0, 5).equals("oauth:"))
            oauth = oauth.substring(6);

        //  Must be a length of 30
        if (oauth.length() < 30 || oauth.length() > 30)
            throw new IllegalOauthKey("OAUTH toke is the wrong length. Need: 30 Received: " + oauth.length());

        this.oauth = oauth;
    }

    public void setNick(String nick)
    {
        this.nick = nick.toLowerCase();
    }

    public String getNick() { return this.nick; }

    public String getOauth() { return this.oauth; }
}
