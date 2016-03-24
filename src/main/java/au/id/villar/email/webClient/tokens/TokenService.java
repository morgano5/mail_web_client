package au.id.villar.email.webClient.tokens;

import au.id.villar.email.webClient.domain.Role;

import java.util.Collection;

public interface TokenService {

    TokenInfo createToken(String username, String password, Collection<Role> roles);

    TokenInfo getTokenInfo(String token);

    void removeToken(String token);

    long getExpiryTimeMillis();
}
