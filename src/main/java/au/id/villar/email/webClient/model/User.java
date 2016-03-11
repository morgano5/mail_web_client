package au.id.villar.email.webClient.model;

import javax.persistence.*;

@Entity
@Table(name = "user")
@NamedQueries({
		@NamedQuery(
				name = "user.findByUsername",
				query = "select u from User u where u.username = :username"
		)
})
@NamedNativeQueries({
		@NamedNativeQuery(
				name = "user.hashPassword",
				query = "UPDATE user SET password = ENCRYPT(password, CONCAT('$6$', SUBSTRING(SHA(RAND()), -16))) WHERE id = :id"),

		@NamedNativeQuery(
				name = "user.checkPassword",
				query = "SELECT ENCRYPT(:password, SUBSTRING(password, 1, 19)) = password FROM user WHERE username = :username"
		)
})
public class User {

	private int id;
	private String username;
	private String password;
	private String email;
	private boolean active;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Column(nullable = false, length = 100)
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	@Column(nullable = false, length = 106)
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Column(nullable = false, length = 100)
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Column(nullable = false)
	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}
