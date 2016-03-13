package au.id.villar.email.webClient.spring;

import au.id.villar.email.webClient.web.JSONMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.List;
import java.util.Properties;

@Configuration
@EnableWebMvc
@EnableTransactionManagement
@ComponentScan(basePackages = "au.id.villar.email.webClient" )
@PropertySource(value = {"classpath:/default.properties", "file:${au.id.villar.config}"},
        ignoreResourceNotFound = true)
@Import(DbConfig.class)
public class AppConfig extends /*WebMvcConfigurationSupport*/ WebMvcConfigurerAdapter {

    @Autowired
    Environment env;




//-----------------------------

	@Bean
	public JSONMessageConverter getJsonMessageConverter() {
		return new JSONMessageConverter();
	}

	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		converters.add(getJsonMessageConverter());
//		addDefaultHttpMessageConverters(converters);
	}

}
