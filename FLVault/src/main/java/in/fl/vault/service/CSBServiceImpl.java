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
public class CSBServiceImpl implements CSBService{

	private final static Logger log = Logger.getLogger(CSBServiceImpl.class);

	@Override
	public BSInfo parseCSB1(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering CSBServiceImpl parseCSB1 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		try {
			String filepath = request.getFileName();
			String text = CommonUtils.extractTextFromPdf(filepath, "3");

			bankStatementInfo
					.setName(CommonUtils.extractField(text, "Customer\\s*Name\\s*:(.*)Time").replaceAll("\\s+", " "));
			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*Number\\s*:\\s*(\\S*)"));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*(\\S*)"));
			bankStatementInfo.setAccountType(CommonUtils.extractField(text, "Type\\s*of\\s*Account.*\\n.{99}(.*)"));
			bankStatementInfo.setNominee(CommonUtils.extractField(text, "Nominee\\s*regd.:(.*)"));
			bankStatementInfo.setStartDate(
					CommonUtils.dateFormatter(CommonUtils.extractField(text, "Period\\s*(\\S*)"), "dd-MMM-yyyy"));
			bankStatementInfo.setEnDate(CommonUtils
					.dateFormatter(CommonUtils.extractField(text, "Period\\s*\\S*\\s*-(.*)"), "dd-MMM-yyyy"));
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Branch.*\\n.*\\n\\s*(\\S*)"));
			String[] addressInLines = CommonUtils.extractField(text, "Customer\\s*Address([\\s\\S]*?)\\n\\s*CKYC")
					.split("\n");
			bankStatementInfo.setAddress(getAddressCSB_1(addressInLines).replaceAll("\\s+", " "));
			bankStatementInfo.setTransactions(getTransactionsCSB_1(text, bankStatementInfo.getAccountNo()));
		} catch (Exception e) {
			log.error("Error in CSBServiceImpl parseCSB1: ", e);
		}

		log.info("Exiting CSBServiceImpl parseCSB1: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for CSBServiceImpl is ==>" + timeTaken);
		return bankStatementInfo;
	}
	
	@Override
	public BSInfo parseCSB2(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering CSBServiceImpl parseCSB2 with request : " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "5");
			bankStatementInfo.setName(CommonUtils.extractField(text, "\\s*(.*?)\\s{30}.*\\n\\s*Account\\s*Address")
					.replaceAll("\\s+", " "));
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Home\\s*Branch:(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "Branch\\s*IFSC:(.*)"));
			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*Number:\\s*(\\S*)"));
			bankStatementInfo.setAccountType(
					CommonUtils.extractField(text, "Account\\s*Type:(.*)\\s{30}").replaceAll("\\s+", " "));
			bankStatementInfo.setNominee(
					CommonUtils.extractField(text, "Nomination\\s*Status:(.*)\\s{30}").replaceAll("\\s+", " "));
			String[] addressInLines = CommonUtils
					.extractField(text, "Account\\s*Address:.*\\n([\\s\\S]*?)\\n\\s*Account").split("\n");
			bankStatementInfo.setAddress(getAddressCSB_2(addressInLines).replaceAll("\\s+", " "));
			bankStatementInfo.setStartDate(
					CommonUtils.dateFormatter(CommonUtils.extractField(text, "for.*period(.*)to"), "dd-MMM-yyyy"));
			bankStatementInfo.setEnDate(
					CommonUtils.dateFormatter(CommonUtils.extractField(text, "for.*period.*to(.*)"), "dd-MMM-yyyy"));
			bankStatementInfo.setTransactions(extractTransactionsCSB_2(filepath, bankStatementInfo.getAccountNo()));

		} catch (Exception e) {
			log.error("Error in CSBServiceImpl parseCSB2: ", e);
		}

		log.info("Exiting CSBServiceImpl parseCSB2 : " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for CSBServiceImpl parseCSB2 is ==>" + timeTaken);
		return bankStatementInfo;
	}

	@Override
	public BSInfo parseCSB3(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering CSBServiceImpl parseCSB3 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		try {
			String filepath = request.getFileName();
			String text = CommonUtils.extractTextFromPdf(filepath, "5");
			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*Number\\s*(\\S*)"));
			bankStatementInfo.setName(CommonUtils.extractField(text, "Account\\s*Name\\s*(.*)").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setStartDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "Period\\s*From\\s*(.*)"),"dd-MM-yyyy"));
			bankStatementInfo.setEnDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "Period\\s*To\\s*(.*)"),"dd-MM-yyyy"));
			bankStatementInfo.setTransactions(extractTransactionsCSB_3(filepath, bankStatementInfo.getAccountNo()));
		} catch (Exception e) {
			log.error("Error in CSBServiceImpl parseCSB3: ", e);
		}

		log.info("Exiting CSBServiceImpl parseCSB3: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for CSBServiceImpl parseCSB3 is ==>" + timeTaken);
		return bankStatementInfo;
	}
	
	@Override
	public BSInfo parseCSB4(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering CSBServiceImpl parseCSB4 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String pdfText = CommonUtils.extractTextFromPdf(filePath, "4");

			bankStatementInfo.setAccountNo(CommonUtils.extractField(pdfText, "Account\\s*Number\\s*:\\s*(\\S*)"));
			bankStatementInfo.setAccountType(
					CommonUtils.extractField(pdfText, "Account\\s*Type\\s*:\\s*(.*)").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*:\\s*(\\S*)"));
			bankStatementInfo.setEmail(CommonUtils.extractField(pdfText, "Email\\s*:\\s*(\\S*)"));
			bankStatementInfo.setPhone1(CommonUtils.extractField(pdfText, "Phone\\s*:\\s*(\\S*)"));
			bankStatementInfo.setBranch(CommonUtils.extractField(pdfText, "Home\\s*Branch\\s*:\\s*(.*)"));
			String name = CommonUtils.extractMultiLinesField(pdfText,
					"Home\\s*Branch.*\\n([\\s\\S]*?)(?=\\s*Account\\s*Address)", 70);
			bankStatementInfo.setName(name.replaceAll("\\s+", " ").trim());
			String address = CommonUtils.extractMultiLinesField(pdfText,
					"Account\\s*Address.*([\\s\\S]*?)(?=\\s*Account\\s*Statement)", 70);
			bankStatementInfo.setAddress(address.replaceAll("\\s+", " "));

			String[] period = CommonUtils.extractMultiGroupArray(pdfText,
					"for\\s*the\\s*period\\s*(\\d{2}-\\w{3}-\\d{4}).*(\\d{2}-\\w{3}-\\d{4})");
			String dateFormat = "dd-MMM-yyyy";
			if (period != null && period.length > 1) {
				bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
				bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
			}
			List<Transaction> transactions = extractTransactionsCSB_4(filePath, bankStatementInfo.getAccountNo());
			bankStatementInfo.setTransactions(transactions);
		} catch (Exception e) {
			log.error("Error in CSBServiceImpl parseCSB4: " + e);
		}

		log.info("Exiting CSBServiceImpl parseCSB4: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for CSBServiceImpl parseCSB4 is ==>" + timeTaken);
		return bankStatementInfo;
	}
	
	@Override
	public BSInfo parseCSB5(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering CSBServiceImpl parseCSB5 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String pdfText = CommonUtils.extractTextFromPdf(filePath, "3");

			bankStatementInfo.setAccountNo(CommonUtils.extractField(pdfText, "Account\\s*Number\\s*:\\s*(\\S*)"));
			bankStatementInfo.setAccountType(CommonUtils
					.extractField(pdfText, "Type\\s*of\\s*Account\\s*:\\s*(.{0,30})").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setNominee(CommonUtils.extractField(pdfText, "Nominee\\s*:\\s*(.{0,40})"));
			bankStatementInfo.setBranch(CommonUtils.extractField(pdfText, "Home\\s*Branch\\s*:\\s*(.{0,50})"));
			bankStatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFS\\s*Code\\s*:\\s*(\\S*)"));

			Pattern pattern = Pattern.compile("(^[\\s\\S]*?)(?=^.*Customer\\s*ID)", Pattern.MULTILINE);
			Matcher matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				String[] addressLine = matcher.group(1).split("\\n");
				String address = "";
				for (int line = 0; line < addressLine.length; line++) {
					String text = (addressLine[line].length() > 70 ? addressLine[line].substring(0, 70).trim()
							: addressLine[line]);
					text = text.replaceAll("\\s+", " ").trim();
					if (line == 0) {
						bankStatementInfo.setName(text);
					} else if (line < addressLine.length - 1) {
						address += " " + text;
					} else {
						if (text.contains("@"))
							bankStatementInfo.setEmail(text);
						else
							address += " " + text;
					}
				}
				bankStatementInfo.setAddress(address);
			}

			String[] period = CommonUtils.extractMultiGroupArray(pdfText,
					"for\\s*the\\s*period\\s*:\\s*(\\d{2}\\s*\\w{3}\\s*\\d{4})\\s*to\\s*(\\d{2}\\s*\\w{3}\\s*\\d{4})");
			String dateFormat = "dd MMM yyyy";
			if (period != null && period.length > 1) {
				bankStatementInfo
						.setStartDate(CommonUtils.dateFormatter(period[0].replaceAll("\\s+", " "), dateFormat));
				bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
			}
			List<Transaction> transactions = getTransactionsCSB_5(pdfText, bankStatementInfo.getAccountNo());
			bankStatementInfo.setTransactions(transactions);
		} catch (Exception e) {
			log.error("Error in CSBServiceImpl parseCSB5: " + e);
		}

		log.info("Exiting CSBServiceImpl parseCSB5: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for CSBServiceImpl parseCSB5 is ==>" + timeTaken);
		return bankStatementInfo;
	}
	
	@Override
	public BSInfo parseCSB6(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering CSBServiceImpl parseCSB6 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String pdfText = CommonUtils.extractTextFromPdf(filePath, "3");

			bankStatementInfo.setAccountNo(CommonUtils.extractField(pdfText, "Account\\s*Number\\s*:\\s*(\\S*)"));
			bankStatementInfo.setAccountType(CommonUtils
					.extractField(pdfText, "Type\\s*of\\s*Account\\s*:\\s*(.{0,30})").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setBranch(CommonUtils.extractField(pdfText, "Home\\s*Branch\\s*:\\s*(.{0,50})"));
			bankStatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFS\\s*Code\\s*:\\s*(\\S*)"));
			bankStatementInfo.setAccountType(CommonUtils.extractField(pdfText, "Type\\s*Of\\s*Account\\s*:\\s*(.{5,})").replaceAll("\\s+", ""));
			
			String[] lines = pdfText.split("\\n");
			String name = "";
			String address = "";
			Pattern endPattern = Pattern.compile("Customer\\s*ID");
			Pattern emailPattern = Pattern.compile("(\\w*@\\w*\\.[a-z]*)");
			for (String eachLine : lines) {
				Matcher endMatcher = endPattern.matcher(eachLine);
				Matcher emaiMatcher = emailPattern.matcher(eachLine);
				if(endMatcher.find()) {
					break;
				}
				if(emaiMatcher.find()) {
					bankStatementInfo.setEmail(emaiMatcher.group(1));
					continue;
				}
				if(!eachLine.trim().equalsIgnoreCase("")) {
					if(name == "") {
						name = eachLine;
					}
					address+=eachLine;
				}
			}
			bankStatementInfo.setAddress(address.replaceAll("_", "").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setName(name.replaceAll("\\s+", " ").trim());

			String[] period = CommonUtils.extractMultiGroupArray(pdfText,"period\\s*:\\s*(\\d{2}-[A-Za-z]{3}-\\d{4}).*(\\d{2}-[A-Za-z]{3}-\\d{4})");
			String dateFormat = "dd-MMM-yyyy";
			if (period != null && period.length > 1) {
				bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
				bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
			}
			List<Transaction> transactions = extractTransactionsCSB_6(filePath, bankStatementInfo.getAccountNo(), dateFormat);
			bankStatementInfo.setTransactions(transactions);
		} catch (Exception e) {
			log.error("Error in CSBServiceImpl parseCSB6: " + e);
		}

		log.info("Exiting CSBServiceImpl parseCSB6: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for CSBServiceImpl parseCSB6 is ==>" + timeTaken);
		return bankStatementInfo;
	}
	
	@Override
	public BSInfo parseCSB7(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering CSBServiceImpl parseCSB7 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String pdfText = CommonUtils.extractTextFromPdf(filePath, "4");

			bankStatementInfo.setAccountNo(CommonUtils.extractField(pdfText, "Account\\s*No\\s*:\\s*(\\S*)"));
			bankStatementInfo.setAccountType(CommonUtils.extractField(pdfText, "Account\\s*Type\\s*:\\s*(.{0,40})")
					.replaceAll("\\s+", " ").trim());
			bankStatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*:\\s*(\\S*)"));

			Matcher phone = Pattern.compile("(^\\s*Mobile[\\s\\S]*? EmailId.*)", Pattern.MULTILINE).matcher(pdfText);
			if (phone.find()) {
				String number = phone.group(1).replaceAll("\\r?\\n", "");
				Matcher numMatcher = Pattern.compile("\\b(\\d{8,})\\b").matcher(number);
				while (numMatcher.find()) {
					bankStatementInfo.setPhone1(numMatcher.group(1));
				}

				Matcher emailMatcher = Pattern.compile("([a-zA-Z0-9._%+-]+@gmail\\.com)").matcher(number);
				while (emailMatcher.find()) {
					bankStatementInfo.setEmail(emailMatcher.group(1));
				}
			}
			bankStatementInfo.setBranch(CommonUtils.extractField(pdfText, "Account\\s*Branch\\s*:\\s*(\\S*)"));
			bankStatementInfo.setName(CommonUtils.extractField(pdfText, "Account\\s*Title\\s*:\\s*(.{0,40})"));
			String address = CommonUtils.extractMultiLinesField(pdfText,
					"\\n(\\s*Address[\\s\\S]*)?\\n\\s*Mobile\\s*No", 70);
			bankStatementInfo.setAddress(address.replaceAll("\\s+", " "));

			String[] period = CommonUtils.extractMultiGroupArray(pdfText,
					"From\\s*Date\\s*:\\s*(\\S*)\\s*To\\s*Date\\s*:\\s*(\\S*)");
			String dateFormat = "dd-MMM-yyyy";
			if (period != null && period.length > 1) {
				bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
				bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
			}
			List<Transaction> transactions = extractTransactionsCSB_7(filePath, bankStatementInfo.getAccountNo());
			bankStatementInfo.setTransactions(transactions);
		} catch (Exception e) {
			log.error("Error in CSBServiceImpl parseCSB7: " + e);
		}

		log.info("Exiting CSBServiceImpl parseCSB7: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for CSBServiceImpl parseCSB7 is ==>" + timeTaken);
		return bankStatementInfo;
	}
	
	
	@Override
	public BSInfo parseCSB8(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering CSBServiceImpl parseCSB8 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String pdfText = CommonUtils.extractTextFromPdf(filePath, "3");

			bankStatementInfo.setAccountNo(CommonUtils.extractField(pdfText, "Account\\s*Number\\s*:\\s*(\\S*)"));
			bankStatementInfo.setAccountType(CommonUtils
					.extractField(pdfText, "Type\\s*of\\s*Account\\s*:\\s*(.{0,30})").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setNominee(CommonUtils.extractField(pdfText, "Nominee\\s*.*:\\s*(.*)"));
			bankStatementInfo.setBranch(CommonUtils.extractField(pdfText, "Home\\s*Branch\\s*:\\s*(.*)"));
			bankStatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*Code\\s*:\\s*(CSBK\\w{7})"));

			Pattern pattern = Pattern.compile("(^[\\s\\S]*?)(?=^.*Customer\\s*ID)", Pattern.MULTILINE);
			Matcher matcher = pattern.matcher(pdfText);
			if (matcher.find()) {
				String[] addressLine = matcher.group(1).split("\\n");
				String address = "";
				for (int line = 0; line < addressLine.length; line++) {
					String text = (addressLine[line].length() > 70 ? addressLine[line].substring(0, 70).trim()
							: addressLine[line]);
					text = text.replaceAll("\\s+", " ").trim();
					if (line == 0) {
						bankStatementInfo.setName(text);
					} else if (line < addressLine.length - 1) {
						address += " " + text;
					} else {
						if (text.contains("@"))
							bankStatementInfo.setEmail(text);
						else
							address += " " + text;
					}
				}
				bankStatementInfo.setAddress(address);
			}

			String[] period = CommonUtils.extractMultiGroupArray(pdfText,
					"period\\s*:\\s*(\\d{2}-\\w{3}-\\d{4}).*(\\d{2}-\\w{3}-\\d{4})");
			String dateFormat = "dd-MMM-yyyy";
			if (period != null && period.length > 1) {
				bankStatementInfo
						.setStartDate(CommonUtils.dateFormatter(period[0].replaceAll("\\s+", " "), dateFormat));
				bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
			}
			List<Transaction> transactions = getTransactionsCSB_8(pdfText, bankStatementInfo.getAccountNo());
			bankStatementInfo.setTransactions(transactions);
		} catch (Exception e) {
			log.error("Error in CSBServiceImpl parseCSB8: " + e);
		}

		log.info("Exiting CSBServiceImpl parseCSB8: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for CSBServiceImpl parseCSB8 is ==>" + timeTaken);
		return bankStatementInfo;
	}
	
	private List<Transaction> getTransactionsCSB_5(String pdfText, String accountNo)
			throws IOException, InterruptedException {
		List<Transaction> transactions = new ArrayList<>();
		Pattern removePattern = Pattern.compile("(?m)^\\s*Page[\\s\\S]*?Balance\\s*\\n?", Pattern.MULTILINE);
		Matcher removeMatcher = removePattern.matcher(pdfText);
		pdfText = removeMatcher.replaceAll("");

		Pattern pattern = Pattern.compile("(^\\s*\\d{2}-\\w{3}-\\d{4}[\\s\\S]*?)(?=^\\s*\\d{2}-\\w{3}-\\d{4}|\\s*Page)",
				Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(pdfText);

		int serialNumCount = 1;
		String firstLine = "";
		while (matcher.find()) {
			String txnDate = "", valueDate = "", credit = "", debit = "";
			String description = "", balance = "";
			String[] lines = matcher.group(1).split("\\n");
			String line1 = lines[0].replaceFirst("^\\s+", "");
			String line1Desc = line1.substring(39, 100).trim();

			if (line1Desc.isEmpty() && lines.length > 1) {
				if (lines.length > 2) {
					description = firstLine + " " + lines[1];
					firstLine = lines[2];
				} else {
					description = firstLine + " " + lines[1];
					firstLine = "";
				}
			}
			if (!line1Desc.isEmpty()) {
				description = line1.substring(39, 108);
				if (lines.length > 1)
					firstLine = lines[1].trim();
				else
					firstLine = "";
			}

			description = description.replaceAll("\\s+", " ").trim();
			Transaction transaction = new Transaction();
//			System.out.println(line1);
			if (line1.length() > 156) {
				txnDate = line1.substring(0, 17).trim();
				valueDate = line1.substring(17, 39);
				debit = line1.substring(128, 144).trim();
				credit = line1.substring(144, 156).trim();
				balance = line1.substring(156).trim();
				if (debit.equals("0.00") || debit.isEmpty()) {
					transaction.setAmount(credit);
					transaction.setTxnType("CREDIT");
				} else {
					transaction.setAmount(debit);
					transaction.setTxnType("DEBIT");
				}
				transaction.setDebit(debit);
				transaction.setCredit(credit);
				transaction.setDescription(description);
				transaction.setAccNo(accountNo);
				transaction.setBalance(balance);
				transaction.setTxnDate(CommonUtils.dateFormatter(txnDate, "dd-MMM-yyyy"));
				transaction.setValueDate(CommonUtils.dateFormatter(valueDate, "dd-MMM-yyyy"));
			}
			transactions.add(transaction);
			transaction.setsNo(Integer.toString(serialNumCount++));
		}
		return transactions;
	}

	
	private String getAddressCSB_1(String[] addressArray) {
		String result = "";
		boolean isFirstLine = true;
		for (String str : addressArray) {
			if (isFirstLine) {
				isFirstLine = false;
				continue;
			}
			if (str.length() > 54) {
				result += (CommonUtils.extractField(str, "^(.{52})") + " ");
			} else {
				result += (CommonUtils.extractField(str, "(.*)") + " ");
			}
		}
		return result.trim();
	}
	
	private String getAddressCSB_2(String[] addressArray) {
		String result = "";
		for (String str : addressArray) {
			if (str.length() > 80) {
				result += (CommonUtils.extractField(str, "\\s*(.*)\\s{30}.*") + " ");
			} else {
				result += (CommonUtils.extractField(str, "(.*)") + " ");
			}
		}
		return result.trim();
	}

	private List<Transaction> getTransactionsCSB_1(String pdfText, String accountNo)
			throws IOException, InterruptedException {
		List<Transaction> transactions = new ArrayList<>();

		pdfText = pdfText.replaceAll("\\s*STATEMENT[\\s\\S]*?Date.*Balance\\n.*Balance.*", "")
				.replaceAll("\\s*Total[\\s\\S]*", "").replaceAll("\\s*Balance.*", "");

		String[] textInArray = pdfText.split("\n");
		String regex = "^\\s*(\\d{2}-\\w{3}-\\d{4})\\s+(\\S+)\\s+(.+?)\\s*([\\d\\,]+\\.\\d{2})(\\s*)([\\d\\,]+\\.\\d{2}\\s*\\w{2})";

		String txnDate = "", valueDate = "", credit = "", debit = "", txnType = "";
		String description = "", amount = "", balance = "";
		int serialNumCount = 1;

		for (String eachLine : textInArray) {
			String data[] = CommonUtils.extractMultiGroupArray(eachLine, regex);
			// it matches the pattern
			if (data != null && data.length == 6) {

				if (!description.replaceAll("\\s+", " ").trim().equals("")) {

					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNumCount++));
					transaction.setTxnDate(CommonUtils.dateFormatter(txnDate, "dd-MMM-yyyy"));
					transaction.setValueDate(CommonUtils.dateFormatter(valueDate, "dd-MMM-yyyy"));
					transaction.setDescription(description.replaceAll("\\s+", " ").trim());
					transaction.setCredit(credit);
					transaction.setDebit(debit);
					transaction.setTxnType(txnType);
					transaction.setAmount(amount);
					transaction.setAccNo(accountNo);
					transaction.setBalance(balance);
//					System.out.println("Hii " + transaction);
					transactions.add(transaction);
				}
				txnDate = data[0];
				valueDate = data[1];
				description = data[2];
				if (data[4].length() < 27) { // credit
					credit = data[3];
					debit = "";
					txnType = "CREDIT";
					amount = credit;
				} else {
					debit = data[3];
					credit = "";
					txnType = "DEBIT";
					amount = debit;
				}
				balance = data[5].substring(0, data[5].length() - 3).trim();

			} else {
				description += (" " + CommonUtils.extractField(eachLine, "^\\s*(.*)"));
			}
		}
		Transaction transaction = new Transaction();
		transaction.setsNo(Integer.toString(serialNumCount++));
		transaction.setTxnDate(CommonUtils.dateFormatter(txnDate, "dd-MMM-yyyy"));
		transaction.setValueDate(CommonUtils.dateFormatter(valueDate, "dd-MMM-yyyy"));
		transaction.setDescription(description.replaceAll("\\s+", " ").trim());
		transaction.setCredit(credit);
		transaction.setDebit(debit);
		transaction.setTxnType(txnType);
		transaction.setAmount(amount);
		transaction.setAccNo(accountNo);
		transaction.setBalance(balance);
		transactions.add(transaction);

		return transactions;
	}

	private List<Transaction> extractTransactionsCSB_2(String fileName, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
		
		int serialNoCount = 1;
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");
				
				if(line.trim().isEmpty()) {
					continue;
				}
				// Assuming first column is txnDate, second column is value
				String[] data = line.split("\\|");
				if (data.length == 7 && !data[0].equals("Date") && !data[1].equals("Opening Balance")
						&& !data[1].equals("Closing Balance")) {

					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], "dd-MMM-yyyy"));
					transaction.setDescription(data[1]);
					transaction.setValueDate(CommonUtils.dateFormatter(data[3], "dd-MMM-yyyy"));
					transaction.setDebit(data[4]);
					transaction.setCredit(data[5]);
					if (data[5].equals("0.00")) {
						transaction.setAmount(transaction.getDebit());
						transaction.setTxnType("DEBIT");
						transaction.setCredit("");
					} else {
						transaction.setAmount(transaction.getCredit());
						transaction.setTxnType("CREDIT");
						transaction.setDebit("");
					}
					transaction.setAccNo(accountNo);
					transaction.setBalance(data[6]);
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsCSB_3(String fileName, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
		
		int serialNoCount = 1;
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");
				
				if(line.trim().isEmpty()) {
					continue;
				}
		
				String[] data = line.split("\\|");
				if (data.length == 5 && !data[0].equals("Date") && !data[1].equalsIgnoreCase("OPENING BALANCE")&& !data[1].equals("Closing Balance")){
					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], "dd-MM-yyyy"));
					transaction.setDescription(data[1]);
					transaction.setDebit(data[2]);
					transaction.setCredit(data[3]);
					if (data[3].equals("")) {
						transaction.setAmount(transaction.getDebit());
						transaction.setTxnType("DEBIT");
						transaction.setCredit("");
					} else {
						transaction.setAmount(transaction.getCredit());
						transaction.setTxnType("CREDIT");
						transaction.setDebit("");
					}
					transaction.setAccNo(accountNo);
					transaction.setBalance(data[4]);
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsCSB_4(String filePath, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(filePath);

		int serialNoCount = 1;
		String dateFormat = "dd-MMM-yyyy";
		Pattern datePattern = Pattern.compile("\\d{2}-\\w{3}-\\d{4}");
		if (txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}
				String[] lineArr = line.split("\\|");
				if (lineArr.length <= 1)
					continue;
				Matcher dateMatcher = datePattern.matcher(lineArr[0]);
				Matcher dateMatcher2 = datePattern.matcher(lineArr[3]);
				if (dateMatcher.find() && dateMatcher2.find()) {
					Transaction transaction = new Transaction();
					transaction.setsNo(String.valueOf(serialNoCount++));
					transaction.setAccNo(accountNo);
					transaction.setTxnDate(CommonUtils.dateFormatter(lineArr[0], dateFormat));
					transaction.setValueDate(CommonUtils.dateFormatter(lineArr[3], dateFormat));
					String desc = lineArr[1];
					transaction.setCredit(lineArr[5]);
					transaction.setDebit(lineArr[4]);
					transaction.setDescription(desc);
					if (transaction.getDebit().equalsIgnoreCase("0.00") || transaction.getDebit() == null) {
						transaction.setAmount(transaction.getCredit());
						transaction.setTxnType("CREDIT");
						transaction.setDebit("");
					} else {
						transaction.setAmount(transaction.getDebit());
						transaction.setTxnType("DEBIT");
						transaction.setCredit("");
					}
					transaction.setBalance(lineArr[6]);
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}
	
	private List<Transaction> extractTransactionsCSB_5(String pdfText, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		pdfText = pdfText.replaceAll(".*Page.*\\n([\\s\\S])*?Transaction\\s*Date.*Balance", "").replaceAll("Legends\\s*for\\s*transactions[\\s\\S]*? Page.*", "");
		
		Pattern txnStartPatt = Pattern.compile("Transaction\\s*Date.*Balance");
		String[] lines = pdfText.split("\\n");
		
		List<String> txnLines = new ArrayList<>();
		
		boolean status = false;
		for(String eachLine: lines) {
			if(status) {
				txnLines.add(eachLine);
				continue;
			}
			Matcher matcher = txnStartPatt.matcher(eachLine);
			if(!status && matcher.find()) {
				status = true;
			}
		}
		
		
		for (String eachTxnLine : txnLines) {
			System.out.println(eachTxnLine);
		}
		
		Pattern lineWithoutDesc = Pattern.compile("\\d{2}-[A-Z]{3}-\\d{4}\\s*\\d{2}-[A-Z]{3}-\\d{4}\\s{20,}\\d*\\s*(-?\\d*,?\\d*,?\\d+\\.\\d+)\\s*(-?\\d*,?\\d*,?\\d+\\.\\d+)\\s*(-?\\d*,?\\d*,?\\d+\\.\\d+)");
		
		String dateFormat = "dd-MMM-yyyy";
		Pattern datePattern = Pattern.compile("\\d{2}-\\w{3}-\\d{4}");
		int serialNoCount = 1;
		
		
		return transactions;
	}
	
	private List<Transaction> extractTransactionsCSB_6(String fileName, String accountNo, String dateFormat) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
		int serialNumCount = 1;
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}
				String[] data = line.split("\\|");
				if (data.length == 7 && !data[0].contains("TRANS")) {
					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNumCount++));
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat)); 
					transaction.setValueDate(CommonUtils.dateFormatter(data[1], dateFormat)); 
					transaction.setDescription(data[2]);
					transaction.setBalance(data[6]);
					transaction.setCredit(data[5]);
					transaction.setDebit(data[4]);
					if (transaction.getDebit().equals("0.00")) {
						transaction.setAmount(transaction.getCredit());
						transaction.setTxnType("CREDIT");
					}else {
						transaction.setAmount(transaction.getDebit());
						transaction.setTxnType("DEBIT");
					}
					transaction.setAccNo(accountNo);
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}
	
	private List<Transaction> extractTransactionsCSB_7(String filePath, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(filePath);

		int serialNoCount = 1;
		String dateFormat = "dd-MMM-yyyy";
		Pattern datePattern = Pattern.compile("\\d{2}-\\w{3}-\\s*\\d{4}");
		if (txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}
				String[] lineArr = line.split("\\|");
				if (lineArr.length <= 6)
					continue;
				Matcher dateMatcher = datePattern.matcher(lineArr[0]);
				Matcher dateMatcher2 = datePattern.matcher(lineArr[1]);
				if (!dateMatcher.find() || !dateMatcher2.find())
					continue;

				Transaction transaction = new Transaction();
				transaction.setsNo(String.valueOf(serialNoCount++));
				transaction.setAccNo(accountNo);
				String txnDate = lineArr[0].replaceAll("\\s*", "");
				String valueDate = lineArr[1].replaceAll("\\s*", "");
				transaction.setTxnDate(CommonUtils.dateFormatter(txnDate, dateFormat));
				transaction.setValueDate(CommonUtils.dateFormatter(valueDate, dateFormat));
				String desc = lineArr[3];
				transaction.setCredit(lineArr[5]);
				transaction.setDebit(lineArr[4]);
				transaction.setDescription(desc);
				if (transaction.getDebit().equalsIgnoreCase("0.00") || transaction.getDebit() == null) {
					transaction.setAmount(transaction.getCredit());
					transaction.setTxnType("CREDIT");
					transaction.setDebit("");
				} else {
					transaction.setAmount(transaction.getDebit());
					transaction.setTxnType("DEBIT");
					transaction.setCredit("");
				}
				transaction.setBalance(lineArr[6]);
				transactions.add(transaction);
			}
		}
		return transactions;
	}
	
	
