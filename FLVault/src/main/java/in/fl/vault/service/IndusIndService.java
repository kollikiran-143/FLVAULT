package in.fl.vault.service;

import java.io.IOException;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface IndusIndService {
	
	public BSInfo parseIndusInd1(ParseBankStmtRequestDTO request) throws IOException;
	
	public BSInfo parseIndusInd2(ParseBankStmtRequestDTO request) throws IOException;
	
	public BSInfo parseIndusInd3(ParseBankStmtRequestDTO request) throws IOException;
	
	public BSInfo parseIndusInd4(ParseBankStmtRequestDTO request) throws IOException;
	
	public BSInfo parseIndusInd5(ParseBankStmtRequestDTO request) throws IOException;

	public BSInfo parseIndusInd6(ParseBankStmtRequestDTO request);

	public BSInfo parseIndusInd7(ParseBankStmtRequestDTO request);
}
