package au.id.villar.email.webClient.service;

import au.id.villar.email.webClient.dao.UserDao;
import au.id.villar.email.webClient.domain.Role;
import au.id.villar.email.webClient.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService {

    private UserDao dao;

    @Autowired
    public UserServiceImpl(UserDao dao) {
        this.dao = dao;
    }

    @Override
    public User find(String username, String password) {
        return dao.find(username, password);
    }

    @Transactional
    @Override
    public User create(String username, String password, boolean active, Role... roles) {
        User user = dao.create(username, password);
        user.setActive(active);
        Set<Role> roleSet = new HashSet<>(roles.length);
        Collections.addAll(roleSet, roles);
        user.setRoles(roleSet);
        return user;
    }
}
