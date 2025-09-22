package in.fl.vault.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import in.fl.vault.repository.FlvaultAuditRepository;
import in.fl.vault.request.VGParserFunnelRequestDTO;
import in.fl.vault.response.ExternalApiFunnelResponseDTO;
import in.fl.vault.utils.WebConstants;

@Service
public class ExternalApiFunnelServiceImpl implements ExternalApiFunnelService{
	
	@Autowired
	private FlvaultAuditRepository flvaultAuditRepository;
	
	private final Logger log = Logger.getLogger(ExternalApiFunnelServiceImpl.class);
	
	@Override
	public ExternalApiFunnelResponseDTO getVGParserFunnelData(VGParserFunnelRequestDTO requestDTO) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering ExternalApiFunnelServiceImpl getVGParserFunnelData with request: "+ requestDTO);
		
		ExternalApiFunnelResponseDTO response = new ExternalApiFunnelResponseDTO();

		SimpleDateFormat reqFormat = new SimpleDateFormat("yyyy-MM-dd");
		int requested = 0;
		int success = 0;
		int failure = 0;
		
		try {
			Date reportDate = new Date();
			if (requestDTO.getReportDate() != null && !requestDTO.getReportDate().equalsIgnoreCase("")) {
				reportDate = reqFormat.parse(requestDTO.getReportDate());
			} else {
				reportDate = reqFormat.parse(reqFormat.format(reportDate));
			}
			log.info("ExternalApiFunnelServiceImpl getVGParserFunnelData reportDate: " + reqFormat.format(reportDate));

			requested = flvaultAuditRepository.totalRequested(reportDate);
			success = flvaultAuditRepository.totalSuccessCount(reportDate);
			failure = requested - success;
			
			response.setRequested(String.valueOf(requested));
			response.setSuccess(String.valueOf(success));
			response.setFailure(String.valueOf(failure));
			response.setStatusCode(WebConstants.SUCCESS_CODE);
			response.setStatusMessage(WebConstants.SUCCESS_MSG);

		} catch (ParseException e) {
			log.info("Error in ExternalApiFunnelServiceImpl getVGParserFunnelData: " + e.getMessage());
			response.setStatusCode(WebConstants.FAILURE_CODE);
			response.setStatusMessage(e.getMessage());
		}

		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("ExternalApiFunnelServiceImpl getVGParserFunnelData Response: " + response);
		log.info("Time Taken for ExternalApiFunnelServiceImpl getVGParserFunnelData is ==>" + timeTaken);
		return response;
	}

}
