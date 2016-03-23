package au.id.villar.email.webClient.service;

import au.id.villar.email.webClient.dao.UserDao;
import au.id.villar.email.webClient.domain.Role;
import au.id.villar.email.webClient.domain.User;
import org.apache.commons.codec.digest.Crypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.NoResultException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Pattern HASH_PASSWORD_PATTERN = Pattern.compile("^(\\$[^$]+\\$[^$]+)");

    private UserDao dao;

    @Autowired
    public UserServiceImpl(UserDao dao) {
        this.dao = dao;
    }

    @Override
    public User find(String username, String password) {
        try {
            User user = dao.find(username);
            if(user == null || !user.isActive()) return null;
            Matcher matcher = HASH_PASSWORD_PATTERN.matcher(user.getPassword());
            if(!matcher.find())
                throw new RuntimeException(
                        "Password stored in database doesn't have a proper format for user #" + user.getId());
            String salt = matcher.group();
            String hash = Crypt.crypt(password, salt);
            if(!hash.equals(user.getPassword())) return null;
            user.getRoles().size(); // to force loading of roles
            return user;
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public User create(String username, String password, boolean active, Role... roles) {
        String salt = String.format("$6$%016X", new Random().nextLong());
        password = Crypt.crypt(password, salt);
        User user = dao.create(username, password);
        user.setActive(active);
        Set<Role> roleSet = new HashSet<>(roles.length);
        Collections.addAll(roleSet, roles);
        user.setRoles(roleSet);
        return user;
    }
}
