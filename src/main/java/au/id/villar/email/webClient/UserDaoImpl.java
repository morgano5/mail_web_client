package au.id.villar.email.webClient;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDaoImpl implements UserDao {

	private DataSource dataSource;

	public UserDaoImpl(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public boolean authOk(String username, String password) throws SQLException {
		boolean r = false;
		try(
				Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"select ENCRYPT(?, substring(password, 1, 19)) = password " +
								"from user where username = ?")) {

			statement.setString(1, password);
			statement.setString(2, username);

			try(ResultSet resultSet = statement.executeQuery()) {
				return resultSet.next() && resultSet.getBoolean(1);
			}
		}
	}


}
