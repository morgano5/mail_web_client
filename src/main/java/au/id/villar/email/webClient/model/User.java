package au.id.villar.email.webClient.model;

import au.id.villar.email.webClient.dao.UserDao;

import javax.persistence.*;

@Entity
@Table(name = "user")
@NamedQueries(
        @NamedQuery(
                name = "user.findByUsername",
                query = "select u from User u where u.username = :username"
        )
)
@NamedNativeQueries({

        @NamedNativeQuery(
                name = "user.findByUsernameAndPassword",
                query = "SELECT * from user where ENCRYPT(:password, SUBSTRING(password, 1, 19)) = password and username = :username LIMIT 1",
                resultSetMapping = "user"
        ),

        @NamedNativeQuery(
                name = "user.getHashedPassword",
                query = "SELECT ENCRYPT(:password, CONCAT('$6$', SUBSTRING(SHA(RAND()), -16)))"
        )

})
@SqlResultSetMapping(name = "user", entities = @EntityResult(entityClass = User.class))
public class User {

    private int id;
    private String username;
    private String password;
    private boolean active;
    private int version;
    private UserDao dao;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(nullable = false, length = 100, unique = true)
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
        if(dao != null) {
            this.password = dao.hashPassword(password != null? password: "");
        }
    }

    @Column(nullable = false)
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Version
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Transient
    public void setDao(UserDao dao) {
        this.dao = dao;
    }

}
