public class IllegalOauthKey extends IllegalArgumentException
{
    private String message;

    public IllegalOauthKey(String message) { this.message = message; }

    @Override
    public String getMessage() { return message; }
}
