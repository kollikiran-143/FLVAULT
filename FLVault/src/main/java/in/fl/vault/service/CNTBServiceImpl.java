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
public class CNTBServiceImpl implements CNTBService{
	
	private final static Logger log = Logger.getLogger(CNTBServiceImpl.class);
	
	@Override
	public BSInfo parseCNTB1(ParseBankStmtRequestDTO request) throws IOException, InterruptedException {
		
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering CNTBServiceImpl parseCNTB1 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();
        
        try {
        	String pdfText = CommonUtils.extractTextFromPdf(filePath, "3");
            
            String accountNo = CommonUtils.extractField(pdfText, "Account\\s*No\\.\\s*:\\s*(X*\\d*)");
            bankStatementInfo.setAccountNo(accountNo);
            bankStatementInfo.setName(CommonUtils.extractField(pdfText, "Customer\\s*Name\\s*:\\s*(.*)Branch").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setBranch(CommonUtils.extractField(pdfText, "Branch\\s*:\\s*(.*)"));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(pdfText, "(\\s*Customer\\s*Address\\s*:[\\s\\S]*?)\\n\\s*Account\\s*No", 100)
									.replaceAll("Customer\\s+:?", " ").trim());
			
			String dateFormat = "dd/MM/yyyy";
			String txnDateFormat = "dd/MM/yy";
			String[] period = CommonUtils.extractMultiGroupArray(pdfText, "(\\d{2}\\/\\d{2}\\/\\d{4})\\s*TO\\s*(\\d{2}\\/\\d{2}\\/\\d{4})");
            if (period != null && period.length>1) {
            	bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
            	bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
            }
            List<Transaction> transactions = extractTransactionsCNTB_1(filePath, accountNo, txnDateFormat);
            
            bankStatementInfo.setTransactions(transactions);

        } catch (Exception e) {
//        	e.printStackTrace();
        	log.error("Error in CNTBServiceImpl parseCNTB1: "+e);
        }
        
        log.info("Exiting CNTBServiceImpl parseCNTB1: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for CNTBServiceImpl parseCNTB1 is ==>" + timeTaken);
        return bankStatementInfo;
	}
	
	@Override
	public BSInfo parseCNTB2(ParseBankStmtRequestDTO request) throws IOException, InterruptedException{
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering CNTBServiceImpl parseCNTB2 with request: " + request);

		BSInfo bsInfo = new BSInfo();
		String filePath = request.getFileName();
		List<Transaction> transactions = new ArrayList<>();
		
		try {
			String pdfText = CommonUtils.extractTextFromPdf(filePath, "4");
			Pattern txnPattern = Pattern.compile("(\\s*?\\d{2}/\\d{2}/\\d{2}\\s*\\d{2}/\\d{2}/\\d{2}[\\s\\S]*?)(?=\\s*CARRIED|^\\s*\\d{2}/\\d{2}/\\d{2})",Pattern.MULTILINE);
			Matcher txnMatcher = txnPattern.matcher(pdfText);
			
			String nameRegion=CommonUtils.extractField(pdfText, "CKYC\\s*NO..*\\n([\\s\\S]*?)(?=\\s*Product)");			
			String []dates = CommonUtils.extractMultiGroupArray(pdfText, "Statement\\s*From\\s*(\\d{2}/\\d{2}/\\d{4})\\s*to\\s*(\\d{2}/\\d{2}/\\d{4})");
			String accountNo = CommonUtils.extractField(pdfText, "Account\\s*No.\\s*:\\s*(\\w*)").trim();
			bsInfo.setAccountNo(accountNo);
			bsInfo.setEmail(CommonUtils.extractField(pdfText, "Nomination.*?E-mail\\s*:\\s*(.*)").trim());
			bsInfo.setBranch(CommonUtils.extractField(pdfText, "STATEMENT\\s*OF\\s*ACCOUNT.*\\n(.*)").replaceAll("\\s+", " ").trim());
				
			String dateFormat = "dd/MM/yyyy";
			if(dates!=null && dates.length==2){
				bsInfo.setStartDate(CommonUtils.dateFormatter(dates[0].trim(), dateFormat));
				bsInfo.setEnDate(CommonUtils.dateFormatter(dates[1].trim(), dateFormat));
			}
			
			String []nameLine=nameRegion.split("\\n");
			String name="";
			String address="";
			for(int i=0;i<nameLine.length;i++){
				String line=nameLine[i];
				if(i==0) {
					name=(line.length()>39)?line.substring(0,39).trim():line.trim();
				} else {
					address+=(line.length()>39)?(" "+line.substring(0,39).trim()):(" "+line.trim());
				}
			}
			name=name.replaceAll("\\s+", " ");
			address=address.replaceAll("\\s+"," ");
			bsInfo.setName(name);
			bsInfo.setAddress(address);

			int serialNoCount = 1;
			while(txnMatcher.find()) {
				String region=txnMatcher.group(1);
				String description = "";
				String date = "";
				String debit = "";
				String credit = "";
				String balance = "";
				String valueDate="";
				Transaction transaction = new Transaction();
				
				String []listlist=region.split("\\n");
				int first = 0;
				for(int i=0;i<=listlist.length-1;i++){
	        	   String line=listlist[i];
	        	   if (line.trim().length() == 0) {
                       first = 0;
                       continue;
                   }
	        	   if(first==0) {
	        		   date = line.substring(19, 34).trim();
		           	   valueDate=line.substring(0,19).trim();
		           	   description+= line.substring(34,63).trim();
		           	   debit+=line.substring(81,104).trim();
		           	   credit+= line.substring(104, 132).trim();
		           	   balance+= line.substring(132).trim();  
	        	   } else{
	        		   description+= line.trim();  		
		    	   }
	        	   first++;
				}
		        if(credit.length()>0){
		    	    transaction.setTxnType("CREDIT");
		    	   
		        } else {
		    	    transaction.setTxnType("DEBIT");
		        }
		        description=description.replaceAll("\\s+"," ");
		        transaction.setCredit(credit.equalsIgnoreCase("-") ? "" : credit);
				transaction.setDebit(debit.equalsIgnoreCase("-") ? "" : debit);
	         	transaction.setsNo(String.valueOf(serialNoCount++));
	         	String txnDateFormat = "dd/MM/yy";
	            transaction.setValueDate(CommonUtils.dateFormatter(valueDate, txnDateFormat));
				transaction.setTxnDate(CommonUtils.dateFormatter(date, txnDateFormat));
				transaction.setBalance(balance.substring(0,balance.length()-2));
				transaction.setAccNo(accountNo);
				transaction.setDescription(description.trim());
				transactions.add(transaction);
			}
			bsInfo.setTransactions(transactions);
		}catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in CNTBServiceImpl parseCNTB2: ", e);
		}
		log.info("Exiting CNTBServiceImpl parseCNTB2: " + bsInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for CNTBServiceImpl parseCNTB2 is ==>" + timeTaken+" ms");
		return bsInfo;
	}
	
