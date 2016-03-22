package au.id.villar.email.webClient.service;

import au.id.villar.email.webClient.dao.UserDao;
import au.id.villar.email.webClient.model.User;

public class UserServiceImpl implements UserService {

    private UserDao dao;

    public UserServiceImpl(UserDao dao) {
        this.dao = dao;
    }

    @Override
    public User find(String username, String password) {
        return dao.find(username, password);
    }
}
