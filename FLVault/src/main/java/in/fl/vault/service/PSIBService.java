package in.fl.vault.service;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;

public interface PSIBService {

	BSInfo parsePSIB1(ParseBankStmtRequestDTO request);

	BSInfo parsePSIB2(ParseBankStmtRequestDTO request);

}