	@Override
	public BSInfo parseCNTB3(ParseBankStmtRequestDTO request) throws IOException, InterruptedException {
		
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering CNTBServiceImpl parseCNTB3 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();
        
        try {
        	String pdfText = CommonUtils.extractTextFromPdf(filePath, "5");
            String accountNo = CommonUtils.extractField(pdfText, "Account\\s*Number\\s*:\\s*(\\d*)");
            bankStatementInfo.setAccountNo(accountNo);
            bankStatementInfo.setName(CommonUtils.extractField(pdfText, "Product.*\\n(.*)").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setBranch(CommonUtils.extractField(pdfText, "Branch\\s*Code\\s*:\\s*(\\d*)"));
			bankStatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*Code\\s*:(CBIN\\w{7})"));
			bankStatementInfo.setAddress(CommonUtils.extractField(pdfText, "Product.*\\n([\\s\\S]*)Email").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setEmail(CommonUtils.extractField(pdfText, "Email\\s*:(\\w*@\\w*\\.[a-z]*)"));
			
			String dateFormat = "dd/MM/yyyy";
			String txnDateFormat = "dd/MM/yy";
			String[] period = CommonUtils.extractMultiGroupArray(pdfText, "STATEMENT\\s*OF\\s*ACCOUNT\\s*from\\s*(\\d{2}\\/\\d{2}\\/\\d{4}).*(\\d{2}\\/\\d{2}\\/\\d{4})");
            if (period != null && period.length>1) {
            	bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
            	bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
            }
            List<Transaction> transactions = extractTransactionsCNTB_1(filePath, accountNo, txnDateFormat);
            
            bankStatementInfo.setTransactions(transactions);

        } catch (Exception e) {
//        	e.printStackTrace();
        	log.error("Error in CNTBServiceImpl parseCNTB3: "+e);
        }
        
        log.info("Exiting CNTBServiceImpl parseCNTB3: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for CNTBServiceImpl parseCNTB3 is ==>" + timeTaken);
        return bankStatementInfo;
	}

	@Override
	public BSInfo parseCNTB4(ParseBankStmtRequestDTO request) throws IOException, InterruptedException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering CNTBServiceImpl parseCNTB4 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String pdfText = CommonUtils.extractTextFromPdf(filePath, "5");
			bankStatementInfo.setName(CommonUtils.extractField(pdfText, "\\n\\s*Name\\s*(.{50})").replaceAll("\\s+", " "));
			bankStatementInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*Code\\s*(\\S*)"));
			bankStatementInfo.setPhone1(CommonUtils.extractField(pdfText, "Phone\\s*(\\S*)"));
			bankStatementInfo.setBranch(CommonUtils.extractField(pdfText, "Branch\\s*Name\\s*(.*)").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setAccountNo(CommonUtils.extractField(pdfText, "Statement\\s*for\\s*A\\/c\\s*(\\d*)"));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(pdfText, "\\n(\\s*Address[\\s\\S]*?)\\n\\s*Phone", 78));
			String dateFormat = "dd-MMM-yyyy";
			String[] period = CommonUtils.extractMultiGroupArray(pdfText, "Between\\s*(\\d{2}-[A-Za-z]{3}-\\d{4}).*(\\d{2}-[A-Za-z]{3}-\\d{4})");
	        if(period != null && period.length>=2) {
	        	bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
	        	bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
	        }
			bankStatementInfo.setTransactions(extractTransactionsCNTB_4(pdfText, bankStatementInfo.getAccountNo()));
		
		} catch (Exception e) {
//	    	e.printStackTrace();
			log.error("Error in CNTBServiceImpl parseCNTB4: ", e);
		}
		log.info("Exiting CNTBServiceImpl parseCNTB4: " + request);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for CNTBServiceImpl is ==>" + timeTaken);
		return bankStatementInfo;
	}
	
