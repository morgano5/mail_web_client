package au.id.villar.email.webClient.tokens;

import java.util.Collection;

public interface TokenInfo {

    String getToken();

    boolean containsAtLeastOne(Collection<String> roles);

    String getUsername();

    String getPassword();

    long getCreationTime();
}
