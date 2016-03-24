package au.id.villar.email.webClient.web;

import au.id.villar.email.webClient.domain.User;
import au.id.villar.email.webClient.service.UserService;
import au.id.villar.email.webClient.tokens.Login;
import au.id.villar.email.webClient.tokens.Logout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Controller("/login")
public class LoginController {

    private UserService userService;

    @Autowired
    public LoginController(UserService userService) {
        this.userService = userService;
    }

    @Login
    @RequestMapping(method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public void createToken(@RequestBody Map<String, String> credentials, HttpServletRequest request) {
        String username = credentials.get("username");
        String password = credentials.get("password");
        if(username == null | password == null) throw new UnauthorizedException();

        User user = userService.find(username, password);
        if(user == null) throw new UnauthorizedException();

        request.setAttribute("user", user);
        request.setAttribute("password", password);
    }

    @Logout
    @RequestMapping(method = RequestMethod.DELETE,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public void deleteToken() {
    }

}
