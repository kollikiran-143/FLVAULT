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
public class UNIONServiceImpl implements UNIONService{

	private final static Logger log = Logger.getLogger(UNIONServiceImpl.class);

	@Override
	public BSInfo parseUNION1(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering UNIONServiceImpl parseUNION1 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filepath = request.getFileName();

		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "3");
			
			bankStatementInfo.setName(CommonUtils.extractField(text, "Name(.*)Customer").replaceAll("\\s+", " "));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "Customer\\/CIF.*\\n([\\s\\S]*?)\\n\\s*City", 100));
			bankStatementInfo.setPhone1(CommonUtils.extractField(text, "Mobile\\s*No\\s*(\\+?\\d*)"));
			bankStatementInfo.setEmail(CommonUtils.extractField(text, "Email\\s*Id\\s*(\\S*)"));
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Home\\s*branch\\s*(.*?)(\\n|Statement)")
					.replaceAll("\\s+", " ").trim());
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*(UBIN\\w{7})"));
			bankStatementInfo
					.setAccountType(CommonUtils.extractField(text, "Account\\s*Type(.*)").replaceAll("\\s+", " "));
			String accountNo = CommonUtils.extractField(text, "Account\\s*Number(.*)");
			if(accountNo.equalsIgnoreCase("")) {
				accountNo = CommonUtils.extractField(text, "Account\\s*(\\S*)\\n.*Number");
			}
			bankStatementInfo.setAccountNo(accountNo);
			String dateFormat = "dd/MM/yyyy";
			bankStatementInfo.setStartDate(
					CommonUtils.dateFormatter(CommonUtils.extractField(text, "Statement\\s*Period(.*)To"), dateFormat));
			bankStatementInfo.setEnDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "Period.*To(.*)")
					+ CommonUtils.extractField(text, "Period.*To.*?(\\/\\d{4})", Pattern.DOTALL), dateFormat));

			bankStatementInfo.setTransactions(
					extractTransactionsUNION_1(filepath, accountNo, dateFormat));

		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in UNIONServiceImpl parseUNION1: " + e);
		}

		log.info("Exiting UNIONServiceImpl parseUNION1: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for UNIONServiceImpl parseUNION1 is ==>" + timeTaken);
		return bankStatementInfo;
	}

	@Override
	public BSInfo parseUNION2(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering UNIONServiceImpl parseUNION2 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		try {
			String filepath = request.getFileName();
			String text = CommonUtils.extractTextFromPdf(filepath, "3");

			bankStatementInfo
					.setName(CommonUtils.extractField(text, "(.*)Account\\s*Number\\s*:").replaceAll("\\s+", " "));
			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*Number\\s*:(.*)IFSC"));
			bankStatementInfo
					.setAccountType(CommonUtils.extractField(text, "Account\\s*Type\\s*:(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setNominee(CommonUtils.extractField(text, "Nomination(.*)"));
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*:(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*:(.*)"));
			bankStatementInfo.setPhone1(CommonUtils.extractField(text, "Phone\\s*:(.*)"));
			bankStatementInfo.setEmail(CommonUtils.extractField(text, "E-Mail\\s*:(.*)"));
			String dateFormat = "dd-MM-yyyy";
			String txnDateFormat = "dd-MM-yyyy";
			bankStatementInfo.setStartDate(
					CommonUtils.dateFormatter(CommonUtils.extractField(text, "PERIOD\\s*FROM(.*)TO"), dateFormat));
			bankStatementInfo.setEnDate(
					CommonUtils.dateFormatter(CommonUtils.extractField(text, "PERIOD\\s*FROM.*TO(.*)"), dateFormat));
			String[] addressInArray = CommonUtils
					.extractField(text, "Account\\s*Number\\s*:.*?\\n(.*?)\\s*STATEMENT", Pattern.DOTALL).split("\n");
			String[] regexInArray = { "(.{56,57}).*", "(.{56,57}).*", "(.{56,57}).*", "(.{56,57}).*", "(.{56,57}).*" };
			bankStatementInfo.setAddress(CommonUtils.extractFromMultiLinesAndRegex(addressInArray, regexInArray)
					.replaceAll("\\s+", " ").trim());
			bankStatementInfo.setTransactions(
					extractTransactionsUNION_2(filepath, bankStatementInfo.getAccountNo(), txnDateFormat));

		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error UNIONServiceImpl parseUNION2:", e);
		}
		log.info("Exiting UNIONServiceImpl parseUNION2: " + bankStatementInfo);
		log.info("Time Taken for UNIONServiceImpl parseUNION1 is ==>" + (System.currentTimeMillis() - startTimeInMillis));
		return bankStatementInfo;
	}

	@Override
	public BSInfo parseUNION3(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering UNIONServiceImpl parseUNION3 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		try {
			String filepath = request.getFileName();
			String text = CommonUtils.extractTextFromPdf(filepath, "5");
			
			bankStatementInfo.setName(CommonUtils.extractField(text, "Statement\\s*of\\s*Account.*\\n\\s*(.{80})").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*No\\s*(\\d*)"));
			bankStatementInfo.setAccountType(CommonUtils.extractField(text, "Account\\s*Type\\s*(.*)").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*(.*)"));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*Code\s*(UBIN\\w{7})"));
			bankStatementInfo.setPhone1(CommonUtils.extractField(text, "Mobile\\s*No\\s*([\\+\\d]*)"));	
			bankStatementInfo.setEmail(CommonUtils.extractField(text, "E-mail\\s*(\\w*@[A-Za-z]*\\.[A-Za-z]*)"));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "Statement\\s*of\\s*Account.*\\n([\\s\\S]*?)\\n\\s*City", 90));
			String dateFormat = "dd/MM/yyyy";
			String txnDateFormat = "dd-MM-yyyy";
			String[] period = CommonUtils.extractMultiGroupArray(text, "Statement\\s*Period\\s*From\\s*-(\\d{2}\\/\\d{2}\\/\\d{4}).*(\\d{2}\\/\\d{2}\\/\\d{4})");
            if (period != null && period.length>1) {
            	bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
            	bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
            }
			bankStatementInfo.setTransactions(extractTransactionsUNION_3(filepath, bankStatementInfo.getAccountNo(), txnDateFormat));
		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error UNIONServiceImpl parseUNION3:", e);
		}
		log.info("Exiting UNIONServiceImpl parseUNION3: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for UNIONServiceImpl parseUNION3 is ==>" + timeTaken);
		return bankStatementInfo;
	}
	
	@Override
	public BSInfo parseUNION4(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering UNIONServiceImpl parseUNION4 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filepath = request.getFileName();

		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "3");
			
			bankStatementInfo.setName(CommonUtils.extractField(text, "ACCOUNT\\s*(.*)MICR").replaceAll("\\s+", " "));
			bankStatementInfo.setAddress(CommonUtils.extractField(text, "SCHEME.*?(ADDRESS[\\s\\S]*?)\\n\\s*EMAIL")
					.replaceAll("(ADDRESS|LINE\\s*\\d*|NUMBER|\\s+|\\n)", " ").trim());
			bankStatementInfo.setPhone1(CommonUtils.extractField(text, "MOBILE\\s*(\\S*)"));
			bankStatementInfo.setEmail(CommonUtils.extractField(text, "EMAIL\\s*(\\S*)"));
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "BRANCH\\s*(\\S*)"));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s+(UBIN\\w{7})"));
			bankStatementInfo.setAccountType(CommonUtils.extractField(text, "SCHEME\\s+(\\S*)"));
			String accountNo = CommonUtils.extractField(text, "TO\\s*DATE.*ACCOUNT\\s+(\\S*)");
			bankStatementInfo.setAccountNo(accountNo);
			String dateFormat = "dd-MM-yyyy";
			bankStatementInfo.setStartDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "FROM\\s*DATE\\s*(\\d{2}-\\d{2}-\\d{4})"), dateFormat));
			bankStatementInfo.setEnDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "TO\\s*DATE\\s*(\\d{2}-\\d{2}-\\d{4})"), dateFormat));

			bankStatementInfo.setTransactions(extractTransactionsUNION_4(filepath, accountNo, dateFormat));

		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in UNIONServiceImpl parseUNION4: " + e);
		}
		log.info("Exiting UNIONServiceImpl parseUNION4: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for UNIONServiceImpl parseUNION4 is ==>" + timeTaken);
		return bankStatementInfo;
	}
	
	private List<Transaction> extractTransactionsUNION_1(String fileName, String accountNo, String dateFormat) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
		
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r", " ").replaceAll("\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}
				String[] data = line.split("\\|");
				if (data.length == 6 && !data[0].equals("S.No")) {

					Transaction transaction = new Transaction();
					transaction.setsNo(data[0]);
					transaction.setTxnDate(CommonUtils.dateFormatter(data[1], dateFormat));
					transaction.setTxnId(data[2]);
					transaction.setDescription(data[3]);
					String amount = data[4].substring(0, data[4].length() - 4).trim();
					transaction.setAmount(amount);
					if (data[4].contains("Dr")) {
						transaction.setDebit(amount);
						transaction.setCredit("");
						transaction.setTxnType("DEBIT");
					} else {
						transaction.setDebit("");
						transaction.setCredit(amount);
						transaction.setTxnType("CREDIT");
					}
					transaction.setAccNo(accountNo);
					transaction.setBalance(data[5].substring(0, data[5].length() - 4).trim());

					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsUNION_2(String fileName, String accountNo, String dateFormat) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
		
		int serialNumCount = 1;
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {

				String line = String.join("|", row).replaceAll("\\r", " ").replaceAll("\\s+", " ");
				if (line.trim().isEmpty() || line.contains("Opening Balance")) {
					continue;
				}
				// Assuming first column is txnDate, second column is value
				String[] data = line.split("\\|");
				if (data.length == 7 && !data[0].equals("SI")) {

					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNumCount++));
					transaction.setTxnDate(CommonUtils.dateFormatter(data[1], dateFormat));
					transaction.setDescription(data[2]);
					transaction.setDebit(data[4]);
					transaction.setCredit(data[5]);
					if (data[5] == null || data[5].equals("")) {
						transaction.setAmount(transaction.getDebit());
						transaction.setTxnType("DEBIT");
					} else {
						transaction.setAmount(transaction.getCredit());
						transaction.setTxnType("CREDIT");
					}
					transaction.setAccNo(accountNo);
					transaction.setBalance(data[6].substring(0, data[6].length() - 3));

					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}
	
	private List<Transaction> extractTransactionsUNION_3(String fileName, String accountNo, String dateFormat)
			throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
		
		int serialNumCount = 1;
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {

				String line = String.join("|", row);
				line = line.replaceAll("\\r", " ");
				line = line.replaceAll("\\s+", " ");
				
				if (line.trim().isEmpty()) {
					continue;
				}
				// Assuming first column is txnDate, second column is value
				String[] data = line.split("\\|");
				if (data.length == 8 && !data[0].equals("Date")) {

					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNumCount++));
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0].substring(0, 10), dateFormat));
					transaction.setDescription(data[1]);
					transaction.setTxnId(data[2]);
					transaction.setDebit(data[5]);
					transaction.setCredit(data[6]);
					if (data[6] == null || data[6].equals("")) {
						transaction.setAmount(transaction.getDebit());
						transaction.setTxnType("DEBIT");
					} else {
						transaction.setAmount(transaction.getCredit());
						transaction.setTxnType("CREDIT");
					}
					transaction.setAccNo(accountNo);
					transaction.setBalance(data[7]);

					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}
	
	private List<Transaction> extractTransactionsUNION_4(String fileName, String accountNo, String dateFormat) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
		
		Pattern datePattern  = Pattern.compile("\\d{2}-\\d{2}-\\d{4}");
		
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r", " ").replaceAll("\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}
				Matcher dateMatcher = datePattern.matcher(line);
				String[] data = line.split("\\|");
				if (data.length == 7 && dateMatcher.find()) {

					Transaction transaction = new Transaction();
					transaction.setsNo(data[0]);
					transaction.setTxnDate(CommonUtils.dateFormatter(data[1], dateFormat));
					transaction.setDescription(data[2]);
					transaction.setDebit(data[4]);
					transaction.setCredit(data[5]);
					if (data[5] == null || data[5].equals("")) {
						transaction.setAmount(transaction.getDebit());
						transaction.setTxnType("DEBIT");
					} else {
						transaction.setAmount(transaction.getCredit());
						transaction.setTxnType("CREDIT");
					}
					transaction.setAccNo(accountNo);
					transaction.setBalance(data[6]);

					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}
}