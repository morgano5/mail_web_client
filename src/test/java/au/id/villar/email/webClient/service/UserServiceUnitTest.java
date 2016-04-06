package au.id.villar.email.webClient.service;

import au.id.villar.email.webClient.TestAppConfig;
import au.id.villar.email.webClient.users.User;
import au.id.villar.email.webClient.users.UserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = TestAppConfig.class)
@Transactional
public class UserServiceUnitTest {

    @Autowired
    UserService service;

    @Test
    public void authentication() {
        User user = service.create("user", "password", false);

        assertNull(service.find("user", "password"));

        user.setActive(true);

        assertNotNull(service.find("user", "password"));
        assertNull(service.find("user", "password2"));
        assertNull(service.find("user2", "password"));
    }

}
