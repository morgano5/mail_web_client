package au.id.villar.email.webClient.web;

import au.id.villar.email.webClient.TestAppConfig;
import au.id.villar.email.webClient.domain.Role;
import au.id.villar.email.webClient.domain.User;
import au.id.villar.email.webClient.service.UserService;
import au.id.villar.email.webClient.tokens.AuthenticationHandlerInterceptor;
import au.id.villar.email.webClient.tokens.MemoryTokenService;
import au.id.villar.email.webClient.tokens.PermissionsResolver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = TestAppConfig.class)
public class AuthenticationHandlerInterceptorUnitTest {

    @Autowired
    UserService userService;

    @Test
    @Transactional
    public void basicTest() {

        PermissionsResolver resolver = (String username, String password) -> {
            User user = userService.find(username, password);
            if(user == null) return null;
            return user.getRoles().stream().map(Role::name).collect(Collectors.toSet());
        };

        AuthenticationHandlerInterceptor interceptor =
                new AuthenticationHandlerInterceptor(new MemoryTokenService(), resolver, Permissions.class, "value",
                        null);
    }

}
