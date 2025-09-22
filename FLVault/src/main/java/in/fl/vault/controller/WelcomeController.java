package in.fl.vault.controller;


import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WelcomeController {

	private final Logger log = Logger.getLogger(WelcomeController.class);
	
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public @ResponseBody String greet() {
		log.info("Enter Test controller getGreet");
		return "<html><head><title>FL Vault</title><style>body {background-color: #D1F23D;text-align: center;color: #7800cc;font-family: Arial, Helvetica, sans-serif;position: fixed;top: 50%;left: 50%;margin-top: -100px;margin-left: -200px;}</style></head><body><h1>Welcome to FL Vault</h1></body></html>";
	}
}