private List<Transaction> getTransactionsCSB_8(String pdfText, String accountNo){
		
		pdfText = pdfText.replaceAll(".*Page[\\s\\S]*?Debit\\s*Credit\\s*Balance\\s*\\n", "");
		List<Transaction> transactions = new ArrayList<>();
		
		Pattern txnPattern1 = Pattern.compile("([A-Z]{3}\\s*\\d{1,2}\\s*,\\s*\\d{4})\\s*(.{80})\\s*(\\S+)\\s*(\\S+)\\s*(-?INR\\s*\\S+)\\s*[C|D]?r?");
		Pattern txnStart = Pattern.compile("Date.*Debit\\s*Credit\\s*Balance");

		String dateFormat1 = "MMMddyyyy";
		String dateFormat2 = "MMMdyyyy";
		int serialNumCount = 1;
		String [] lines = pdfText.split("\\n");
		boolean txnStatus = false;
		Transaction transaction = new Transaction();
		for(String eachline: lines) {
			Matcher txnMatcher1 = txnPattern1.matcher(eachline);
			Matcher txnStartMatcher1 = txnStart.matcher(eachline);
			if(eachline.replaceAll("\\s+", " ").contains("End of statement")) {
				if(transaction.getTxnDate() != null) {
					transactions.add(transaction);
				}
				break;
			}
			
			if(txnStartMatcher1.find()) {
				txnStatus = true;
				continue;
			}
			
			if(txnStatus) {
				if(txnMatcher1.find()) {
					if(transaction.getTxnDate() != null) {
						transactions.add(transaction);
						transaction = new Transaction();
					}
					transaction.setsNo(Integer.toString(serialNumCount++));
					transaction.setAccNo(accountNo);
					String date = txnMatcher1.group(1).replaceAll("[\\s,]", "").trim();
					if(date.length() == 9) {
						transaction.setTxnDate(CommonUtils.dateFormatter(date, dateFormat1));
					}else if(date.length() == 8) {
						transaction.setTxnDate(CommonUtils.dateFormatter(date, dateFormat2));
					}
					transaction.setDescription(txnMatcher1.group(2).replaceAll("\\s+", " "));
					String debit = txnMatcher1.group(3);
					String credit = txnMatcher1.group(4);
					String balance = txnMatcher1.group(5).replaceAll("\\s*", "").replaceAll("INR", "");
					transaction.setBalance(balance);
					if(debit.equalsIgnoreCase("-")) {
						transaction.setAmount(credit);
						transaction.setTxnType("CREDIT");
						transaction.setCredit(credit);
					}else if(credit.equalsIgnoreCase("-")) {
						transaction.setAmount(debit);
						transaction.setDebit(debit);
						transaction.setTxnType("DEBIT");
					}
				}else {
					String desc = "";
					if(transaction != null && transaction.getDescription() != null) {
						desc = transaction.getDescription();
					}
					desc += eachline;
					transaction.setDescription(desc.replaceAll("\\s+", " ").trim());		
				}
			}
		}
		return transactions;
	}

}