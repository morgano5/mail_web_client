package au.id.villar.email.webClient;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ComponentScan(basePackages = { "au.id.villar.email.webClient" })
@PropertySource("classpath:/properties.xml")
public class AppConfig {

}
