package in.fl.vault.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;
import in.fl.vault.request.ParseBankStmtRequestDTO;
import in.fl.vault.response.BSInfo;
import in.fl.vault.response.Transaction;
import in.fl.vault.utils.CommonUtils;

@Service
public class RBLServiceImpl implements RBLService{
	
	private final static Logger log = Logger.getLogger(RBLServiceImpl.class);

	@Override
	public BSInfo parseRBL1(ParseBankStmtRequestDTO request) {

		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering RBLServiceImpl parseRBL1 with request: " + request);
		BSInfo bankStatementInfo = new BSInfo();
		
		try {
			String filepath = request.getFileName();
			String text = CommonUtils.extractTextFromPdf(filepath, "5");

			bankStatementInfo.setName(CommonUtils.extractField(text, "Account\\s*holder\\s*Name:(.*)Home").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "(Customer\\s*Address[\\s\\S]*?)\\n\\s*Phone", 90).replaceAll("Customer\s*", "").trim());
			bankStatementInfo.setPhone1(CommonUtils.extractField(text, "Phone\\s*:\\s*(\\S*)"));
			bankStatementInfo.setEmail(CommonUtils.extractField(text, "Email\\s*Id\\s*:\\s*(\\S*)"));
			bankStatementInfo.setNominee(CommonUtils.extractField(text, "Nomination\\s*:\\s*(\\S*)"));
			bankStatementInfo.setAccountType(CommonUtils.extractField(text, "A\\/C\\s*Type\\s*:\\s*(\\S*)"));
			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*Number\\s*:\\s*(\\S*)"));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC.*:\\s*(RATN\\w{7})"));
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Home\\s*Branch:\\s*(.*)").replaceAll("\\s+", " ").trim());
			
            List<Transaction> transactions = extractTranscationsRBL_1(text, bankStatementInfo.getAccountNo());
			bankStatementInfo.setTransactions(transactions);
			// Order is reverse, take care
			bankStatementInfo.setStartDate(transactions.get(transactions.size()-1).getTxnDate());
            bankStatementInfo.setEnDate(transactions.get(0).getTxnDate());
			
		}  catch (Exception e) {
//			e.printStackTrace();
        	log.error("Error in RBLServiceImpl parseRBL1: "+e);
        }
        
        log.info("Exiting RBLServiceImpl parseRBL1: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for RBLServiceImpl parseRBL1 is ==>" + timeTaken);
        return bankStatementInfo;
	}
	
	@Override
	public BSInfo parseRBL2(ParseBankStmtRequestDTO request) {

		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering RBLServiceImpl parseRBL2 with request: " + request);
		BSInfo bankStatementInfo = new BSInfo();

		try {
			String filepath = request.getFileName();
			String text = CommonUtils.extractTextFromPdf(filepath, "4");
			String[] details = CommonUtils.extractMultiGroupArray(text,
					"Account\\s*No.\\s*MICR\\s*IFSC.*\\n\\s*(\\S*)\\s*\\S*\\s*(\\S*)\\s*(.{26})");
			if (details != null && details.length == 3) {
				bankStatementInfo.setAccountNo(details[0]);
				bankStatementInfo.setIfsc(details[1]);
				bankStatementInfo.setNominee(details[2].trim());
			}
			bankStatementInfo.setPan(CommonUtils.extractField(text, "\\s*.*?\\[Pan:\\s*(\\S*?)\\]").trim());
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "Customer\\s*ID\\s*:.*\\n([\\S\\s]*?)\\n\\s*Deposit\\s*Account", 85));
			bankStatementInfo.setName(CommonUtils.extractField(text, "Customer\\s*ID\\s*:.*\\n(.{85})").replaceAll("\\s+", " ").trim());
			
			String dateFormat = "dd-MM-yyyy";
			String txnDateFormat = "dd-MM-yy";
			String[] period = CommonUtils.extractMultiGroupArray(text, "Statement\\s*Period\\s*:\\s*(\\d{2}-\\d{2}-\\d{4})\\s*to\\s*(\\d{2}-\\d{2}-\\d{4})");
			if(period != null) {
				bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0].trim(), dateFormat));
				bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1].trim(), dateFormat));
			}
			bankStatementInfo.setTransactions(
					extractTransactionsRBL_2(filepath, bankStatementInfo.getAccountNo(), txnDateFormat));

		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in RBLServiceImpl parseRBL2: " + e);
		}

		log.info("Exiting RBLServiceImpl parseRBL2: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for RBLServiceImpl parseRBL2 is ==>" + timeTaken);
		return bankStatementInfo;
	}
	
	@Override
	public BSInfo parseRBL3(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering RBLServiceImpl parseRBL3 with request: " + request);
		BSInfo bsInfo = new BSInfo();
		
		try {
			String filepath = request.getFileName();
			String text = CommonUtils.extractTextFromPdf(filepath, "4");

			bsInfo.setName(CommonUtils.extractField(text, "Account\\s*holder\\s*Name\\s*:(.*)").replaceAll("\\s+", " ").trim());
			bsInfo.setAddress(CommonUtils.extractMultiLinesField(text, "\\n(\\s*Customer\\s*Address[\\s\\S]*?)\\n\\s*Phone", 75).replaceAll("Customer\\s*", "").trim());
			bsInfo.setPhone1(CommonUtils.extractField(text, "Phone\\s*:\\s*(\\S*)"));
			bsInfo.setEmail(CommonUtils.extractField(text, "Email\\s*Id\\s*:\\s*(\\S*)"));
			bsInfo.setNominee(CommonUtils.extractField(text, "Nomination\\s*:\\s*(\\S*)"));
			bsInfo.setAccountType(CommonUtils.extractField(text, "A\\/c\\s*Type\\s*:\\s*(.{50})"));
			bsInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*No\\.\\s*(\\S*)"));
			bsInfo.setIfsc(CommonUtils.extractField(text, "IFSC.*:\\s*(RATN\\w{7})"));
			bsInfo.setBranch(CommonUtils.extractField(text, "Home\\s*Branch\\s*:\\s*(.*)").replaceAll("\\s+", " ").trim());
			
            List<Transaction> transactions = extractTransactionsRBL_3(filepath, bsInfo.getAccountNo());
			bsInfo.setTransactions(transactions);
			// Order is reverse, take care
			bsInfo.setStartDate(transactions.get(transactions.size()-1).getTxnDate());
            bsInfo.setEnDate(transactions.get(0).getTxnDate());
			
		}  catch (Exception e) {
//			e.printStackTrace();
        	log.error("Error in RBLServiceImpl parseRBL3: "+e);
        }
        
        log.info("Exiting RBLServiceImpl parseRBL3:" + bsInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for RBLServiceImpl parseRBL3 is ==>" + timeTaken);
        return bsInfo;
	}
	
	@Override
	public BSInfo parseRBL4(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering RBLServiceImpl parseRBL4 with request: " + request);
		BSInfo bsInfo = new BSInfo();
		
		try {
			String filepath = request.getFileName();
			String text = CommonUtils.extractTextFromPdf(filepath, "8");

			bsInfo.setName(CommonUtils.extractField(text, "Account\\s*Name\\s*([\\s\\S]*?)\\n\\s*Address1").replaceAll("\\s+|\\n", " ").trim());
			bsInfo.setAddress(CommonUtils.extractMultiLinesField(text, "(Address1[\\s\\S]*?)\\n\\s*FromDate", 75).trim());
			bsInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*Number\\s*(\\S*)"));
			
            List<Transaction> transactions = extractTransactionsRBL_4(filepath, bsInfo.getAccountNo());
			bsInfo.setTransactions(transactions);
			bsInfo.setStartDate(transactions.get(0).getTxnDate());
            bsInfo.setEnDate(transactions.get(transactions.size()-1).getTxnDate());
			
		}  catch (Exception e) {
//			e.printStackTrace();
        	log.error("Error in RBLServiceImpl parseRBL4: "+e);
        }
        log.info("Exiting RBLServiceImpl parseRBL4:" + bsInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for RBLServiceImpl parseRBL4 is ==>" + timeTaken);
        return bsInfo;
	}

	private List<Transaction> extractTranscationsRBL_1(String pdfText, String accountNo) {
		List<Transaction> transactions = new ArrayList<>();
		try {
			pdfText = pdfText.replaceAll("\\s*Date\\s*and\\s*Time\\s*:.*", "");
			String regex = "^\\s*(\\d{2}\\/\\d{2}\\/\\d{4})\\s*([\\s\\S]*?)\\s*(\\d{2}\\/\\d{2}\\/\\d{4})\\s*(\\S*)(\\s*)(\\S*)";
			
			pdfText = Arrays.stream(pdfText.split("\\r?\\n"))
			        .filter(line -> !line.contains("Page"))
			        .collect(Collectors.joining("\n"));
			
			int serialNoCount = 1;
			
			Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
			Matcher matcher = pattern.matcher(pdfText);
			while(matcher.find()) {
				Transaction transaction = new Transaction();
				transaction.setsNo(Integer.toString(serialNoCount++));
				transaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(1), "dd/MM/yyyy"));
				transaction.setDescription(matcher.group(2).replaceAll("\\n", " ").replaceAll("\\s+", " ").trim());
				transaction.setValueDate(CommonUtils.dateFormatter(matcher.group(3), "dd/MM/yyyy"));
				transaction.setAmount(matcher.group(4));
				transaction.setBalance(matcher.group(6));
				if(matcher.group(5).length()>32) { // debit
					transaction.setDebit(transaction.getAmount());
					transaction.setCredit("");
					transaction.setTxnType("DEBIT");
				}else {
					transaction.setCredit(transaction.getAmount());
					transaction.setTxnType("CREDIT");
					transaction.setDebit("");
				}
				transaction.setAccNo(accountNo);
				transactions.add(transaction);
			}
		} catch (Exception e) {
			log.error("Error in RBLServiceImpl extractTranscationsRBL_1: ", e);
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsRBL_2(String fileName, String accountNo, String dateFormat)
			throws IOException {
		
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

				if (data.length == 6
						&& !CommonUtils.extractField(data[0], "(\\d{2}-\\d{2}-\\d{2})").equals("")) {
					Transaction transaction = new Transaction();
					transaction.setsNo(String.valueOf(serialNoCount++));
					transaction.setAccNo(accountNo);
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat));
					transaction.setDescription(data[1]);

					if (data[4].equals("0.00")) {
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
		}
		return transactions;
	}
	
	private List<Transaction> extractTransactionsRBL_3(String fileName, String accountNo)
			throws IOException {
		
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
		String dateFormat = "dd-MMM-yyyy";
		
		int serialNoCount = 1;
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {

				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}
				// Assuming first column is txnDate, second column is value
				String[] data = line.split("\\|");

				if (data.length == 7 && !data[0].equalsIgnoreCase("") && !data[0].equalsIgnoreCase("DATE")) {
					Transaction transaction = new Transaction();
					transaction.setsNo(String.valueOf(serialNoCount++));
					transaction.setAccNo(accountNo);
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat));
					transaction.setValueDate(CommonUtils.dateFormatter(data[3], dateFormat));
					transaction.setDescription(data[1]);

					if (data[4] != null && !data[4].equalsIgnoreCase("")) {
						transaction.setAmount(data[4]);
						transaction.setDebit(data[4]);
						transaction.setCredit("");
						transaction.setTxnType("DEBIT");
					} else {
						transaction.setAmount(data[5]);
						transaction.setCredit(data[5]);
						transaction.setDebit("");
						transaction.setTxnType("CREDIT");
					}
					transaction.setBalance(data[6].replaceAll("[CD]r", "").trim());
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}
	
	private List<Transaction> extractTransactionsRBL_4(String fileName, String accountNo)
			throws IOException {
		
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
		String dateFormat = "dd-MM-yyyy";
		
		Pattern datePattern  = Pattern.compile("\\d{2}-\\d{2}-\\d{4}");
		
		int serialNoCount = 1;
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}
				Matcher dateMatcher = datePattern.matcher(line);
				// Assuming first column is txnDate, second column is value
				String[] data = line.split("\\|");
				

				if (data.length == 7 && dateMatcher.find()) {
					Transaction transaction = new Transaction();
					transaction.setsNo(String.valueOf(serialNoCount++));
					transaction.setAccNo(accountNo);
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0].replaceAll("\\s*", "").substring(0, 10), dateFormat));
					transaction.setValueDate(CommonUtils.dateFormatter(data[1], dateFormat));
					transaction.setDescription(data[2]);

					if (data[4] != null && !data[4].equalsIgnoreCase("")) {
						transaction.setAmount(data[4]);
						transaction.setDebit(data[4]);
						transaction.setCredit("");
						transaction.setTxnType("DEBIT");
					} else {
						transaction.setAmount(data[5]);
						transaction.setCredit(data[5]);
						transaction.setDebit("");
						transaction.setTxnType("CREDIT");
					}
					transaction.setBalance(data[6]);
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}
}