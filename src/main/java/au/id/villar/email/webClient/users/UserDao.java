package au.id.villar.email.webClient.users;

public interface UserDao {

    User find(String username);

    User find(int id);

    User create(String username, String password);

    void remove(int userId);

}
