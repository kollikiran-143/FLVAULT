package in.fl.vault.service;

import java.io.*;
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
public class CanaraServiceImpl implements CanaraService{
	
	private final Logger log = Logger.getLogger(StmtServiceImpl.class);
    
	@Override
    public BSInfo parseCanara1(ParseBankStmtRequestDTO request) throws IOException, InterruptedException {
    	
    	long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering CanaraServiceImpl parseCanara1 with request: "+ request);
		
        BSInfo bsInfo = new BSInfo();
		String filePath = request.getFileName();
		try {
	        String pdfText = CommonUtils.extractTextFromPdf(filePath, "5");
	       
	        bsInfo.setName(CommonUtils.extractMultiLinesField(pdfText, "\\n(\\s*Name[\\s\\S]*?)\\n\\s*Phone", 60).replaceAll("Name\\s*", ""));
	        bsInfo.setPhone1(CommonUtils.extractField(pdfText, "Phone\\s*(\\+\\d+)"));
	        bsInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*Code\\s*(\\w+)"));
			bsInfo.setAddress(CommonUtils.extractMultiLinesField(pdfText, "\\n(\\s*A\\s*d\\s*d\\s*r\\s*e\\s*s\\s*s[\\s\\S]*?)\\n\\s*Date", 60));
        	bsInfo.setBranch(CommonUtils.extractMultiLinesField(pdfText, "\\n(.*Branch\\s*Name[\\s\\S]*?)\\n.*IFSC\\s*Code", 60, 60).replaceAll("Branch\s*Name\s*", ""));
        	String stmtAccNoPeriodRegex = "Statement\\s*for\\s*A/c\\s+([X\\d]*)\\s*.*(\\d{2}-[A-Za-z]{3}-\\d{4}).*(\\d{2}-[A-Za-z]{3}-\\d{4})";
			String[] period = CommonUtils.extractMultiGroupArray(pdfText, stmtAccNoPeriodRegex);
			String dateFormat = "dd-MMM-yyyy";
            if (period != null) {
            	bsInfo.setAccountNo(period[0]);
            	bsInfo.setStartDate(CommonUtils.dateFormatter(period[1], dateFormat));
            	bsInfo.setEnDate(CommonUtils.dateFormatter(period[2], dateFormat));
            }
            List<Transaction> transactions=extractTransactionsCanara_1(pdfText, bsInfo.getAccountNo());
            
            bsInfo.setTransactions(transactions);
		}catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in CanaraServiceImpl parseCanara1: "+e);
		}
		
