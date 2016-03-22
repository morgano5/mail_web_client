package au.id.villar.email.webClient.dao;

import au.id.villar.email.webClient.model.User;

public interface UserDao {

    User find(String username, String password);

    User find(String username);

    User find(int id);

    User create(String username, String password);

    void remove(int userId);

}
