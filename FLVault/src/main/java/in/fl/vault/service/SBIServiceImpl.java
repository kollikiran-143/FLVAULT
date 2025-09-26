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
public class SBIServiceImpl implements SBIService {

	private final static Logger log = Logger.getLogger(SBIServiceImpl.class);

	@Override
	public BSInfo parseSBI1(ParseBankStmtRequestDTO request) throws IOException {

		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering SBIServiceImpl parseSBI1 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String text = CommonUtils.extractTextFromPdf(filePath, "3");

			String accountNo = CommonUtils.extractField(text, "Account\\s*Number\\s*:\\s*(\\d+)");
			bankStatementInfo.setAccountNo(accountNo);
			bankStatementInfo.setName(CommonUtils.extractField(text, "Account\\s*Name\\s*:\\s*(.+)").replaceAll("\\s+", " "));
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*:\\s*(.+)").replaceAll("\\s+", " "));
			bankStatementInfo.setAccountType(CommonUtils.extractField(text, "Account\\s*Description\\s*:\\s*(.+)").replaceAll("\\s+", " "));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFS\\s*Code\\s*:\\s*([A-Z\\d]+)"));
			bankStatementInfo.setAddress(CommonUtils.extractField(text, "Address\\s*:\\s*(.+?)(?=Date)", Pattern.DOTALL).replaceAll("\\s+", " "));
			String dateFormat = "dd MMM yyyy";
			bankStatementInfo.setStartDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "from\\s+(\\d+\\s+[A-Za-z]+\\s+\\d{4})").replaceAll("\\s+", " "), dateFormat));
			bankStatementInfo.setEnDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "to\\s+(\\d+\\s+[A-Za-z]+\\s+\\d{4})").replaceAll("\\s+", " "), dateFormat));
			List<Transaction> transactions = extractTransactionsSBI_1_4_6(filePath, accountNo, dateFormat);

			bankStatementInfo.setTransactions(transactions);
		} catch (Exception e) {
//        	e.printStackTrace();
			log.error("Error in SBIServiceImpl parseSBI1: " + e);
		}

		log.info("Exiting SBIServiceImpl parseSBI1: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for SBIServiceImpl parseSBI1 is ==>" + timeTaken);
		return bankStatementInfo;
	}

	public BSInfo parseSBI2(ParseBankStmtRequestDTO request) throws IOException {

		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering SBIServiceImpl parseSBI2 with request: " + request);

		BSInfo bankstatementInfo = new BSInfo();
		String filepath = request.getFileName();

		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "3");

			bankstatementInfo.setName(CommonUtils.extractField(text, "Name\\s*of\\s*the\\s*Account\\s*Holder(.*)").replaceAll("\\s+", " "));
			bankstatementInfo.setAddress(CommonUtils.extractField(text, "\\s{4}Address([\\s\\S]*?)\\n\\s*Mode").replaceAll("\n", " ").replaceAll("\\s+", " ").trim());
			bankstatementInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*Name(.*)").replaceAll("\\s+", " "));
			bankstatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*Code\\s*(SBIN\\w{7})"));
			bankstatementInfo.setNominee(CommonUtils.extractField(text, "Nominee\\s*Registered(.*)"));
			bankstatementInfo.setAccountNo(CommonUtils.extractField(text, "SAVING\\s*ACCOUNT\\n(.*)"));
			bankstatementInfo.setPan(CommonUtils.extractField(text, "PAN\\s*(\\S*)"));
			bankstatementInfo.setEmail(CommonUtils.extractField(text, "\\s*(\\S*)\\n\\s*Email"));
			bankstatementInfo.setPhone1(CommonUtils.extractField(text, "Mobile\\s*\\n\\s*([\\dX]*)"));

			String dateFormat = "dd-MM-yy";

			List<Transaction> transactions = extractTransactionsSBI_2(filepath, bankstatementInfo.getAccountNo(), dateFormat);
			bankstatementInfo.setTransactions(transactions);
			bankstatementInfo.setStartDate(transactions.get(0).getTxnDate());
			bankstatementInfo.setEnDate(transactions.get(transactions.size() - 1).getTxnDate());

		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in SBIServiceImpl parseSBI2: " + e);
		}

		log.info("Exiting SBIServiceImpl parseSBI2: " + bankstatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for SBIServiceImpl parseSBI2 is: " + timeTaken);
		return bankstatementInfo;
	}

	public BSInfo parseSBI3(ParseBankStmtRequestDTO request) throws IOException {

		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering SBIServiceImpl parseSBI3 with request:" + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String text = CommonUtils.extractTextFromPdf(filePath, "3");

			String[] arrayOfStrings = CommonUtils.extractMultiGroupArray(text, "\\s+Statement\\s*of(.+)\\(A\\/c-(.+)\\)\\s+between\\s+(\\d+-[A-Za-z]+-\\d+)\\s+to\\s+(\\d+-[A-Za-z]+-\\d+)");

			String dateFormat = "dd-MMM-yyyy";
			String txnDateFormat = "dd/MM/yyyy";
			if (arrayOfStrings != null) {
				bankStatementInfo.setName(arrayOfStrings[0].replaceAll("\\s+", " ").trim());
				bankStatementInfo.setAccountNo(arrayOfStrings[1]);
				bankStatementInfo.setStartDate(CommonUtils.dateFormatter(arrayOfStrings[2], dateFormat));
				bankStatementInfo.setEnDate(CommonUtils.dateFormatter(arrayOfStrings[3], dateFormat));
			}

			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*Code\\s*:\\s*(\\d*)"));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "Important.*\\n([\\s\\S]*?)\\n\\s*Mobile", 80));
			bankStatementInfo.setPhone1(CommonUtils.extractField(text, "\\s+Mobile\\s*:\\s*(\\d+)"));
			bankStatementInfo.setTransactions(extractTransactionsSBI_3(filePath, bankStatementInfo.getAccountNo(), txnDateFormat));

		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in SBIServiceImpl parseSBI3: " + e);
		}

		log.info("Exiting SBIServiceImpl parseSBI3: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for SBIServiceImpl parseSBI3 is ==>" + timeTaken);
		return bankStatementInfo;
	}

	public BSInfo parseSBI4(ParseBankStmtRequestDTO request) throws IOException {

		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering SBIServiceImpl parseSBI4 with request :" + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String text = CommonUtils.extractTextFromPdf(filePath, "3");

			String accountNo = CommonUtils.extractField(text, "Account\\s*Number\\s*:\\s*(\\d+)");
			bankStatementInfo.setAccountNo(accountNo);
			bankStatementInfo.setName(CommonUtils.extractField(text, "Customer\\s*Name\\s*:\\s*(.+)").replaceAll("\\s+", " "));
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*:\\s*(.+)").replaceAll("\\s+", " "));
			bankStatementInfo.setAccountType(CommonUtils.extractField(text, "Account\\s*Description\\s*:\\s*(.+)Currency").replaceAll("\\s+", " "));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*:\\s*([A-Z\\d]+)"));
			bankStatementInfo.setEmail(CommonUtils.extractField(text, "Email\\s*:\\s*(.+)"));
			bankStatementInfo.setAddress(CommonUtils.extractField(text, "Address\\s*:\\s*(.+)").replaceAll("\\s+", " "));

			String txnDateFormat = "dd-MM-yy";
			List<Transaction> transactions = extractTransactionsSBI_1_4_6(filePath, accountNo, txnDateFormat);

			bankStatementInfo.setTransactions(transactions);
			bankStatementInfo.setStartDate(transactions.get(0).getTxnDate());
			bankStatementInfo.setEnDate(transactions.get(transactions.size() - 1).getTxnDate());

		} catch (Exception e) {
//        	e.printStackTrace();
			log.error("Error in SBIServiceImpl parseSBI4: " + e);
		}

		log.info("Exiting SBIServiceImpl SBI4: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for SBIServiceImpl parseSBI4 is ==>" + timeTaken);
		return bankStatementInfo;
	}

	public BSInfo parseSBI5(ParseBankStmtRequestDTO request) throws IOException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering SBIServiceImpl parseSBI5 with request :" + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String text = CommonUtils.extractTextFromPdf(filePath, "5");

			String accountNo = CommonUtils.extractField(text, "Account\\s*Number\\s*:\\s*(\\d+)");
			bankStatementInfo.setAccountNo(accountNo);
			bankStatementInfo.setName(CommonUtils.extractField(text, "Account\\s*Name\\s*:(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*:\\s*(\\w*)"));
			bankStatementInfo.setAccountType(CommonUtils.extractField(text, "Account\\s*Description\\s*:(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFS\\s*Code\\s*:\\s*([A-Z\\d]+)"));

			bankStatementInfo.setAddress(CommonUtils.extractField(text, "Address([\\s\\S]*)Date\\s*:").replaceAll("\\s+", " "));

			String dateFormat = "ddMMMyyyy";
			String txnDateFormat = "dd MMM yyyy";
			String[] period = CommonUtils.extractMultiGroupArray(text, "from\\s*(\\d{1,2}\\s*[A-Za-z]{3}\\s*\\d{4}).*(\\d{1,2}\\s*[A-Za-z]{3}\\s*\\d{4})");
			if (period != null && period.length > 1) {
				bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0].replaceAll("\\s*", ""), dateFormat));
				bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1].replaceAll("\\s*", ""), dateFormat));
			}

			List<Transaction> transactions = extractTransactionsSBI_5(filePath, accountNo, txnDateFormat);
			bankStatementInfo.setTransactions(transactions);

		} catch (Exception e) {
//        	e.printStackTrace();
			log.error("Error in SBIServiceImpl parseSBI5: " + e);
		}

		log.info("Exiting SBIServiceImpl SBI5: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for SBIServiceImpl parseSBI5 is ==>" + timeTaken);
		return bankStatementInfo;
	}

	public BSInfo parseSBI6(ParseBankStmtRequestDTO request) throws IOException {

		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering SBIServiceImpl parseSBI6 with request :" + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String text = CommonUtils.extractTextFromPdf(filePath, "3");

			String dateFormat = "dd-MM-yyyy";
			String txnDateFormat = "dd-MM-yyyy";
			String[] period = CommonUtils.extractMultiGroupArray(text, "Statement\\s*From\\s*:\\s*(\\d{2}-\\d{2}-\\d{4})\\s*To\\s*(\\d{2}-\\d{2}-\\d{4})");
			if (period != null && period.length > 1) {
				bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
				bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
			}
			String accountNo = CommonUtils.extractField(text, "Account\\s*No\\s*:\\s*(\\d+)");
			bankStatementInfo.setAccountNo(accountNo);
			bankStatementInfo.setName(CommonUtils.extractField(text, "(.+)Branch\\s*Code").replaceAll("\\s+", " "));
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*Code\\s*:\\s*(.+)").replaceAll("\\s+", " "));
			bankStatementInfo.setAccountType(CommonUtils.extractField(text, "Product\\s*:\\s*(.+)").replaceAll("\\s+", " "));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*Code\\s*:\\s*([A-Z\\d]+)"));
			bankStatementInfo.setEmail(CommonUtils.extractField(text, "\\s{15}Email\\s*:\\s*(.+)").replaceAll("\\s+", " "));
			bankStatementInfo.setNominee(CommonUtils.extractField(text, "Nominee\\s*Name\\s*:\\s*(.*)\\n?").replaceAll("\\s+", " "));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "Branch\\s*Code.*\\n([\\s\\S]*?)\\s*CIF\\s*No", 100));

			List<Transaction> transactions = extractTransactionsSBI_1_4_6(filePath, accountNo, txnDateFormat);
			bankStatementInfo.setTransactions(transactions);

		} catch (Exception e) {
//        	e.printStackTrace();
			log.error("Error in SBIServiceImpl parseSBI6: " + e);
		}

		log.info("Exiting SBIServiceImpl, SBI6: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for SBIServiceImpl parseSBI6 is ==>" + timeTaken);
		return bankStatementInfo;
	}

	public BSInfo parseSBI7(ParseBankStmtRequestDTO request) throws IOException {

		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering SBIServiceImpl parseSBI7 with request :" + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String text = CommonUtils.extractTextFromPdf(filePath, "3");

			bankStatementInfo.setName(CommonUtils.extractField(text, "Account\\s*Name\\s*(.+)").replaceAll("\\s+", " "));
			bankStatementInfo.setAddress(CommonUtils.extractField(text, "Address\\s*(.+?)(?=Date)", Pattern.DOTALL).replaceAll("\\s+", " "));
			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*Number\\s*(\\d+)"));
			bankStatementInfo.setAccountType(CommonUtils.extractField(text, "Account\\s*Description\\s*(.+)").replaceAll("\\s+", " "));
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*(.+)").replaceAll("\\s+", " "));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFS\\s*Code\\s*([A-Z\\d]+)"));
			bankStatementInfo.setNominee(CommonUtils.extractField(text, "Nomination\\s*Registered\\s*([A-Za-z]+)"));
			String dateFormat = "dd MMM yyyy";
			String txnDateFormat = "dd MMM yyyy";
			bankStatementInfo.setStartDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "to\\s+(\\d+\\s+[A-Za-z]+\\s+\\d{4})").replaceAll("\\s+", " "), dateFormat));
			bankStatementInfo.setEnDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "Search\\s*for\\s+(\\d+\\s+[A-Za-z]+\\s+\\d{4})").replaceAll("\\s+", " "), dateFormat));
			bankStatementInfo.setTransactions(extractTransactionsSBI_7(filePath, bankStatementInfo.getAccountNo(), txnDateFormat));
		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in SBIServiceImpl parseSBI7: " + e);
		}

		log.info("Exiting SBIServiceImpl, parseSBI7: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for SBIServiceImpl parseSBI7 is ==>" + timeTaken);
		return bankStatementInfo;
	}

	public BSInfo parseSBI8(ParseBankStmtRequestDTO request) throws IOException {

		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering SBIServiceImpl parseSBI8 with request :" + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String text = CommonUtils.extractTextFromPdf(filePath, "3");

			bankStatementInfo.setName(CommonUtils.extractField(text, "Account\\s*Name\\s*(.+)").replaceAll("\\s+", " "));
			bankStatementInfo.setAddress(CommonUtils.extractField(text, "Address\\s*(.+?)(?=Date)", Pattern.DOTALL).replaceAll("\\s+", " "));
			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*Number\\s*(\\d+)"));
			bankStatementInfo.setAccountType(CommonUtils.extractField(text, "Account\\s*Description\\s*(.+)").replaceAll("\\s+", " "));
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*(.+)").replaceAll("\\s+", " "));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFS\\s*Code\\s*([A-Z\\d]+)"));
			bankStatementInfo.setNominee(CommonUtils.extractField(text, "Nomination\\s*Registered\\s*([A-Za-z]+)"));
			String dateFormat = "dd MMM yyyy";
			String txnDateFormat = "dd MMM yyyy";
			bankStatementInfo.setStartDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "to\\s+(\\d+\\s+[A-Za-z]+\\s+\\d{4})").replaceAll("\\s+", " "), dateFormat));
			bankStatementInfo.setEnDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "Search\\s*for\\s+(\\d+\\s+[A-Za-z]+\\s+\\d{4})").replaceAll("\\s+", " "), dateFormat));
			bankStatementInfo.setTransactions(extractTransactionsSBI_8(text, bankStatementInfo.getAccountNo(), txnDateFormat));

		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in SBIServiceImpl parseSBI8: " + e);
		}

		log.info("Exiting SBIServiceImpl, parseSBI8: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for SBIServiceImpl parseSBI8 is ==>" + timeTaken);
		return bankStatementInfo;
	}

	public BSInfo parseSBI9(ParseBankStmtRequestDTO request) throws IOException {

		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering SBIServiceImpl parseSBI9 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String text = CommonUtils.extractTextFromPdf(filePath, "3");

			bankStatementInfo.setName(CommonUtils.extractField(text, "Account\\s*Name\\s*(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*Number\\s*(\\S*)"));
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setAddress(CommonUtils.extractField(text, "Address([\\s\\S]*?)Account").replaceAll("\\n", " ").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setAccountType(CommonUtils.extractField(text, "Account\\s*Type\\s*(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFS\\s*\\(.*\\)\\s*(\\S*)\\n"));
			bankStatementInfo.setNominee(CommonUtils.extractField(text, "Nomination\\s*Registered\\s*(.*)").replaceAll("\\s+", " "));

			String startDate = "", endDate = "";
			Pattern pattern = Pattern.compile("Account\\s*Statement\\s*for\\s*the\\s*period\\s*(.*)\\s*to\\s*(.*)");
			Matcher matcher = pattern.matcher(text);
			while (matcher.find()) {
				if (endDate.equals("")) {
					endDate = matcher.group(2);
				}
				startDate = matcher.group(1);
			}
			bankStatementInfo.setStartDate(CommonUtils.dateFormatter(startDate, "dd/MM/yyyy"));
			bankStatementInfo.setEnDate(CommonUtils.dateFormatter(endDate, "dd/MM/yyyy"));

			bankStatementInfo.setTransactions(extractTransactionsSBI_9(text, bankStatementInfo.getAccountNo(), ""));
		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in SBIServiceImpl parseSBI9: " + e);
		}

		log.info("Exiting SBIServiceImpl parseSBI9: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for SBIServiceImpl parseSBI9 is ==>" + timeTaken);
		return bankStatementInfo;
	}

	public BSInfo parseSBI10(ParseBankStmtRequestDTO request) {

		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering SBIServiceImpl parseSBI10 with request: " + request);

		BSInfo bsInfo = new BSInfo();
		String filepath = request.getFileName();

		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "5");

			String dateFormat = "ddMMMyyyy";

			String[] dates = CommonUtils.extractMultiGroupArray(text, "Statement\\s*From\\s*:\\s*(\\d{2}\\s*\\w{3}\\s*\\d{4}).*?(\\d{2}\\s*\\w{3}\\s*\\d{4})");
			if (dates != null && dates.length == 2) {
				bsInfo.setStartDate(CommonUtils.dateFormatter(dates[0].replaceAll("\\s*", ""), dateFormat));
				bsInfo.setEnDate(CommonUtils.dateFormatter(dates[1].replaceAll("\\s*", ""), dateFormat));

			}
			bsInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*Number\\s*:\\s*(\\S*)"));

			bsInfo.setName(CommonUtils.extractMultiLinesField(text, "Name\\s*DoB.*([\\s\\S]*?)(?=\\s*SUMMARY)", 0, 17).trim());
			bsInfo.setPhone1(CommonUtils.extractMultiLinesField(text, "Name\\s*DoB.*([\\s\\S]*?)(?=\\s*SUMMARY)", 35, 11).trim());
			bsInfo.setDob(CommonUtils.extractMultiLinesField(text, "Name\\s*DoB.*([\\s\\S]*?)(?=\\s*SUMMARY)", 18, 18).trim());
			bsInfo.setEmail(CommonUtils.extractMultiLinesField(text, "Name\\s*DoB.*([\\s\\S]*?)(?=\\s*SUMMARY)", 54, 27).trim());
			bsInfo.setPan(CommonUtils.extractMultiLinesField(text, "Name\\s*DoB.*([\\s\\S]*?)(?=\\s*SUMMARY)", 89, 13).trim());
			bsInfo.setAddress(CommonUtils.extractMultiLinesField(text, "Name\\s*DoB.*([\\s\\S]*?)(?=\\s*SUMMARY)", 101, 27).trim());
			bsInfo.setNominee(CommonUtils.extractMultiLinesField(text, "Name\\s*DoB.*([\\s\\S]*?)(?=\\s*SUMMARY)", 143, 15).trim());
			List<Transaction> transactions = extractTransactionsSBI_10(filepath, bsInfo.getAccountNo(), bsInfo);
			bsInfo.setTransactions(transactions);

		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in SBIServiceImpl parseSBI10: " + e);
		}

		log.info("Exiting SBIServiceImpl parseSBI10:" + bsInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for SBIServiceImpl parseSBI10 is: " + timeTaken);
		return bsInfo;
	}

	private List<Transaction> extractTransactionsSBI_1_4_6(String fileName, String accountNo, String dateFormat) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);

		int serialNoCount = 1;

		if (txnRows != null && !txnRows.isEmpty()) {
			Pattern headerPattern = Pattern.compile("Txn\\s*Date");

			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r", " ").replaceAll("\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}
				// Assuming first column is txnDate, second column is value
				String[] data = line.split("\\|");
				Matcher headerMatcher = headerPattern.matcher(line);
				if (line.contains("CLOSING BALANCE") || line.contains("BROUGHT FORWARD") || data.length != 7 || headerMatcher.find() || data[4].equalsIgnoreCase("debit"))
					continue;

				Transaction transaction = new Transaction();
				transaction.setsNo(String.valueOf(serialNoCount++));
				transaction.setAccNo(accountNo);
				transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat));
				transaction.setValueDate(CommonUtils.dateFormatter(data[1], dateFormat));
				transaction.setDescription(data[2]);
				String debit = data[4];
				String credit = data[5];
				String balance = data[6];
				if (debit.contains("DR")) {
					debit = debit.substring(0, debit.length() - 2).trim();
				}
				if (credit.contains("CR")) {
					credit = credit.substring(0, credit.length() - 2).trim();
				}
				if (balance.contains("CR") || balance.contains("DR")) {
					balance = balance.substring(0, balance.length() - 2).trim();
				}
				transaction.setDebit(debit);
				transaction.setCredit(credit);
				transaction.setBalance(balance);
				if (data[4] != null && !data[4].equalsIgnoreCase("") && !data[4].equals("-")) {
					transaction.setAmount(debit);
					transaction.setTxnType("DEBIT");
					transaction.setCredit("");
				} else {
					transaction.setDebit("");
					transaction.setAmount(credit);
					transaction.setTxnType("CREDIT");
				}
				transactions.add(transaction);
			}
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsSBI_2(String fileName, String accountNo, String dateFormat) throws IOException {
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

				if (data.length == 6 && !data[0].equals("Date")) {
					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat));
					transaction.setDescription(data[1]);

					if (!(data[4].equalsIgnoreCase("") || data[4].equals("-"))) {
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

	private List<Transaction> extractTransactionsSBI_3(String fileName, String accountNo, String dateFormat) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);

		int serialNoCount = 1;
		if (txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {

				String line = String.join("|", row);
				line = line.replaceAll("\\r", " ");
				line = line.replaceAll("\\s+", " ");

				if (line.trim().isEmpty()) {
					continue;
				}
				// first column is Txn Date, second column is value
				String[] data = line.split("\\|");
				if (data.length == 0 || data.length < 6 || data[0].equalsIgnoreCase("Txn Date"))
					continue;

				Transaction transaction = new Transaction();
				transaction.setsNo(String.valueOf(serialNoCount++));
				transaction.setAccNo(accountNo);
				transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat));
				transaction.setValueDate(CommonUtils.dateFormatter(data[1], dateFormat));
				transaction.setDescription(data[2]);

				if (data[3] != null && !data[3].equalsIgnoreCase("") && !data[3].equals("-")) {
					transaction.setAmount(data[3]);
					transaction.setDebit(data[3]);
					transaction.setCredit("");
					transaction.setTxnType("DEBIT");
				} else {
					transaction.setAmount(data[4]);
					transaction.setCredit(data[4]);
					transaction.setDebit("");
					transaction.setTxnType("CREDIT");
				}
				transaction.setBalance(data[5]);
				transactions.add(transaction);
			}
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsSBI_5(String fileName, String accountNo, String dateFormat) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);

		Pattern dateFormat1 = Pattern.compile("\\d{1,2}\\s[A-Za-z]{3}\\s\\d{4}");
		Pattern dateFormat2 = Pattern.compile("\\d{2}\\/\\d{2}\\/\\d{4}");
		String txnDateFormat = "";

		int serialNoCount = 1;
		if (txnRows != null && !txnRows.isEmpty()) {

			for (String[] row : txnRows) {

				String line = String.join("|", row).replaceAll("\\r", " ").replaceAll("\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}
				// first column is Txn Date, second column is value
				String[] lineArr = line.split("\\|");
				int lineArrLength = lineArr.length;
				if (lineArr.length == 0 || lineArr[0].equalsIgnoreCase("Txn Date")) {
					continue;
				}

				Transaction transaction = new Transaction();
				transaction.setsNo(String.valueOf(serialNoCount++));
				transaction.setAccNo(accountNo);

				String date = lineArr[0];
				if (txnDateFormat.equalsIgnoreCase("")) {
					Matcher dateMatcher1 = dateFormat1.matcher(date);
					Matcher dateMatcher2 = dateFormat2.matcher(date);

					if (dateMatcher1.find()) {
						txnDateFormat = "dd MMM yyyy";
					} else if (dateMatcher2.find()) {
						txnDateFormat = "dd/MM/yyyy";
					}
				}
				transaction.setTxnDate(CommonUtils.dateFormatter(lineArr[0], txnDateFormat));
				transaction.setValueDate(CommonUtils.dateFormatter(lineArr[1], txnDateFormat));

				transaction.setDescription(lineArr[2]);
				String debit = lineArr[lineArrLength - 3];
				String credit = lineArr[lineArrLength - 2];
				String balance = lineArr[lineArrLength - 1];
				transaction.setBalance(balance);

				if (credit != null && !credit.equalsIgnoreCase("") && !credit.equals("-")) {
					transaction.setDebit("");
					transaction.setCredit(credit);
					transaction.setAmount(credit);
					transaction.setTxnType("CREDIT");
				} else {
					transaction.setCredit("");
					transaction.setDebit(debit);
					transaction.setAmount(debit);
					transaction.setTxnType("DEBIT");
				}
				transactions.add(transaction);
			}
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsSBI_7(String fileName, String accountNo, String dateFormat) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);

		int serialNoCount = 1;
		if (txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}
				// first column is Txn Date, second column is value
				String[] data = line.split("\\|");
				if (data.length == 0 || data[0].equalsIgnoreCase("Date"))
					continue;

				Transaction transaction = new Transaction();

				transaction.setsNo(Integer.toString(serialNoCount++));
				transaction.setAccNo(accountNo);
				transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat));
				transaction.setDescription(data[1]);
				String debit = data[3].equalsIgnoreCase("-") ? "" : data[3];
				String credit = data[4].equalsIgnoreCase("-") ? "" : data[4];
				transaction.setDebit(debit);
				transaction.setCredit(credit);
				if (debit != null && !debit.equalsIgnoreCase("")) {
					transaction.setAmount(debit);
					transaction.setTxnType("DEBIT");
				} else {
					transaction.setAmount(credit);
					transaction.setTxnType("CREDIT");
				}
				transaction.setBalance(data[5]);
				transactions.add(transaction);
			}
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsSBI_8(String pdfText, String accountNo, String dateFormat) throws IOException {
		List<Transaction> transactions = new ArrayList<>();

		pdfText = pdfText.replaceAll("\\s*State[\\s\\S]*?\\s*Search.*\\n", "").replaceAll("\\s*Date\\s*Details.*\\n.*", "").replaceAll("Please[\\s\\S]*", "");

		String[] textInArray = pdfText.split("\n");
		String txnDate = "", description = "", debit = "", credit = "", balance = "";
		int serialNumCount = 1;

		for (String eachLine : textInArray) {
			eachLine = eachLine.trim();
			if (eachLine.equals("")) {
				continue;
			}

			String data[] = CommonUtils.extractMultiGroupArray(eachLine, "\\s*(\\d{2}\\s+\\w{3}\\s+\\d{4})\\s*([\\s\\S]+?)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)$");
			if (data != null) {
				if (!description.equals("")) {
					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNumCount++));
					transaction.setTxnDate(CommonUtils.dateFormatter(txnDate, dateFormat));
					transaction.setDescription(description.replaceAll("\\s+", " ").trim());
					transaction.setAccNo(accountNo);
					transaction.setBalance(balance);
					if (credit.equals("-")) {
						transaction.setDebit(debit);
						transaction.setCredit("");
						transaction.setAmount(debit);
						transaction.setTxnType("DEBIT");
					} else {
						transaction.setCredit(credit);
						transaction.setDebit("");
						transaction.setAmount(credit);
						transaction.setTxnType("CREDIT");
					}
					transactions.add(transaction);
				}
				txnDate = data[0].replaceAll("\\s+", " ");
				description = data[1];
				debit = data[2];
				credit = data[3];
				balance = data[4];
			} else {
				description += (" " + eachLine.trim());
			}

		}
		Transaction transaction = new Transaction();
		transaction.setsNo(Integer.toString(serialNumCount++));
		transaction.setTxnDate(CommonUtils.dateFormatter(txnDate, dateFormat));
		transaction.setDescription(description.replaceAll("\\s+", " ").trim());
		transaction.setAccNo(accountNo);
		transaction.setBalance(balance);
		if (credit.equals("-")) {
			transaction.setDebit(debit);
			transaction.setCredit("");
			transaction.setAmount(debit);
			transaction.setTxnType("DEBIT");
		} else {
			transaction.setCredit(credit);
			transaction.setDebit("");
			transaction.setAmount(credit);
			transaction.setTxnType("CREDIT");
		}
		transactions.add(transaction);
		return transactions;
	}

	private List<Transaction> extractTransactionsSBI_9(String pdfText, String accountNo, String dateFormat) throws IOException {
		List<Transaction> transactions = new ArrayList<>();

		Pattern pattern = Pattern.compile(
				"(\\d{2}-\\w{3}-\\d{2})\\s*(.{43}).*?([\\d,]+\\.\\d{2})(\\s+)([\\d,]+\\.\\d{2})\\n\\s*\\((\\d{2}-\\w{3}-\\d{4})\\)\\s*(.{1,43}).*\\n([\\s\\S]*?)\\s*(?=(\\d{2}-\\w{3}-\\d{2})|\\*\\*This)");
		Matcher matcher = pattern.matcher(pdfText);
		int serialNoCount = 1;

		while (matcher.find()) {
			Transaction transaction = new Transaction();
			transaction.setsNo(String.valueOf(serialNoCount++));
			transaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(1), "dd-MMM-yy"));
			String description = matcher.group(2) + " " + matcher.group(7) + " " + matcher.group(8);
			transaction.setDescription(description.replaceAll("\\n", " ").replaceAll("\\s+", " ").trim());
			transaction.setAmount(matcher.group(3));
			if (matcher.group(4).length() > 25) {
				transaction.setDebit(transaction.getAmount());
				transaction.setCredit("");
				transaction.setTxnType("DEBIT");
			} else {
				transaction.setDebit("");
				transaction.setCredit(transaction.getAmount());
				transaction.setTxnType("CREDIT");
			}
			transaction.setBalance(matcher.group(5));
			transaction.setValueDate(CommonUtils.dateFormatter(matcher.group(6), "dd-MMM-yyyy"));
			transaction.setAccNo(accountNo);
			transactions.add(transaction);
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsSBI_10(String fileName, String accountNo, BSInfo bsInfo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);

		int serialNoCount = 1;
		if (txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {

				String line = String.join("|", row);
				line = line.replaceAll("\\r", " ");
				line = line.replaceAll("\\s+", " ");

				if (line.trim().isEmpty()) {
					continue;
				}

				String[] data = line.split("\\|");

				if (data.length > 10 && !data[0].equalsIgnoreCase("Opening Date") && data[6].equalsIgnoreCase("INR")) {
					bsInfo.setAccountType(data[1]);
					bsInfo.setIfsc("SBIN" + data[2]);
				}

				if (data.length < 7 || !data[1].matches("\\d{4}-\\d{2}-\\d{2}") || !data[5].matches("CREDIT|DEBIT"))
					continue;

				Transaction transaction = new Transaction();
				transaction.setsNo(String.valueOf(serialNoCount++));
				transaction.setAccNo(accountNo);
				transaction.setValueDate(CommonUtils.dateFormatter(data[1], "dd-MM-yy"));
				String txnDate = data[2].split("\\s")[0];
				transaction.setTxnDate(CommonUtils.dateFormatter(txnDate, "dd/MM/yyyy"));
				transaction.setDescription(data[4]);
				transaction.setTxnType(data[5].trim());
				transaction.setBalance(data[7]);
				transaction.setAmount(data[6]);
				if (data[5].trim().equals("DEBIT")) {
					transaction.setDebit(data[6]);
					transaction.setCredit("");
				} else {
					transaction.setCredit(data[6]);
					transaction.setDebit("");
				}
				transactions.add(transaction);
			}
		}
		return transactions;
	}

}
