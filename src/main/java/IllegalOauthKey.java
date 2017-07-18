public class IllegalOauthKey extends IllegalArgumentException
{

    private String message;

    public IllegalOauthKey(String message) { this.message = message; }

    public String getMessage() { return message; }
}
