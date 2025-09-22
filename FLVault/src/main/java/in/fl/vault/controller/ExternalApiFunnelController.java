package in.fl.vault.controller;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import in.fl.vault.request.VGParserFunnelRequestDTO;
import in.fl.vault.response.ExternalApiFunnelResponseDTO;
import in.fl.vault.service.ExternalApiFunnelService;
import in.fl.vault.utils.CommonUtils;

@RestController
public class ExternalApiFunnelController {
	
	@Autowired
	private ExternalApiFunnelService externalApiFunnelService;
	
	private final Logger log = Logger.getLogger(ExternalApiFunnelController.class);
	
	@RequestMapping(value = "/getVGParserFunnelData", method = RequestMethod.POST, consumes = { "application/json" })
	public @ResponseBody ExternalApiFunnelResponseDTO getOlaDistanceApiFunnelData(
			@RequestBody VGParserFunnelRequestDTO requestDTO) throws Exception {
		CommonUtils.mdcPut("CustomerId", "-1234");
		log.info("Entering ExternalApiFunnelController getVGParserFunnelData: " + requestDTO);

		ExternalApiFunnelResponseDTO response = externalApiFunnelService.getVGParserFunnelData(requestDTO);

		log.info("Exiting ExternalApiFunnelController getVGParserFunnelData Response: " + response);
		MDC.remove("CustomerId");
		return response;
	}
}
