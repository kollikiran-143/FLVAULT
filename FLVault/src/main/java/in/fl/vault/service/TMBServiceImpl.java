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
public class TMBServiceImpl implements TMBService {

	private final static Logger log = Logger.getLogger(SBIServiceImpl.class);

	@Override
	public BSInfo parseTMB1(ParseBankStmtRequestDTO request) throws IOException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering TMBServiceImpl parseTMB1 with request:" + request);

		BSInfo bsInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String pdfText = CommonUtils.extractTextFromPdf(filePath, "3");
			List<Transaction> listTransaction = new ArrayList<>();

			Pattern pattern1 = Pattern.compile("\\s*Statement\\s*for\\s*A/c\\s*(\\d{1,})\\s*Between\\s*([\\d]{1,}-[\\w]*-[\\w]*?)\\s*and\\s*([\\d]{1,}-[\\w]*-[\\w]*)");
			Pattern pattern3 = Pattern.compile("Address\\s*[\\s\\S]*?:([\\s\\S]*?)(?=A/c\\s*Type)");
			Pattern pattern4 = Pattern.compile("Name\\s*:?\\s*(.*?)(?=Branch)");
			Pattern pattern5 = Pattern.compile("IFSC\\s*Code\\s*:?\\s*([\\w]*)");
			Pattern pattern7 = Pattern.compile("Branch\\s*Name\\s*:?\\s*(.*)");
			Pattern pattern8 = Pattern.compile("A/c\\s*Type\\s*:?\\s*(\\S+)");
			Pattern pattern10 = Pattern.compile("E-Mail\\s*ID\\s*:?\\s*(\\S+)");
			Pattern pattern6 = Pattern.compile("(^\\s*\\d{2}-\\d{2}-\\d{4}[\\s\\S]*?)(?=^\\s{1,14}\\d{2}-\\d{2}-\\d{4}|\\s*Page|\\s*Closing\\s*Balance)", Pattern.MULTILINE);

			Matcher matcher = pattern1.matcher(pdfText);
			Matcher matcher3 = pattern3.matcher(pdfText);
			Matcher matcher4 = pattern4.matcher(pdfText);
			Matcher matcher5 = pattern5.matcher(pdfText);
			Matcher matcher6 = pattern6.matcher(pdfText);
			Matcher matcher7 = pattern7.matcher(pdfText);
			Matcher matcher8 = pattern8.matcher(pdfText);
			Matcher matcher10 = pattern10.matcher(pdfText);

			String dateFormat = "dd-MM-yyyy";
			if (matcher.find()) {
				bsInfo.setAccountNo(matcher.group(1));
				bsInfo.setStartDate(CommonUtils.dateFormatter(matcher.group(2), dateFormat));
				bsInfo.setEnDate(CommonUtils.dateFormatter(matcher.group(3), dateFormat));
			}
			if (matcher3.find()) {
				String[] address = matcher3.group(1).split("\n");
				String customerAddress = "";
				for (int i = 0; i < address.length; i++) {
					if (address[i].length() < 50) {
						continue;
					}
					customerAddress += " " + address[i].substring(0, 50).trim();
					bsInfo.setAddress(customerAddress.replaceAll("\\s+", " ").trim());
				}
			}
			if (matcher4.find()) {
				String name = matcher4.group(1).trim();
				bsInfo.setName(name.replaceAll("\\s+", " "));
			}
			if (matcher5.find()) {
				bsInfo.setIfsc(matcher5.group(1));
			}

			if (matcher7.find()) {
				String branchName = matcher7.group(1).trim();
				bsInfo.setBranch(branchName.replaceAll("\\s+", " "));
			}
			if (matcher8.find()) {
				bsInfo.setAccountType(matcher8.group(1));
			}

