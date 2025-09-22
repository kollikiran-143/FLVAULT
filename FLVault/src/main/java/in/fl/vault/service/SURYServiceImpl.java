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
public class SURYServiceImpl implements SURYService {

	private final Logger log = Logger.getLogger(SURYServiceImpl.class);

	@Override
	public BSInfo parseSURY1(ParseBankStmtRequestDTO request) throws IOException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering SBIServiceImpl parseSURY1 with request: " + request);

		BSInfo bankstatementInfo = new BSInfo();
		String filepath = request.getFileName();

		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "4");
			bankstatementInfo.setName(CommonUtils.extractField(text, "To.*?\\n\\s*(.{55})").replaceAll("\\s+", " ").trim());
			bankstatementInfo.setAccountNo(CommonUtils.extractField(text, "Currency[\\s\\S]*?\\d{2}-\\d{2}-\\d{4}.*?(\\d{8,})\\s+"));
			bankstatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*:\\s*(\\S+)"));
			bankstatementInfo.setNominee(CommonUtils.extractField(text, "Nominee\\s+Name\\s*:\\s*(.*)\\s*\\n").replaceAll("\\s+", " "));

			String dateFormat = "dd-MM-yyyy";
			bankstatementInfo.setStartDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "PERIOD\\s+OF\\s+(\\d{2}-\\d{2}-\\d{4})"), dateFormat));
			bankstatementInfo.setEnDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "PERIOD\\s+OF.+?To\\s*(\\d{2}-\\d{2}-\\d{4})"), dateFormat));
			List<Transaction> transactions = extractTransactionsSURY_1(filepath, bankstatementInfo.getAccountNo(), dateFormat);
			bankstatementInfo.setTransactions(transactions);

		} catch (Exception e) {
			log.error("Error in SBIServiceImpl parseSURY1: " + e);
		}

		log.info("Exiting SBIServiceImpl parseSURY1: " + bankstatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for SBIServiceImpl parseSURY1 is: " + timeTaken);
		return bankstatementInfo;
	}

	private List<Transaction> extractTransactionsSURY_1(String fileName, String accountNo, String dateFormat) throws IOException {
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

				if (data.length == 7 && !data[0].equals("Tran Date")) {
					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat));
					transaction.setValueDate(CommonUtils.dateFormatter(data[1], dateFormat));
					transaction.setDescription(data[3]);

					if (!(data[4].equalsIgnoreCase("") || data[4].equals("-"))) { // Debit
						transaction.setAmount(data[4]);
						transaction.setDebit(data[4]);
						transaction.setCredit("");
						transaction.setTxnType("DEBIT");
					} else {
						transaction.setAmount(data[5]);
						transaction.setCredit(data[5]);
						transaction.setTxnType("CREDIT");
						transaction.setDebit("");
					}
					transaction.setBalance(data[6]);
					transaction.setAccNo(accountNo);
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}
}
