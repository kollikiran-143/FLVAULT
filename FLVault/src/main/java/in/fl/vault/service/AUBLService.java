package in.fl.vault.service;

import java.io.IOException;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface AUBLService {
	public BSInfo parseAUBL1(ParseBankStmtRequestDTO request) throws IOException;

	public BSInfo parseAUBL2(ParseBankStmtRequestDTO request) throws IOException;

	public BSInfo parseAUBL3(ParseBankStmtRequestDTO request);

}
