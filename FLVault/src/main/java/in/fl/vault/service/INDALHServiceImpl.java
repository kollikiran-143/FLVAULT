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
public class INDALHServiceImpl implements INDALHService{

	private final static Logger log = Logger.getLogger(INDALHServiceImpl.class);

	@Override
	public BSInfo parseINDALH1(ParseBankStmtRequestDTO request) throws IOException, InterruptedException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering INDALHServiceImpl parseINDALH1 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filepath = request.getFileName();

		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "3");

			bankStatementInfo
					.setName(CommonUtils.extractField(text, "Customer\\s*Name\\s*:(.*)CIF").replaceAll("\\s+", " "));
			String[] addressInArray = CommonUtils.extractMultiGroupArray(text,
					"Address\\s*:(.*)Account.*\\n([\\s\\S]*?)\\s*State");
			bankStatementInfo.setAddress((addressInArray[0] + " " + addressInArray[1]).replaceAll("\\n", " ")
					.replaceAll("\\s+", " ").trim());
			bankStatementInfo.setPhone1(CommonUtils.extractField(text, "Mobile\\s*No\\s*:(.*)Currency"));
			String[] emailInArray = CommonUtils.extractMultiGroupArray(text,
					"Email\\s*ID\\s*:\\s*(.*)\\s*Home.*\\n([\\s\\S]*?)\\s*Branch");
			bankStatementInfo.setEmail((emailInArray[0] + emailInArray[1]).replaceAll("\\s+", " ").trim());
			bankStatementInfo
					.setAccountType(CommonUtils.extractField(text, "Account\\s*Type\\s*:(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*Number\\s*:(.*)"));
			bankStatementInfo
					.setBranch(CommonUtils.extractField(text, "Home\\s*Branch\\s*:(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "Branch\\s*IFSC\\s*:(.*)"));
			String dateFormatString = "dd/MM/yyyy";
			bankStatementInfo.setStartDate(
					CommonUtils.dateFormatter(CommonUtils.extractField(text, "Period.*From(.*)To"), dateFormatString));
			bankStatementInfo.setEnDate(CommonUtils
					.dateFormatter(CommonUtils.extractField(text, "Period.*From.*To(.*)Statement"), dateFormatString));
			bankStatementInfo
					.setTransactions(extractTransactionsINDALH_1(filepath, bankStatementInfo.getAccountNo()));

		} catch (Exception e) {
//	    	e.printStackTrace();
			log.error("Error in INDALHServiceImpl parseINDALH1: ", e);
		}

		log.info("Exiting INDALHServiceImpl parseINDALH1: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for IndianAllahaService parseINDALH1 is ==>" + timeTaken);
		return bankStatementInfo;
	}

	@Override
	public BSInfo parseINDALH2(ParseBankStmtRequestDTO request) throws IOException, InterruptedException{
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering INDALHServiceImpl parseINDALH2 with request: " + request);

		BSInfo bsInfo = new BSInfo();
		String filepath = request.getFileName();
		
		try {
			String pdfText = CommonUtils.extractTextFromPdf(filepath, "7");
			
			String accountNo=CommonUtils.extractField(pdfText, "Account\\s*Number\\s*:(.*)").trim();
			String []txnDateRegion=CommonUtils.extractMultiGroupArray(pdfText, "STATEMENT\\s*OF\\s*ACCOUNT\\s*from\\s*(\\d{2}/\\d{2}/\\d{4})\\s*to\\s*(\\d{2}/\\d{2}/\\d{4})");
		    String addressregion=CommonUtils.extractField(pdfText, "Account\\s*Number\\s*:.*\\n([\\s\\S]*?)(?=\\s*Value\\s*Post)").trim();
          
		    bsInfo.setBranch(CommonUtils.extractField(pdfText, "Branch\\s*Code\\s*:(.*)").trim());
			bsInfo.setAccountNo(accountNo);
			
			String []addressLine=addressregion.split("\n");
			String address="";
			for(int i=0;i<addressLine.length;i++) {
				if(i==0) {
					bsInfo.setName(addressLine[0].replaceAll("\\s+"," "));
				} else {
					address+=addressLine[i].trim();
				}
			}
			bsInfo.setAddress(address.replaceAll("\\s+"," "));
			String dateFormat = "dd/MM/yyyy";
			if(txnDateRegion.length==2) {
				bsInfo.setStartDate(CommonUtils.dateFormatter(txnDateRegion[0], dateFormat));
				bsInfo.setEnDate(CommonUtils.dateFormatter(txnDateRegion[1], dateFormat));	
			}
			bsInfo.setTransactions(extractTransactionsINDALH_2(filepath, accountNo));

		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in INDALHServiceImpl parseINDALH2: "+e);
		}
		
		log.info("Exiting INDALHServiceImpl parseINDALH2:" + bsInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for INDALHServiceImpl parseINDALH2 is ==>" + timeTaken);
		return bsInfo;
	}

	@Override
	public BSInfo parseINDALH3(ParseBankStmtRequestDTO request) throws IOException, InterruptedException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering INDALHServiceImpl parseINDALH3 with request: " + request);

		BSInfo bsInfo = new BSInfo();
		String filepath = request.getFileName();
		List<Transaction> transactions = new ArrayList<>();

		try {
			String pdfText = CommonUtils.extractTextFromPdf(filepath, "3");

			Pattern pattern1 = Pattern.compile("^\\s+[a-zA-Z]{3}\\s+\\d{2}\\s+\\d{4}", Pattern.MULTILINE);
			Pattern pattern2 = Pattern.compile("^\\s+\\d{2}\\s+[a-zA-Z]{3}\\s+\\d{4}", Pattern.MULTILINE);
			Matcher matcher1 = pattern1.matcher(pdfText);
			Matcher matcher2 = pattern2.matcher(pdfText);

			Pattern patternTxn = null;
			String dateFormat = "";
			if (matcher1.find()) {
				patternTxn = Pattern.compile(
						"^(\\s+[a-zA-Z]{3}\\s+\\d{2}\\s+\\d{4}[\\s\\S]*?)(?=\\s*Date|\\s*\\w+\\s+\\d{2}\\s+\\d{4}|\\s*Ending\\s*Balance)",
						Pattern.MULTILINE);
				dateFormat = "MMMddyyyy";
			} else if (matcher2.find()) {
				patternTxn = Pattern.compile(
						"^(\\s+\\d{2}\\s+[a-zA-Z]{3}\\s+\\d{4}[\\s\\S]*?)(?=\\s*Date|\\s+\\d{2}\\s+[a-zA-Z]{3}\\s+\\d{4}|\\s*Ending\\s*Balance)",
						Pattern.MULTILINE);
				dateFormat = "ddMMMyyyy";
			}
			Matcher matcher = patternTxn.matcher(pdfText);
			String name = CommonUtils.extractMultiLinesField(pdfText,
					"(.*Account\\s*Holder\\s*Name\\s*[\\s\\S]*?)(?=\\s*Account\\s*Type)", 47, 47);
			String address = CommonUtils.extractMultiLinesField(pdfText,
					"(.*Customer's\\s*Address[\\s\\S]*?)(?=\\s*Branch\\s*Name)", 47, 47);

			String accountNo = CommonUtils.extractField(pdfText, "Account\\s*Number\\s*([\\w\\s]*?)\\s{10,}");
			bsInfo.setAccountNo(accountNo);
			bsInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*(\\w*)"));
			bsInfo.setBranch(CommonUtils.extractField(pdfText, "Branch\\s*Name\\s*([\\w\\s]*?)\\s{10,}")
					.replaceAll("\\s+", " ").trim());
			bsInfo.setAccountType(CommonUtils.extractField(pdfText, "Account\\s*Type\\s*([\\w\\s]*?)\\s{10,}")
					.replaceAll("\\s+", " ").trim());
			String dates[] = CommonUtils.extractMultiGroupArray(pdfText,
					"For\\s*period\\s*:\\s*(\\d{2}\\s*\\w*\\s*\\d{4})\\s*-\\s*(\\d{2}\\s*\\w*\\s*\\d{4})");
			if (dates != null) {
				bsInfo.setStartDate(CommonUtils.dateFormatter(dates[0].trim().replaceAll("\\s*", ""), "ddMMMyyyy"));
				bsInfo.setEnDate(CommonUtils.dateFormatter(dates[1].trim().replaceAll("\\s*", ""), "ddMMMyyyy"));
			}

			bsInfo.setAddress(address.replaceAll("\\s+", " "));
			bsInfo.setName(name.replaceAll("\\s+", " "));

			int serialCount = 1;
			while (matcher.find()) {
				Transaction transaction = new Transaction();
				String[] lines = matcher.group(1).split("\\n");
				String txnDate = lines[0].trim().substring(0, 15).trim().replaceAll("\\s*", "");
				String description = lines[0].trim().substring(15, 63);
				String debit = lines[0].trim().substring(63, 91).trim();
				String credit = lines[0].trim().substring(91, 121).trim();
				String balance = lines[0].trim().substring(121).trim();
				balance = balance.replaceAll("INR|CR|DR", "").trim();

				for (int j = 1; j <= lines.length - 1; j++) {
					description += lines[j];
				}
				description = description.replaceAll("\\s+", " ");
				transaction.setsNo(String.valueOf(serialCount++));
				transaction.setAccNo(accountNo);
				transaction.setTxnDate(CommonUtils.dateFormatter(txnDate, dateFormat));
				transaction.setDescription(description.trim());
				if (debit.equalsIgnoreCase("-")) {
					transaction.setTxnType("CREDIT");
					if (!credit.equalsIgnoreCase("-")) {
						credit = credit.substring(4);
						transaction.setAmount(credit.trim());
					} else {
						credit = "0";
						transaction.setAmount(credit);
					}
				} else {
					transaction.setTxnType("DEBIT");
					debit = debit.substring(4);
					transaction.setAmount(debit.trim());
				}
				transaction.setDebit(debit.equalsIgnoreCase("-") ? "" : debit.trim());
				transaction.setCredit(credit.equalsIgnoreCase("-") ? "" : credit.trim());
				transaction.setBalance(balance.trim());

				transactions.add(transaction);
			}
			if(bsInfo.getStartDate() == null || bsInfo.getStartDate().equals("")) {
				bsInfo.setStartDate(transactions.get(0).getTxnDate());
				bsInfo.setEnDate(transactions.get(transactions.size()-1).getTxnDate());
			}
			bsInfo.setTransactions(transactions);
		} catch (Exception e) {
//	    	e.printStackTrace();
			log.error("Error in INDALHServiceImpl parseINDALH3: ", e);
		}
		log.info("Exiting INDALHServiceImpl parseINDALH3:" + bsInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for INDALHServiceImpl parseINDALH3 is ==>" + timeTaken);
		return bsInfo;
	}

	@Override
	public BSInfo parseINDALH4(ParseBankStmtRequestDTO request) throws IOException, InterruptedException{
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering INDALHServiceImpl parseINDALH4 with request: " + request);

		BSInfo bsInfo = new BSInfo();
		String filepath = request.getFileName();
		
		try {
			String pdfText = CommonUtils.extractTextFromPdf(filepath, "7");
			
			String accountNo = CommonUtils.extractField(pdfText, "Account\\s*Number\\s*:\\s*(\\d*)");
			bsInfo.setAccountNo(accountNo);
			bsInfo.setName(CommonUtils.extractField(pdfText, "Product\\s*type.*\\n\\s*(.*)").replaceAll("\\s+", " ").trim());
		    bsInfo.setAddress(CommonUtils.extractMultiLinesField(pdfText, "Product\\s*type.*\\n([\\s\\S]*?)\\n\\s*Nominee", 85));
		    bsInfo.setBranch(CommonUtils.extractField(pdfText,"INDIAN\\s*BANK\\n\\s*([\\s\\S]*?)\\n\\s*IFSC").replaceAll("\\s+", " ").trim());
		    bsInfo.setEmail(CommonUtils.extractField(pdfText, "Email\\s*:\s*(\\w*@[a-z]*\\.[a-z]*)"));
		    bsInfo.setIfsc(CommonUtils.extractField(pdfText, "IFSC\\s*CODE\\s*:\\s*(IDIB\\w{7})"));
		    bsInfo.setNominee(CommonUtils.extractField(pdfText, "Nominee\\s*Name\\s*:\\s*(.*)").replaceAll("\\s+", " ").trim());
		    
		    String dateFormat = "dd/MM/yyyy";
		    String[] period = CommonUtils.extractMultiGroupArray(pdfText, "STATEMENT\\s*OF\\s*ACCOUNT\\s*from\\s*(\\d{2}\\/\\d{2}\\/\\d{4}).*(\\d{2}\\/\\d{2}\\/\\d{4})");
	        if(period != null && period.length>=2) {
	        	bsInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
	        	bsInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
	        }
			bsInfo.setTransactions(extractTransactionsINDALH_4(filepath, accountNo));
		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in INDALHServiceImpl parseINDALH4: "+e);
		}
		
		log.info("Exiting INDALHServiceImpl parseINDALH4:" + bsInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for INDALHServiceImpl parseINDALH4 is ==>" + timeTaken);
		return bsInfo;
	}
	
	private List<Transaction> extractTransactionsINDALH_1(String fileName, String accountNo) throws IOException {
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
				if (data.length == 5 && !data[0].equals("TRANSACTION DATE")) {

					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					String dataFormatString = "dd/MM/yyyy";
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dataFormatString));
					transaction.setDescription(data[1]);
					transaction.setDebit(data[2].equalsIgnoreCase("-") ? "" : data[2]);
					transaction.setCredit(data[3].equalsIgnoreCase("-") ? "" : data[3]);
					if (data[2] != null && !data[2].equalsIgnoreCase("") && !data[2].equals("-")) {
						transaction.setAmount(data[2]);
						transaction.setTxnType("DEBIT");
					} else {
						transaction.setAmount(data[3]);
						transaction.setTxnType("CREDIT");
					}
					transaction.setAccNo(accountNo);
					transaction.setBalance(data[4].substring(0, data[4].length() - 2).trim());

					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}

	private List<Transaction> extractTransactionsINDALH_2(String filePath, String accountNo) throws IOException {
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

				if (data.length < 4 || !matchesDatePattern2(data)){
					continue;
				}
//				System.out.println("----------------->line is: " + line);
				Transaction transaction = new Transaction();
				transaction.setsNo(String.valueOf(serialNoCount++));
				transaction.setAccNo(accountNo);
				String txnDateFormat = "dd/MM/yyyy";
				transaction.setTxnDate(CommonUtils.dateFormatter(data[1], txnDateFormat));
				transaction.setValueDate(CommonUtils.dateFormatter(data[0], txnDateFormat));
				transaction.setDescription(data[5]);
				transaction.setBalance(data[4]);
			    String debit=data[3];
                 if(debit!=null && debit.equalsIgnoreCase("")) {
                     transaction.setTxnType("CREDIT");
                     transaction.setAmount(data[2]);
                     transaction.setDebit("");
                     transaction.setCredit(data[2]);
                 }else {
                     transaction.setTxnType("DEBIT"); 
                     transaction.setAmount(data[3]);
                     transaction.setCredit("");
                     transaction.setDebit(data[3]);
                }
                transactions.add(transaction);
			}
		}
		return transactions;
	}
	
	private List<Transaction> extractTransactionsINDALH_4(String filePath, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(filePath);
		
		int serialNoCount = 1;
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}
				String[] data = line.split("\\|");

				if (data.length < 4 || !matchesDatePattern4(data))
					continue;
