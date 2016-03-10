package au.id.villar.email.webClient;

import java.sql.SQLException;

public interface UserDao {

	boolean authOk(String username, String password) throws SQLException;

}
