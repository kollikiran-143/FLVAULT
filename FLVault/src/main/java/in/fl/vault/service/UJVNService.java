package in.fl.vault.service;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface UJVNService {
	BSInfo parseUJVN1(ParseBankStmtRequestDTO request);
}
