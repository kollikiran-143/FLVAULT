package in.fl.vault.service;

import java.io.IOException;

import in.fl.vault.request.ParseBankStmtRequestDTO;

public interface DetectBankService {
	
	public String detectBank(String filePath) throws IOException, InterruptedException;
	
	public Boolean checkFakeBankStmt(ParseBankStmtRequestDTO request, String fileUrl) throws IOException;
}
