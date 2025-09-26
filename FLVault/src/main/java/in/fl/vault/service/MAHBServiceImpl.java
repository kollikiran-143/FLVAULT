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
public class MAHBServiceImpl implements MAHBService{
	
	private final static Logger log = Logger.getLogger(MAHBServiceImpl.class);
	
	@Override
	public BSInfo parseMAHB1(ParseBankStmtRequestDTO request) throws IOException{

		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering MAHBServiceImpl parseMAHB1 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "5");
			String[] period = CommonUtils.extractMultiGroupArray(text, "Statement\\s*.*from(.*?)to(.*)");
			String accountNo = CommonUtils.extractField(text, "Account\\s*No\\s+(\\S+)");
			bankStatementInfo.setAccountNo(accountNo);
			bankStatementInfo.setName(CommonUtils.extractField(text, "Customer\\s*Details\\s*\\n\\s*Name(.*)CIF").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setPhone1(CommonUtils.extractField(text, "Mobile\\s+(\\S+)"));
			bankStatementInfo.setEmail(CommonUtils.extractField(text, "Email\\s+(\\S+)"));
			bankStatementInfo.setAddress(CommonUtils.extractField(text, "Address\\s+(.*)").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setNominee(CommonUtils.extractField(text, "Nominee\\s*Name\\s+(.*)").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*Name\\s+(.*)IFSC").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*(MAHB\\S+)"));
			
			String dateFormat = "dd/MM/yyyy";
			String txnDateFormat = "dd/MM/yyyy";
			if(period!=null) {
				bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
				bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
			}
			List<Transaction> txnList = extractTransactionsMAHB_1(text, accountNo, txnDateFormat);
//			if(txnList == null || txnList.size() == 0) {
//				log.info("Calling extractTransactionsMAHB_1_2");
//				txnList = extractTransactionsMAHB_1_2(filepath, accountNo, txnDateFormat);
//			}
			bankStatementInfo.setTransactions(txnList);
		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in MAHBServiceImpl parseMAHB1: " + e);
		}
		log.info("Exiting MAHBServiceImpl parseMAHB1: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for MAHBServiceImpl parseMAHB1 is ==>" + timeTaken);
		return bankStatementInfo;
	}
	
	@Override
	public BSInfo parseMAHB2(ParseBankStmtRequestDTO request) throws IOException{
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering MAHBServiceImpl parseMAHB2 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "3");

			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*No\\.:-\\s*(\\S*)"));
			bankStatementInfo.setName(
					CommonUtils.extractField(text, "Holder\\s*Name:-(.*)Branch").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setStartDate(CommonUtils
					.dateFormatter(CommonUtils.extractField(text, "Statement\\s*From:-(\\S*)"), "dd/MM/yyyy"));
			bankStatementInfo.setEnDate(CommonUtils
					.dateFormatter(CommonUtils.extractField(text, "Statement\\s*From.*?To\\s*(\\S*)"), "dd/MM/yyyy"));
			bankStatementInfo
					.setTransactions(extractTransactionsMAHB_2(text, bankStatementInfo.getAccountNo(), "dd-MMM-yyyy"));

		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in MAHBServiceImpl parseMAHB2: " + e);
		}
		log.info("Exiting MAHBServiceImpl parseMAHB2: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for MAHBServiceImpl parseMAHB2 is ==>" + timeTaken);
		return bankStatementInfo;
	}

	private List<Transaction> extractTransactionsMAHB_1(String pdfText, String accountNo, String dateFormat)
			throws IOException {
		List<Transaction> transactions = new ArrayList<>();

		Pattern pattern = Pattern.compile(
				"\\d+\\s+(\\d{2}\\/\\d{2}\\/\\d{4})\\s*(.*?)\\s*(\\S*)\\s+(-|[\\d,]+\\.\\d{2})\\s+(-|[\\d,]+\\.\\d{2})\\s+([\\d,]+\\.\\d{2})\\s+(.*)");
		Matcher matcher = pattern.matcher(pdfText);
		int serialNoCount = 1;

		while (matcher.find()) {
			Transaction transaction = new Transaction();
			transaction.setsNo(String.valueOf(serialNoCount++));
			transaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(1), dateFormat));
			transaction.setDescription(matcher.group(2).replaceAll("\\s+", " ").trim());
			transaction.setTxnId(matcher.group(3));
			transaction.setDebit(matcher.group(4));
			transaction.setCredit(matcher.group(5));
			if (transaction.getDebit().equals("") || transaction.getDebit().equals("-")) { // credit
				transaction.setDebit("");
				transaction.setAmount(transaction.getCredit());
				transaction.setTxnType("CREDIT");
			} else {
				transaction.setCredit("");
				transaction.setAmount(transaction.getDebit());
				transaction.setTxnType("DEBIT");
			}
			transaction.setBalance(matcher.group(6));
			if (transaction.getDescription().equals(""))
				transaction.setDescription(matcher.group(7).replaceAll("\\s+", " ").trim());
			transaction.setAccNo(accountNo);
			transactions.add(transaction);
		}
		return transactions;
	}
	
	private List<Transaction> extractTransactionsMAHB_1_2(String fileName, String accountNo, String dateFormat) throws IOException {
	List<Transaction> transactions = new ArrayList<>();
	List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
	Pattern datePattern = Pattern.compile("\\d{2}\\/\\d{2}\\/\\d{4}");
	int serialNoCount = 1;
	if (txnRows != null && !txnRows.isEmpty()) {
		for (String[] row : txnRows) {
	
			String line = String.join("|", row).replaceAll("\\r|\\s+", " ");
			if (line.trim().isEmpty()) {
				continue;
			}
	
			String[] data = line.split("\\|");
			Transaction transaction = new Transaction();
			Matcher matcher1 = datePattern.matcher(data[1]);
			Matcher matcher2 = null;
			if (data.length >= 16) {
				matcher2 = datePattern.matcher(data[3]);
			}
			if (data.length == 8 && !data[0].equals("Sr No") && matcher1.find()) {
				System.out.println(line);
				transaction.setsNo(Integer.toString(serialNoCount++));
				transaction.setTxnDate(CommonUtils.dateFormatter(data[1], dateFormat));
				transaction.setDescription(data[2]);
				transaction.setDebit(data[4]);
				transaction.setCredit(data[5]);
				transaction.setAccNo(accountNo);
				transaction.setBalance(data[6]);
				if (data[4] == null || data[4].equals("-")) { // credit
					transaction.setAmount(transaction.getCredit());
					transaction.setTxnType("CREDIT");
					transaction.setDebit("");
				} else {
					transaction.setAmount(transaction.getDebit());
					transaction.setTxnType("DEBIT");
					transaction.setCredit("");
				}
				transactions.add(transaction);
			} else if (data.length >= 16 && !data[0].equals("Sr No") && matcher2.find()) {
				transaction.setsNo(Integer.toString(serialNoCount++));
				transaction.setTxnDate(CommonUtils.dateFormatter(data[3], dateFormat));
				transaction.setDescription(data[5]);
				transaction.setTxnId(data[7]);
				transaction.setDebit(data[9]);
				transaction.setCredit(data[11]);
				transaction.setAccNo(accountNo);
				transaction.setBalance(data[13]);
				if (data[9] == null || data[9].equals("-")) { // credit
					transaction.setAmount(transaction.getCredit());
					transaction.setTxnType("CREDIT");
					transaction.setDebit("");
				} else {
					transaction.setAmount(transaction.getDebit());
					transaction.setTxnType("DEBIT");
					transaction.setCredit("");
				}
				transactions.add(transaction);
			}
		}
	}
	return transactions;
}

	
	private List<Transaction> extractTransactionsMAHB_2(String text, String accountNo, String txnDateFormat)
			throws IOException {

		List<Transaction> transactions = new ArrayList<>();
		Pattern pattern = Pattern.compile("\\d*\\s*(\\d{2}-\\w{3}-\\d{4})\\s*(.{35}).{20}\\s*([\\d,\\.]+)(\\s+)([\\d,\\.]+)");
		Matcher matcher = pattern.matcher(text);
		int serialNoCount = 1;

		while (matcher.find()) {
			Transaction transaction = new Transaction();
			transaction.setsNo(String.valueOf(serialNoCount++));
			transaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(1), txnDateFormat));
			transaction.setDescription(matcher.group(2).replaceAll("\\s+", " ").trim());
			transaction.setAmount(matcher.group(3));
			if (matcher.group(4).length() > 40) { // debit
				transaction.setDebit(transaction.getAmount());
				transaction.setCredit("");
				transaction.setTxnType("DEBIT");
			} else {
				transaction.setCredit(transaction.getAmount());
				transaction.setDebit("");
				transaction.setTxnType("CREDIT");
			}
			transaction.setBalance(matcher.group(5));
			transaction.setAccNo(accountNo);

			transactions.add(transaction);
		}
		return transactions;
	}

}