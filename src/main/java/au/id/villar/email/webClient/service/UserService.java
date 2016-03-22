package au.id.villar.email.webClient.service;

import au.id.villar.email.webClient.domain.Role;
import au.id.villar.email.webClient.domain.User;

public interface UserService {

    User find(String username, String password);

    User create(String username, String password, boolean active, Role ... roles);

}
