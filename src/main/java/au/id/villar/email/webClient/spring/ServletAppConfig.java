package au.id.villar.email.webClient.spring;

import au.id.villar.email.webClient.domain.User;
import au.id.villar.email.webClient.service.UserService;
import au.id.villar.email.webClient.tokens.*;
import au.id.villar.email.webClient.web.JSONMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.util.List;

@Configuration
@ComponentScan(basePackages = "au.id.villar.email.webClient.web")
public class ServletAppConfig extends WebMvcConfigurationSupport {

    @Autowired
    private UserService userService;

//    @Bean
    public JSONMessageConverter getJsonMessageConverter() {
        return new JSONMessageConverter();
    }

//    @Bean
    public TokenService tokenService() {
        return new MemoryTokenService();
    }

    private PermissionsResolver permissionsResolver() {
        return ((username, password) -> {
            User user;
            return ((user = userService.find(username, password)) != null)? user.getRoles(): null;
        });
    }

    @Override
    protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(getJsonMessageConverter());
//		addDefaultHttpMessageConverters(converters);
    }

    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthenticationHandlerInterceptor(tokenService(), permissionsResolver(), this));
    }

    @Override
    protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new AuthenticationArgumentResolver());
    }

    //    @Bean
//    public RequestMappingHandlerMapping requestMappingHandlerMapping() {
//        return super.requestMappingHandlerMapping();
//    }
}