        log.info("Exiting CanaraServiceImpl parseCanara1 with response:" + bsInfo.printWithoutTrxs());
        long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for CanaraServiceImpl parseCanara1 is ==>" + timeTaken);
        return bsInfo;
    }

	@Override
    public BSInfo parseCanara4(ParseBankStmtRequestDTO request) throws IOException, InterruptedException {
		
    	long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering CanaraServiceImpl parseCanara4 with request: "+ request);
		
        BSInfo bsInfo = new BSInfo();
		String filePath = request.getFileName();
		try {
	        List<Transaction> transactions=new ArrayList<>();
	        Pattern pattern2 = Pattern.compile("\\n*\\s*Client\\s*([\\d]*)\\s*Branch\\s*Code\\s*[a-zA-z\\s]*([\\d]*)\\n");
	        Pattern pattern3 = Pattern.compile("(Address\\s*[\\s\\S]*?Address\\s*[\\s\\S]*?(?=Phone))");
	        Pattern pattern4 = Pattern.compile("(Name\\s*[\\s\\S]*?Branch\\s*Name\\s*[\\s\\S]*?(?=Address))");
	        Pattern pattern5 = Pattern.compile("\\n*\\s*Phone\\s*([\\d\\+]*)\\s*IFSC\\s*Code\\s*([\\w]*)\\n");
	        Pattern pattern6 = Pattern.compile("(^\\s*\\d{2}-\\d{2}-\\d{4}[\\s\\S]*?)(?=^\\s*\\d{2}-\\d{2}-\\d{4}|^\\s*Page|^\\s*Closing)", Pattern.MULTILINE);
	        
	        String pdfText = CommonUtils.extractTextFromPdf(filePath, "3");

			Matcher matcher2 = pattern2.matcher(pdfText);
			Matcher matcher3 = pattern3.matcher(pdfText);
			Matcher matcher4 = pattern4.matcher(pdfText);
			Matcher matcher5 = pattern5.matcher(pdfText);
			Matcher matcher6 = pattern6.matcher(pdfText);
			
			String dateFormat = "dd-MMM-yyyy";
			
			String[] accNoPeriod = CommonUtils.extractMultiGroupArray(pdfText, "Statement\\s*for\\s*A\\/c\\s*(\\d*)\\s*Between\\s*(\\d{2}-[A-Za-z]{3}-\\d{4}).*(\\d{2}-[A-Za-z]{3}-\\d{4})");
			if(accNoPeriod != null) {
				bsInfo.setAccountNo(accNoPeriod[0]);
				bsInfo.setStartDate(CommonUtils.dateFormatter(accNoPeriod[1], dateFormat));
				bsInfo.setEnDate(CommonUtils.dateFormatter(accNoPeriod[2], dateFormat));
			}
//			if(matcher.find()){
//				bsInfo.setAccountNo(matcher.group(1));
//				bsInfo.setStartDate(CommonUtils.dateFormatter(matcher.group(2), dateFormat));
//				bsInfo.setEnDate(CommonUtils.dateFormatter(matcher.group(3), dateFormat));
//			}
			if(matcher2.find()){
				bsInfo.setBranch(matcher2.group(2).replaceAll("\\s+", " "));
			}
			if(matcher3.find()) {
				String []address=matcher3.group(1).split("\n");
				String customerAddress="";
				for(int i=0;i<address.length;i++)
				{
					if(address[i].length()<=76)
					{
						continue;
					}
					customerAddress+= address[i].substring(23, 76).trim();				   			
					bsInfo.setAddress(customerAddress.replaceAll("\\s+", " "));
				}
			}
			if(matcher4.find()) {
				String []address=matcher4.group(1).split("\n");
				String name="";
				String branchName="";
				for(int i=0;i<address.length;i++){
					if(address[i].length()<=76){
						 continue;
					}
					name+= address[i].substring(23, 76).trim();				   			
					branchName+=address[i].substring(116, address[i].length()).trim()+" ";
				}
				bsInfo.setName(name.replaceAll("\\s+"," "));
				bsInfo.setBranch(branchName.replaceAll("\\s+"," ").trim());
			}
			if(matcher5.find()){
				 bsInfo.setPhone1(matcher5.group(1));
				 bsInfo.setIfsc(matcher5.group(2));
			}
			List<String> regions = new ArrayList<>();
			int count=1;
	        while (matcher6.find()) {
	            regions.add(matcher6.group());
	        }
	        for (String region : regions) {
	        	String []lines=region.split("\\n");
	            String particular="";
	            Transaction transaction=new Transaction();
		       	String date = "";
		        String debit ="";
		        String credit ="";
		        String balance ="";
	            for(int i=0;i<lines.length;i++) {
	            	if(i==0){
	            		String templine1=lines[0].trim();
	            		if(templine1.length() > 159) {
	            			date=templine1.substring(0,20).trim();
		   	            	particular=templine1.substring(20,95).trim();
		               		debit=templine1.substring(114,137).trim();
		               	    credit=templine1.substring(137,159).trim();
		               	    balance=templine1.substring(159).trim();
	            		}
	            	}else {
	            		particular+=lines[i].trim();
	            	}
	            }
	            particular=particular.replaceAll("\s+"," ");
	            transaction.setTxnDate(CommonUtils.dateFormatter(date, "dd-MM-yyyy"));
	            transaction.setsNo(String.valueOf(count++));
	            transaction.setBalance(balance);
	            transaction.setCredit(credit);
	            transaction.setDebit(debit);
	            transaction.setDescription(particular); 
	            if (debit != null && !debit.equalsIgnoreCase("") && !debit.equals("-")) {
					transaction.setAmount(debit);
					transaction.setTxnType("DEBIT");
				} else {
					transaction.setAmount(credit);
					transaction.setTxnType("CREDIT");
				}
	            transactions.add(transaction);
	        }
	        bsInfo.setTransactions(transactions); 
		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in CanaraServiceImpl parseCanara4: "+e);
		}
		
        log.info("Exiting CanaraServiceImpl parseCanara4 with response:" + bsInfo.printWithoutTrxs());
        long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for CanaraServiceImpl parseCanara4 is ==>" + timeTaken);
        return bsInfo;
    }
	
	@Override
    public BSInfo parseCanara5(ParseBankStmtRequestDTO request) throws IOException, InterruptedException {
    	long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering CanaraServiceImpl parseCanara5 with request: "+ request);
		
        BSInfo bsInfo = new BSInfo();
		String filePath = request.getFileName();
		try {
	        String pdfText = CommonUtils.extractTextFromPdf(filePath, "6");
	        bsInfo.setName(CommonUtils.extractField(pdfText, "Account\\s*Holders\\s*Name\\s*(.*)").replaceAll("\\s+", " "));
			bsInfo.setBranch(CommonUtils.extractField(pdfText, "Branch\\s*Name\\s*(.*)").replaceAll("\\s+", " "));
			bsInfo.setAccountType(CommonUtils.extractField(pdfText, "([\\w\\&\\s]+Account)\\s*Statement\\n").replaceAll("\\s+", " "));
			bsInfo.setAddress(CommonUtils.extractMultiLinesField(pdfText, "Current.*\\n([\\s\\S]*?)\\n\\s*Account\\s*Statement", 30));
			
			String dateFormat = "ddMMMyyyy";
			String[] period = CommonUtils
					.extractMultiGroupArray(pdfText, "IFSC\\s*Code\\s*(CNRB\\w{7})[\\s\\S]*?From\\s*(\\d{2}\\s*[A-Za-z]{3}\\s*\\d{4}).*(\\d{2}\\s*[A-Za-z]{3}\\s*\\d{4})\\n\\s*Account\\s*Number\\s*(\\d*)");
            if (period != null) {
            	bsInfo.setIfsc(period[0]);
            	bsInfo.setStartDate(CommonUtils.dateFormatter(period[1], dateFormat));
            	bsInfo.setEnDate(CommonUtils.dateFormatter(period[2], dateFormat));
            	bsInfo.setAccountNo(period[3]);
            }
            
            List<Transaction> transactions=extractTransactionsCanara_5(filePath, bsInfo.getAccountNo());
            bsInfo.setTransactions(transactions);
		}catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in CanaraServiceImpl parseCanara5: "+e);
		}
		
        log.info("Exiting CanaraServiceImpl parseCanara5 with response:" + bsInfo.printWithoutTrxs());
        long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for CanaraServiceImpl parseCanara5 is ==>" + timeTaken);
        return bsInfo;
    }
    
	@Override
    public BSInfo parseCanara6(ParseBankStmtRequestDTO request) throws IOException, InterruptedException {
    	long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering CanaraServiceImpl parseCanara6 with request: "+ request);
		
        BSInfo bsInfo = new BSInfo();
		String filePath = request.getFileName();
		try {
	        String pdfText = CommonUtils.extractTextFromPdf(filePath, "3");
			
	        String accountNo = CommonUtils.extractField(pdfText, "Account\\s*No\\s*:\\s*(\\d*)");
	        bsInfo.setAccountNo(accountNo);
	        bsInfo.setName(CommonUtils.extractField(pdfText, "Customer\\s*Name\\s*:\\s*(.*)").replaceAll("\\s+", " "));
	        bsInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*:\\s*(\\w*)"));
			bsInfo.setBranch(CommonUtils.extractField(pdfText, "Account\\s*Branch\\s*:(.*)").replaceAll("\\s+", " "));
			bsInfo.setNominee(CommonUtils.extractField(pdfText, "Nominee\\s*Name\\s*:(.*)").replaceAll("\\s+", " "));
			bsInfo.setAddress(CommonUtils.extractMultiLinesField(pdfText, "\\n(\\s*Address[\\s\\S]*?)\\n\\s*Nominee\\s*Reference\\s*num", 150));
			
			String dateFormat = "dd-MM-yyyy";
			String[] period = CommonUtils.extractMultiGroupArray(pdfText, "Period\\s*:\\s*(\\d{2}-\\d{2}-\\d{4})\s*To\s*(\\d{2}-\\d{2}-\\d{4})");
            if (period != null && period.length>1) {
            	bsInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
            	bsInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
            }
            
            List<Transaction> transactions=extractTransactionsCanara_6(filePath, accountNo);
            
            bsInfo.setTransactions(transactions);
		}catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in CanaraServiceImpl parseCanara6: "+e);
		}
		
        log.info("Exiting CanaraServiceImpl parseCanara6 with response:" + bsInfo.printWithoutTrxs());
        long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for CanaraServiceImpl parseCanara6 is ==>" + timeTaken);
        return bsInfo;
    }
    
	@Override
    public BSInfo parseCanara7(ParseBankStmtRequestDTO request) throws IOException, InterruptedException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering CanaraServiceImpl parseCanara7 with request: " + request);
		
		BSInfo bankStatementInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "5");
			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*Number\\s*:(.*)"));
			String[] startDate = CommonUtils.extractMultiGroupArray(text, "Statement\\s*From\\s*:\\s*(\\d{2}\\s*\\w{3})\\s*[\\s\\S]*?(\\d{4})");
			String startdate = "";
			if(startDate !=null && startDate.length>=2) {
				startdate += startDate[0]+" "+startDate[1];
				startdate = startdate.trim();
			}
			String[] endDate = CommonUtils.extractMultiGroupArray(text, "Statement\\s*To\\s*:\\s*(\\d{2}\\s*\\w{3})\\s*[\\s\\S]*?\\d{4}\\s*(\\d{4})");
			String enddate = "";
			if(enddate!=null && endDate.length>=2) {
				enddate += endDate[0]+" "+endDate[1];
				enddate = enddate.trim();
			}
			String dateFormat="dd MMM yyyy";
			bankStatementInfo.setStartDate(CommonUtils.dateFormatter(startdate,dateFormat));
			bankStatementInfo.setEnDate(CommonUtils.dateFormatter(enddate,dateFormat));
			bankStatementInfo = extractTransactionsCanara_7(filepath, bankStatementInfo);
		} catch (Exception e) {
//			e.printStackTrace();
        	log.error("Error in CanaraServiceImpl parseCanara7: "+e);
        }

        log.info("Exiting CanaraServiceImpl parseCanara7: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for CanaraServiceImpl parseCanara7 is ==>" + timeTaken);

        return bankStatementInfo;
	}
    
    public List<Transaction> extractTransactionsCanara_1(String pdfText, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();

		pdfText = pdfText.replaceAll("\\s*Statement[\\s\\S]*?Opening\\s*Balance.*", "")
						.replaceAll(".*page[\\s\\S]*?Balance.*", "");
		String[] lines = Arrays.stream(pdfText.split("\\r?\\n"))
						        .filter(line -> !line.trim().isEmpty())
						        .toArray(String[] :: new);
		
		/*
		 * 
		 	(\d{2}-\d{2}-\d{4})(.*?)(-?\d*,?\d*,?\d+\.\d+)(\s{5,})(-?\d*,?\d*,?\d+\.\d+)
			(\d{2}-\d{2}-\d{4})(.{10,45})
			(.*?)(-?\d*,?\d*,?\d+\.\d+)(\s{5,})(-?\d*,?\d*,?\d+\.\d+)
			Chq\s*:\s*\d*
		 */
        
        String date = "";
        String desc = "";
        String credit = "";
        String debit = "";
        String amount = "";
        String balance = "";
        
        Pattern linePtrn1 = Pattern.compile("(\\d{2}-\\d{2}-\\d{4})(.*?)(-?\\d*,?\\d*,?\\d+\\.\\d+)(\\s{5,})(-?\\d*,?\\d*,?\\d+\\.\\d+)");
        Pattern linePtrn2 = Pattern.compile("(\\d{2}-\\d{2}-\\d{4})(.{5,45})");
        Pattern linePtrn3 = Pattern.compile("(.*?)(-?\\d*,?\\d*,?\\d+\\.\\d+)(\\s{5,})(-?\\d*,?\\d*,?\\d+\\.\\d+)");
        Pattern txnEndPtrn = Pattern.compile("Chq\\s*:\\s*\\d*");
        
        Pattern pageEndPattern = Pattern.compile("Closing\s*Balance");	
        
        int serialNoCount = 1;
        for (String line : lines) {
        	Matcher pageEndMatcher = pageEndPattern.matcher(line);
        	Matcher linePtrn1Match = linePtrn1.matcher(line);
        	Matcher linePtrn2Match = linePtrn2.matcher(line);
        	Matcher linePtrn3Match = linePtrn3.matcher(line);
        	Matcher txnEndPtrnMatch = txnEndPtrn.matcher(line);
        	if(pageEndMatcher.find()) {
        		break;
        	}
        	if(txnEndPtrnMatch.find()) {
        		desc += " "+ line;
                Transaction transaction = new Transaction();
                transaction.setsNo(String.valueOf(serialNoCount++));
                transaction.setAccNo(accountNo);
                transaction.setTxnDate(CommonUtils.dateFormatter(date, "dd-MM-yyyy"));
                transaction.setDescription(desc.replaceAll("\\s+", " ").trim());
                transaction.setCredit(credit);
                transaction.setDebit(debit);
                transaction.setBalance(balance);
                if (debit != null && !debit.equalsIgnoreCase("")) {
        			transaction.setTxnType("DEBIT");
        			transaction.setAmount(debit);
        		} else {
        			transaction.setTxnType("CREDIT");
        			transaction.setAmount(credit);
        		}
                transactions.add(transaction);
                
                date = desc = credit = debit = amount = balance = "";
        	}else if(linePtrn1Match.find()) {
        		date = linePtrn1Match.group(1);
        		desc += linePtrn1Match.group(2);
        		amount = linePtrn1Match.group(3);
        		balance = linePtrn1Match.group(5);
        		if(linePtrn1Match.group(4).length() >20) {
        			credit = amount;
        		}else {
        			debit = amount;
        		}
        	}else if(linePtrn2Match.find()) {
        		date = linePtrn2Match.group(1);
        		desc += linePtrn2Match.group(2);
        	}else if(linePtrn3Match.find()) {
        		desc += linePtrn3Match.group(1);
        		amount = linePtrn3Match.group(2);
        		balance = linePtrn3Match.group(4);
        		if(linePtrn3Match.group(3).length() >20) {
        			credit = amount;
        		}else {
        			debit = amount;
        		}
        	}else {
        		desc += " " + line;
        	}
        }
		return transactions;
	}

    private List<Transaction> extractTransactionsCanara_5(String fileName, String accountNo) throws IOException {
    	List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
		
		int serialNoCount = 1;
		Pattern datePattern = Pattern.compile("\\d{2}-\\d{2}-\\d{4}");
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");
				
				if(line.trim().isEmpty()) {
					continue;
				}
				// first column is txnDate, second column is value
				String[] data = line.split("\\|");
				Matcher dateMatcher = datePattern.matcher(data[0]);
//						System.out.println("line---------->"+data.length);
				if(dateMatcher.find() && data.length==8) {
					Transaction transaction = new Transaction();
					transaction.setsNo(String.valueOf(serialNoCount++));
					transaction.setAccNo(accountNo);
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], "dd-MM-yyyy HH:mm:ss"));
					transaction.setValueDate(CommonUtils.dateFormatter(data[1], "dd MMM yyyy"));
					transaction.setDescription(data[3]);
					transaction.setDebit(data[5].equalsIgnoreCase("-") ? "" : data[5]);
					transaction.setCredit(data[6].equalsIgnoreCase("-") ? "" : data[6]);
					transaction.setBalance(data[7]);
					if (data[5] != null && !data[5].equalsIgnoreCase("")) {
						transaction.setAmount(transaction.getDebit());
						transaction.setTxnType("DEBIT");
					} else {
						transaction.setAmount(transaction.getCredit());
						transaction.setTxnType("CREDIT");
					}
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}
    
    private List<Transaction> extractTransactionsCanara_6(String fileName, String accountNo) throws IOException {
    	List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
		
		int serialNoCount = 1;
		Pattern datePattern = Pattern.compile("\\d{2}-[A-Z]{3}-\\d{2}");
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");
				
				if(line.trim().isEmpty()) {
					continue;
				}
				// first column is txnDate, second column is value
				String[] data = line.split("\\|");
				Matcher dateMatcher = datePattern.matcher(data[0]);
//						System.out.println("line---------->"+line);
				if(dateMatcher.find() && data.length==8) {
					Transaction transaction = new Transaction();
					transaction.setsNo(String.valueOf(serialNoCount++));
					transaction.setAccNo(accountNo);
					String txnDateFormat = "dd-MMM-yy";
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], txnDateFormat));
					transaction.setValueDate(CommonUtils.dateFormatter(data[1], txnDateFormat));
					transaction.setDescription(data[4]);
					String debit = data[5].equalsIgnoreCase("0.00") ? "" : data[5];
					String credit = data[6].equalsIgnoreCase("0.00") ? "" : data[6];
					transaction.setDebit(debit);
					transaction.setCredit(credit);
					transaction.setBalance(data[7]);
					if (debit != null && !debit.equalsIgnoreCase("")) {
						transaction.setAmount(debit);
						transaction.setTxnType("DEBIT");
					} else {
						transaction.setAmount(credit);
						transaction.setTxnType("CREDIT");
					}
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}
	
    private BSInfo extractTransactionsCanara_7(String fileName, BSInfo bankStatementInfo) throws IOException {
    	List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
		
		int serialNoCount = 1;
		int type = 0;
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");
				
				if(line.trim().isEmpty() || row.length <6 ) {
					continue;
				}
				String[] data = line.split("\\|");
				if (data.length >= 8) {
					if (data[0].equals("Name")) {
						type = 1;
						continue;
					} else if (data[0].equals("Opening Date")) {
						type = 2;
						continue;
					} else if (data[0].equals("Trxn ID")) {
						type = 3;
						continue;
					}
					if (type == 1) {
						bankStatementInfo.setName(data[0]);
						String dateFormat ="yyyy-MM-dd";
						bankStatementInfo.setDob(CommonUtils.dateFormatter(data[1], dateFormat));
						bankStatementInfo.setPhone1(data[2].replaceAll(" ", ""));
						bankStatementInfo.setEmail(data[3]);
						bankStatementInfo.setPan(data[4]);
						bankStatementInfo.setAddress(data[5]);
						bankStatementInfo.setNominee(data[7]);
					} 
					if (type == 2) {
						bankStatementInfo.setAccountType(data[1]);
						bankStatementInfo.setBranch(data[2]);
						bankStatementInfo.setIfsc(data[3].replaceAll("\\s+", ""));
					} 	
					if (type == 3) {
						Transaction transaction = new Transaction();
						String dateFormat ="yyyy-MM-dd";
						transaction.setTxnDate(CommonUtils.dateFormatter(data[1], dateFormat));
						transaction.setDescription(data[4]);
						transaction.setTxnType(data[5]);
						transaction.setAccNo(bankStatementInfo.getAccountNo());
						if(data[5].equalsIgnoreCase("DEBIT")) {
							transaction.setDebit(data[6]);
						}else {
							transaction.setCredit(data[6]);
						}
						transaction.setAmount(data[6]);
						transaction.setBalance(data[7]);
						transaction.setsNo(String.valueOf(serialNoCount++));
						transactions.add(transaction);
					}
				}
			}
		}
		bankStatementInfo.setTransactions(transactions);
		return bankStatementInfo;

    }

}