	private List<Transaction> extractTransactionsCNTB_1(String fileName, String accountNo, String dateFormat) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
		
		int serialNoCount = 1;
		Pattern datePattern = Pattern.compile("\\d{2}\\/\\d{2}\\/\\d{2}");
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");
				
				if(line.trim().isEmpty()) {
					continue;
				}
				// first column is txnDate, second column is value
				String[] data = line.split("\\|");
				Matcher dateMatcher = datePattern.matcher(data[0]);
				if(dateMatcher.find() && data.length==8) {
					Transaction transaction = new Transaction();
					transaction.setsNo(String.valueOf(serialNoCount++));
					transaction.setAccNo(accountNo);
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat));
					transaction.setValueDate(CommonUtils.dateFormatter(data[1], dateFormat));
					transaction.setDescription(data[4]);
					transaction.setDebit(data[5]);
					transaction.setCredit(data[6]);
					transaction.setBalance(data[7].replace("CR", "").trim());
					if (data[5] != null && !data[5].equalsIgnoreCase("") && !data[5].equals("-")) {
						transaction.setAmount(data[5]);
						transaction.setTxnType("DEBIT");
					} else {
						transaction.setAmount(data[6]);
						transaction.setTxnType("CREDIT");
					}
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}
	
	private List<Transaction> extractTransactionsCNTB_4(String pdfText, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		
		try {
			pdfText = pdfText.replaceAll("\\s*Page\\d*of\\d*.*\\n\\s*Date.*Deposits\\s*Balance", "");
			pdfText =pdfText.replaceAll("ClosingBalance","00-00-0000");
			String regex ="(\\d{2}-\\d{2}-\\d{4})\\s*([\\s{0, 8}\\S]*?)\\s*(\\S*)(\\s*)([\\d\\.,]*)\\n([\\s\\S]*?)(?=\\d{2}-\\d{2}-\\d{4})";
			int serialNoCount = 1;
			
			Pattern pattern =Pattern.compile(regex);     //am using this pattern i need loop evry data thats why i havent used common utils multigrparray
			Matcher matcher =pattern.matcher(pdfText);
			
			while (matcher.find()) {
				Transaction transaction = new Transaction();
				transaction.setsNo(Integer.toString(serialNoCount++));
				transaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(1), "dd-MM-yyyy"));
				transaction.setDescription((matcher.group(2)+ " " + matcher.group(6)).replaceAll("\\s+", " ").trim());
				transaction.setAmount(matcher.group(3));
				if(matcher.group(4).length()>16) { // debit
					transaction.setDebit(transaction.getAmount());
					transaction.setTxnType("DEBIT");
				} else {
					transaction.setCredit(transaction.getAmount());
					transaction.setTxnType("CREDIT");
				}
				transaction.setBalance(matcher.group(5));
				transaction.setAccNo(accountNo);
				transactions.add(transaction);
			}
			
		} catch (Exception e) {
			log.error("Error in CNTBServiceImpl extractTransactionsCNTB_4: ", e);
		}
		return transactions;
	}

}
