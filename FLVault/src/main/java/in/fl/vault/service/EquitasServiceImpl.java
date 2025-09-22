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
public class EquitasServiceImpl implements EquitasService{

	private final Logger log = Logger.getLogger(EquitasServiceImpl.class);

	@Override
	public BSInfo parseEquitas1(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering EquitasServiceImpl parseEquitas1 with request : " + request);

		BSInfo bankStatementInfo = new BSInfo();
		try {
			String filepath = request.getFileName();
			String text = CommonUtils.extractTextFromPdf(filepath, "3");

			bankStatementInfo.setName(CommonUtils.extractField(text, "(.*)Account\\s*No").replaceAll("\\s+", " "));
			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*No\\s*:(.*)"));
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*:(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setPhone1(CommonUtils.extractField(text, "Phone\\s*Nos:(.*),.*"));
			bankStatementInfo.setPhone2(CommonUtils.extractField(text, "Phone\\s*Nos:.*,(.*)"));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*:(.*)"));
			bankStatementInfo.setNominee(CommonUtils.extractField(text, "Nomination\\s*:(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setStartDate(
					CommonUtils.dateFormatter(CommonUtils.extractField(text, "Period:(.*)to"), "dd-MMM-yy"));
			bankStatementInfo.setEnDate(
					CommonUtils.dateFormatter(CommonUtils.extractField(text, "Period.*to(.*)"), "dd-MMM-yy"));
			String[] dataArr = CommonUtils.extractMultiGroupArray(text,
					"Account\\s*No.*\\n\\s*(.*?)\\s{4}(.*)\\n(.*)Branch.*\\n.*\\n\\s*(.*?)\\s*(\\d{6}?).*\\n\\s*(.*?)\\s{10}");
			if(dataArr != null) {
				bankStatementInfo.setAccountType(dataArr[1].replaceAll("\\s+", " "));
				bankStatementInfo.setAddress(
						(dataArr[0] + " " + dataArr[2] + " " + dataArr[3] + " " + dataArr[4] + " " + dataArr[5]).replaceAll("\\s+", " "));
			}
			bankStatementInfo
					.setTransactions(extractTransactionsEquita_1(filepath, bankStatementInfo.getAccountNo()));

		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in EquitasServiceImpl parseEquitas1: ", e);
		}
		log.info("Exiting EquitasServiceImpl parseEquitas1: " + bankStatementInfo);
		log.info("Time Taken for EquitasServiceImpl parseEquitas1 is ==>"
				+ (System.currentTimeMillis() - startTimeInMillis));
		return bankStatementInfo;
	}
	
	@Override
	public BSInfo parseEquitas2(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering EquitasServiceImpl parseEquitas2 with request : " + request);

		BSInfo bankStatementInfo = new BSInfo();
		try {
			String filepath = request.getFileName();
			String text = CommonUtils.extractTextFromPdf(filepath, "3");

			bankStatementInfo.setName(CommonUtils.extractField(text, "Customer\\s*Name(.*)Branch").replaceAll("\\s+", " "));
			bankStatementInfo.setNominee(CommonUtils.extractField(text, "Nominee\\s*Name(.*)Branch").replaceAll("\\s+", " "));
			bankStatementInfo.setPhone1(CommonUtils.extractField(text, "Mobile\\s*(\\d*)\\s*"));
			bankStatementInfo.setEmail(CommonUtils.extractField(text, "Email\\s*(\\S*)"));
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*Name(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setAccountType(CommonUtils.extractField(text, "Account\\s*Type(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*Number(.*)"));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*Code(.*)"));
			String txnDate = CommonUtils.extractField(text, "Date.*?ClosingBalance\\n.*?INR\\n\\s*(\\S*)");
			String dateFormat = "dd-MMM-yyyy";
			bankStatementInfo.setStartDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "Period\\s*from(.*)to"), dateFormat));
			bankStatementInfo.setEnDate(CommonUtils.dateFormatter(CommonUtils.extractField(text, "Period\\s*from.*to(.*)"), dateFormat));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "\\n(\\s*Address[\\s\\S]*?)\\n\\s*Joint\\s*Holder", 100));
			if (txnDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
				dateFormat = "yyyy-MM-dd";
			}
			bankStatementInfo.setTransactions(extractTransactionsEquita_2(text, bankStatementInfo.getAccountNo(), dateFormat));
		} catch (Exception e) {
			log.error("Error in EquitasServiceImpl parseEquitas2 :", e);
		}
		log.info("Exiting EquitasServiceImpl parseEquitas2 : " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for EquitasServiceImpl parseEquitas2 is ==>" + timeTaken);
		return bankStatementInfo;
	}
	
	@Override
	public BSInfo parseEquitas3(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering EquitasServiceImpl parseEquitas3 with request : " + request);

		BSInfo bankStatementInfo = new BSInfo();
		try {
			String filepath = request.getFileName();
			String text = CommonUtils.extractTextFromPdf(filepath, "3");

			bankStatementInfo.setName(CommonUtils.extractField(text, "(.*)Email\\s*ID").replaceAll("\\s+", " "));
			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "account\\s*no.:(.*)Account"));
			bankStatementInfo.setEmail(CommonUtils.extractField(text, "Email\\s*ID\\s*:(.*)"));
			bankStatementInfo.setPhone1(CommonUtils.extractField(text, "Mobile\\s*No\\s*:(.*)"));
			bankStatementInfo.setPhone2(CommonUtils.extractField(text, "Phone\\s*No\\s*:(.*)"));
			bankStatementInfo
					.setAccountType(CommonUtils.extractField(text, "Account\\s*Type\\s*:(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*Code\\s*:(.*)MICR"));
			bankStatementInfo
					.setNominee(CommonUtils.extractField(text, "Nominee.*\\n\\s*\\d*\\s*(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setStartDate(CommonUtils
					.dateFormatter(CommonUtils.extractField(text, "Statement.*period\\s*from(.*)To"), "dd/MM/yyyy"));
			bankStatementInfo.setEnDate(CommonUtils
					.dateFormatter(CommonUtils.extractField(text, "Statement.*period\\s*from.*To(.*)"), "dd/MM/yyyy"));
			String[] addressInLines = CommonUtils
					.extractField(text, "Email\\s*ID.*\\n([\\s\\S]*?)\\n\\s*Your\\s*relationship").split("\n");
			bankStatementInfo.setAddress(getAddressEquita_3(addressInLines).replaceAll("\\s+", " "));
			bankStatementInfo
					.setTransactions(extractTransactionsEquita_3(filepath, bankStatementInfo.getAccountNo()));

		} catch (Exception e) {
			log.error("Error in EquitasServiceImpl parseEquitas3:", e);
		}
		log.info("Exiting EquitasServiceImpl parseEquitas3: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for EquitasServiceImpl parseEquitas3 is ==>" + timeTaken);
		return bankStatementInfo;
	}

	private String getAddressEquita_3(String[] addressArray) {
		String result = "";
		for (String str : addressArray) {
			if (str.length() > 109) {
				result += (str.substring(0, 105) + " ");
			} else {
				result += (str + " ");
			}
//			System.out.println("Result is : " + result + " after " + str);
		}
		return result.trim();
	}
	
	@Override
	public BSInfo parseEquitas4(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering EquitasServiceImpl parseEquitas4 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		try {
			String filepath = request.getFileName();
			String text = CommonUtils.extractTextFromPdf(filepath, "4");

			bankStatementInfo
					.setName(CommonUtils.extractField(text, "Customer\\s*Name\\s*([\\w\\s]*)Branch").replaceAll("\\s+", " "));
			bankStatementInfo
					.setNominee(CommonUtils.extractField(text, "Nominee\\s*Name\\s*([\\w\\s-]*)Branch\\s*Add").replaceAll("\\s+", " "));
			bankStatementInfo.setPhone1(CommonUtils.extractField(text, "Mobile\\s*(\\d*)"));
			bankStatementInfo.setEmail(CommonUtils.extractField(text, "Email\\s*(\\S*)"));
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*Name(.*)"));
			bankStatementInfo
					.setAccountType(CommonUtils.extractField(text, "Account\\s*Type(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*Number(.*)"));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*Code(.*)"));
			
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "\\n(\\s*Address[\\s\\S]*?)\\n\\s*Statement", 75));
			
			String dateFormat = "MMM dd yyyy";
			String[] period = CommonUtils.extractMultiGroupArray(text, "Period\\s*from\\s*[A-Za-z]{3}\\s*([A-Za-z]{3}\\s*\\d{2}\\s*\\d{4}).*to\\s*[A-Za-z]{3}\\s*([A-Za-z]{3}\\s*\\d{2}\\s*\\d{4})");
            if (period != null && period.length>1) {
            	bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0].replaceAll("\\s+", " "), dateFormat));
            	bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1].replaceAll("\\s+", " "), dateFormat));
            }
			bankStatementInfo
			.setTransactions(extractTransactionsEquita_4(text, bankStatementInfo.getAccountNo()));

		} catch (Exception e) {
			log.error("Error in EquitasServiceImpl parseEquitas4: ", e);
		}
		log.info("Exiting EquitasServiceImpl parseEquitas4 : " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for EquitasServiceImpl parseEquitas4 is ==>" + timeTaken);
		return bankStatementInfo;
	}

	private List<Transaction> extractTransactionsEquita_1(String fileName, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
		
		int serialNoCount = 1;
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}
				// Assuming first column is txnDate, second column is value
				String[] data = line.split("\\|");
				if (data.length == 7 && !data[0].equals("Tran Date") && !data[0].equals("Date of TXN")
						&& !data[0].equals("") && !data[0].equals("Transaction Date")) {
					// Last table is not a transaction
					if (data[0].equals("Opening Balance")) {
						break;
					}
					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], "dd-MMM-yy"));
					transaction.setValueDate(CommonUtils.dateFormatter(data[1], "dd-MMM-yy"));
					transaction.setDescription(data[3]);
					transaction.setDebit(data[4]);
					transaction.setCredit(data[5]);
					if (data[5].equals("0.00") || data[5].equals("")) {
						transaction.setAmount(data[4]);
						transaction.setTxnType("DEBIT");
						transaction.setCredit("");
					} else {
						transaction.setAmount(data[5]);
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
	
	private List<Transaction> extractTransactionsEquita_2(String pdfText, String accountNo, String txnDateFormat) throws IOException {

		log.info("Entering EquitasServiceImpl extractTransactionsEquita_2");
		List<Transaction> transactions = new ArrayList<>();

		String regex = "";
		if (txnDateFormat.equals("yyyy-MM-dd")) {
			regex = "(\\d{4}-\\d{2}-\\d{2})\\s+\\S+\\s+(.+?)\\s*([\\d,]+\\.\\d{2})(\\s+)([\\d,]+\\.\\d{2})\\n([\\s\\S]*?)(?=(\\s*\\d{4}-\\d{2}-\\d{2})|(\\s*Page))";
		} else {
			regex = "(\\d{2}-\\w{3}-\\d{4})\\s+\\S+\\s+(.+?)\\s*(-?[\\d,]+\\.\\d{2})(\\s+)(-?[\\d,]+\\.\\d{2})\\n([\\s\\S]*?)(?=(\\s*\\d{2}-\\w{3}-\\d{4})|(\\s*Page))";
		}

		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(pdfText);
		int serialNoCount = 1;

		while (matcher.find()) {
			Transaction transaction = new Transaction();
			transaction.setsNo(String.valueOf(serialNoCount++));
			transaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(1), txnDateFormat));
			String description = matcher.group(2) + " " + matcher.group(6);
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
			transaction.setAccNo(accountNo);
			transactions.add(transaction);
		}
		log.info("Exiting EquitasServiceImpl extractTransactionsEquita_2");
		return transactions;
	}

	private List<Transaction> extractTransactionsEquita_3(String fileName, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);

		int serialNoCount = 1;
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}
				// Assuming first column is txnDate, second column is value
				String[] data = line.split("\\|");
				if (data.length == 7 && !data[0].equals("Tran Date") && !data[0].equals("Date of TXN")
						&& !data[0].equals("") && !data[0].equals("Transaction Date")) {
					// Last table is not a transaction
					if (data[0].equals("Opening Balance")) {
						break;
					}
					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], "dd/MM/yyyy"));
					transaction.setValueDate(CommonUtils.dateFormatter(data[1], "dd/MM/yyyy"));
					transaction.setDescription(data[3]);
					transaction.setDebit(data[4]);
					transaction.setCredit(data[5]);
					if (data[5].equals("0.00") || data[5].equals("")) {
						transaction.setAmount(data[4]);
						transaction.setTxnType("DEBIT");
						transaction.setCredit("");
					} else {
						transaction.setAmount(data[5]);
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
	
	private List<Transaction> extractTransactionsEquita_4(String pdfText, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		
		String[] lineArr = Arrays.stream(pdfText.split("\\r?\\n"))
		        .filter(line -> !line.trim().isEmpty())
		        .map(line -> line.replaceAll("\\s{20,}", "|").replaceAll("\\s{3,}", "##")
		        		.replaceAll("\\(?Coordinated", "##").replaceAll("Universal", "").replaceAll("Time\\)?", "")
		        		.replaceAll("GMT\\+0000", "").replaceAll("^#*", "").replaceAll("^\\|", "").trim())
		        .toArray(String[]::new);

		Pattern pageBreakPattern1 = Pattern.compile("Toll\\s*Free\\s*:");
		
		StringBuilder csvOutput = new StringBuilder();
		boolean found = false;
		
		for (String line : lineArr) {
			if (line.trim().isEmpty()) {
				continue;
			}
			Matcher pageBreakMatcher1 = pageBreakPattern1.matcher(line);
			if(line.startsWith("Page")) {
				found = false;
			}else if(pageBreakMatcher1.find() || line.startsWith("Date") || line.contains("INR")) {
				found = true;
			}else if(found) {
				csvOutput.append(line).append("\n");
			}
		}
		
		String resultString = csvOutput.toString();
		String[] lines = resultString.split("\\r?\\n");
		
		String date = "";
		String desc = "";
		String credit = "";
		String debit = "";
		String balance = "";
//		
		
		Pattern datePattern1 = Pattern.compile("[A-Z]\\s*[a-z]\\s*[a-z]\\s*[A-Z]\\s*[a-z]\\s*[a-z]\\s*\\d{2}");
		Pattern datePattern2 = Pattern.compile("\\d{2}:\\d{2}:\\d{2}");
		Pattern pdfEndPattern = Pattern.compile("End\\s*of\\s*the\\s*Statement");
//		Pattern p
		String dateFormat = "MMMddyyyy";
		
		int serialNoCount = 1;
		for (String line : lines) {
			Matcher dateMatcher1 = datePattern1.matcher(line);
			Matcher dateMatcher2 = datePattern2.matcher(line);
			Matcher pdfEndMatcher = pdfEndPattern.matcher(line);
			
			String[] lineDataArr = line.split("\\|");
			
			if (dateMatcher1.find() || pdfEndMatcher.find()) {
				if(!date.equalsIgnoreCase("")) {
					Transaction transaction = new Transaction();
					transaction.setsNo(String.valueOf(serialNoCount++));
					transaction.setAccNo(accountNo);
					transaction.setTxnDate(CommonUtils.dateFormatter(date.replaceAll("\\s", "").substring(3,12), dateFormat));
					transaction.setDescription(desc.replaceAll("##", " ").replaceAll("\\s+", " "));
					transaction.setCredit(credit);
					transaction.setDebit(debit);
					transaction.setBalance(balance);
					if (debit != null && !debit.equalsIgnoreCase("")) {
						transaction.setAmount(debit);
						transaction.setTxnType("DEBIT");
					} else {
						transaction.setAmount(credit);
						transaction.setTxnType("CREDIT");
					}
					transactions.add(transaction);
					date = desc = credit = debit = balance = "";
				}
				date = line;
			}else if(dateMatcher2.find()){
				date += lineDataArr[0];
				if(lineDataArr.length == 2) {
					desc += lineDataArr[1];
				}
			}else if(lineDataArr.length == 2) {
				if(lineDataArr[1].contains("##")) {
					String[] txn = lineDataArr[1].split("##");
					credit = txn[0];
					balance = txn[1];
				}else {
					balance = lineDataArr[1];
				}
				if(lineDataArr[0].contains("##")) {
					String[] txn = lineDataArr[0].split("##");
					
					for (int i = 0; i < txn.length; i++) {
						if(i == txn.length-1) {
							if(credit.equalsIgnoreCase("")) {
								debit = txn[i];
							}else {
								desc += txn[i];
							}
							continue;
						}
						if(txn.length > 2 && i==0) {
							continue;
						}
						desc += txn[i];
					}
				}else {
					desc += lineDataArr[0];
				}
			}else if(lineDataArr.length == 3) {
				desc += lineDataArr[0];
				debit = lineDataArr[1];
				balance = lineDataArr[2];
			}else if(lineDataArr.length == 1) {
				String descArr[] = line.split("##");
				for (int i = 0; i < descArr.length; i++) {
					if(descArr.length >= 2 && i==0) {
						continue;
					}
					desc += descArr[i];
				}
			}
		}
		return transactions;
	}

}