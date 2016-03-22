package au.id.villar.email.webClient.spring;

import au.id.villar.email.webClient.tokens.MemoryTokenService;
import au.id.villar.email.webClient.tokens.TokenService;
import au.id.villar.email.webClient.web.JSONMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.List;

@Configuration
@EnableTransactionManagement
@ComponentScan(basePackages = "au.id.villar.email.webClient.service")
@PropertySource(value = {"classpath:/default.properties", "file:${au.id.villar.config}"},
        ignoreResourceNotFound = true)
@Import(DbConfig.class)
public class RootAppConfig {

    @Autowired
    Environment env;

    @Bean
    public TokenService getTokenService() {
        return new MemoryTokenService();
    }





}
