package au.id.villar.email.webClient.service;

import au.id.villar.email.webClient.model.User;

public interface UserService {

    User find(String username, String password);

}
