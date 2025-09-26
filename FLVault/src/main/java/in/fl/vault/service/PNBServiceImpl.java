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
public class PNBServiceImpl implements PNBService{

	private final static Logger log = Logger.getLogger(PNBServiceImpl.class);
	
	@Override
	public BSInfo parsePNB1(ParseBankStmtRequestDTO request) throws IOException {

		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering PNBServiceImpl parsePNB1 with request: " + request);
		
		BSInfo bsInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "3");
			bsInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*Name\\s*:\\s*(.*)").replaceAll("\\s+", " ").trim());
			bsInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*Code\\s*:\\s*(PUNB\\w{7})"));
			bsInfo.setName(CommonUtils.extractField(text, "Customer\\s*Name\\s*:\\s*(.*)").replaceAll("\\s+", " "));
			bsInfo.setAddress(CommonUtils.extractField(text, "Customer\\s*Address\\s*:([\\s\\S]*?)\\n\\s*City").replaceAll("\\s+", " ").trim());
			String[] accNoPeriod = CommonUtils.extractMultiGroupArray(text,
					"Statement.*Account:(\\S*)\\s*For.*:\\s*(\\d{2}\\/\\d{2}\\/\\d{4}).*(\\d{2}\\/\\d{2}\\/\\d{4})");
			String dateFormat = "dd/MM/yyyy";
			if(accNoPeriod !=null) {
				bsInfo.setAccountNo(accNoPeriod[0]);
				bsInfo.setStartDate(CommonUtils.dateFormatter(accNoPeriod[1], dateFormat));
				bsInfo.setEnDate(CommonUtils.dateFormatter(accNoPeriod[2], dateFormat));
			}
			bsInfo.setTransactions(extractTransactionsPNB_1(filepath, bsInfo.getAccountNo()));
		} catch (Exception e) {
//			e.printStackTrace();
        	log.error("Error in PNBServiceImpl parsePNB1: "+e);
        }
        
        log.info("Exiting PNBServiceImpl parsePNB1:" + bsInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for PNBServiceImpl parsePNB1 is ==>" + timeTaken);
        return bsInfo;
	}
	
	@Override
	public BSInfo parsePNB2(ParseBankStmtRequestDTO request) throws IOException {

		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering PNBServiceImpl parsePNB2 with request: " + request);
		
		BSInfo bsInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "4");
			bsInfo.setAccountNo(CommonUtils.extractField(text, "Statement\\s*For\\s*Account\\s*:\\s*(\\d*)"));
			bsInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*Name:\\s*(.*)").replaceAll("\\s+", " ").trim());
			bsInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*Code\\s*:\\s*(PUNB\\w{7})"));
			bsInfo.setName(CommonUtils.extractField(text, "(?:Customer|Account)\\s*Name\\s*:\\s*(.*)").replaceAll("\\s+", " "));
			bsInfo.setNominee(CommonUtils.extractField( text, "Nominee\\s*:\\s*(.*)").replaceAll("\\s+", " ").trim());
			bsInfo.setAddress(CommonUtils.extractField(text, "Customer\\s*Address\\s*:([\\s\\S]*?)\\n\\s*City").replaceAll("\\s+", " ").trim());
			String[] period = CommonUtils.extractMultiGroupArray(text,
					"Statement\\s*Period\\s*:\\s*(\\d{2}\\/\\d{2}\\/\\d{4}).*(\\d{2}\\/\\d{2}\\/\\d{4})");

			String dateFormat = "dd/MM/yyyy";
			if(period != null) {
				bsInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
				bsInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
			}
			bsInfo.setTransactions(extractTransactionsPNB_2(filepath, bsInfo.getAccountNo()));
		} catch (Exception e) {
//			e.printStackTrace();
        	log.error("Error in PNBServiceImpl parsePNB2: "+e);
        }
        
        log.info("Exiting PNBServiceImpl parsePNB2:" + bsInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for PNBServiceImpl parsePNB2 is ==>" + timeTaken);
        return bsInfo;
	}

	@Override
	public BSInfo parsePNB3(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering PNBServiceImpl parsePNB3 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String pdfText = CommonUtils.extractTextFromPdf(filePath, "3");
			bankStatementInfo.setAccountNo(CommonUtils.extractField(pdfText, "Statement\\s*of\\s*Account.*?:\\s*(\\S*)"));
			bankStatementInfo.setName(CommonUtils.extractField(pdfText, "Customer\\s*Name\\s*:\\s*(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setAddress(CommonUtils.extractField(pdfText, "\\n(\\s*Customer\\s*Address[\\s\\S]*?)\\n\\s*Branch\\s*Address").replaceAll("\\n|\\s+", " ").trim());
			bankStatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*Code\\s*:\\s*(\\S*)"));
			String[] period = CommonUtils.extractMultiGroupArray(pdfText,
					"Statement\\s*for.*?:\\s*(\\d{2}-\\d{2}-\\d{4}).*?(\\d{2}-\\d{2}-\\d{4})");
			if (period != null ) {
				bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], "dd-MM-yyyy"));
				bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], "dd-MM-yyyy"));
			}
			List<Transaction> transactions = extractTransactionsPNB_3(filePath, bankStatementInfo.getAccountNo());
			bankStatementInfo.setTransactions(transactions);

		} catch (Exception e) {
//        	e.printStackTrace();
			log.error("Error in PNBServiceImpl parsePNB3: " + e);
		}
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for PNBServiceImpl parsePNB3 is ==>" + timeTaken);
		return bankStatementInfo;
	}
	
	@Override
	public BSInfo parsePNB4(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering PNBServiceImpl parsePNB4 with request: " + request);
		
		BSInfo bsInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "4");
			bsInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*Number\\s*(\\d*)"));
			bsInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*Name:\\s*(.*)").replaceAll("\\s+", " ").trim());
			bsInfo.setIfsc(CommonUtils.extractField(text, "IFSC.*(PUNB\\w{7})"));
			bsInfo.setName(CommonUtils.extractField(text, "Customer\\s*Details[\\s\\S]*?Name\\s*:\\s*(.*)").replaceAll("\\s+", " "));
			bsInfo.setNominee(CommonUtils.extractField( text, "Nominee\\s*:\\s*(.*)\\n?\\s*Statement").replaceAll("\\s+", " ").trim());
			bsInfo.setAddress(CommonUtils.extractField(text, "Customer\\s*Address\\s*:([\\s\\S]*?)\\n\\s*(Nominee|Statement)")
					.replaceAll("(City\\s*:)?(Pin\\s*:)?\\s+", " ").replaceAll("\\s+", " ").trim());
			String[] period = CommonUtils.extractMultiGroupArray(text,
					"Statement\\s*Period\\s*:\\s*(\\d{2}-\\d{2}-\\d{4}).*(\\d{2}-\\d{2}-\\d{4})");

			String dateFormat = "dd-MM-yyyy";
			if(period != null) {
				bsInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
				bsInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
			}
			bsInfo.setTransactions(extractTransactionsPNB_4(filepath, bsInfo.getAccountNo(), dateFormat));
		} catch (Exception e) {
//			e.printStackTrace();
        	log.error("Error in PNBServiceImpl parsePNB4: "+e);
        }
        
        log.info("Exiting PNBServiceImpl parsePNB4:" + bsInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for PNBServiceImpl parsePNB4 is ==>" + timeTaken);
        return bsInfo;
	}
	
	@Override
	public BSInfo parsePNB5(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering PNBServiceImpl parsePNB4 with request: " + request);

		BSInfo bsInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "4");
			bsInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*No:\\s*(\\S*)"));
			bsInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*Name:\\s*(.*)").replaceAll("\\s+", " ").trim());
			bsInfo.setPhone1(CommonUtils.extractField(text, "Mobile\\s*:\\s*(\\S*)"));

			bsInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*Code\\s*:\\s*(PUNB\\w{7})"));
			bsInfo.setName(CommonUtils.extractMultiLinesField(text,
					"Account\\s*No.*([\\s\\S]*?)(?=\\s*Customer\\s*Address)", 60));

			bsInfo.setAddress(
					CommonUtils.extractMultiLinesField(text, "(Customer\\s*Address[\\s\\S]*?)(?=\\s*Mobile)", 60)
							.replaceAll("Customer|Address|:", "").trim());

			String[] period = CommonUtils.extractMultiGroupArray(text,
					"Statement\\s*Period\\s*:\\s*(\\d{2}-\\d{2}-\\d{4}).*(\\d{2}-\\d{2}-\\d{4})");

			String dateFormat = "dd-MM-yyyy";
			if (period != null) {
				bsInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
				bsInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
			}
			bsInfo.setTransactions(extractTransactionsPNB_5(filepath, bsInfo.getAccountNo()));
		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in PNBServiceImpl parsePNB4: " + e);
		}

		log.info("Exiting PNBServiceImpl parsePNB4:" + bsInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for PNBServiceImpl parsePNB4 is ==>" + timeTaken);
		return bsInfo;
	}
	
	private List<Transaction> extractTransactionsPNB_1(String fileName, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
		
		int serialNoCount = 1;
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {

				String line = String.join("|", row).replaceAll("\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}
				String[] data = line.split("\\|");
			
				if (data.length >= 6 && !data[0].equals("Date")) {
					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					String dateFormat = "dd/MM/yyyy";
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat)); 
					transaction.setDescription(data[5]); 
					
					String amount = data[2];
					if (data[3].equalsIgnoreCase("DR")) {
						transaction.setTxnType("DEBIT");
						transaction.setDebit(amount);
					} else if (data[3].equalsIgnoreCase("CR")) {
						transaction.setTxnType("CREDIT");
						transaction.setCredit(amount);
					}
					transaction.setAmount(amount); 
					transaction.setBalance(data[4]); 
					transaction.setAccNo(accountNo);
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}
	
	private List<Transaction> extractTransactionsPNB_2(String fileName, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
		
		int serialNoCount = 1;
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {

				String line = String.join("|", row).replaceAll("\\s+", " ");
				if(line.trim().isEmpty() || line.contains("Transaction Date")){
					continue;
				}
				String[] data = line.split("\\|");
				if (data.length == 6) {
					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					String dateFormat = "dd/MM/yyyy";
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat)); 
					transaction.setDescription(data[5]); 
					
					if (data[2] != null && !data[2].equalsIgnoreCase("")) {
						transaction.setTxnType("DEBIT");
						transaction.setDebit(data[2]);
						transaction.setAmount(data[2]);
						transaction.setCredit("");
					} else {
						transaction.setTxnType("CREDIT");
						transaction.setCredit(data[3]);
						transaction.setAmount(data[3]);
						transaction.setDebit("");
					}
					transaction.setBalance(data[4].replaceAll("Cr\\.", "").trim()); 
					transaction.setAccNo(accountNo);
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsPNB_3(String filePath, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(filePath);

		int serialNoCount = 1;
		if (txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}
				String[] data = line.split("\\|");

				if (data.length < 5)
					continue;

				Pattern pattern = Pattern.compile("\\d{2}-\\d{2}-\\d{4}");
				Pattern pattern2 = Pattern.compile("[\\d,\\.]*");

				Matcher matcher1 = pattern.matcher(data[0]);
				Matcher matcher2 = pattern2.matcher(data[1]);
				if (!matcher1.find() || !matcher2.find())
					continue;

				Transaction transaction = new Transaction();
				transaction.setsNo(Integer.toString(serialNoCount++));
				transaction.setTxnDate(CommonUtils.dateFormatter(data[0], "dd-MM-yyyy"));
				transaction.setDescription(data[6]);
				transaction.setDebit(data[1].trim());
				transaction.setCredit(data[2].trim());
				if (data[2] == null || data[2].equals("")) {
					transaction.setAmount(data[1]);
					transaction.setTxnType("DEBIT");
					transaction.setCredit("");
				} else {
					transaction.setAmount(data[2]);
					transaction.setTxnType("CREDIT");
					transaction.setDebit("");
				}
				String balance = data[3].trim();
				balance = balance.substring(0, balance.length() - 3);
				transaction.setAccNo(accountNo);
				transaction.setBalance(balance);

				transactions.add(transaction);
			}
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsPNB_4(String fileName, String accountNo, String dateFormat) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
		
		Pattern dateFormat1 = Pattern.compile("\\d{2}-\\d{2}-\\d{4}");
		Pattern dateFormat2 = Pattern.compile("\\d{2}\\/\\d{2}\\/\\d{4}");
		String txnDateFormat = "";
		
		int serialNoCount = 1;
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {

				String line = String.join("|", row).replaceAll("\\s+", " ");
				if(line.trim().isEmpty() || line.contains("Txn Date")){
					continue;
				}
				String[] data = line.split("\\|");
				if (data.length >= 8) {
					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					transaction.setTxnId(data[0]);
					String date = data[1];
					if(txnDateFormat.equalsIgnoreCase("")) {
						Matcher dateMatcher1 = dateFormat1.matcher(date);
						Matcher dateMatcher2 = dateFormat2.matcher(date);
						
						if(dateMatcher1.find()) {
							txnDateFormat= "dd-MM-yyyy";
						}else if(dateMatcher2.find()) {
							txnDateFormat = "dd/MM/yyyy";
						}
					}
					transaction.setTxnDate(CommonUtils.dateFormatter(data[1], txnDateFormat)); 
					transaction.setDescription(data[2]); 
					
					if (data[5] != null && !data[5].equalsIgnoreCase("")) {
						transaction.setTxnType("DEBIT");
						transaction.setDebit(data[5]);
						transaction.setAmount(data[5]);
						transaction.setCredit("");
					} else {
						transaction.setTxnType("CREDIT");
						transaction.setCredit(data[6]);
						transaction.setAmount(data[6]);
						transaction.setDebit("");
					}
					transaction.setBalance(data[7].replaceAll("Cr\\.", "").trim()); 
					transaction.setAccNo(accountNo);
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}
	
	private List<Transaction> extractTransactionsPNB_5(String fileName, String accountNo) throws IOException {
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

				if (data.length >= 6 && !data[0].equals("Date") && data[0].matches("\\d{2}-\\d{2}-\\d{4}")) {
					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					String dateFormat = "dd-MM-yyyy";
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat));
					transaction.setDescription(data[5]);

					String amount = data[1].replaceAll("₹", "").trim();
					String txnType = data[2].trim();
					if (txnType.equalsIgnoreCase("DEBIT")) {
						transaction.setAmount(amount);
						transaction.setDebit(amount);
						transaction.setCredit("");
					} else if (txnType.equalsIgnoreCase("CREDIT")) {
						transaction.setAmount(amount);
						transaction.setCredit(amount);
						transaction.setDebit("");
					}
					String balance = data[4].replaceAll("₹", "").trim();

					transaction.setBalance(balance);
					transaction.setAccNo(accountNo);
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}

}