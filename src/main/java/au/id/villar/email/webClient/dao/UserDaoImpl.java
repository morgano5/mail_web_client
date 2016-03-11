package au.id.villar.email.webClient.dao;

import au.id.villar.email.webClient.model.User;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class UserDaoImpl implements UserDao {

	private EntityManager entityManager;

	private DataSource dataSource;

	public UserDaoImpl(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public UserDaoImpl(DataSource dataSource) {
		this.dataSource = dataSource;
	}


	@Override
	public boolean authOk(String username, String password) throws SQLException {

		List<Boolean> passedOrEmpty = entityManager.createNamedQuery("user.checkPassword", Boolean.class)
				.setParameter("username", username)
				.setParameter("password", password)
				.setMaxResults(1).getResultList();




		boolean r = false;
		try(
				Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"select ENCRYPT(?, substring(password, 1, 19)) = password from user where username = ?")) {

			statement.setString(1, password);
			statement.setString(2, username);

			try(ResultSet resultSet = statement.executeQuery()) {
				return resultSet.next() && resultSet.getBoolean(1);
			}
		}
	}

	@Override
	public void changePassword(String username, String password) throws SQLException {
		try {

			User user = entityManager.createNamedQuery("user.findByUsername", User.class)
					.setParameter("username", username).getSingleResult();
			user.setPassword(password);
			entityManager.createNamedQuery("user.hashPassword").setParameter("id", user.getId()).executeUpdate();
			entityManager.clear();

		} catch (NoResultException e) {
			throw new SQLException(e);
		}

	}
}
