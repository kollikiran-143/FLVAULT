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
public class IndusIndServiceImpl implements IndusIndService{
	
	private final static Logger log = Logger.getLogger(IndusIndServiceImpl.class);
	
	@Override
	public BSInfo parseIndusInd1(ParseBankStmtRequestDTO request) throws IOException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering IndusIndServiceImpl parseIndusInd1 with request: " + request);
		
		BSInfo bankStatementInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "3");
			String accountNo = CommonUtils.extractField(text, "Account\\s*No.\\s*:\\s*(\\d*)");
			bankStatementInfo.setAccountNo(accountNo);
			bankStatementInfo.setName(CommonUtils.extractField(text, "Account\\n\\s*(.*)Generation").replaceAll("\\s+", " "));
			bankStatementInfo.setAccountType(CommonUtils.extractField(text, "Account\\s*Type\\s*:\\s*(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "Customer\\s*Id.*\\n([\\s\\S]*?)\\n\\s*Date", 75));
			
			String dateFormat = "dd-MMM-yyyy";
			String[] period = CommonUtils.extractMultiGroupArray(text, "Period\\s*:\\s*(\\d{2}-[A-Za-z]{3}-\\d{4})\\s*To\\s*(\\d{2}-[A-Za-z]{3}-\\d{4})");
            if (period != null && period.length>1) {
            	bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
            	bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
            }
			
			List<Transaction> transactions = extractTransactionsIndusInd_1(text, accountNo);
			bankStatementInfo.setTransactions(transactions);
			
		} catch (Exception e) {
//			e.printStackTrace();
        	log.error("Error in IndusIndServiceImpl parseIndusInd1: "+e);
        }
        
        log.info("Exiting IndusIndServiceImpl parseIndusInd1: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for IndusIndServiceImpl parseIndusInd1 is ==>" + timeTaken);
        return bankStatementInfo;
	}
	
	@Override
	public BSInfo parseIndusInd2(ParseBankStmtRequestDTO request) throws IOException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering IndusIndServiceImpl parseIndusInd2 with request: " + request);
		
		BSInfo bankStatementInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "6");
			String accountNo = CommonUtils.extractField(text, "Account\\s*No.\\s*:\\s(\\d*)");
			bankStatementInfo.setAccountNo(accountNo);
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Account\\s*Branch\\s*:\\s*(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setName(CommonUtils.extractField(text, "Account\\s*Branch\\s*:.*\\n(.*)Address").replaceAll("\\s+", " "));
			bankStatementInfo.setAccountType(CommonUtils.extractField(text, "Account\\s*type\\s*:\\s(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*Code\\s*:\\s(\\w*)"));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "Address\\s*:.*\\n([\\s\\S]*?)\\n\\s*From\\s*:\\s*\\d{2}\\s*[A-Za-z]{3}\\s*\\d{4}", 85));
			
			String dateFormat = "ddMMMyyyy";
			String[] period = CommonUtils.extractMultiGroupArray(text, "From\\s*:\\s*(\\d{2}\\s*[A-Za-z]{3}\\s*\\d{4}).*:(\\d{2}\\s*[A-Za-z]{3}\\s*\\d{4})");
            if (period != null && period.length>1) {
            	bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0].replaceAll("\\s*", ""), dateFormat));
            	bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1].replaceAll("\\s*", ""), dateFormat));
            }
			
			List<Transaction> transactions = extractTransactionsIndusInd_2(text, accountNo);
			bankStatementInfo.setTransactions(transactions);
			
		} catch (Exception e) {
//			e.printStackTrace();
        	log.error("Error in IndusIndServiceImpl parseIndusInd2: "+e);
        }
        
        log.info("Exiting IndusIndServiceImpl parseIndusInd2: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for IndusIndServiceImpl parseIndusInd2 is ==>" + timeTaken);
        return bankStatementInfo;
	}
	
	@Override
	public BSInfo parseIndusInd3(ParseBankStmtRequestDTO request) throws IOException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering IndusIndServiceImpl parseIndusInd3 with request: " + request);
		
		List<Transaction>transactions=new ArrayList<>();
		BSInfo bsInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String pdfText = CommonUtils.extractTextFromPdf(filepath, "3");
	
			Pattern patternTxn=Pattern.compile("^\\s*\\d{2}-\\w{3}-\\d{4}[\\s\\S]*?(?=\\s*\\d{2}-\\w{3}-\\d{4}|\\s*For\\s*any\\s*queries)",Pattern.MULTILINE);	
			Pattern addressPattern=Pattern.compile("(Date[\\s\\S]*?)(?=\\s*Mob.)");
			Matcher matcher=patternTxn.matcher(pdfText);
			Matcher addressMatcher=addressPattern.matcher(pdfText);
			
			String branchRegion=CommonUtils.extractField(pdfText, "(.*Branch\\s*Address\\s*:[\\s\\S]*?)(?=\s*PAN)");
			String[] dates=CommonUtils.extractMultiGroupArray(pdfText, "Period\\s*:\\s*(\\d{2}-\\w*-\\d{4})\\s*To\\s*(\\d{2}-\\w*-\\d{4})");
			String accountRegion=CommonUtils.extractField(pdfText, "(Account\\s*No\\s*Account\\s*Type.*\\n.*)");
			
			bsInfo.setBranch(CommonUtils.extractField(pdfText, "Home\\s*Branch\\s*:(.*)").replaceAll("\\s+", " "));
			bsInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\s*Code\\s*:(.*)"));
			bsInfo.setPan(CommonUtils.extractField(pdfText, "PAN\\s*:\\s*(\\w*)"));
			bsInfo.setPhone1(CommonUtils.extractField(pdfText, "Mobile\\s*No\\s*for\\s*SMS\\s*Alerts\\s*:(.*)"));
			bsInfo.setName(CommonUtils.extractField(pdfText, "Account\\s*Number\\s*Name.*\\n(.*)").substring(17,73).replaceAll("\\s+", " ").trim());

	        String[] branchLine=branchRegion.split("\\n");
	        String branch="";
	        for(String line:branchLine){
	        	if(line.length()>117){
	        		branch+=line.substring(117)+" ";
	        	}else {
	        		branch+=line.substring(0)+" ";
	        	}
	        }
	        branch = branch.replaceAll("\\s+", " ").trim();
			bsInfo.setBranch(branch.replaceAll("\\s+", " "));
			
			String[] accountLine=accountRegion.split("\\n");
            if(accountLine.length>1){
            	bsInfo.setAccountNo(accountLine[1].substring(0,32).trim());
            	bsInfo.setAccountType(accountLine[1].substring(32,65).replaceAll("\\s+", " ").trim());
            }
            String dateFormat = "dd-MMM-yyyy";
			bsInfo.setStartDate(CommonUtils.dateFormatter(dates[0].trim(), dateFormat));
			bsInfo.setEnDate(CommonUtils.dateFormatter(dates[1].trim(), dateFormat));
			
			String[] addressRegion = null;
			if(addressMatcher.find()){
				addressRegion=addressMatcher.group(1).split("\\n");	
			}
			String address="";
			for(int i=1;i<addressRegion.length;i++){
				String line=addressRegion[i];
				address+=" "+line.substring(0,60).trim();
			}
			
			bsInfo.setAddress(address.replaceAll("\\s+", " ").trim());

			List<String> regions=new ArrayList<>();
			while (matcher.find()) {
				regions.add(matcher.group());
			}
			int serialNoCount=1;  
			for(int i=0;i<regions.size();i++){
				if(i==regions.size()-1 || i==0){
					continue;
				}
			
			    Transaction transaction=new Transaction();	
				String description="";
				String credit="";
				String debit="";
				String txnDate="";
				String balance="";
				String []lines=regions.get(i).split("\\n");
				if(i==0){
					bsInfo.setStartDate(lines[0].substring(0,24).trim());
				}
				if(i==regions.size()-1){
					bsInfo.setEnDate(lines[0].substring(0,24).trim());
				}
				for(int j=1;j<=lines.length;j++) {
					if(j==1){    
      						txnDate=lines[0].substring(0,24).trim();
    						description+=lines[0].substring(24,88);
    						debit=lines[0].substring(115,142).trim();
    						credit=lines[0].substring(142,169).trim();
    						balance=lines[0].substring(170).trim();
					}else{
						description+=lines[j-1];
					}				
				}
				description=description.replaceAll("\\s+", " ");
				transaction.setsNo(String.valueOf(serialNoCount++));
				transaction.setAccNo(bsInfo.getAccountNo());
				transaction.setTxnDate(CommonUtils.dateFormatter(txnDate, "dd-MMM-yyyy"));
				transaction.setDescription(description.trim());
				transaction.setDebit(debit);
				transaction.setCredit(credit);
				transaction.setBalance(balance);
				
				if(debit.equalsIgnoreCase("")) {
                    transaction.setTxnType("CREDIT");
                    transaction.setAmount(credit);
                } else {
           		    transaction.setCredit("");
                    transaction.setTxnType("DEBIT"); 
                    transaction.setAmount(debit);
                }
				transactions.add(transaction);
			}
			bsInfo.setTransactions(transactions);
			
		} catch (Exception e) {
//			e.printStackTrace();
        	log.error("Error in IndusIndServiceImpl parseIndusInd3: "+e);
        }
        
        log.info("Exiting IndusIndServiceImpl parseIndusInd3: " + bsInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for IndusIndServiceImpl parseIndusInd3 is ==>" + timeTaken);
        return bsInfo;
	}
	
	@Override
	public BSInfo parseIndusInd4(ParseBankStmtRequestDTO request) throws IOException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering IndusIndServiceImpl parseIndusInd4 with request: " + request);
		BSInfo bankStatementInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "5");
			
			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*Number\\s*:?(.*)"));
			String[] startDate = CommonUtils.extractMultiGroupArray(text, "Statement\\s*From\\s*:\\s*(\\d{2}\\s*\\w{3})\\s*[\\s\\S]*?(\\d{4})");
			String startdate = "";
			if(startDate.length>=2) {
				startdate += startDate[0]+" "+startDate[1];
				startdate = startdate.trim();
			}
			String[] endDate = CommonUtils.extractMultiGroupArray(text, "Statement\\s*To\\s*:\\s*(\\d{2}\\s*\\w{3})\\s*[\\s\\S]*?\\d{4}\\s*(\\d{4})");
			String enddate = "";
			if(endDate != null && endDate.length>=2) {
				enddate += endDate[0]+" "+endDate[1];
				enddate = enddate.trim();
			}
			String dateFormat="dd MMM yyyy";
			bankStatementInfo.setStartDate(CommonUtils.dateFormatter(startdate, dateFormat));
			bankStatementInfo.setEnDate(CommonUtils.dateFormatter(enddate, dateFormat));
			bankStatementInfo = extractTransactionsIndusInd_4(filepath, bankStatementInfo);
		} catch (Exception e) {
//			e.printStackTrace();
        	log.error("Error in IndusIndServiceImpl parseIndusInd4: "+e);
        }

        log.info("Exiting IndusIndServiceImpl parseIndusInd4: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for IndusIndServiceImpl parseIndusInd4 is ==>" + timeTaken);
        return bankStatementInfo;
	}
	
	public BSInfo parseIndusInd5_(ParseBankStmtRequestDTO request) throws IOException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering IndusIndServiceImpl parseIndusInd5 with request: " + request);
		
		List<Transaction>transactions=new ArrayList<>();
		BSInfo bsInfo = new BSInfo();
		String filepath = request.getFileName();
		
		try {
			String pdfText = CommonUtils.extractTextFromPdf(filepath, "3");
			pdfText=pdfText.replaceAll(".*Date\\s*Particulars.*\\n.*\\n.*","");
	
			Pattern patternTxn=Pattern.compile("^\\s*(\\d{2})\\s+(.*?)([\\d\\.,]*)\\s*([\\d\\.,]*)\\s*([\\d\\.,]*)\\n(.*)\\n*([\\s\\S]*?\\d{4})",Pattern.MULTILINE);	
			Pattern addressPattern=Pattern.compile("(Date:\\s*\\d{2}\\s*\\w*\\s*\\d{4}\\n[\\s\\S]*?)(?=\\s*Mob.No)");
			Matcher matcher=patternTxn.matcher(pdfText);
			Matcher addressMatcher=addressPattern.matcher(pdfText);
			
			String[] dates=CommonUtils.extractMultiGroupArray(pdfText, "Statement\\s*Period:\\s*(\\d{2}\\s*\\w*\\s*\\d{4})\\s*-\\s*(\\d{2}\\s*\\w*\\s*\\d{4})");

			bsInfo.setEmail(CommonUtils.extractField(pdfText, "Email Id for E Statement:.*\\n(.*)").trim());
			
			bsInfo.setPhone1(CommonUtils.extractField(pdfText, "Mobile\\s*No\\s*for\\s*SMS\\s*Alerts\\s*:(.*)"));
			bsInfo.setPan(CommonUtils.extractField(pdfText, "PAN\\s*:\\s*(\\w*)"));
			bsInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\s*Code\\s*:(.*)"));
			String nameRegion=CommonUtils.extractField(pdfText, "\\s*Account\\s*Number.\\s*Name.*\\n\\s*(.*)");
			String[] account=CommonUtils.extractMultiGroupArray(pdfText, "\\s*Account\\s*No.\\s*Account\\s*Type.*\\n\\s*(\\w*)\\s*(\\w*)");
			if(account.length==2) {
				bsInfo.setAccountType(account[1].trim().replaceAll("\\s+", " "));
				bsInfo.setAccountNo(account[0].trim());
			}
			String name="";
			if(nameRegion!=null && !nameRegion.equalsIgnoreCase("")) {
				name=nameRegion.substring(25,70).trim();
				bsInfo.setName(name.replaceAll("\\s+", " "));
			}						
			String dateFormat = "dd MMM yyyy";
			if(dates.length==2) {
				bsInfo.setStartDate(CommonUtils.dateFormatter(dates[0].trim(), dateFormat));
				bsInfo.setEnDate(CommonUtils.dateFormatter(dates[1].trim(), dateFormat));
			}			
			String addressRegion[] = null;
			if(addressMatcher.find()) {
				addressRegion=addressMatcher.group(1).split("\\n");	
				String address="";
				for(int i=1;i<addressRegion.length;i++) {
					String line=addressRegion[i].trim();
					if(line.length()>60) {
						address+=" "+line.substring(0,60).trim();
					}else {
						address+=" "+line.substring(0).trim();
					}
				}
				bsInfo.setAddress(address.replaceAll("\\s+", " ").trim());
			}
			long serialNoCount = 1;  
			while (matcher.find()) {
			   Transaction transaction=new Transaction();	
				String txnDate=matcher.group(1);
				String description=matcher.group(2).substring(0,85).trim();
				String debit=matcher.group(3);
				String credit=matcher.group(4);
				String balance=matcher.group(5);
				String[] datelines=matcher.group(7).split("\n");
				String group6=matcher.group(6).trim();
				if(group6.length()>11) {
					description+=group6.substring(7).trim();
					txnDate+=" "+group6.substring(0, 7);
				} else {
					txnDate+=" "+group6;
				}
				for(String line:datelines) {
						txnDate+=" "+line.trim();	
				}
				txnDate=txnDate.replaceAll("\\s+", " ");
				transaction.setsNo(String.valueOf(serialNoCount++));
				transaction.setTxnDate(CommonUtils.dateFormatter(txnDate.trim(), "dd MMM yyyy"));
				transaction.setDescription(description.replaceAll("\\s+", " ").trim());
				transaction.setDebit(debit);
				transaction.setCredit(credit);
				transaction.setBalance(balance);
				
				if(debit.equalsIgnoreCase("0.00")) {
                    transaction.setTxnType("CREDIT");
                    transaction.setAmount(credit);
				} else {
                    transaction.setTxnType("DEBIT"); 
                    transaction.setAmount(debit);
           	    }
				transactions.add(transaction);
			}
			bsInfo.setTransactions(transactions);
			
		} catch (Exception e) {
//			e.printStackTrace();
        	log.error("Error in IndusIndServiceImpl parseIndusInd5: "+e);
        }
        log.info("Exiting IndusIndServiceImpl parseIndusInd5: " + bsInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for IndusIndServiceImpl parseIndusInd5 is ==>" + timeTaken);
        return bsInfo;
	}
	
	
	@Override
	public BSInfo parseIndusInd5(ParseBankStmtRequestDTO request) throws IOException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering IndusIndServiceImpl parseIndusInd5 with request: " + request);
		
		BSInfo bsInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String pdfText = CommonUtils.extractTextFromPdf(filepath, "3");
			String accountNo = CommonUtils.extractField(pdfText, "Account\\s*Statement.*\\n\\s*(\\S*)");
			bsInfo.setAccountNo(accountNo);
			bsInfo.setName(CommonUtils.extractField(pdfText, "(.*)Date[\\s\\S]*?Period").replaceAll("\\s+", " ").trim());
			bsInfo.setEmail(CommonUtils.extractField(pdfText, "Email\\s*Id.*:\\s*(\\S*)"));
			bsInfo.setPhone1(CommonUtils.extractField(pdfText, "Mobile\\s*No.*:\\s*(\\S*)"));
			bsInfo.setPan(CommonUtils.extractField(pdfText, "PAN\\s*:\\s*(\\w*)"));
			bsInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\s*Code\\s*:(.*)"));
			bsInfo.setAddress(CommonUtils.extractMultiLinesField(pdfText, "Date.*\\n([\\s\\S]*?)\\n\\s*Mob\\.No", 90));
			
			String dateFormat = "ddMMMyyyy";
			String[] period = CommonUtils.extractMultiGroupArray(pdfText, "Period\\s*:\\s*(\\d{2}\\s*[A-Za-z]{3}\\s*\\d{4}).*(\\d{2}\\s*[A-Za-z]{3}\\s*\\d{4})");
			if(period != null && period.length == 2) {
				bsInfo.setStartDate(CommonUtils.dateFormatter(period[0].replaceAll("\\s*", ""), dateFormat));
				bsInfo.setEnDate(CommonUtils.dateFormatter(period[1].replaceAll("\\s*", ""), dateFormat));
			}
			
			List<Transaction> transactions = extractTransactionsIndusInd_5(pdfText, accountNo);
			bsInfo.setTransactions(transactions);
			
		} catch (Exception e) {
//			e.printStackTrace();
        	log.error("Error in IndusIndServiceImpl parseIndusInd5: "+e);
        }
        log.info("Exiting IndusIndServiceImpl parseIndusInd5: " + bsInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for IndusIndServiceImpl parseIndusInd5 is ==>" + timeTaken);
        return bsInfo;
	}
	
	@Override
	public BSInfo parseIndusInd6(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering IndusIndServiceImpl parseIndusInd6 with request: " + request);
		
		BSInfo bsInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String pdfText = CommonUtils.extractTextFromPdf(filepath, "3");
			String accountNo = CommonUtils.extractField(pdfText, "Account\\s*Number\\s*:\\s*(\\S*)");
			bsInfo.setAccountNo(accountNo);
			bsInfo.setName(CommonUtils.extractField(pdfText, "(.*)\\s*Date\\s*:\\s*\\d{1,2}\\s*\\w*").replaceAll("\\s+", " ").trim());
			bsInfo.setPhone1(CommonUtils.extractField(pdfText, "(\\d*)\\s*Account\\s*Number"));
			bsInfo.setAddress(CommonUtils.extractMultiLinesField(pdfText, "Indus.*Bank.*\\n([\\s\\S]*)\\n\\s*Statement.*Account", 100));
			
			List<Transaction> transactions = extractTransactionsIndusInd_6(filepath, accountNo);
			if(transactions != null) {
				bsInfo.setEnDate(transactions.get(0).getTxnDate());
				bsInfo.setStartDate(transactions.get(transactions.size()-1).getTxnDate());
			}
			bsInfo.setTransactions(transactions);
			
		} catch (Exception e) {
//			e.printStackTrace();
        	log.error("Error in IndusIndServiceImpl parseIndusInd6: "+e);
        }
        log.info("Exiting IndusIndServiceImpl parseIndusInd6: " + bsInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for IndusIndServiceImpl parseIndusInd6 is ==>" + timeTaken);
        return bsInfo;
	}
	
	@Override
	public BSInfo parseIndusInd7(ParseBankStmtRequestDTO request) {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering IndusIndServiceImpl parseIndusInd7 with request: " + request);
		
		BSInfo bankStatementInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "7");
			String accountNo = CommonUtils.extractField(text, "Account\\s*Number\\s*:\\s(\\S*)");
			bankStatementInfo.setAccountNo(accountNo);
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Account\\s*Branch\\s*:\\s*(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setName(CommonUtils.extractField(text, "(.*)\\s*Cust\\.Reln").replaceAll("\\s+", " "));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*Code\\s*:\\s(\\w*)"));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "MICR\\s*CODE.*\\n([\\s\\S]*?)\\n\\s*CKYCID", 70));
			
			String dateFormat = "dd-MM-yyyy";
			String[] period = CommonUtils.extractMultiGroupArray(text, "Period\\s*:\\s*(\\d{2}-\\d{2}-\\d{4}).*(\\d{2}-\\d{2}-\\d{4})");
            if (period != null && period.length>1) {
            	bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0].replaceAll("\\s*", ""), dateFormat));
            	bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1].replaceAll("\\s*", ""), dateFormat));
            }
			
			List<Transaction> transactions = extractTransactionsIndusInd_7(text, accountNo);
			bankStatementInfo.setTransactions(transactions);
			
		} catch (Exception e) {
//			e.printStackTrace();
        	log.error("Error in IndusIndServiceImpl parseIndusInd7: "+e);
        }
        
        log.info("Exiting IndusIndServiceImpl parseIndusInd7: " + bankStatementInfo);
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for IndusIndServiceImpl parseIndusInd7 is ==>" + timeTaken);
        return bankStatementInfo;
	}
	
	private List<Transaction> extractTransactionsIndusInd_1(String pdfText, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		// (\d{2}-[A-Za-z]{3}-\d{4})\s*([\s\S]*?)[A-Z]\d{3,}\s*(\S+)(\s*)(\S+)
		pdfText = pdfText.replaceAll(".*Page[\\s\\S]*?Account\\s*No.*", "")
					.replaceAll(".*Statement\\s*of[\\s\\S]*?Deposit\\s*Balance.*", "");
		Pattern txnPattern = Pattern.compile("(\\d{2}-[A-Za-z]{3}-\\d{4})\\s+(.+?)\\s+[A-Z]\\d{3,}\\s+(-?[\\d,\\.]+)(\\s+)(-?[\\d,\\.]+)");
		Pattern pdfEndPattern = Pattern.compile("\\d{2}-[A-Za-z]{3}-\\d{4}\\s{50,}[\\d,\\.]*");
		
		String[] lines = pdfText.split("\\r?\\n");
		int serialNoCount = 1;
		Transaction transaction = new Transaction();
		for (String eachLine : lines) {
			Matcher txnMatcher = txnPattern.matcher(eachLine);
			Matcher pdfEndMatcher = pdfEndPattern.matcher(eachLine);
			if(pdfEndMatcher.find()) {
				transactions.add(transaction);
				break;
			}
			if(txnMatcher.find()) {
				if(transaction.getTxnDate() != null) {
					transactions.add(transaction);
					transaction = new Transaction();
				}
				transaction.setsNo(Integer.toString(serialNoCount++));
				transaction.setAccNo(accountNo);
				transaction.setTxnDate(CommonUtils.dateFormatter(txnMatcher.group(1), "dd-MMM-yyyy"));
				transaction.setDescription(txnMatcher.group(2).replaceAll("\\s+", " ").trim());
				transaction.setBalance(txnMatcher.group(5));
				String whitespaces = txnMatcher.group(4);
				String amount = txnMatcher.group(3);
				transaction.setAmount(amount);
				
				if(whitespaces.length() > 25) {
					transaction.setDebit(amount);
					transaction.setTxnType("DEBIT");
					transaction.setCredit("");
				}else {
					transaction.setCredit(amount);
					transaction.setTxnType("CREDIT");
					transaction.setDebit("");
				}
			}else {
				String desc = transaction.getDescription() + eachLine.trim();
				transaction.setDescription(desc.replaceAll("\\s+", " ").trim());
			}
		}
		return transactions;
	}
	
	private List<Transaction> extractTransactionsIndusInd_2(String pdfText, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		pdfText = pdfText.replaceAll("Account\\s*Branch[\\s\\S]*?Balance.*", "").replaceAll(".*Page[\\s\\S]*?Balance.*", "");
//		\s+(\d{2}\s*[A-Za-z]{3}\s*\d{4})\s*.{15}\s*(.*?)\s{10,}(\S*)\s*(\S*)\s*(\S*)
//		(\d{2}\s*[A-Za-z]{3}\s\d{4})\s\S*\s*([\s\S]*?)\s{10,}(\S*)\s*(\S*)\s*(\S*)
		Pattern txnPattern = Pattern.compile("\\s+(\\d{2}\\s*[A-Za-z]{3}\\s*\\d{4})\\s*.{15}\\s*(.*?)\\s{10,}(\\S*)\\s*(\\S*)\\s*(\\S*)");
		Pattern pdfEndPattern = Pattern.compile("computer\\s*generated\\s*statement");
		
		String[] lines = pdfText.split("\\r?\\n");
		int serialNoCount = 1;
		Transaction transaction = new Transaction();
		for (String eachLine : lines) {
			Matcher pdfEndMatcher = pdfEndPattern.matcher(eachLine);
			Matcher txnMatcher = txnPattern.matcher(eachLine);
			if(eachLine.trim().isEmpty() || eachLine.contains("Page"))
				continue;
			if(pdfEndMatcher.find()) {
				transactions.add(transaction);
				continue;
			}
			
			if(txnMatcher.find()) {
				if(transaction.getTxnDate() != null) {
					transactions.add(transaction);
					transaction = new Transaction();
				}
				transaction.setsNo(Integer.toString(serialNoCount++));
				transaction.setAccNo(accountNo);
				transaction.setTxnDate(CommonUtils.dateFormatter(txnMatcher.group(1).replaceAll("\\s*", ""), "ddMMMyyyy"));
				String desc = txnMatcher.group(2);
				transaction.setDescription(desc.replaceAll("\\s+", " ").trim());
				
				String debit = txnMatcher.group(3);
				String credit = txnMatcher.group(4);
				transaction.setBalance(txnMatcher.group(5));
				if(debit != null && !debit.equalsIgnoreCase("-")) {
					transaction.setDebit(debit);
					transaction.setAmount(debit);
					transaction.setCredit("");
					transaction.setTxnType("DEBIT");
				}else {
					transaction.setCredit(credit);
					transaction.setAmount(credit);
					transaction.setTxnType("CREDIT");
					transaction.setDebit("");
				}
			}else {
				String desc = (transaction.getDescription() == null) ? "" : transaction.getDescription().trim() + eachLine.trim();
				transaction.setDescription(desc.replaceAll("\\s+", " ").trim());
			}
		}
		return transactions;
	}
	
	private BSInfo extractTransactionsIndusInd_4(String fileName, BSInfo bankStatementInfo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
		
		int serialNoCount = 1;
		int type = 0;
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");
			
				if (!line.trim().isEmpty() && row.length >= 6) {
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
							bankStatementInfo.setPhone1(data[2]);
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
		}
		bankStatementInfo.setTransactions(transactions);
		return bankStatementInfo;

	}
	
	private List<Transaction> extractTransactionsIndusInd_5(String pdfText, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		// (\d{2}\s*[A-Za-z]{3}\s*\d{4})\s*([\s\S]*?)\s{5,}(-?\d*,?\d*,?\d*\.\d+)\s*(-?\d*,?\d*,?\d*\.\d+)\s*(-?\d*,?\d*,?\d*\.\d+)
		pdfText = pdfText.replaceAll(".*Date\\s*Particulars.*Balance.*", "");
		
		Pattern txnPattern = Pattern.compile("(\\d{2}\\s*[A-Za-z]{3}\\s*\\d{4})\\s*([\\s\\S]*?)\\s{5,}(-?\\d*,?\\d*,?\\d*\\.\\d+)\\s*(-?\\d*,?\\d*,?\\d*\\.\\d+)\\s*(-?\\d*,?\\d*,?\\d*\\.\\d+)");
		Pattern pdfEndPattern = Pattern.compile("This\\s*is\\s*a\\s*computer\\s*generated\\s*statement");
		
		String[] lines = pdfText.split("\\r?\\n");
		int serialNoCount = 1;
		Transaction transaction = new Transaction();
		for (String eachLine : lines) {
			Matcher txnMatcher = txnPattern.matcher(eachLine);
			Matcher pdfEndMatcher = pdfEndPattern.matcher(eachLine);
			if(pdfEndMatcher.find()) {
				transactions.add(transaction);
				break;
			}
			if(txnMatcher.find()) {
				if(transaction.getTxnDate() != null) {
					transactions.add(transaction);
					transaction = new Transaction();
				}
				transaction.setsNo(Integer.toString(serialNoCount++));
				transaction.setAccNo(accountNo);
				transaction.setTxnDate(CommonUtils.dateFormatter(txnMatcher.group(1).replaceAll("\\s*", ""), "ddMMMyyyy"));
				String[] descArray = txnMatcher.group(2).split("\\s+");
				String desc= "";
				for(int i= 0; i<descArray.length; i++) {
					if(i==descArray.length-1) {
						continue;
					}
					desc += descArray[i] + " ";
				}
				transaction.setDescription(desc.replaceAll("\\s+", " ").trim());
				transaction.setBalance(txnMatcher.group(5));
				String debit = txnMatcher.group(3);
				String credit = txnMatcher.group(4);
				
				if(debit != null && !debit.equalsIgnoreCase("") && !debit.equalsIgnoreCase("0.00")) {
					transaction.setDebit(debit);
					transaction.setTxnType("DEBIT");
					transaction.setCredit("");
					transaction.setAmount(debit);
				}else {
					transaction.setCredit(credit);
					transaction.setTxnType("CREDIT");
					transaction.setDebit("");
					transaction.setAmount(credit);
				}
			}else {
				String desc = transaction.getDescription() + eachLine.trim();
				transaction.setDescription(desc.replaceAll("\\s+", " ").trim());
			}
		}
		return transactions;
	}
	
	
	private List<Transaction> extractTransactionsIndusInd_6(String fileName, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
		String dateFormat = "yyyy-MM-dd";
		
		int serialNoCount = 1;
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}
				String[] data = line.split("\\|");
				
				if (data.length >= 5 && !data[0].equalsIgnoreCase("Date")) {
					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat));
					transaction.setDescription(data[1]); 
					transaction.setTxnId(data[2]);

					if (!data[3].isEmpty() && !data[3].equals("-")) {
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
					transaction.setAccNo(accountNo);
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}
	
	
	private List<Transaction> extractTransactionsIndusInd_7(String pdfText, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		pdfText = pdfText.replaceAll("Branch\\s*Address[\\s\\S]*?Phone\\s*Banking\\s*Numbers.*", "");
		Pattern txnPattern = Pattern.compile("(\\d{2}-[A-Z]{3}-\\d{2})\\s*(.{50})\\s*(\\S*)(\\s*)(\\S*)");
		Pattern pdfEndPattern = Pattern.compile("\\d{2}-[A-Z]{3}-\\d{2}\\s*Carried\\s*Forward");
		Pattern pdfStartPattern = Pattern.compile("\\d{2}-[A-Z]{3}-\\d{2}\\s*Brought\\s*Forward");
		
		String[] lines = pdfText.split("\\r?\\n");
		int serialNoCount = 1;
		boolean txnStart = false;
		Transaction transaction = new Transaction();
		for (String eachLine : lines) {
			Matcher pdfStartMatcher = pdfStartPattern.matcher(eachLine);
			Matcher pdfEndMatcher = pdfEndPattern.matcher(eachLine);
			Matcher txnMatcher = txnPattern.matcher(eachLine);
			
			if(pdfStartMatcher.find()) { 
				txnStart = true;
				continue;
			}
			if(pdfEndMatcher.find()) {
				if(transaction.getTxnDate() != null) {
					transactions.add(transaction);
				}
				break;
			}
			if(txnStart) {
				if(txnMatcher.find()) {
					if(transaction.getTxnDate() != null) {
						transactions.add(transaction);
						transaction = new Transaction();
					}
					transaction.setsNo(Integer.toString(serialNoCount++));
					transaction.setAccNo(accountNo);
					transaction.setTxnDate(CommonUtils.dateFormatter(txnMatcher.group(1).replaceAll("\\s*", ""), "dd-MMM-yy"));
					String desc = txnMatcher.group(2);
					transaction.setDescription(desc.replaceAll("\\s+", " ").trim());
					
					String amount = txnMatcher.group(3).replaceAll("[CD]r", "");
					transaction.setAmount(amount);
					String whiteSpaces = txnMatcher.group(4);
					String balance = txnMatcher.group(5).replaceAll("[CD]r", "");
					transaction.setBalance(balance);
					if(whiteSpaces.length() > 20) {
						transaction.setDebit(amount);
						transaction.setCredit("");
						transaction.setTxnType("DEBIT");
					}else {
						transaction.setCredit(amount);
						transaction.setTxnType("CREDIT");
						transaction.setDebit("");
					}
				}else {
					String desc = (transaction.getDescription() == null) ? "" : transaction.getDescription().trim() + eachLine.trim();
					transaction.setDescription(desc.replaceAll("\\s+", " ").trim());
				}
			}
		}
		return transactions;
	}

}