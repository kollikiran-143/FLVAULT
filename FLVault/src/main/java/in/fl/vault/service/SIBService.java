package in.fl.vault.service;

import java.io.IOException;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface SIBService {
	
	public BSInfo parseSIB1(ParseBankStmtRequestDTO request);
	
	public BSInfo parseSIB2(ParseBankStmtRequestDTO request);
	
	public BSInfo parseSIB3(ParseBankStmtRequestDTO request);

	public BSInfo parseSIB4(ParseBankStmtRequestDTO request);
}
