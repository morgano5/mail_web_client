package au.id.villar.email.webClient.tokens;

public class MemoryTokenService implements TokenService {

    @Override
    public String createToken(String... pemissions) {
        // TODO implement
        return null;
    }

    @Override
    public String[] retrievePermissions(String token) {
        // TODO implement
        return new String[0];
    }

    @Override
    public void removeToken() {
        // TODO implement

    }
}
