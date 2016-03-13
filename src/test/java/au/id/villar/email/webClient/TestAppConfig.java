package au.id.villar.email.webClient;

import au.id.villar.email.webClient.spring.DbConfig;
import org.springframework.context.annotation.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@PropertySource("classpath:/default.properties")
@Import(DbConfig.class)
public class TestAppConfig {

}
