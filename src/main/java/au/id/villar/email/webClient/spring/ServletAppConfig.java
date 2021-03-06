package au.id.villar.email.webClient.spring;

import au.id.villar.email.webClient.spring.config.AuthenticationArgumentResolver;
import au.id.villar.email.webClient.spring.config.AuthenticationHandlerInterceptor;
import au.id.villar.email.webClient.users.Role;
import au.id.villar.email.webClient.users.User;
import au.id.villar.email.webClient.users.UserService;
import au.id.villar.email.webClient.tokens.*;
import au.id.villar.email.webClient.spring.config.JSONMessageConverter;
import au.id.villar.email.webClient.spring.config.Permissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
@ComponentScan("au.id.villar.email.webClient.web")
public class ServletAppConfig extends WebMvcConfigurationSupport {

    @Autowired
    private UserService userService;

    @Override
    protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new JSONMessageConverter());
    }

    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(
                new AuthenticationHandlerInterceptor(
                        tokenService(), permissionsResolver(), Permissions.class, "value", this));
    }

    @Override
    protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new AuthenticationArgumentResolver());
    }

    private TokenService tokenService() {
        return new MemoryTokenService();
    }

    private PermissionsResolver permissionsResolver() {
        return ((username, password) -> {
            User user = userService.find(username, password);
            if(user == null) return null;
            return user.getRoles().stream().map(Role::name).collect(Collectors.toSet());
        });
    }

}
