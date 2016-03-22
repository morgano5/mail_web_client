package au.id.villar.email.webClient.dao;

import au.id.villar.email.webClient.model.User;
import org.apache.commons.codec.digest.Crypt;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Repository
public class UserDaoImpl implements UserDao {

    private static final Pattern HASH_PASSWORD_PATTERN = Pattern.compile("^(\\$[^$]+\\$[^$]+)");

    private EntityManager entityManager;

    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public User find(String username, String password) {
        try {
            User user = find(username);
            if(user == null) return null;
            Matcher matcher = HASH_PASSWORD_PATTERN.matcher(user.getPassword());
            if(!matcher.find())
                throw new RuntimeException(
                        "Password stored in database doesn't have a proper format for user #" + user.getId());
            String salt = matcher.group();
            String hash = Crypt.crypt(password, salt);
            return hash.equals(user.getPassword())? user: null;
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public User find(String username) {
        try {
            return  (User) entityManager
                    .createNamedQuery("user.findByUsername")
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public User find(int id) {
        return entityManager.find(User.class, id);
    }

    @Override
    public User create(String username, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        entityManager.persist(user);
        return user;
    }

    @Override
    public void remove(int userId) {
        User user = find(userId);
        entityManager.remove(user);
    }

}
