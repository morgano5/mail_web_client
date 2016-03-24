package au.id.villar.email.webClient.spring;

import au.id.villar.email.webClient.service.UserService;
import au.id.villar.email.webClient.tokens.MemoryTokenService;
import au.id.villar.email.webClient.tokens.TokenService;
import au.id.villar.email.webClient.web.AuthenticationHandlerInterceptor;
import au.id.villar.email.webClient.web.JSONMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.Map;

@Configuration
//@EnableWebMvc
@ComponentScan(basePackages = "au.id.villar.email.webClient.web")
public class ServletAppConfig extends WebMvcConfigurationSupport /*WebMvcConfigurerAdapter*/ {

    @Autowired
    private UserService userService;

//    @Bean
    public JSONMessageConverter getJsonMessageConverter() {
        return new JSONMessageConverter();
    }

//    @Bean
    public TokenService getTokenService() {
        return new MemoryTokenService();
    }

    @Override
    protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(getJsonMessageConverter());
//		addDefaultHttpMessageConverters(converters);
    }

    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthenticationHandlerInterceptor(getTokenService(), userService, this));
    }

//    @Bean
//    public RequestMappingHandlerMapping requestMappingHandlerMapping() {
//        return super.requestMappingHandlerMapping();
//    }
}
