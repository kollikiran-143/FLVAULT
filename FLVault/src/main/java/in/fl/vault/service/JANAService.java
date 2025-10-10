package in.fl.vault.service;

import java.io.IOException;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface JANAService {

	public BSInfo parseJANA1(ParseBankStmtRequestDTO request) throws IOException;
}
