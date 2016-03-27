package au.id.villar.email.webClient.tokens;

import java.util.Collection;

public interface TokenService {

    TokenInfo createToken(String username, String password, Collection<String> roles);

    TokenInfo getTokenInfo(String token);

    void removeToken(String token);

    long getExpiryTimeMillis();
}
