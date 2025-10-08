package in.fl.vault.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
public class StandardCharteredServiceImpl implements StandardCharteredService {

	private final static Logger log = Logger.getLogger(IndusIndServiceImpl.class);

	@Override
	public BSInfo parseSTND1(ParseBankStmtRequestDTO request) throws IOException, InterruptedException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering StandardCharteredServiceImpl parseSTND1 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "3");
			String accountNo = CommonUtils.extractField(text, "ACCOUNT\\s*NO.\\s*:\\s(\\d*)");
			bankStatementInfo.setAccountNo(accountNo);
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "BRANCH\\s*:(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setName(CommonUtils.extractField(text, "STATEMENT\\s*DATE.*\\n(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC:\\s*(SCBL\\d{7})"));
			bankStatementInfo.setAccountType(CommonUtils.extractField(text, "ACCOUNT\\s*TYPE\\s*:\\s(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setNominee(CommonUtils.extractField(text, "NOMINEE\\s*REGISTERED\\s*:\\s(.*)"));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "STATEMENT\\s*DATE.*\\n([\\s\\S]*?)\\n\\s*BRANCH\\s*ADDRESS", 100));

			String startDate = null;
			String endDate = null;
			Pattern pattern = Pattern.compile("\\s{5,}(\\d{2}\\s*[A-Z]{1}[a-z]{2}\\s*\\d{2})");
			Matcher matcher = pattern.matcher(text);

			while (matcher.find()) {
				if (startDate == null) {
					startDate = matcher.group(1).trim(); // Capture first match
				}
				endDate = matcher.group(1).trim(); // Capture last match
			}
			String dateFormat = "dd MMM yy";
			bankStatementInfo.setStartDate(CommonUtils.dateFormatter(startDate, dateFormat));
			bankStatementInfo.setEnDate(CommonUtils.dateFormatter(endDate, dateFormat));

			List<Transaction> transactions = extractTransactionsSTND_1(text, accountNo);
			bankStatementInfo.setTransactions(transactions);

		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in StandardCharteredServiceImpl parseSTND1: " + e);
		}

		log.info("Exiting StandardCharteredServiceImpl parseSTND1: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for StandardCharteredServiceImpl parseSTND1 is ==>" + timeTaken);
		return bankStatementInfo;
	}

	@Override
	public BSInfo parseSTND2(ParseBankStmtRequestDTO request) throws IOException, InterruptedException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering StandardCharteredServiceImpl parseSTND2 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "4");
			String accountNo = CommonUtils.extractField(text, "ACCOUNT\\s*NO\\s*:\\s*(\\d*)");
			bankStatementInfo.setAccountNo(accountNo);
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "BRANCH\\s*:\\s*(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setName(CommonUtils.extractField(text, "(.*)BRANCH\\s*:").replaceAll("\\s+", " "));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*:\\s*(SCBL\\d{7})"));
			bankStatementInfo.setAccountType(CommonUtils.extractField(text, "ACCOUNT\\s*TYPE\\s*:\\s(.*)"));
			bankStatementInfo.setNominee(CommonUtils.extractField(text, "NOMINEE\\s*REGISTERED\\s*:\\s(.*)"));

			String dateFormat = "ddMMMyyyy";
			List<Transaction> transactions = new ArrayList<>();
			String[] period = CommonUtils.extractMultiGroupArray(text, "STATEMENT\\s*DATE\\s*:\\s*(\\d{2}\\s*[A-Za-z]{3}\\s*\\d{4}).*(\\d{2}\\s*[A-Za-z]{3}\\s*\\d{4})");
			if (period != null && period.length > 1) {
				bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0].replaceAll(" ", ""), dateFormat));
				bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1].replaceAll(" ", ""), dateFormat));

//				System.out.println(period[0] + " " + period[1]);
				String startYear = bankStatementInfo.getStartDate().substring(0, 4);
