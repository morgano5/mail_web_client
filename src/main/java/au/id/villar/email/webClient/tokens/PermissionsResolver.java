package au.id.villar.email.webClient.tokens;

import au.id.villar.email.webClient.domain.Role;

import java.util.Set;

public interface PermissionsResolver {

    Set<Role> resolve(String username, String password);

}
