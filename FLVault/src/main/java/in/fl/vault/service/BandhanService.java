package in.fl.vault.service;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface BandhanService {
	
	public BSInfo parseBandhan1(ParseBankStmtRequestDTO request);

	public BSInfo parseBandhan2(ParseBankStmtRequestDTO request);

	public BSInfo parseBandhan3(ParseBankStmtRequestDTO request);
}
