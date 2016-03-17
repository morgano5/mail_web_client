package au.id.villar.email.webClient.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller("/login")
public class LoginController {


	@RequestMapping(method = RequestMethod.POST,
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody String createToken(@RequestBody Map<String, String> credentials) {
		//String username, String password
		System.out.println(" NEWTOKEN INPUT >>> " + credentials);
		return "TOMATUTOKEN";
	}


	@RequestMapping(method = RequestMethod.DELETE,
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody String deleteToken(@RequestBody String token) {
		System.out.println(" DELTOKEN INPUT >>> " + token);
		return "TOKENBORRADO";
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
