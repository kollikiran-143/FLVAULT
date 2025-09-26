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
public class KVBServiceImpl implements KVBService{
	
	private final static Logger log = Logger.getLogger(KVBServiceImpl.class);

	@Override
	public BSInfo parseKVB1(ParseBankStmtRequestDTO request) throws IOException, InterruptedException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering KVBServiceImpl parseKVB1 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filepath = request.getFileName();

		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "4");

			String accountNo = CommonUtils.extractField(text, "Acc\\.\\s*No\\.\\s*:\\s*(\\d*)");
			bankStatementInfo.setAccountNo(accountNo);
			bankStatementInfo.setName(CommonUtils.extractField(text, "Statement\\s*\\n(.{85})").replaceAll("\\s+", " "));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "Statement\\s*\\n([\\s\\S]*?)\\n\\s*Account\\s*Summary", 85));
			bankStatementInfo.setPhone1(CommonUtils.extractField(text, "Mobile\\s*No\\.\\s*:\\s*(\\+?\\d*)"));
			bankStatementInfo.setEmail(CommonUtils.extractField(text, "Email\\s*Id\\s*:\\s*(\\w*@[a-z]*\\.[a-z]*)"));
			bankStatementInfo.setAccountType(CommonUtils.extractField(text, "Acc\\.\\s*Type\\s*:\\s*(.*)").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "HOME\\s*BRANCH\\s*:\\s*(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*CODE\\s*-\\s*(KVBL\\w{7})"));

			String dateFormat = "dd/MM/yyyy";
		    String[] period = CommonUtils.extractMultiGroupArray(text, "St\\.\\s*Period\\s*:\\s*(\\d{2}\\/\\d{2}\\/\\d{4}).*(\\d{2}\\/\\d{2}\\/\\d{4})");
	        if(period != null && period.length>=2) {
	        	bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
	        	bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
	        }
			bankStatementInfo.setTransactions(extractTransactionsKVB_1(text, bankStatementInfo.getAccountNo(), dateFormat));
		} catch (Exception e) {
//	    	e.printStackTrace();
			log.error("Error in KVBServiceImpl parseKVB1: ", e);
		}

		log.info("Exiting KVBServiceImpl parseKVB1: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for KVBServiceImpl is ==>" + timeTaken);
		return bankStatementInfo;
	}
	
	@Override
	public BSInfo parseKVB2(ParseBankStmtRequestDTO request) throws IOException, InterruptedException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering KVBServiceImpl parseKVB2 with request: " + request);

		BSInfo bsInfo = new BSInfo();
		String filepath = request.getFileName();
		
		try {
			String pdfText = CommonUtils.extractTextFromPdf(filepath, "5");
		    
			String startDate=CommonUtils.extractField(pdfText, "From\\s*Date(.*)");
			String endDate=CommonUtils.extractField(pdfText, "To\\s*Date(.*)");
			bsInfo.setStartDate(CommonUtils.dateFormatter(startDate, "dd-MMM-yyyy"));
			bsInfo.setEnDate(CommonUtils.dateFormatter(endDate, "dd-MMM-yyyy"));
			bsInfo.setBranch(CommonUtils.extractField(pdfText, "^\\s*Branch(.*)",Pattern.MULTILINE));
            String accountNo=CommonUtils.extractField(pdfText, "^\\s*AccountNumber(.*)",Pattern.MULTILINE);
            bsInfo.setAccountNo(accountNo);	
            bsInfo.setName(CommonUtils.extractField(pdfText, "^\\s*AccountName(.*)",Pattern.MULTILINE));
            String []addressLine=CommonUtils.extractField(pdfText, "Account\\s*Statement.*\\n\\s* asof.*\\n.*\\n([\\s\\S]*?)(?=\\s*AccountName)").split("\\n");
            String address="";
		    for(String line:addressLine){
		        	address += " "+line;	
		    }
		    bsInfo.setAddress(address.replaceAll("\\s+", " ").trim());
			bsInfo.setTransactions(extractTransactionsKVB_2(filepath, accountNo));
		} catch (Exception e) {
//				e.printStackTrace();
			log.error("Error in KVBServiceImpl parseKVB2: "+e);
		}
		
		log.info("Exiting KVBServiceImpl parseKVB2:" + bsInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for KVBServiceImpl parseKVB2 is ==>" + timeTaken);
		return bsInfo;
	}

	@Override
	public BSInfo parseKVB3(ParseBankStmtRequestDTO request) {

		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering KVBServiceImpl parseKVB3 with request: " + request);

		BSInfo bsInfo = new BSInfo();
		String filePath = request.getFileName();
		try {
			String pdfText = CommonUtils.extractTextFromPdf(filePath, "3");

			bsInfo.setAccountType(CommonUtils.extractField(pdfText, "Acc\\s*.\\s*Type\\s*:\\s*(.*)").replaceAll("\\s+", " "));
			bsInfo.setAccountNo(CommonUtils.extractField(pdfText, "Acc\\s*.\\s*No\\s*.\\s*:\\s*(\\S*)"));
			bsInfo.setPhone1(CommonUtils.extractField(pdfText, "Mobile\\s*No.\\s*:\\s*(\\S*)"));
			bsInfo.setEmail(CommonUtils.extractField(pdfText, "Email\\s*Id.\\s*:\\s*(\\S*)").replaceAll("\\s+", " ").trim());
			String dateFormat = "dd-MMM-yyyy";
			String[] period = CommonUtils.extractMultiGroupArray(pdfText, "St.\\s*Period\\s*:\\s*(\\d{2}-\\w{3}-\\d{4})\\s*to\\s*(\\d{2}-\\w{3}-\\d{4})");
			if (period != null && period.length > 1) {
				bsInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
				bsInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
			}

			String[] region = CommonUtils
					.extractField(pdfText, "ACCOUNT\\s*STATEMENT.*\\n([\\s\\S]*?)(?=\\s*Current\\s*Balance)")
					.split("\\n");
			List<String> temp = new ArrayList<>();
			for (String line : region) {
				if (line.length() > 80 && line.substring(0, 80).trim().length() > 0) {
					temp.add(line.substring(0, 80).trim());
				} else if (line.length() < 80) {
					temp.add(line.trim());
				}
			}
			String address = "";
			for (int i = 0; i < temp.size(); i++) {
				if (i == 0) {
					bsInfo.setName(temp.get(i).replaceAll("\\s+", " "));
				} else {
					address += " " + temp.get(i);
				}
			}

			address = address.replaceAll("\\s+", " ");
			bsInfo.setAddress(address);

			List<Transaction> transactions = extractTransactionsKVB_3(pdfText, bsInfo.getAccountNo());
			bsInfo.setTransactions(transactions);
		} catch (Exception e) {
//          	e.printStackTrace();
			log.error("Error in KVBServiceImpl parseKVB3: " + e);
		}
		log.info("Exiting KVBServiceImpl parseKVB3:" + bsInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for KVBServiceImpl parseKVB3 is ==>" + timeTaken);
		return bsInfo;
	}
	
	private List<Transaction> extractTransactionsKVB_1(String pdfText, String accountNo, String dateFormat) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		
		Pattern txnPattern = Pattern.compile("(\\d{2}\\/\\d{2}\\/\\d{4})\\s*(\\d{2}\\/\\d{2}\\/\\d{4})\\s*\\d{4}\\s*([\\s\\S]*?)(-?\\d*,?\\d*,?\\d*\\.\\d+)(\\s*)(-?\\d*,?\\d*,?\\d*\\.\\d+)");
		
		String[] lines = pdfText.split("\\r?\\n");
        
        int serialNoCount = 1;
        for (String line : lines) {
        	Matcher txnMatcher = txnPattern.matcher(line);
            if (txnMatcher.find()) {
	    		Transaction transaction = new Transaction();
	            transaction.setsNo(String.valueOf(serialNoCount++));
	            transaction.setAccNo(accountNo);
	            transaction.setTxnDate(CommonUtils.dateFormatter(txnMatcher.group(1), dateFormat));
	            transaction.setValueDate(CommonUtils.dateFormatter(txnMatcher.group(2), dateFormat));
	            String descArr[] = txnMatcher.group(3).trim().split("\\s+");
	            String desc = Arrays.stream(descArr)
	                    .limit(descArr.length - 1) // Exclude last element
	                    .collect(Collectors.joining(" "));
	
	            transaction.setDescription(desc.trim());
	            String amount = txnMatcher.group(4);
	            String whiteSpaces = txnMatcher.group(5);
	            transaction.setBalance(txnMatcher.group(6));

	            if (whiteSpaces.length() > 20) {
					transaction.setDebit(amount);
					transaction.setCredit("");
					transaction.setTxnType("DEBIT");
				} else {
					transaction.setCredit(amount);
					transaction.setDebit("");
					transaction.setTxnType("CREDIT");
				}
	            transaction.setAmount(amount);
	            transactions.add(transaction);
            }
        }	
		return transactions;
	}

	private List<Transaction> extractTransactionsKVB_2(String filePath, String accountNo) throws IOException {
		
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(filePath);
		
		int serialNoCount = 1;
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {

				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}
				// Assuming first column is txnDate, second column is value
				String[] data = line.split("\\|");

				if (data.length < 4 || !matchesDatePattern3(data))
					continue;
//						System.out.println("----------------->line is: " + line);
				Transaction transaction = new Transaction();
				transaction.setsNo(String.valueOf(serialNoCount++));
				transaction.setAccNo(accountNo);
				String txnDateFormat = "dd-MM-yyyy HH:mm:ss";
				String valueDateFormat="dd-MMM-yyyy";
				transaction.setTxnDate(CommonUtils.dateFormatter(data[0], txnDateFormat));
				transaction.setValueDate(CommonUtils.dateFormatter(data[1], valueDateFormat));
				transaction.setDescription(data[4]);
				transaction.setBalance(data[7]);
			    String debit=data[5];
                if(debit!=null && debit.equalsIgnoreCase("")) {
                    transaction.setTxnType("CREDIT");
                    transaction.setAmount(data[6]);
                    transaction.setDebit("");
                    transaction.setCredit(data[6]);
                }else {
                    transaction.setTxnType("DEBIT"); 
                    transaction.setAmount(data[5]);
                    transaction.setCredit("");
                    transaction.setDebit(data[5]);
            	}
                transactions.add(transaction);
			}
		}
		return transactions;
	}
	
	public boolean matchesDatePattern3(String[] dateStr) {
		String datePattern = "\\d{2}-\\d{2}-\\d{4}\\s*\\d{2}:\\d{2}:\\d{2}";
		String noPattern = "\\d{2}-\\w{3}-\\d{4}";
		Pattern pattern1 = Pattern.compile(datePattern);
		Pattern pattern2 = Pattern.compile(noPattern);
		Matcher matcher1 = pattern1.matcher(dateStr[0]);
		Matcher matcher2 = pattern2.matcher(dateStr[1]);
		return matcher1.find() && matcher2.find();
	}
	
	/*
	 * 	(\d{2}-[A-Z]{3}-\d{4})(.*?)(-?\d*,?\d*,?\d*\.\d+)\s*(-?\d*,?\d*,?\d*\.\d+)\s*(-?\d*,?\d*,?\d*\.\d+)
		(\d{2}-[A-Z]{3}-\d{4})(.*)
		(\d{2}:\d{2}:\d{2})\s*(.*)
	 */
	
	private List<Transaction> extractTransactionsKVB_3(String pdfText, String accountNo) {
		List<Transaction> transactions = new ArrayList<>();
		pdfText = pdfText.replaceAll("Note\\s*:\\s*This\\s*is[\\s\\S]*?Credit\\s*Balance.*", "");
		
		
		Pattern linePattern1 = Pattern.compile("(\\d{2}-[A-Z]{3}-\\d{4})(.*?)(-?\\d*,?\\d*,?\\d*\\.\\d+)\\s*(-?\\d*,?\\d*,?\\d*\\.\\d+)\\s*(-?\\d*,?\\d*,?\\d*\\.\\d+)");
		Pattern linePattern2 = Pattern.compile("(\\d{2}-[A-Z]{3}-\\d{4})(.*)");
		Pattern linePattern3 = Pattern.compile("(\\d{2}:\\d{2}:\\d{2})\\s*(.*)");
		
		String[] lines = pdfText.split("\n");
		String dateFormat = "dd-MMM-yyyy";
		
		String txnDate = "";
		String valueDate = "";
        String desc = "";
        String credit = "";
        String debit = "";
        String balance = "";
		
		int serialNoCount = 1;
		Transaction transaction = new Transaction();
		for (String line : lines) {
			if(line.contains("B/F...")) {
				continue;
			}
			Matcher lineMatcher1 = linePattern1.matcher(line);
			Matcher lineMatcher2 = linePattern2.matcher(line);
			Matcher lineMatcher3 = linePattern3.matcher(line);
			
			if(lineMatcher1.find()) {
				txnDate = lineMatcher1.group(1);
				desc += lineMatcher1.group(2);
				debit = lineMatcher1.group(3);
				credit = lineMatcher1.group(4);
				balance = lineMatcher1.group(5);
				
			}else if(lineMatcher2.find()) {
				valueDate = lineMatcher2.group(1);
				desc = lineMatcher2.group(2);
			}else if(lineMatcher3.find()) {
				transaction.setsNo(String.valueOf(serialNoCount++));
				transaction.setAccNo(accountNo);
				desc += lineMatcher3.group(2);
				transaction.setDescription(desc.replaceAll("\\s+", " ").trim());
				transaction.setTxnDate(CommonUtils.dateFormatter(txnDate, dateFormat));
				transaction.setValueDate(CommonUtils.dateFormatter(valueDate, dateFormat));
				
				if(debit != null && !debit.equalsIgnoreCase("0.00")) {
					transaction.setDebit(debit);
					transaction.setCredit("");
					transaction.setTxnType("DEBIT");
					transaction.setAmount(debit);
				}else {
					transaction.setCredit(credit);
					transaction.setDebit("");
					transaction.setTxnType("CREDIT");
					transaction.setAmount(credit);
				}
				transaction.setBalance(balance);
				transactions.add(transaction);
				transaction = new Transaction();
				txnDate = valueDate =  desc = credit = debit = balance = "";
			}
		}
		return transactions;
	}
}
