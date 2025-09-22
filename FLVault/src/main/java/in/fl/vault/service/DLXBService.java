package in.fl.vault.service;


import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface DLXBService {
	BSInfo parseDLXB1(ParseBankStmtRequestDTO request);

	BSInfo parseDLXB2(ParseBankStmtRequestDTO request);
}
