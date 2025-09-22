package in.fl.vault.service;

import java.io.IOException;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface ESMFService {

	BSInfo parseESMF1(ParseBankStmtRequestDTO request) throws IOException;

}
