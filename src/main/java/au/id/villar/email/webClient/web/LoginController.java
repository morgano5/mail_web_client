package au.id.villar.email.webClient.web;

import au.id.villar.email.webClient.domain.Role;
import au.id.villar.email.webClient.domain.User;
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

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/login")
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
    public @ResponseBody String createToken(@RequestBody Map<String, String> credentials, HttpServletRequest request) {

        String username = credentials.get("username");
        String password = credentials.get("password");
        if(username == null | password == null) throw new UnauthorizedException();

        User user = userService.find(username, password);
        if(user == null) throw new UnauthorizedException();
        List<String> permissionList = user.getRoles().stream().map(Role::toString).collect(Collectors.toList());
        String[] permissions = permissionList.toArray(new String[permissionList.size()]);

        request.setAttribute("username", username);
        request.setAttribute("password", password);
        request.setAttribute("permissions", permissions);

        return "{\"errorCode\": \"OK\"}";
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
    @RequestMapping(value = "/testing", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String testing() {
        return "{\"TEST\": \"OK\"}";
    }


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
