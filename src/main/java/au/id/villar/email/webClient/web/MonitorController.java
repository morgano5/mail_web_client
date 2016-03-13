package au.id.villar.email.webClient.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class MonitorController {

	public MonitorController() {
		System.out.println("zz        ");
	}

	@RequestMapping(value = "/data/test", method = RequestMethod.GET,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody
	List<Integer> test() {
		return Arrays.asList(2, 4, 6, 8, 10);
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