//				System.out.println("----------------->line is: " + line);
				Transaction transaction = new Transaction();
				transaction.setsNo(String.valueOf(serialNoCount++));
				transaction.setAccNo(accountNo);
				String txnDateFormat = "dd/MM/yyyy";
				data[1]=data[1].replaceAll("\\s*","");
				data[0]=data[0].replaceAll("\\s*","");
				transaction.setTxnDate(CommonUtils.dateFormatter(data[1], txnDateFormat));
				transaction.setValueDate(CommonUtils.dateFormatter(data[0], txnDateFormat));
				transaction.setDescription(data[3]);
				transaction.setBalance(data[7].substring(0,data[7].length()-2));
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
	
	private boolean matchesDatePattern2(String[] dateStr) {
		String datePattern = "\\d{2}/\\d{2}/\\s*\\d{4}";
		Pattern pattern1 = Pattern.compile(datePattern);
		Matcher matcher1 = pattern1.matcher(dateStr[1]);
		Matcher matcher2 = pattern1.matcher(dateStr[0]);
		return matcher1.find() && matcher2.find();
	}
	
	public boolean matchesDatePattern4(String[] dateStr) {
		String datePattern = "\\d{2}/\\d{2}\\s/*?\\d{4}";
		Pattern pattern1 = Pattern.compile(datePattern);
		Matcher matcher1 = pattern1.matcher(dateStr[0]);
		Matcher matcher2 = pattern1.matcher(dateStr[1]);
		return matcher1.find() && matcher2.find();
	}

}