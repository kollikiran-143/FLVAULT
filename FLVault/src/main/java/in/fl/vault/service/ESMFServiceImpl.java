package in.fl.vault.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;
import in.fl.vault.response.Transaction;
import in.fl.vault.utils.CommonUtils;

@Service
public class ESMFServiceImpl implements ESMFService{

	private final static Logger log = Logger.getLogger(ESMFServiceImpl.class);

	@Override
	public BSInfo parseESMF1(ParseBankStmtRequestDTO request) throws IOException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering ESMFServiceImpl parseESMF1 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		try {
			String filepath = request.getFileName();
			String text = CommonUtils.extractTextFromPdf(filepath, "3");

			bankStatementInfo.setName(CommonUtils.extractField(text, "Name\\s*:\\s*(.*)\\s*Branch").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*Code\\s*:\\s*(\\S*)"));
			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*Number\\s*:\\s*(\\S*)"));
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*of\\s*Ownership\\s*:\\s*(.*)\\n").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setPhone1(CommonUtils.extractField(text, "Phone\\s*No\\s*:\\s*(\\S*)"));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "Name\\s*:\\s*.*\\n([\\s\\S]*?)\\s*Statement\\s*of", 90));

			String dateFormat = "dd/MM/yyyy";
			bankStatementInfo.setStartDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "Statement.*?Account\\n\\s*From\\s*:\\s*(\\S*)"), dateFormat));
			bankStatementInfo.setEnDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "Statement.*?Account\\n\\s*From.*?To\\s*:\\s*(\\S*)"), dateFormat));
			bankStatementInfo.setTransactions(getTransactionsESMF_1(text, bankStatementInfo.getAccountNo(), dateFormat));
		} catch (Exception e) {
			log.error("Error in ESMFServiceImpl parseESMF1: ", e);
		}

		log.info("Exiting ESMFServiceImpl parseESMF1: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for ESMFServiceImpl is ==>" + timeTaken);
		return bankStatementInfo;
	}

	private List<Transaction> getTransactionsESMF_1(String pdfText, String accountNo, String dateFormat) throws IOException {
		List<Transaction> transactions = new ArrayList<>();

		Pattern pattern = Pattern.compile(
				"\\s+(\\d{2}\\/\\d{2}\\/\\d{4})\\s+(\\d{2}\\/\\d{2}\\/\\d{4})(.*?)\\s+([\\d,]+\\.\\d{2})(\\s+)([\\d,]+\\.\\d{2})([\\s\\S]*?)(?=\\s+\\d{2}\\/\\d{2}\\/\\d{4}|\\s+Unless|\\s+Statement)");
		Matcher matcher = pattern.matcher(pdfText);
		int serialNoCount = 1;

		while (matcher.find()) {
			Transaction transaction = new Transaction();
			transaction.setsNo(String.valueOf(serialNoCount++));
			transaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(1), dateFormat));
			transaction.setValueDate(CommonUtils.dateFormatter(matcher.group(2), dateFormat));
			String description = matcher.group(3) + " " + matcher.group(7);
			transaction.setDescription(description.replaceAll("\\n", " ").replaceAll("\\s+", " ").trim());
			transaction.setAmount(matcher.group(4));
			if (matcher.group(5).length() > 27) {
				transaction.setDebit(transaction.getAmount());
				transaction.setCredit("");
				transaction.setTxnType("DEBIT");
			} else {
				transaction.setDebit("");
				transaction.setCredit(transaction.getAmount());
				transaction.setTxnType("CREDIT");
			}
			transaction.setBalance(matcher.group(6));
			transaction.setAccNo(accountNo);
			transactions.add(transaction);
		}
		return transactions;
	}

}