			if (matcher10.find()) {
				String email = matcher10.group(1).trim();
				bsInfo.setEmail(email);
			}
			int SerialNoCount = 1;
			while (matcher6.find()) {

				String[] lines = matcher6.group(1).split("\\n");
				String description = "";
				String date = "";
				String debit = "";
				String credit = "";
				String balance = "";
				Transaction transaction = new Transaction();
				for (int i = 0; i < lines.length; i++) {
					if (i == 0) {
						String templine1 = lines[0].trim();
						int len = templine1.length();
						if (len > 151) {
							date = templine1.substring(0, 15).trim();
							description = templine1.substring(15, 83).trim().replaceAll("\\s+", " ");
							String line = templine1.substring(97).trim();
							String temp = line.replaceAll("\\s+", "");
							String[] money = CommonUtils.extractMultiGroupArray(line, "([\\d,\\.]*)\\s*([\\d,\\.-]*)");
							if (money.length == 2) {
								if ((line.length() - temp.length()) > 30) {
									debit = money[0];
									balance = money[1];
								} else {
									credit = money[0];
									balance = money[1];
								}
							}
							balance = balance.replaceAll("\\-", "");
						}

					} else {
						description += lines[i].trim();
					}
				}
				transaction.setTxnDate(CommonUtils.dateFormatter(date, "dd-MM-yyyy"));
				transaction.setBalance(balance);
				transaction.setCredit(credit);
				transaction.setDebit(debit);
				transaction.setDescription(description.replaceAll("\\s+", " ").trim());
				if (debit != null && !debit.equalsIgnoreCase("") && !debit.equals("-")) {
					transaction.setAmount(debit);
					transaction.setTxnType("DEBIT");
				} else {
					transaction.setAmount(credit);
					transaction.setTxnType("CREDIT");
				}
				transaction.setsNo(String.valueOf(SerialNoCount++));
				transaction.setAccNo(bsInfo.getAccountNo());
				listTransaction.add(transaction);
			}
			bsInfo.setTransactions(listTransaction);
		} catch (Exception e) {
//	        e.printStackTrace();
			log.error("Error in MBService parseTMB1: " + e);
		}

		log.info("Exiting TMBServiceImpl parseTMB1 with response:" + bsInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for TMBServiceImpl parseTMB1 is ==> " + timeTaken);
		return bsInfo;
	}

	@Override
	public BSInfo parseTMB2(ParseBankStmtRequestDTO request) throws IOException {

		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering TMBServiceImpl parseTMB2 with request: " + request);

		BSInfo bsInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String pdfText = CommonUtils.extractTextFromPdf(filePath, "5");
			List<Transaction> listTransaction = new ArrayList<>();
			Pattern pattern1 = Pattern.compile("Statement\\s*for\\s*account\\s*number\\s*(\\d*)\\s*Between\\s*([\\d]{1,}-[\\w]*-[\\w]*?)\\s*and\\s*([\\d]{1,}-[\\w]*-[\\w]*)");
			Pattern pattern2 = Pattern.compile("Mobile\\s*No.\\s*(\\S+)");
			Pattern pattern3 = Pattern.compile("(.*\\n+\\s*Address[\\s\\S]*?)(?=A/c)", Pattern.MULTILINE);
			Pattern pattern4 = Pattern.compile("^\\s*Name\\s*(.*)", Pattern.MULTILINE);
			Pattern pattern5 = Pattern.compile("IFSC\\s*Code\\s*([\\w]*)");
			Pattern pattern6 = Pattern.compile("(^\\s*\\d{2}-\\w{3}-\\d{4}[\\s\\S]*?)(?=^\\s*\\d{2}-\\w{3}-\\d{4}|\\s*Closing\\s*Balance|\\s*Page\\s*\\d{1,}\\s*of)", Pattern.MULTILINE);
			Pattern pattern7 = Pattern.compile("Branch\\s*Name\\s*(.*)");
			Pattern pattern8 = Pattern.compile("A/c\\s*Type\\s*(\\S+)");
			Pattern pattern10 = Pattern.compile("E-Mail\\s*ID\\s*\\s*(\\S*)\\n");

			Matcher matcher1 = pattern1.matcher(pdfText);
			Matcher matcher2 = pattern2.matcher(pdfText);
			Matcher matcher3 = pattern3.matcher(pdfText);
			Matcher matcher4 = pattern4.matcher(pdfText);
			Matcher matcher5 = pattern5.matcher(pdfText);
			Matcher matcher6 = pattern6.matcher(pdfText);
			Matcher matcher7 = pattern7.matcher(pdfText);
			Matcher matcher8 = pattern8.matcher(pdfText);
			Matcher matcher10 = pattern10.matcher(pdfText);

			String dateFormat = "dd-MM-yyyy";

			if (matcher1.find()) {
				bsInfo.setAccountNo(matcher1.group(1));
				bsInfo.setStartDate(CommonUtils.dateFormatter(matcher1.group(2), dateFormat));
				bsInfo.setEnDate(CommonUtils.dateFormatter(matcher1.group(3), dateFormat));
			}
			if (matcher2.find()) {
				bsInfo.setPhone1(matcher2.group(1));
			}
			if (matcher3.find()) {
				String[] address = matcher3.group(1).split("\n");
				String customerAddress = "";
				for (int i = 0; i < address.length; i++) {
					if (address[i].length() < 50) {
						continue;
					}
					customerAddress += " " + address[i].substring(20, 50).trim();
				}
				bsInfo.setAddress(customerAddress.replaceAll("\\s+", " ").trim());
			}
			if (matcher4.find()) {
				String name = "";
				if (matcher4.group(1).length() > 50) {
					name = matcher4.group(1).substring(0, 50).trim();
				} else {
					name = matcher4.group(1).trim();
				}
				bsInfo.setName(name.replaceAll("\\s+", " ").replaceAll("Branch\s*Name", "").trim());
			}
			if (matcher5.find()) {
				bsInfo.setIfsc(matcher5.group(1));
			}
			if (matcher7.find()) {
				String branchName = matcher7.group(1).trim();
				String branch = branchName;
				branch = branch.replaceAll("\\s+", " ");
				bsInfo.setBranch(branch);
			}
			if (matcher8.find()) {
				bsInfo.setAccountType(matcher8.group(1));
			}
			if (matcher10.find()) {
				String email = matcher10.group(1).trim();
				bsInfo.setEmail(email);
			}
			int cnt = 1;
			while (matcher6.find()) {
				String[] lines = matcher6.group(1).split("\\n");
				String description = "";
				String date = "";
				String debit = "";
				String credit = "";
				String balance = "";
				Transaction transaction = new Transaction();
				int diff = lines[0].length() - lines[0].trim().length();
				for (int i = 0; i < lines.length; i++) {
					int len = lines[i].length();
					date += (len - diff > 12) ? lines[i].substring(diff, diff + 12) : "";
					description += (len - diff > 63) ? lines[i].substring(diff + 12, diff + 63) : (len - diff > diff + 12) ? lines[i].substring(diff + 12) : "";
					if (len - diff > 66) {
						String line = lines[i].substring(diff + 66).trim();
//						System.out.println(line);
						Pattern moneyPattern = Pattern.compile("([\\d,.]*)\\s{5,}([\\d,.]*)");
						Pattern spacePattern = Pattern.compile("\\s{17,}");
						Matcher matcherMoney = moneyPattern.matcher(line);
						Matcher spaceMatcher = spacePattern.matcher(line);
						if (spaceMatcher.find()) {
							if (matcherMoney.find()) {
								debit = matcherMoney.group(1);
								balance = matcherMoney.group(2);
							}
						} else {
							if (matcherMoney.find()) {
								credit = matcherMoney.group(1);
								balance = matcherMoney.group(2);
							}
						}
					}
				}
				transaction.setTxnDate(CommonUtils.dateFormatter(date, "dd-MMM-yyyy"));
				transaction.setBalance(balance.trim());
				transaction.setCredit(credit.trim());
				transaction.setDebit(debit.trim());
				transaction.setDescription(description.replaceAll("\\s+", " ").trim());
				transaction.setsNo(String.valueOf(cnt++));
				if (debit != null && !debit.equalsIgnoreCase("") && !debit.equals("-")) {
					transaction.setAmount(debit);
					transaction.setTxnType("DEBIT");
				} else {
					transaction.setAmount(credit);
					transaction.setTxnType("CREDIT");
				}
				transaction.setAccNo(bsInfo.getAccountNo());
				listTransaction.add(transaction);
			}
			bsInfo.setTransactions(listTransaction);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Error in MBService parseTMB2: " + e);
		}

		log.info("Exiting TMBServiceImpl parseTMB2 with response:" + bsInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for TMBServiceImpl parseTMB2 is ==>" + timeTaken);
		return bsInfo;
	}

	@Override
	public BSInfo parseTMB3(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering TMBServiceImpl parseTMB3 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String pdfText = CommonUtils.extractTextFromPdf(filePath, "6");
			String accountNo = CommonUtils.extractField(pdfText, "Account\\s*Number\\s*:\\s*(.*)");
			bankStatementInfo.setAccountNo(accountNo);
			bankStatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*:\\s*(\\S*)"));
			bankStatementInfo.setBranch(CommonUtils.extractField(pdfText, "Branch\\s*Name\\s*:\\s*(\\S*)"));
			String[] details = extractAddressTMB_3(pdfText);
			if (details != null && details.length >= 2) {
				bankStatementInfo.setAddress(details[0].trim());
				bankStatementInfo.setName(details[1].trim());
			}
			String[] period = CommonUtils.extractMultiGroupArray(pdfText,
					"Transaction\\s*Date\\s*From\\s*.*\\s*:\\s*(\\d{2}/\\d{2}/\\d{4})\\s*Transaction\\s*Date\\s*To\\s*:\\s*.*\\s*(\\d{2}/\\d{2}/\\d{4})");
			if (period != null && period.length >= 2) {
				bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], "dd/MM/yyyy"));
				bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], "dd/MM/yyyy"));
			}
			List<Transaction> transactions = extractTransactionsTMB_3(pdfText, accountNo);
			bankStatementInfo.setTransactions(transactions);

		} catch (Exception e) {
//            e.printStackTrace();
			log.error("Error in TMBServiceImpl parseTMB3: " + e);
		}
		log.info("Exiting TMBServiceImpl parseTMB3 with response: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for TMBServiceImpl parseTMB3 is ==>" + timeTaken);
		return bankStatementInfo;
	}

	public BSInfo parseTMB4(ParseBankStmtRequestDTO request) throws IOException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering TMBServiceImpl parseTMB4 with request: " + request);

		String filePath = request.getFileName();
		BSInfo bankStatementInfo = new BSInfo();
		try {
			String pdfText = CommonUtils.extractTextFromPdf(filePath, "3");

			bankStatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*CODE\\s*-\\s*(\\S*)"));
			bankStatementInfo.setName(CommonUtils.extractField(pdfText, "TO\\s*:(.*)DATE").replaceAll("\\s+", " "));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(pdfText, "TO\\s*:.*\\n([\\s\\S]*?)\\n\\s*STATEMENT", 80));
			String[] accNoPeriod = CommonUtils.extractMultiGroupArray(pdfText, "ACCOUNT\\s*NO\\s*:\\s*(\\S*).*FROM(.*)to(.*)");
			if (accNoPeriod != null) {
				bankStatementInfo.setAccountNo(accNoPeriod[0]);
				bankStatementInfo.setStartDate(CommonUtils.dateFormatter(accNoPeriod[1], "dd-MM-yyyy"));
				bankStatementInfo.setEnDate(CommonUtils.dateFormatter(accNoPeriod[2], "dd-MM-yyyy"));
			}
			bankStatementInfo.setTransactions(extractTransactionsTMB_4(pdfText, bankStatementInfo.getAccountNo()));
		} catch (Exception e) {
//          e.printStackTrace();
			log.error("Error in TMBServiceImpl parseTMB4: " + e);
		}
		log.info("Exiting TMBServiceImpl parseTMB4: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for TMBServiceImpl parseTMB4 is ==>" + timeTaken);
		return bankStatementInfo;
	}

	@Override
	public BSInfo parseTMB5(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering TMBServiceImpl parseTMB5 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String pdfText = CommonUtils.extractTextFromPdf(filePath, "3");
			bankStatementInfo.setAccountNo(CommonUtils.extractField(pdfText, "Account\\s*Number\\s*:\\s*(\\S*)"));
			bankStatementInfo.setName(CommonUtils.extractField(pdfText, "Account\\s*Name\\s*:\\s*(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*CODE.*?(\\S{3,})"));
			String[] period = CommonUtils.extractMultiGroupArray(pdfText, "for\\s*the\\s*Period.*?(\\d{2}-\\d{2}-\\d{4})\\s*to\\s*(\\d{2}-\\d{2}-\\d{4})");
			String dateFormat = "dd-MM-yyyy";
			if (period != null && period.length > 1) {
				bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0].replaceAll("\\s+", " "), dateFormat));
				bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1].replaceAll("\\s+", " "), dateFormat));
			}
			List<Transaction> transactions = extractTransactionsTMB_5(filePath, bankStatementInfo.getAccountNo());
