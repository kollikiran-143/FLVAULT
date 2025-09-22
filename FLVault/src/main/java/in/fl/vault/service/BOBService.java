package in.fl.vault.service;

import java.io.IOException;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface BOBService {
	public BSInfo parseBOB1(ParseBankStmtRequestDTO request) throws IOException;
	
	public BSInfo parseBOB2(ParseBankStmtRequestDTO request) throws IOException;

	public BSInfo parseBOB3(ParseBankStmtRequestDTO request) throws IOException;
	
	public BSInfo parseBOB4(ParseBankStmtRequestDTO request) throws IOException;
	
}
