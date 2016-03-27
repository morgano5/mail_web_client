package au.id.villar.email.webClient.tokens;

import java.util.Set;

public interface PermissionsResolver {

    Set<String> resolve(String username, String password);

}
