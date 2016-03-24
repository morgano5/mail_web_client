package au.id.villar.email.webClient.web;

import au.id.villar.email.webClient.TestAppConfig;
import au.id.villar.email.webClient.service.UserService;
import au.id.villar.email.webClient.spring.ServletAppConfig;
import au.id.villar.email.webClient.tokens.MemoryTokenService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = TestAppConfig.class)
public class AuthenticationHandlerInterceptorUnitTest {

    @Autowired
    UserService userService;

    @Test
    @Transactional
    public void basicTest() {
        AuthenticationHandlerInterceptor interceptor =
                new AuthenticationHandlerInterceptor(new MemoryTokenService(), userService, null);
    }

}
