package in.fl.vault.controller;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.ParseBankStmtResponseDTO;
import in.fl.vault.service.StmtService;
import in.fl.vault.utils.CommonUtils;
import in.fl.vault.utils.WebConstants;

@RestController
public class ParseBankStmtController {

	@Autowired
	private StmtService stmtService;

	private final Logger log = Logger.getLogger(ParseBankStmtController.class);

	@RequestMapping(value = "/parseBankStatement", method = RequestMethod.POST, consumes = { "application/json" })
	public @ResponseBody ParseBankStmtResponseDTO parseBankStatement(@RequestBody ParseBankStmtRequestDTO request) throws Exception {
		CommonUtils.mdcPut("CustomerId", request.getCustomerId());
		log.info("Entering ParseBankStmtController parseBankStatement: " + request);
		ParseBankStmtResponseDTO response = new ParseBankStmtResponseDTO();
		if(request.getCustomerId() == null || request.getCustomerId().equalsIgnoreCase("")
				|| request.getFileName() == null || request.getFileName().equalsIgnoreCase("")) {
			response.setStatusCode(WebConstants.FAILURE_CODE);
			response.setStatusMessage("Please provide all the details.");
		}else {
			response = stmtService.parseStatement(request);

		}
		log.info("ParseBankStmtController parseBankStatement Response: " + response);
		MDC.remove("CustomerId");
		return response;
	}
}
