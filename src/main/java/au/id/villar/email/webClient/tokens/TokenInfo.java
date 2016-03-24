package au.id.villar.email.webClient.tokens;

import au.id.villar.email.webClient.domain.Role;

import java.util.Collection;

public interface TokenInfo {

    String getToken();

    boolean containsAtLeastOne(Collection<Role> roles);

    String getUsername();

    String getPassword();

    long getCreationTime();
}
