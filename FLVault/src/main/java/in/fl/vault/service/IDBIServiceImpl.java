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
public class IDBIServiceImpl implements IDBIService{
	
	private final static Logger log = Logger.getLogger(IDBIServiceImpl.class);
	
	@Override
	public BSInfo parseIDBI1(ParseBankStmtRequestDTO request) throws IOException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering IDBIServiceImpl parseIDBI2 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "3");
            String ifsCode = "";
            String nominee = "";
            String combinedRegex = "(?:Account\\s*Number\\s+MICR\\s*Code\\s+IFS\\s*Code\\s+Nomination\\s*)\\n*(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(.+)";
            Pattern pattern = Pattern.compile(combinedRegex);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
            	ifsCode = matcher.group(3).trim();
                nominee = matcher.group(4).trim();
            }
            bankStatementInfo.setIfsc(ifsCode);
            bankStatementInfo.setNominee(nominee.replaceAll("\\s+", " "));
            bankStatementInfo.setName(CommonUtils.extractField(text, "A\\/c\\s+Name\\s*:.*?\\d*\\.\\s*(.+)").replaceAll("\\s+", " "));
            bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "ACCOUNT\\s*Number\\s*:\\s*(\\d+)\\s*in"));
            bankStatementInfo.setAccountType(CommonUtils.extractField(text, "(?:\\d+\\s+){2}\\S+\\s+(.+?)\\s+INR").replaceAll("\\s+", " "));
            bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "Customer\\s*ID\\s*:.*\\n([\\s\\S]*?)\\n\\s*SAC\\s*Code", 90));
            
            String datePattern1 = "(\\d{2}-[A-Za-z]+-\\d{4})\\s*To\\s*(\\d{2}-[A-Za-z]+-\\d{4})";
    		String datePattern2 = "(\\d{2}-\\d{2}-\\d{4})\\s*To\\s*(\\d{2}-\\d{2}-\\d{4})";
    		String dateFormat = "dd-MMM-yyyy";
            String[] period = CommonUtils.extractMultiGroupArray(text, datePattern1);
            if(period == null) {
            	period = CommonUtils.extractMultiGroupArray(text, datePattern2);
            	dateFormat = "dd-MM-yyyy";
            }
            if(period != null && period.length>1) {
            	bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
                bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
            }
            List<Transaction> transactions = extractTransactionsIDBI_1(text, bankStatementInfo.getAccountNo());
            bankStatementInfo.setTransactions(transactions);

        } catch (Exception e) {
//        	e.printStackTrace();
			log.error("Error in IDBIServiceImpl parseIDBI1: ", e);
		}
		log.info("Exiting IDBIServiceImpl parseIDBI1: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for IDBIServiceImpl parseIDBI1 is ==>" + timeTaken);
		return bankStatementInfo;
	}
	
	@Override
	public BSInfo parseIDBI2(ParseBankStmtRequestDTO request) throws IOException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering IDBIServiceImpl parseIDBI2 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "3");
			bankStatementInfo.setName(
					CommonUtils.extractField(text, "Holder\\s*Name\\s*:(.*)Sol\\s*Id").replaceAll("\\s+", " "));
			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*No\\s*:\\s*(\\d*)"));
			bankStatementInfo.setNominee(CommonUtils.extractField(text, "Nominee.*:(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Account\\s*Branch\\s*:\\s*(\\w*)"));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "\\n(\\s*Address[\\s\\S]*?)\\n\\s*Account\\s*No", 90));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*Code\\s*:(.*)"));
			bankStatementInfo.setStartDate(CommonUtils
					.dateFormatter(CommonUtils.extractField(text, "Transaction.*From(.*)to"), "dd-MMM-yyyy"));
			bankStatementInfo.setEnDate(CommonUtils
					.dateFormatter(CommonUtils.extractField(text, "Transaction.*to(.*)Statement"), "dd-MMM-yyyy"));

			bankStatementInfo.setTransactions(extractTransactionsIDBI_2(filepath, bankStatementInfo.getAccountNo()));
		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in IDBIServiceImpl parseIDBI2 :", e);
		}
		log.info("Exiting IDBIServiceImpl parseIDBI2: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for IDBIServiceImpl parseIDBI2 is ==>" + timeTaken);
		return bankStatementInfo;
	}
	
	@Override
	public BSInfo parseIDBI3(ParseBankStmtRequestDTO request) throws IOException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering IDBIServiceImpl parseIDBI3 with request:" + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();
        
        try {
        	String pdfText = CommonUtils.extractTextFromPdf(filePath, "3");
              
            String name = CommonUtils.extractField(pdfText, "Primary\\s*Account\\s*Holder\\s*Name\\s*:\\s*(.+)");
            String accountNo = CommonUtils.extractField(pdfText, "Account\\s*No\\s*:\\s*(\\d+)");
            String branch = CommonUtils.extractField(pdfText, "Account\\s*Branch\\s*:\\s*(.+)");
            String startDate = CommonUtils.extractField(pdfText, "From\\s*:\\s*(\\d{2}/\\d{2}/\\d{4})");
            String endDate = CommonUtils.extractField(pdfText, "to\\s*:\\s*(\\d{2}/\\d{2}/\\d{4})");
            String lines = (CommonUtils.extractField(pdfText, "Address\\s*:\\s*(.*?)Account\\s*No",Pattern.DOTALL));
            String[] parts = lines.split("\\n+"); 
            String address = "";
            for(String part : parts) {
            	address += part.trim() + " ";
            }
            String nominee = CommonUtils.extractField(pdfText, "Nominee\\s*Registered\\s*:\\s*(.+)");
            
            bankStatementInfo.setName(name.replaceAll("\\s+", " "));
            bankStatementInfo.setAccountNo(accountNo);
            bankStatementInfo.setBranch(branch.replaceAll("\\s+", " "));
            bankStatementInfo.setStartDate(CommonUtils.dateFormatter(startDate,"dd/MM/yyyy"));
            bankStatementInfo.setEnDate(CommonUtils.dateFormatter(endDate,"dd/MM/yyyy"));
            bankStatementInfo.setAddress(address.replaceAll("\\s+", " "));
            bankStatementInfo.setNominee(nominee);
            List<Transaction> transactions = extractTransactionsIDBI_3(pdfText, accountNo);
            bankStatementInfo.setTransactions(transactions);

        } catch (Exception e) {
//           	e.printStackTrace();
        	log.error("Error in IDBIServiceImpl parseIDBI3: "+e);
        }
        log.info("Exiting IDBIServiceImpl parseIDBI3: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for IDBIServiceImpl parseIDBI3 is ==>" + timeTaken);
        return bankStatementInfo;
	}
	
	@Override
	public BSInfo parseIDBI4(ParseBankStmtRequestDTO request) throws IOException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering IDBIServiceImpl parseIDBI4 with request:" + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();
        
        try {
        	String pdfText = CommonUtils.extractTextFromPdf(filePath, "6");
            
            String[] nameAccountNoArr = CommonUtils.extractMultiGroupArray(pdfText, "Account\\s*No\\s*:\\s*(\\d*)\\s*\\S*\\s*(.*)");
            if(nameAccountNoArr != null) {
                bankStatementInfo.setAccountNo(nameAccountNoArr[0]);
                bankStatementInfo.setName(nameAccountNoArr[1]);
            }
            bankStatementInfo.setBranch(CommonUtils.extractField(pdfText, "IDBI\\s*BANK\\s*LTD\\s*,?\\s*(\\S*)"));
            String dateFormat = "dd-MM-yyyy";
            String[] period = CommonUtils.extractMultiGroupArray(pdfText, "Ledger\\s*Report\\s*from\\s*(\\d{2}-\\d{2}-\\d{4}).*(\\d{2}-\\d{2}-\\d{4})");
	        if(period !=null && period.length>=2) {
	        	bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
	        	bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
	        }
            List<Transaction> transactions = extractTransactionsIDBI_4(pdfText, bankStatementInfo.getAccountNo(), dateFormat);
            bankStatementInfo.setTransactions(transactions);
        } catch (Exception e) {
//           	e.printStackTrace();
        	log.error("Error in IDBIServiceImpl parseIDBI4: "+e);
        }
        log.info("Exiting IDBIServiceImpl parseIDBI4: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for IDBIServiceImpl parseIDBI4 is ==>" + timeTaken);
        return bankStatementInfo;
	}
	
	private List<Transaction> extractTransactionsIDBI_1(String text, String accountNo) throws IOException {
        List<Transaction> transactions = new ArrayList<>();
        Pattern dateFormat1 = Pattern.compile("\\d{2}-\\d{2}-\\d{4}");       // dd-MM-yyyy
		Pattern dateFormat2 = Pattern.compile("\\d{2}\\/\\d{2}\\/\\d{4}");   // dd/MM/yyyy
		String txnDateFormat = "";
		
        Pattern transactionPattern = Pattern.compile("(^\\s{0,30}\\d{2}[\\/-]\\d{2}[\\/-]\\d{4}[\\s\\S]*?)(?=^\\s*\\d{2}[\\/-]\\d{2}[\\/-]\\d{4}|^\\s*Balance|^\\s*Page)", Pattern.MULTILINE);
        Matcher matcher = transactionPattern.matcher(text);
        int cnt=0;
        while(matcher.find()) {
        	if(cnt==0) {
        		cnt=1;
        		continue;
        	}
            Transaction currentTransaction = new Transaction();
        	String region=matcher.group(1);
        	String description="";
        	String credit="";
        	String debit="";
        	String txnDate="";
        	String balance="";
        	String []lines=region.split("\\n");

        	for(int j=0; j<lines.length;j++) {
        		if(j==0) {
        			String line=lines[j].trim();
        			if(line.length()>154) {
        				txnDate=line.substring(0,13).trim();
            			description+=line.substring(14,94).trim();
            			debit=line.substring(94,133).trim();
            			credit=line.substring(134,154).trim();
            			balance=line.substring(154).trim();
        			}
        		} else {
        			description+=lines[j].trim();
        		}
        	}
			if(txnDateFormat.equalsIgnoreCase("")) {
				Matcher dateMatcher1 = dateFormat1.matcher(txnDate);
				Matcher dateMatcher2 = dateFormat2.matcher(txnDate);
				if(dateMatcher1.find()) {
					txnDateFormat= "dd-MM-yyyy";
				}else if(dateMatcher2.find()) {
					txnDateFormat = "dd/MM/yyyy";
				}
			}
			currentTransaction.setAccNo(accountNo);
        	currentTransaction.setBalance(balance.substring(0,balance.length()-2).trim());
        	currentTransaction.setDescription(description.replaceAll("\\s+"," "));
        	currentTransaction.setTxnDate(CommonUtils.dateFormatter(txnDate, txnDateFormat));
        	currentTransaction.setsNo(String.valueOf(cnt++));
            if (!(debit.equalsIgnoreCase("0.00") || debit.equalsIgnoreCase(""))) {
                currentTransaction.setDebit(debit);
                currentTransaction.setTxnType("DEBIT");
                currentTransaction.setAmount(debit);
                currentTransaction.setCredit("");
            } else {
                currentTransaction.setCredit(credit);
                currentTransaction.setTxnType("CREDIT");
                currentTransaction.setAmount(credit);
                currentTransaction.setDebit("");
            }
            transactions.add(currentTransaction);
        }
        return transactions;
    }

	private List<Transaction> extractTransactionsIDBI_2(String fileName, String accountNo) throws IOException {
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
				if (data.length == 8 && !data[0].equals("S.No")) {
					// Last table is not a transaction
					if (data[0].equals("Opening Balance")) {
						break;
					}
					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					transaction.setTxnDate(CommonUtils.dateFormatter(data[1], "dd/MM/yyyy HH:mm:ss"));
					transaction.setValueDate(CommonUtils.dateFormatter(data[2], "dd/MM/yyyy"));
					transaction.setDescription(data[3]);
					transaction.setDebit(data[5]);
					transaction.setCredit(data[6]);
					if (data[6] == null || data[6].equals("")) {
						transaction.setAmount(data[5]);
						transaction.setTxnType("DEBIT");
					} else {
						transaction.setAmount(data[6]);
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

	private List<Transaction> extractTransactionsIDBI_3(String text, String accountNo) {
        List<Transaction> transactions = new ArrayList<>();
        
        Pattern transactionPattern = Pattern.compile(
                "(\\d+)\\s+(\\d{2}/\\d{2}/\\d{4}|\\d{4}-\\d{2}-\\d{2})\\s+.*?\\s+(\\d{2}/\\d{2}/\\d{4}|\\d{4}-\\d{2}-\\d{2})\\s+(.+?)\\s+(Dr.|Cr.)\\s+INR\\s+([\\d,]*\\.\\d{2})\\s+([\\d,]*\\.\\d{2})"
            );
        Pattern datePattern1 = Pattern.compile("\\d{2}\\/\\d{2}\\/\\d{4}");
        Pattern datePattern2 = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
        
        Matcher matcher = transactionPattern.matcher(text);
        long serialNoCount = 1;
        while (matcher.find()) {
            Transaction transaction = new Transaction();
            transaction.setsNo(String.valueOf(serialNoCount++));
            String dateString = matcher.group(2);
            String txnDate="";
            Matcher dateMatcher1 = datePattern1.matcher(dateString);
            Matcher dateMatcher2 = datePattern2.matcher(dateString);
            if(dateMatcher1.find()) {
            	txnDate = CommonUtils.dateFormatter(dateString, "dd/MM/yyyy");
            }else if(dateMatcher2.find()) {
            	txnDate = dateString;
            }
            transaction.setTxnDate(txnDate);    
            transaction.setValueDate(CommonUtils.dateFormatter(matcher.group(3),"dd/MM/yyyy"));    
            transaction.setDescription(matcher.group(4).replaceAll("\\s+", " "));  
            if(matcher.group(5).equalsIgnoreCase("Dr.")) {
            	transaction.setTxnType("DEBIT");
            	transaction.setDebit(matcher.group(6));
            } else {
            	transaction.setTxnType("CREDIT");
            	transaction.setCredit(matcher.group(6));
            }
            transaction.setAmount(matcher.group(6));      
            transaction.setBalance(matcher.group(7));     

            transactions.add(transaction);
        }
        return transactions;
    }
	
	private List<Transaction> extractTransactionsIDBI_4(String pdfText, String accountNo, String dateFormat){
		List<Transaction> transactions = new ArrayList<>();
		String txnPattern = "(\\d{2}-\\d{2}-\\d{4})\\s*(\\d{2}-\\d{2}-\\d{4})\\s*(\\S*)\\s{3,}([\\s\\S]*?)\\s{5,}(\\S*)(\\s*)(\\S*)Cr";
        int serialNoCount = 1;
        String[] lineArr = pdfText.split("\\r?\\n");
        for (String eachLine : lineArr) {
			Transaction transaction = new Transaction();
			String[] txnArr = CommonUtils.extractMultiGroupArray(eachLine, txnPattern);
			if(txnArr != null) {
				transaction.setsNo(Integer.toString(serialNoCount++));
				transaction.setAccNo(accountNo);
				transaction.setTxnDate(CommonUtils.dateFormatter(txnArr[0], dateFormat));
				transaction.setValueDate(CommonUtils.dateFormatter(txnArr[1], dateFormat));
				transaction.setTxnId(txnArr[2]);
				transaction.setDescription(txnArr[3]);
				String amount = txnArr[4];
				transaction.setAmount(amount);
				transaction.setBalance(txnArr[6]);
				
				String whitespaces = txnArr[5];
				if(whitespaces.length() > 20) {
					transaction.setDebit(amount);
					transaction.setCredit("");
					transaction.setTxnType("DEBIT");
				}else {
					transaction.setCredit(amount);
					transaction.setDebit("");
					transaction.setTxnType("CREDIT");
				}
				transactions.add(transaction);
			}
		}
		return transactions;
	}

}