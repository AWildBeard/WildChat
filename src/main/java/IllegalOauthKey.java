public class IllegalOauthKey extends IllegalArgumentException
{
    private final String MESSAGE;

    public IllegalOauthKey(final String MESSAGE) { this.MESSAGE = MESSAGE; }

    @Override
    public String getMessage() { return MESSAGE; }
}
