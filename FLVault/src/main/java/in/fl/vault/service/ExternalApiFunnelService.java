package in.fl.vault.service;

import in.fl.vault.request.VGParserFunnelRequestDTO;
import in.fl.vault.response.ExternalApiFunnelResponseDTO;

public interface ExternalApiFunnelService {
	
	ExternalApiFunnelResponseDTO getVGParserFunnelData(VGParserFunnelRequestDTO requestDTO);
}
