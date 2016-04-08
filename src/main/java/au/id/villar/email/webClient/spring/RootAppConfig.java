package au.id.villar.email.webClient.spring;

import au.id.villar.email.webClient.mail.MailboxService;
import au.id.villar.email.webClient.mail.MailboxServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@ComponentScan({"au.id.villar.email.webClient.users", "au.id.villar.email.webClient.mail"})
@PropertySource(value = {"classpath:/default.properties", "file:${au.id.villar.config}"}, ignoreResourceNotFound = true)
@Import(DbConfig.class)
public class RootAppConfig {

    @Autowired
    Environment env;

    @Bean
    MailboxService mailboxService() {
        return new MailboxServiceImpl(env.getProperty("au.id.villar.mail.server"), null);
    }
}
