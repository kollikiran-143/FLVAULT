package in.fl.vault.service;

import java.io.IOException;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface HDFCService {
	
	public BSInfo parseHDFC1(ParseBankStmtRequestDTO request);
	
	public BSInfo parseHDFC2(ParseBankStmtRequestDTO request) throws IOException;

	public BSInfo parseHDFC3(ParseBankStmtRequestDTO request);
}