//            	String endYear = bankStatementInfo.getEnDate().substring(0,4);
				transactions = extractTransactionsSTND_2(text, accountNo, startYear);
			}
			bankStatementInfo.setTransactions(transactions);

		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in StandardCharteredServiceImpl parseSTND2: " + e);
		}

		log.info("Exiting StandardCharteredServiceImpl parseSTND2: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for StandardCharteredServiceImpl parseSTND2 is ==>" + timeTaken);
		return bankStatementInfo;
	}

	@Override
	public BSInfo parseSTND3(ParseBankStmtRequestDTO request) throws IOException {

		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering StandardCharteredServiceImpl parseSTND3 with request: " + request);

		BSInfo bsInfo = new BSInfo();
		String filePath = request.getFileName();
		try {
			String pdfText = CommonUtils.extractTextFromPdf(filePath, "4");
			Pattern namePattern = Pattern.compile("(.*?BRANCH[\\s\\S]*?)(?=\\s*BRANCH\\s*ADDRESS)", Pattern.MULTILINE);
			Matcher matcher = namePattern.matcher(pdfText);
			if (matcher.find()) {
				String lines[] = matcher.group(1).split("\\n");
				List<String> temp = new ArrayList<>();
				for (String line : lines) {
					if (line.length() > 60 && line.substring(0, 60).trim().length() > 0) {
						temp.add(line.substring(0, 60).trim());
					} else if (line.length() < 60) {
						temp.add(line.trim());
					}
				}
				String address = "";
				for (int i = 0; i < temp.size(); i++) {
					if (i == 0) {
						bsInfo.setName(temp.get(i).replaceAll("\\s+", " "));
					} else {
						address += " " + temp.get(i).trim();
					}
				}
				bsInfo.setAddress(address.replaceAll("\\s+", " ").trim());
			}

			bsInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*:\\s*(\\S*)"));
			bsInfo.setBranch(CommonUtils.extractField(pdfText, "BRANCH\\s*:\\s*(.*)").trim());
			bsInfo.setAccountNo(CommonUtils.extractField(pdfText, "ACCOUNT\\s*NO\\s*:\\s*(.*)").trim());
			Pattern patternAccount = Pattern.compile("ACCOUNT\\s*TYPE\\s*:([\\s\\S]*?)(?=^.*?ACCOUNT\\s*NO)", Pattern.MULTILINE);
			Matcher matcherAccount = patternAccount.matcher(pdfText);
			if (matcherAccount.find()) {
				String accountType = "";
				String[] arrayAccount = matcherAccount.group(1).split("\\n");
				for (int i = 0; i < arrayAccount.length; i++) {
					if (i == 0) {
						accountType = arrayAccount[i].trim();
					} else {
						accountType += " " + arrayAccount[i].substring(70).trim();
					}
				}
				bsInfo.setAccountType(accountType);
			}
			String dateFormat = "dd MMM yyyy";
			List<Transaction> transactions = extractTransactionsSTN_3(filePath, bsInfo.getAccountNo(), dateFormat);

			bsInfo.setStartDate(transactions.get(0).getTxnDate());
			bsInfo.setEnDate(transactions.get(transactions.size() - 1).getTxnDate());
			bsInfo.setTransactions(transactions);
		} catch (Exception e) {
//          	e.printStackTrace();
			log.error("Error in StandardCharteredServiceImpl parseSTND3: " + e);
		}
		log.info("Exiting StandardCharteredServiceImpl parseSTND3:" + bsInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for StandardCharteredServiceImpl parseSTND3 is ==> " + timeTaken);
		return bsInfo;
	}

	private List<Transaction> extractTransactionsSTND_1(String pdfText, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();

		Pattern datePattern = Pattern.compile("\\d{2}\\s*[A-Z]{1}[a-z]{2}\\s*\\d{2}");
		Pattern pageBreakPattern1 = Pattern.compile("Bank\\s*deposits\\s*are\\s*covered");
		Pattern pageBreakPattern2 = Pattern.compile("BALANCE\\s*FORWARD");

		StringBuilder csvOutput = new StringBuilder();
		boolean found = false;
		boolean dateFound = false;
		String[] linesArr = pdfText.split("\\r?\\n");

		// Clean the data
		for (String line : linesArr) {
			Matcher pageBreakMatcher1 = pageBreakPattern1.matcher(line);
			Matcher pageBreakMatcher2 = pageBreakPattern2.matcher(line);
			if (line.trim().isEmpty() || line.contains("Page") || pageBreakMatcher2.find()) {
				continue;
			}
			line = line.replaceAll("\\s{25,}", "|").replaceAll("\\s{3,}", "##").replaceAll("^\\|", "").replaceAll("^##", "");

			if (line.contains("Date")) {
				if (!dateFound) {
					dateFound = true;
				}
				found = true;
			} else if (pageBreakMatcher1.find()) {
				found = false;
			} else if (line.startsWith("TOTAL")) {
				found = false;
				csvOutput.append(line).append("\n");
			} else if (found) {
				csvOutput.append(line).append("\n");
			}
		}

		String resultString = csvOutput.toString();
		String[] lines = resultString.split("\\r?\\n");
		String[] columns = new String[7];
		Arrays.fill(columns, "");
//		[txnDate, valueDate, desc, chq, credit, debit, balance]

		int serialNoCount = 1;
		for (String line : lines) {
			String[] singleLineArr = line.split("\\|");
			Matcher dateMatcher = datePattern.matcher(line);
			if (line.contains("TOTAL")) {
				if (columns[6] != "") {
					Transaction txn = getTxn(columns, accountNo);
					txn.setsNo(String.valueOf(serialNoCount++));
					transactions.add(txn);
					Arrays.fill(columns, "");
				}
			}
			if (dateMatcher.find()) {
				if (columns[6] != "") {
					Transaction txn = getTxn(columns, accountNo);
					txn.setsNo(String.valueOf(serialNoCount++));
					transactions.add(txn);
					Arrays.fill(columns, "");
				}
//            	System.out.println(singleLineArr[0]);
				columns[0] = singleLineArr[0].substring(0, 10);
				columns[1] = singleLineArr[0].substring(12, 22);
				columns[2] += singleLineArr[0].substring(22);
				if (singleLineArr.length == 2 && singleLineArr[1].contains("##")) {
					String[] arr2 = singleLineArr[1].split("##");
					columns[5] = arr2[0];
					columns[6] = arr2[1];
				} else if (singleLineArr.length == 3) {
					columns[4] = singleLineArr[1];
					columns[6] = singleLineArr[2];
				} else if (singleLineArr.length == 4) {
					columns[3] = singleLineArr[1];
					columns[4] = singleLineArr[2];
					columns[6] = singleLineArr[3];
				}
			} else if (singleLineArr.length == 3) {
				if (columns[6] != "") {
					Transaction txn = getTxn(columns, accountNo);
					txn.setsNo(String.valueOf(serialNoCount++));
					transactions.add(txn);
					Arrays.fill(columns, "");
				}
				columns[2] += singleLineArr[0];
				columns[4] = singleLineArr[1];
				columns[6] = singleLineArr[2];
			} else if (singleLineArr.length == 2 && singleLineArr[1].contains("##")) {
				if (columns[6] != "") {
					Transaction txn = getTxn(columns, accountNo);
					txn.setsNo(String.valueOf(serialNoCount++));
					transactions.add(txn);
					Arrays.fill(columns, "");
				}
				columns[2] += singleLineArr[0];
				String[] arr = singleLineArr[1].split("##");
				columns[5] = arr[0];
				columns[6] = arr[1];
			} else if (singleLineArr.length == 1) {
				columns[2] += line;
			}
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsSTND_2(String pdfText, String accountNo, String startYear) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		String currYear = startYear;
		String prevMonth = "";

		pdfText = pdfText.replaceAll("Page[\\s\\S]*?domestic\\s*debit\\s*card\\s*transactions", "").replaceAll("ACCOUNT\\s*STATEMENT[\\s\\S]*?\\s*Date\\s*Value\\s*Description.*\\n\\s*Date.*", "")
				.replaceAll("\\|", "/");

		String[] lines = pdfText.split("\n");
		Pattern txnPattern1 = Pattern.compile("^\\s*([A-Z]{1}[a-z]{2}\\s*\\d{2})\\s*([A-Z]{1}[a-z]{2}\\s*\\d{2})\\s*([\\s\\S]*?)\\s{15,}(\\S+)(\\s*)(\\S+)");
		Pattern txnPattern2 = Pattern.compile("^\\s*([A-Z]{1}[a-z]{2}\\s*\\d{2})\\s*([\\s\\S]*?)\\s{15,}(\\S+)(\\s*)(\\S+)");
		Pattern trashLine = Pattern.compile("Balance\\s*Brought\\s*Forward.*");

		Transaction currentTransaction = null;
		long serialNoCount = 1;
		for (String line : lines) {
			Matcher matcher1 = txnPattern1.matcher(line);
			Matcher matcher2 = txnPattern2.matcher(line);
			Matcher trashLineMatcher = trashLine.matcher(line);

			if (trashLineMatcher.find()) {
				continue;
			}

			if (line.contains("Total")) {
				currentTransaction = null;
				break;
			}

			if (matcher1.find()) {
				currentTransaction = new Transaction();
				currentTransaction.setsNo(String.valueOf(serialNoCount++));
				currentTransaction.setAccNo(accountNo);
				String txnDateString = matcher1.group(1).replaceAll("\\s*", "");
				String valueDateString = matcher1.group(2).replaceAll("\\s*", "");

				// At Month Transition
				String currMonth = txnDateString.substring(0, 3);
				if (prevMonth.equalsIgnoreCase("")) { // start
					prevMonth = currMonth;
				} else if (!prevMonth.equalsIgnoreCase(currMonth)) { // Month change
					if (currMonth.equalsIgnoreCase("Jan") && prevMonth.equalsIgnoreCase("Dec")) { // Year Transition
						currYear = (Integer.parseInt(currYear) + 1) + "";
					}
					prevMonth = currMonth;
				}

				txnDateString = txnDateString + currYear; // format will be MMMddyyyy
				valueDateString = valueDateString + currYear;

				currentTransaction.setTxnDate(CommonUtils.dateFormatter(txnDateString, "MMMddyyyy"));
				currentTransaction.setValueDate(CommonUtils.dateFormatter(valueDateString, "MMMddyyyy"));
				currentTransaction.setDescription(matcher1.group(3).replaceAll("\\s+", " "));

				String amount = matcher1.group(4) != null ? matcher1.group(4) : "";
				String whitespaces = matcher1.group(5);
				String balance = matcher1.group(6) != null ? matcher1.group(6) : "";

				if (whitespaces.length() > 20) {
					currentTransaction.setDebit(amount);
					currentTransaction.setTxnType("DEBIT");
				} else {
					currentTransaction.setCredit(amount);
					currentTransaction.setTxnType("CREDIT");
				}
				currentTransaction.setAmount(amount);
				currentTransaction.setBalance(balance);
				transactions.add(currentTransaction);

			} else if (matcher2.find()) {
				currentTransaction = new Transaction();
				currentTransaction.setsNo(String.valueOf(serialNoCount++));
				currentTransaction.setAccNo(accountNo);
				String valueDateString = matcher2.group(1).replaceAll("\\s*", "");

				// At Month Transition
				String currMonth = valueDateString.substring(0, 3);
				if (prevMonth.equalsIgnoreCase("")) { // start
					prevMonth = currMonth;
				} else if (!prevMonth.equalsIgnoreCase(currMonth)) { // Month change
					if (currMonth.equalsIgnoreCase("Jan") && prevMonth.equalsIgnoreCase("Dec")) { // Year Transition
						currYear = (Integer.parseInt(currYear) + 1) + "";
					}
					prevMonth = currMonth;
				}
				valueDateString = valueDateString + currYear; // format will be MMMddyyyy
				currentTransaction.setValueDate(CommonUtils.dateFormatter(valueDateString, "MMMddyyyy"));
				currentTransaction.setTxnDate(currentTransaction.getValueDate());
				currentTransaction.setDescription(matcher2.group(2).replaceAll("\\s+", " "));

				String amount = matcher2.group(3) != null ? matcher2.group(3) : "";
				String whitespaces = matcher2.group(4);
				String balance = matcher2.group(5) != null ? matcher2.group(5) : "";

				if (whitespaces.length() > 20) {
					currentTransaction.setDebit(amount);
					currentTransaction.setTxnType("DEBIT");
				} else {
					currentTransaction.setCredit(amount);
					currentTransaction.setTxnType("CREDIT");
				}
				currentTransaction.setAmount(amount);
				currentTransaction.setBalance(balance);
				transactions.add(currentTransaction);
			} else if (currentTransaction != null) {
				currentTransaction.setDescription((currentTransaction.getDescription() + line.replaceAll("\\s+", " ")).trim());
			}
		}
		return transactions;
	}

	private Transaction getTxn(String[] columns, String accountNo) {
		String dateFormat = "dd MMM yy";
		String date = columns[0].replaceAll("\\s+", " ");
		String valueDate = columns[1].replaceAll("\\s+", " ");
		String desc = columns[2].replaceAll("##", " ").trim();
		String credit = columns[4];
		String debit = columns[5];
		String balance = columns[6];
		Transaction transaction = new Transaction();
		transaction.setAccNo(accountNo);
		transaction.setTxnDate(CommonUtils.dateFormatter(date, dateFormat));
		transaction.setValueDate(CommonUtils.dateFormatter(valueDate, dateFormat));
		transaction.setDescription(desc);
		transaction.setCredit(credit);
		transaction.setDebit(debit);
		transaction.setBalance(balance);
		if (debit != null && !debit.equalsIgnoreCase("") && !debit.equals("-")) {
			transaction.setAmount(debit);
			transaction.setTxnType("DEBIT");
		} else {
			transaction.setAmount(credit);
			transaction.setTxnType("CREDIT");
		}
		return transaction;
	}

	private List<Transaction> extractTransactionsSTN_3(String fileName, String accountNo, String dateFormat) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);

		int serialNoCount = 1;

		if (txnRows != null && !txnRows.isEmpty()) {
			Pattern headerPattern = Pattern.compile("Date");
			Pattern datePattern = Pattern.compile("\\d{2}\\s*\\w{3}\\s*\\d{4}");

			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r", " ").replaceAll("\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}
				// Assuming first column is txnDate, second column is value
				String[] data = line.split("\\|");
				Matcher headerMatcher = headerPattern.matcher(line);
				Matcher dateMatcher = datePattern.matcher(line);

				if (line.contains("Total") || line.contains("Page") || line.contains("BALANCE FORWARD") || data.length != 7 || headerMatcher.find() || data[4].equalsIgnoreCase("Deposit")
						|| !dateMatcher.find())
					continue;

				Transaction transaction = new Transaction();
				transaction.setsNo(String.valueOf(serialNoCount++));
				transaction.setAccNo(accountNo);
				transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat));
				transaction.setValueDate(CommonUtils.dateFormatter(data[1], dateFormat));
				transaction.setDescription(data[2]);
				String debit = data[5];
				String credit = data[4];
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
				if (data[5] != null && !data[5].equalsIgnoreCase("") && !data[5].equals("-")) {
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
}
