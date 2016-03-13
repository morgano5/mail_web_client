package au.id.villar.email.webClient.dao;

import au.id.villar.email.webClient.model.User;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

@Repository
public class UserDaoImpl implements UserDao {

    private EntityManager entityManager;

    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public User find(String username, String password) {
        try {
            User user = entityManager
                    .createNamedQuery("user.findByUsernameAndPassword", User.class)
                    .setParameter("username", username)
                    .setParameter("password", password)
                    .getSingleResult();
            user.setDao(this);
            return user;
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public User find(String username) {
        try {
            User user = (User) entityManager
                    .createNamedQuery("user.findByUsername")
                    .setParameter("username", username)
                    .getSingleResult();
            user.setDao(this);
            return user;
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public User find(int id) {
        User user = entityManager.find(User.class, id);
        if(user != null) user.setDao(this);
        return user;
    }

    @Override
    public User create(String username, String password) {
        User user = new User();
        user.setDao(this);
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

    @Override
    public String hashPassword(String password) {
        byte[] hash = (byte[])entityManager
                .createNamedQuery("user.getHashedPassword")
                .setParameter("password", password)
                .getSingleResult();
        return new String(hash);
    }
}
