package in.fl.vault.service;

import java.io.IOException;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface PNBService {
	
	public BSInfo parsePNB1(ParseBankStmtRequestDTO request) throws IOException;

	BSInfo parsePNB2(ParseBankStmtRequestDTO request) throws IOException;

	BSInfo parsePNB3(ParseBankStmtRequestDTO request);

	public BSInfo parsePNB4(ParseBankStmtRequestDTO request);

	BSInfo parsePNB5(ParseBankStmtRequestDTO request);
}	
