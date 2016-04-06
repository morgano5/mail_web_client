package au.id.villar.email.webClient.users;

public interface UserService {

    User find(String username, String password);

    User create(String username, String password, boolean active, Role ... roles);

}
