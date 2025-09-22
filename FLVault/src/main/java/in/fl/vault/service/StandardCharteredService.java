package in.fl.vault.service;

import java.io.IOException;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface StandardCharteredService {
	public BSInfo parseSTND1(ParseBankStmtRequestDTO request) throws IOException, InterruptedException;
	
	public BSInfo parseSTND2(ParseBankStmtRequestDTO request) throws IOException, InterruptedException;

	BSInfo parseSTND3(ParseBankStmtRequestDTO request) throws IOException;
}
