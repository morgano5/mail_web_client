package au.id.villar.email.webClient.tokens;

import java.util.Set;

@FunctionalInterface
public interface PermissionsResolver {

    Set<String> resolve(String username, String password);

}
