package au.id.villar.email.webClient.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@ComponentScan("au.id.villar.email.webClient.service")
@PropertySource(value = {"classpath:/default.properties", "file:${au.id.villar.config}"}, ignoreResourceNotFound = true)
@Import(DbConfig.class)
public class RootAppConfig {

    @Autowired
    Environment env;

}
