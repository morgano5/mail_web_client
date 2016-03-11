package au.id.villar.email.webClient.dao;

import au.id.villar.email.webClient.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.sql.SQLException;

public interface UserDao {

	boolean authOk(String username, String password) throws SQLException;

	void changePassword(String username, String password) throws SQLException;

}
