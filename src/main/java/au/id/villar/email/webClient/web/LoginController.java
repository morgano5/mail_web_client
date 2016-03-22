package au.id.villar.email.webClient.web;

import au.id.villar.email.webClient.domain.Role;
import au.id.villar.email.webClient.service.UserService;
import au.id.villar.email.webClient.tokens.Login;
import au.id.villar.email.webClient.tokens.Logout;
import au.id.villar.email.webClient.tokens.Permissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
    public @ResponseBody String createToken(@RequestBody Map<String, String> credentials, HttpServletResponse response) {
        //String username, String password
        response.setHeader("X-Token", "xxx");
        System.out.println(" NEWTOKEN INPUT >>> " + credentials);
        return "{\"TOMATUTOKEN\": 0}";
    }

    @Logout
    @RequestMapping(method = RequestMethod.DELETE,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String deleteToken(@RequestBody String token) {
        System.out.println(" DELTOKEN INPUT >>> " + token);
        return "TOKENBORRADO";
    }

    @Permissions(Role.ADMINISTRATOR)
    @RequestMapping(path = "/testing", method = RequestMethod.GET)
    public void testing() {}


//	@RequestMapping(value = "/data/memoryUsage", method = RequestMethod.GET,
//			produces = MediaType.APPLICATION_JSON_VALUE)
//	public @ResponseBody
//	Map<String, Long> getMemoryUsage() {
//		Map<String, Long> info = new HashMap<>(3);
//		Runtime runtime = Runtime.getRuntime();
//		info.put("total", runtime.totalMemory());
//		info.put("free", runtime.freeMemory());
//		info.put("max", runtime.maxMemory());
//		return info;
//	}

}
