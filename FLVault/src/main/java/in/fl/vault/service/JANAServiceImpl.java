package in.fl.vault.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;
import in.fl.vault.response.Transaction;
import in.fl.vault.utils.CommonUtils;

@Service
public class JANAServiceImpl implements JANAService {

	private final Logger log = Logger.getLogger(JANAServiceImpl.class);

	@Override
	public BSInfo parseJANA1(ParseBankStmtRequestDTO request) throws IOException {

		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering JANAServiceImpl parseJANA1 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String text = CommonUtils.extractTextFromPdf(filePath, "7");

			bankStatementInfo.setName(CommonUtils.extractField(text, "\\s*(.*?)\\s*CR\\s*N\\s+").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*Number\\s*:\\s*(\\S*)"));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "CR\\s*N\\s+.*?\\n([\\s\\S]+?)\\n\\s*JointHolder1", 0, 62));
			bankStatementInfo.setNominee(CommonUtils.extractField(text, "Nominee\\s*:\\s*(.*?)\\s*BranchAddress").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setPhone1(CommonUtils.extractField(text, "MobileNo\\s*:\\s*(\\S*)"));
			bankStatementInfo.setEmail(CommonUtils.extractField(text, "EmailId\\s*:\\s*(\\S*)"));
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*Nam\\s*e\\s*:\\s*(.*?)\\n").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*C\\s*ode\\s*:\\s*(\\S*)"));

			String dateFormat = "dd/MM/yyyy";
			bankStatementInfo.setStartDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "Statement\\s*Period\\s*:\\s*(\\d{2}\\/\\d{2}\\/\\d{4})\\s*to"), dateFormat));
			bankStatementInfo.setEnDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "Statement\\s*Period\\s*:\\s*.*?to\\s*(\\d{2}\\/\\d{2}\\/\\d{4})"), dateFormat));
			List<Transaction> transactions = extractTransactionsJANA_1(filePath, bankStatementInfo.getAccountNo(), dateFormat);
			bankStatementInfo.setTransactions(transactions);

		} catch (Exception e) {
//        	e.printStackTrace();
			log.error("Error in JANAServiceImpl parseJANA1: " + e);
		}

		log.info("Exiting JANAServiceImpl parseJANA1: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for JANAServiceImpl parseJANA1 is ==>" + timeTaken);
		return bankStatementInfo;
	}

	private List<Transaction> extractTransactionsJANA_1(String fileName, String accountNo, String dateFormat) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);

		int serialNoCount = 1;
		if (txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}
				String[] data = line.split("\\|");

				if (data.length == 6 && !data[0].equals("Txn Date")) {
					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat));
					transaction.setDescription(data[1]);
					transaction.setTxnId(data[2]);

					if (!(data[4].equalsIgnoreCase("") || data[4].equals("-") || data[4].equalsIgnoreCase("0.00"))) {
						transaction.setAmount(data[4]);
						transaction.setDebit(data[4]);
						transaction.setTxnType("DEBIT");
						transaction.setCredit("");
					} else {
						transaction.setAmount(data[3]);
						transaction.setCredit(data[3]);
						transaction.setTxnType("CREDIT");
						transaction.setDebit("");
					}
					transaction.setBalance(data[5]);
					transaction.setAccNo(accountNo);
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}

}
