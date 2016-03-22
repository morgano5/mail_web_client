package au.id.villar.email.webClient.dao;

import au.id.villar.email.webClient.TestAppConfig;
import au.id.villar.email.webClient.domain.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = TestAppConfig.class)
@Transactional
public class UserDaoUnitTest {

    @Autowired
    UserDao dao;

    @Test
    public void authentication() {
        User user = dao.create("user", "password");

        assertNull(dao.find("user", "password"));

        user.setActive(true);

        assertNotNull(dao.find("user", "password"));
        assertNull(dao.find("user", "password2"));
        assertNull(dao.find("user2", "password"));
    }

    @Test
    public void createUserAndFindByUsername() {
        User newUser = dao.create("test", "password");
        User user = dao.find("test");

        assertNotNull(user);
        assertEquals(newUser.getUsername(), user.getUsername());
        assertNull(dao.find("test2"));
        dao.remove(user.getId());
    }

    @Test
    public void find() {
        User user = dao.find(-1);
        assertNull(user);
    }

}
