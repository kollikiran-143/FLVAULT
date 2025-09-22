package in.fl.vault.service;

import java.io.IOException;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface SURYService {

	public BSInfo parseSURY1(ParseBankStmtRequestDTO request) throws IOException;
}
