package au.id.villar.email.webClient.spring;

import au.id.villar.email.webClient.tokens.MemoryTokenService;
import au.id.villar.email.webClient.tokens.TokenService;
import au.id.villar.email.webClient.web.AuthenticationHandlerInterceptor;
import au.id.villar.email.webClient.web.JSONMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.List;

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = "au.id.villar.email.webClient.web")
public class ServletAppConfig extends /*WebMvcConfigurationSupport*/ WebMvcConfigurerAdapter {

//    @Bean
    public JSONMessageConverter getJsonMessageConverter() {
        return new JSONMessageConverter();
    }

//    @Bean
    public TokenService getTokenService() {
        return new MemoryTokenService();
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(getJsonMessageConverter());
//		addDefaultHttpMessageConverters(converters);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthenticationHandlerInterceptor(getTokenService(), ServletAppConfig.class));
    }
}