//			List<Transaction> transactions = extractTransactionsTMB_5_(pdfText, bankStatementInfo.getAccountNo());
			bankStatementInfo.setTransactions(transactions);

		} catch (Exception e) {
//        	e.printStackTrace();
			log.error("Error in TMBServiceImpl parseTMB5: " + e);
		}
		log.info("Exiting TMBServiceImpl parseTMB5: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for TMBServiceImpl parseTMB5 is ==>" + timeTaken);
		return bankStatementInfo;
	}

	@Override
	public BSInfo parseTMB6(ParseBankStmtRequestDTO request) {

		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering TMBServiceImpl parseTMB6 with request: " + request);

		BSInfo bsInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String pdfText = CommonUtils.extractTextFromPdf(filePath, "4");

			List<Transaction> listTransaction = new ArrayList<>();
			Pattern pattern1 = Pattern.compile("Statement\\s*for\\s*account\\s*number\\s*(\\d*)\\s*Between\\s*([\\d]{1,}-[\\w]*-[\\w]*?)\\s*and\\s*([\\d]{1,}-[\\w]*-[\\w]*)");
			Pattern pattern4 = Pattern.compile("^\\s*Name\\s*(.*)", Pattern.MULTILINE);
			Pattern pattern5 = Pattern.compile("IFSC\\s*Code\\s*([\\w]*)");
			Pattern pattern6 = Pattern.compile("(^\\s*\\d{2}-\\w{3}-[\\s\\S]*?)(?=^^\\s*Closing\\s*Balance|^\\s*\\d{2}-\\w{3}-)", Pattern.MULTILINE);

			Pattern pattern8 = Pattern.compile("A/c\\s*Type\\s*(\\S+)");

			Matcher matcher1 = pattern1.matcher(pdfText);
			Matcher matcher4 = pattern4.matcher(pdfText);
			Matcher matcher5 = pattern5.matcher(pdfText);
			Matcher matcher6 = pattern6.matcher(pdfText);
			Matcher matcher8 = pattern8.matcher(pdfText);

			String dateFormat = "dd-MM-yyyy";

			if (matcher1.find()) {
				bsInfo.setAccountNo(matcher1.group(1));
				bsInfo.setStartDate(CommonUtils.dateFormatter(matcher1.group(2), dateFormat));
				bsInfo.setEnDate(CommonUtils.dateFormatter(matcher1.group(3), dateFormat));
			}

			if (matcher4.find()) {
				String name = "";
				if (matcher4.group(1).length() > 50) {
					name = matcher4.group(1).substring(0, 50).trim();
				} else {
					name = matcher4.group(1).trim();
				}
				bsInfo.setName(name.replaceAll("\\s+", " "));
			}
			if (matcher5.find()) {
				bsInfo.setIfsc(matcher5.group(1));
			}
			String branchName = CommonUtils.extractField(pdfText, "Code.*\\n.*?Branch\\n.{85}(.*)");
			if (branchName != null) {
				bsInfo.setBranch(branchName);
			}

			String mobileNo = CommonUtils.extractField(pdfText, "Mobile.*\\n*(.{0,70})");
			if (mobileNo != null) {
				bsInfo.setPhone1(mobileNo);
			}
			String email = CommonUtils.extractField(pdfText, "E-Mail\\s*ID\\s*(\\S*)");
			if (email != null) {
				bsInfo.setEmail(email);
			}

			String address = CommonUtils.extractMultiLinesField(pdfText, "Name.*\\n([\\s\\S]*?)(?=A/c)", 70);
			if (address != null) {
				bsInfo.setAddress(address);
			}

			if (matcher8.find()) {
				bsInfo.setAccountType(matcher8.group(1));
			}

			int serialNoCount = 1;
			while (matcher6.find()) {

				String description = "";
				String date = "";
				String debit = "";
				String credit = "";
				String balance = "";
				String valueDate = "";
				Transaction transaction = new Transaction();

				String[] listlist = matcher6.group(1).split("\\n");

				for (int i = 0; i <= listlist.length - 1; i++) {
					String line = listlist[i];
					int len = line.length();
					if (len > 0) {
						if (len > 11)
							date += line.substring(0, 11).trim();
						else
							date += line.substring(0).trim();
					}
					if (len > 11) {
						if (len > 86)
							description += line.substring(11, 86).trim();
						else
							description += line.substring(11).trim();
					}
					if (len > 86) {
						if (len > 109)
							debit += line.substring(86, 109).trim();
						else
							debit += line.substring(86).trim();
					}
					if (len > 109) {
						if (len > 127)
							credit += line.substring(109, 127).trim();
						else
							credit += line.substring(109).trim();
					}
					if (len > 127) {
						balance += line.substring(127).trim();
					}
				}
				if (credit.length() > 0 && !credit.equalsIgnoreCase("-")) {
					transaction.setAmount(credit);
					transaction.setTxnType("CREDIT");
				} else {
					transaction.setAmount(debit);
					transaction.setTxnType("DEBIT");
				}
				transaction.setsNo(String.valueOf(serialNoCount++));
				String txnDateFormat = "dd-MMM-yyyy";
				balance = balance.replaceAll("C|r", "");
				date = date.replaceAll("\\s*", "");
				transaction.setTxnDate(CommonUtils.dateFormatter(date, txnDateFormat));
				transaction.setValueDate(CommonUtils.dateFormatter(valueDate, txnDateFormat));
				transaction.setBalance(balance);
				transaction.setCredit(credit);
				transaction.setDebit(debit);
				transaction.setAccNo(bsInfo.getAccountNo());
				transaction.setDescription(description.replaceAll("\\s+", " ").trim());
				listTransaction.add(transaction);
			}
			bsInfo.setTransactions(listTransaction);
		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in TMBService parseTMB6: " + e);
		}

		log.info("Exiting TMBServiceImpl parseTMB6 with response:" + bsInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for TMBServiceImpl parseTMB6 is ==>" + timeTaken);
		return bsInfo;
	}

	@Override
	public BSInfo parseTMB7(ParseBankStmtRequestDTO request) throws IOException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering TMBServiceImpl parseTMB7 with request: " + request);

		BSInfo bsInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String pdfText = CommonUtils.extractTextFromPdf(filePath, "4");
			List<Transaction> listTransaction = new ArrayList<>();

			Pattern pattern1 = Pattern.compile("\\s*Statement\\s*for\\s*A/c\\s*(\\d{1,})\\s*Between\\s*([\\d]{1,}-[\\w]*-[\\w]*?)\\s*and\\s*([\\d]{1,}-[\\w]*-[\\w]*)");
			Pattern pattern3 = Pattern.compile("Address\\s*([\\s\\S]*?)(?=\\s*A/c\\s*Type)");
			Pattern pattern4 = Pattern.compile("Name\\s*(.*?)(?=Branch)");
			Pattern pattern5 = Pattern.compile("IFSC\\s*Code\\s*([\\w]*)");
			Pattern pattern7 = Pattern.compile("Branch\\s*Name\\s*(.*)");
			Pattern pattern8 = Pattern.compile("A/c\\s*Type\\s*(\\S+)");
			Pattern pattern10 = Pattern.compile("E-Mail\\s*ID\\s*(\\S+)");
			Pattern pattern6 = Pattern.compile("(^\\s*\\d{2}-\\d{2}-\\d{4}[\\s\\S]*?)(?=^\\s{1,14}\\d{2}-\\d{2}-\\d{4}|\\s*Page|\\s*Closing\\s*Balance)", Pattern.MULTILINE);

			Matcher matcher = pattern1.matcher(pdfText);
			Matcher matcher3 = pattern3.matcher(pdfText);
			Matcher matcher4 = pattern4.matcher(pdfText);
			Matcher matcher5 = pattern5.matcher(pdfText);
			Matcher matcher6 = pattern6.matcher(pdfText);
			Matcher matcher7 = pattern7.matcher(pdfText);
			Matcher matcher8 = pattern8.matcher(pdfText);
			Matcher matcher10 = pattern10.matcher(pdfText);

			String dateFormat = "dd-MM-yyyy";
			if (matcher.find()) {
				bsInfo.setAccountNo(matcher.group(1));
				bsInfo.setStartDate(CommonUtils.dateFormatter(matcher.group(2), dateFormat));
				bsInfo.setEnDate(CommonUtils.dateFormatter(matcher.group(3), dateFormat));
			}
			if (matcher3.find()) {
				String[] address = matcher3.group(1).split("\n");
				String customerAddress = "";
				for (int i = 0; i < address.length; i++) {
					if (address[i].length() < 50) {
						customerAddress += " " + address[i].trim();
					} else {
						customerAddress += " " + address[i].substring(0, 50).trim();
					}
				}
				bsInfo.setAddress(customerAddress.replaceAll("\\s+", " ").trim());
			}
			if (matcher4.find()) {
				String name = matcher4.group(1).trim();
				bsInfo.setName(name.replaceAll("\\s+", " "));
			}
			if (matcher5.find()) {
				bsInfo.setIfsc(matcher5.group(1));
			}

			if (matcher7.find()) {
				String branchName = matcher7.group(1).trim();
				bsInfo.setBranch(branchName.replaceAll("\\s+", " "));
			}
			if (matcher8.find()) {
				bsInfo.setAccountType(matcher8.group(1));
			}

			if (matcher10.find()) {
				String email = matcher10.group(1).trim();
				bsInfo.setEmail(email);
			}
			int SerialNoCount = 1;
			while (matcher6.find()) {

				String[] lines = matcher6.group(1).split("\\n");
				String description = "";
				String date = "";
				String debit = "";
				String credit = "";
				String balance = "";
				Transaction transaction = new Transaction();
				for (int i = 0; i < lines.length; i++) {
					if (i == 0) {
						String templine1 = lines[0].trim();
						int len = templine1.length();
						if (len > 123) {
							date = templine1.substring(0, 12).trim();
							description = templine1.substring(13, 58).trim().replaceAll("\\s+", " ");
							debit = templine1.substring(81, 103).trim();
							credit = templine1.substring(103, 123).trim();
							balance = templine1.substring(123).trim();
							balance = balance.replaceAll("\\-", "");
						}

					} else {
						description += lines[i].trim();
					}
				}
				transaction.setTxnDate(CommonUtils.dateFormatter(date, "dd-MM-yyyy"));
				transaction.setBalance(balance);
				transaction.setCredit(credit);
				transaction.setDebit(debit);
				transaction.setDescription(description.replaceAll("\\s+", " ").trim());
				if (debit != null && !debit.equalsIgnoreCase("") && !debit.equals("0")) {
					transaction.setAmount(debit);
					transaction.setTxnType("DEBIT");
				} else {
					transaction.setAmount(credit);
					transaction.setTxnType("CREDIT");
				}
				transaction.setsNo(String.valueOf(SerialNoCount++));
				transaction.setAccNo(bsInfo.getAccountNo());
				listTransaction.add(transaction);
			}
			bsInfo.setTransactions(listTransaction);
		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in TMBService parseTMB7: " + e);
		}

		log.info("Exiting TMBServiceImpl parseTMB7 with response:" + bsInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for TMBServiceImpl parseTMB7 is ==> " + timeTaken);
		return bsInfo;
	}

	private String[] extractAddressTMB_3(String text) {
		Pattern pattern = Pattern.compile("TRANSACTIONS\\s*([\\s\\S]*?)\\s*(?=Email)", Pattern.DOTALL);
		Matcher matcher = pattern.matcher(text);
		String name = "";
		String address = "";
		if (matcher.find()) {
			int flag = 0;
			String lines = matcher.group();
			String[] parts = lines.split("\n");
			for (int i = 1; i < parts.length; i++) {
				String part = parts[i];
				if (part.length() >= 77 && flag == 1) {
					address += (part.substring(0, 77)).trim();
					address += " ";
				} else if (part.length() > 0 && flag == 1) {
					address += " ";
					address += (part.substring(0, part.length())).trim();
				}

				if (flag == 0) {
					if (part.length() >= 77) {
						name += (part.substring(0, 77)).trim();
						name += " ";
						flag = 1;
					} else if (part.length() > 0) {
						name += " ";
						name += (part.substring(0, part.length())).trim();
						flag = 1;
					}
				}
			}
		}
		return new String[] { address.replaceAll("\\s+", " "), name.replaceAll("\\s+", " ") };
	}

	private List<Transaction> extractTransactionsTMB_3(String pdfText, String accountNo) {
		List<Transaction> transactions = new ArrayList<>();
		pdfText = pdfText.replaceAll(".*Page.*\\n\\s*Tamilnad\\s*Mercantile\\s*Bank.*", "");
		String[] lines = pdfText.split("\n");

		String dateFormat = "dd/MM/yyyy";
		Pattern txnPattern = Pattern.compile("(\\d{2}/\\d{2}/\\d{4})\\s*([\\s\\S]*?)\\s{15,}(\\S+)(\\s*)(\\S+)");
		Pattern pageEnd = Pattern.compile("Grand\\s*Total");

		Transaction currentTransaction = null;
		long serialNoCount = 1;
		for (String line : lines) {
			Matcher pageEndMatcher = pageEnd.matcher(line.trim());
			if (!pageEndMatcher.find()) {
				Matcher txnMatcher = txnPattern.matcher(line);

				if (txnMatcher.find()) {
					currentTransaction = new Transaction();
					currentTransaction.setsNo(String.valueOf(serialNoCount++));
					currentTransaction.setAccNo(accountNo);
					currentTransaction.setTxnDate(CommonUtils.dateFormatter(txnMatcher.group(1), dateFormat));
					currentTransaction.setDescription((txnMatcher.group(2)).replaceAll("\\s+", " ").trim());

					String amount = txnMatcher.group(3) != null ? txnMatcher.group(3) : "";
					String balance = txnMatcher.group(4) != null ? txnMatcher.group(5) : "";
					String whiteSpaces = txnMatcher.group(4);

					if (whiteSpaces.length() > 25) {
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
					currentTransaction.setDescription((currentTransaction.getDescription() + " " + line.replaceAll("\\s+", " ")).trim());
				}
			} else {
				currentTransaction = null;
			}
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsTMB_4(String text, String accountNo) {
		List<Transaction> transactions = new ArrayList<>();
		String[] lines = text.split("\n");

		Pattern transactionPattern = Pattern.compile("(\\d{2}-\\d{2}-\\d{4})\\s*(\\d{2}-\\d{2}-\\d{4})\\s*(.{31}).*?([\\d,\\.]+)(\\s*)([\\d,\\.]+)");
		long sNo = 1;
		for (String line : lines) {
			Matcher matcher = transactionPattern.matcher(line);

			if (matcher.find()) {
				Transaction currentTransaction = new Transaction();
				currentTransaction.setsNo(String.valueOf(sNo++));
				currentTransaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(1), "dd-MM-yyyy"));
				currentTransaction.setValueDate(CommonUtils.dateFormatter(matcher.group(2), "dd-MM-yyyy"));
				currentTransaction.setAccNo(accountNo);
				currentTransaction.setDescription(matcher.group(3).trim());
				currentTransaction.setAmount(matcher.group(4));
				currentTransaction.setBalance(matcher.group(6));
				if (matcher.group(5).length() > 25) { // debit
					currentTransaction.setDebit(currentTransaction.getAmount());
					currentTransaction.setTxnType("DEBIT");
					currentTransaction.setCredit("");
				} else {
					currentTransaction.setCredit(currentTransaction.getAmount());
					currentTransaction.setTxnType("CREDIT");
					currentTransaction.setDebit("");

				}
				transactions.add(currentTransaction);
			}
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsTMB_5_(String pdfText, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();

		int serialNoCount = 1;
		String dateFormat = "dd-MM-yyyy";
		Pattern datePattern = Pattern
				.compile("(\\d{2}-\\d{2}-\\d{4})\\s*(\\d{2}-\\d{2}-\\d{4})\\s*([\\s\\S]*?)\\s+(\\.|\\d{7})\\s+([\\d,]+\\.\\d{2}|\\.)\\s+([\\d,]+\\.\\d{2}|\\.)\\s+([\\d,]+\\.\\d{2})");

		String[] lines = pdfText.split("\\n");

		for (String eachLine : lines) {
			Matcher matcher = datePattern.matcher(eachLine);
			if (matcher.find()) {
				Transaction transaction = new Transaction();
				transaction.setsNo(String.valueOf(serialNoCount++));
				transaction.setAccNo(accountNo);
				transaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(1), dateFormat));
				transaction.setValueDate(CommonUtils.dateFormatter(matcher.group(2), dateFormat));
				transaction.setDescription(matcher.group(3).replaceAll("\\n", " ").replaceAll("\\s+", " ").trim());
				String debit = matcher.group(5);
				String credit = matcher.group(6);
				if (debit.equals(".")) {
					transaction.setCredit(credit);
					transaction.setAmount(credit);
					transaction.setTxnType("CREDIT");
					transaction.setDebit("");
				} else {
					transaction.setDebit(debit);
					transaction.setAmount(debit);
					transaction.setTxnType("DEBIT");
					transaction.setCredit("");
				}
				transaction.setBalance(matcher.group(7));
				transactions.add(transaction);
			}

		}
		return transactions;
	}

	private List<Transaction> extractTransactionsTMB_5(String fileName, String accountNo) throws IOException {
		log.info("Entering extractTransactionsTMB_5");
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
		int serialNumCount = 1;
		String dateFormat = "dd-MM-yyyy";
		Pattern datePattern = Pattern.compile("\\d{2}-\\d{2}-\\d{4}");
		if (txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}
				String[] data = line.split("\\|");
				Matcher dateMatcher = datePattern.matcher(data[0]);
				if (data.length == 7 && dateMatcher.find()) {

					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNumCount++));
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat));
					transaction.setValueDate(CommonUtils.dateFormatter(data[1], dateFormat));
					transaction.setDescription(data[2].replaceAll("\\s+", " ").trim());
					transaction.setDebit(data[4]);
					transaction.setCredit(data[5]);
					transaction.setBalance(data[6].replaceAll("[CD]r", ""));
					if (transaction.getDebit().equals(".")) {
						transaction.setAmount(transaction.getCredit());
						transaction.setTxnType("CREDIT");
						transaction.setDebit("");
					} else {
						transaction.setAmount(transaction.getDebit());
						transaction.setTxnType("DEBIT");
						transaction.setCredit("");
					}
					transaction.setAccNo(accountNo);
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}
}
