package au.id.villar.email.webClient.web;

import au.id.villar.email.webClient.spring.config.Login;
import au.id.villar.email.webClient.spring.config.Logout;
import au.id.villar.email.webClient.tokens.UserPasswordHolder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

@Controller("/login")
public class LoginController {

    @Login
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public void createToken(@RequestBody Map<String, String> credentials, UserPasswordHolder userPasswordHolder) {

        String username = credentials.get("username");
        String password = credentials.get("password");

        userPasswordHolder.setUsername(username);
        userPasswordHolder.setPassword(password);
    }

    @Logout
    @RequestMapping(method = RequestMethod.DELETE)
    public void deleteToken() {
    }

}
