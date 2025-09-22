package in.fl.vault.service;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface BOIService {
	public BSInfo parseBOI1(ParseBankStmtRequestDTO request);

	public BSInfo parseBOI2(ParseBankStmtRequestDTO request);
}
