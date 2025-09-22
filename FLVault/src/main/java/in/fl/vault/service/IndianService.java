package in.fl.vault.service;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface IndianService {

	BSInfo parseINDIAN1(ParseBankStmtRequestDTO request);

	BSInfo parseINDIAN2(ParseBankStmtRequestDTO request);

}
