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
public class FederalServiceImpl implements FederalService{

	private final Logger log = Logger.getLogger(FederalServiceImpl.class);
	
	@Override
	public BSInfo parseFederal1(ParseBankStmtRequestDTO request) throws IOException, InterruptedException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering FederalServiceImpl parseFederal1 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String text = CommonUtils.extractTextFromPdf(filePath, "3");
			
			String name = CommonUtils.extractField(text, "\\s*Name\\s*:\\s*(.*)\\s*Branch\\s*Name").replaceAll("\\s+", " ");
			if(name.isEmpty()) {
				name = CommonUtils.extractField(text, "\\n\\s*Name\\s*:\\s*(.*)\\s*:").replaceAll("\\s+", " ");
			}
			bankStatementInfo.setName(name);
			bankStatementInfo.setBranch(
					CommonUtils.extractField(text, "Branch\\s*Name\\s*:\\s*(.*)").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*Number\\s*:\\s*(.*)"));
			bankStatementInfo.setNominee(CommonUtils.extractField(text, "Nomination\\s*:\\s*(.*)"));
			bankStatementInfo.setPhone1(CommonUtils.extractField(text, "\\s*Regd.\\s*Mobile\\s*Number\\s*:\\s*(\\S*)"));
			bankStatementInfo.setEmail(CommonUtils.extractField(text, "\\s*Email\\s*ID\\s*:\\s*(\\S*)"));
			bankStatementInfo.setAccountType(CommonUtils.extractField(text, "\\s*Type.*?Account\\s*:\\s*(.{40})").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "\\s*IFSC\\s*:\\s*(\\S*)"));

			String startDate = CommonUtils.extractField(text, "for\\s*the\\s*period\\s*(.*)\\s*to");

			Pattern pattern1 = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
			Pattern pattern2 = Pattern.compile("\\d{2}-\\w{3}-\\d{4}");
			Pattern pattern3 = Pattern.compile("\\d{2}-\\d{2}-\\d{4}");
			Matcher matcher1 = pattern1.matcher(startDate);
			Matcher matcher2 = pattern2.matcher(startDate);
			Matcher matcher3 = pattern3.matcher(startDate);
			
			String dateFormat = "";
			if (matcher1.find()) {
				dateFormat = "yyyy-MM-dd";
			} else if (matcher2.find()) {
				dateFormat = "dd-MMM-yyyy";
			} else if (matcher3.find()) {
				dateFormat = "dd-MM-yyyy";
			}

			bankStatementInfo.setStartDate(CommonUtils.dateFormatter(startDate, dateFormat));
			bankStatementInfo.setEnDate(CommonUtils
					.dateFormatter(CommonUtils.extractField(text, "for\\s*the\\s*period.*to\\s*(.*)"), dateFormat));
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(text,
					"(\\n\\s*Communication\\s*Address[\\s\\S]*?)\\n\\s*Address\\s*Last", 98).replaceAll("Communication\\s*:\\s*", ""));
			
//			Pattern txnPattern1 = Pattern.compile("Tran[\\S\\s]*?Tran\\s*ID\\s*Cheque");
			Pattern txnPattern2 = Pattern.compile("Tran\\s*Cheque[\\s\\S]*?Type\\s*Details");
//			Matcher txnMatcher1 = txnPattern1.matcher(text);
			Matcher txnMatcher2 = txnPattern2.matcher(text);
			
			if(txnMatcher2.find()) {
				log.info("Calling parseFederal1 extractTransactionsFederal_1_2");
				bankStatementInfo.setTransactions(extractTransactionsFederal_1_2(filePath, bankStatementInfo.getAccountNo()));
			} else {
				log.info("Calling parseFederal1 extractTransactionsFederal_1_1");
				bankStatementInfo.setTransactions(extractTransactionsFederal_1_1(filePath, bankStatementInfo.getAccountNo()));
			}
			
		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in FederalServiceImpl parseFederal1: " + e);
		}
		log.info("Exiting FederalServiceImpl parseFederal1: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for FederalServiceImpl parseFederal1 is ==>" + timeTaken);
		return bankStatementInfo;
	}
	
	@Override
	public BSInfo parseFederal2(ParseBankStmtRequestDTO request) throws IOException, InterruptedException{
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering FederalServiceImpl parseFederal2 with request: " + request);

		BSInfo bankStatementInfo = new BSInfo();
		String filePath = request.getFileName();

		try {
			String text = CommonUtils.extractTextFromPdf(filePath, "3");

			bankStatementInfo.setName(
					CommonUtils.extractField(text, "Name\\s*(.*)\\s*Branch").replaceAll("\\s+", " "));
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "Branch\\s*Name\\s*(.*)").replaceAll("\\s+", " "));
			bankStatementInfo.setAccountNo(CommonUtils.extractField(text, "Account\\s*Number\s*(\\d*)"));
			bankStatementInfo.setNominee(CommonUtils.extractField(text, "Nomination\\s*(.*)"));
			bankStatementInfo.setPhone1(CommonUtils.extractField(text, "Mobile\\s*Number\\s*(\\+?\\d*)"));
			bankStatementInfo.setEmail(CommonUtils.extractField(text, "Email\\s*ID\\s*(\\w*@[a-z]*\\.[a-z]*)"));
			bankStatementInfo.setAccountType(
					CommonUtils.extractField(text, "Type\\s*Of\\s*Account\\s*(.{48})").replaceAll("\\s+", " ").trim());
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*(FDRL\\w{7})"));
			String dateFormat = "dd-MMM-yyyy";
			String txnDateFormat = "dd/MM/yyyy";
			
			String[] period = CommonUtils.extractMultiGroupArray(text, "ACCOUNT\\s*PERIOD\\s*:(\\d{2}-[A-Za-z]{3}-\\d{4}).*(\\d{2}-[A-Za-z]{3}-\\d{4})");
            if (period != null && period.length>1) {
            	bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
            	bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
            }
			bankStatementInfo.setAddress(CommonUtils.extractMultiLinesField(text, "(Communication\\s*Address[\\s\\S]*?)\\n\\s*Address\\s*Last", 95).replace("Communication", "").replace("Branch", "").trim());
			bankStatementInfo.setTransactions(
					extractTransactionsFederal_2(filePath, bankStatementInfo.getAccountNo(), txnDateFormat));
			
		} catch (Exception e) {
//			e.printStackTrace();
			log.error("Error in FederalServiceImpl parseFederal2: " + e);
		}

		log.info("Exiting FederalServiceImpl parseFederal: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for FederalServiceImpl parseFederal2 is ==>" + timeTaken);
		return bankStatementInfo;
	}
	
	private List<Transaction> extractTransactionsFederal_1_1(String fileName, String accountNo) throws IOException {
		List<Transaction> transactions = new ArrayList<>();
		List<String[]> txnRows = CommonUtils.tabulaExtraction(fileName);
		
		Pattern pattern1 = Pattern.compile("\\d{2}-\\w{3}-\\d{4}");
		Pattern pattern2 = Pattern.compile("\\d{2}\\/\\d{2}\\/\\d{4}");
		Pattern pattern3 = Pattern.compile("\\d{2}-\\d{2}-\\d{4}");
		String txnDateFormat = "";
		int serialNoCount = 1;
		if(txnRows != null && !txnRows.isEmpty()) {
			for (String[] row : txnRows) {
				String line = String.join("|", row).replaceAll("\\r|\\s+", " ");
				if (line.trim().isEmpty()) {
					continue;
				}
				// Assuming first column is txnDate, second column is value
				String[] data = line.split("\\|");
				if (data.length == 10 && !data[0].equals("Date") && !data[0].equals("")) {

					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					
					if(txnDateFormat.equalsIgnoreCase("")) {
						Matcher matcher1 = pattern1.matcher(data[0]);
						Matcher matcher2 = pattern2.matcher(data[0]);
						Matcher matcher3 = pattern3.matcher(data[0]);
						
						if (matcher1.find()) {
							txnDateFormat = "dd-MMM-yyyy";
						} else if (matcher2.find()) {
							txnDateFormat = "dd/MM/yyyy";
						} else if (matcher3.find()) {
							txnDateFormat = "dd-MM-yyyy";
						}
					}
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], txnDateFormat));
					transaction.setValueDate(CommonUtils.dateFormatter(data[1], txnDateFormat));
					transaction.setDescription(data[2]);
					transaction.setTxnId(data[4]);
					
					if (data[6] != null && !data[6].equals("") && !data[6].equals("-")) {
						transaction.setDebit(data[6]);
						transaction.setAmount(data[6]);
						transaction.setTxnType("DEBIT");
						transaction.setCredit("");
					} else {
						transaction.setCredit(data[7]);
						transaction.setAmount(data[7]);
						transaction.setTxnType("CREDIT");
						transaction.setDebit("");
					}
					transaction.setAccNo(accountNo);
					transaction.setBalance(data[8]);

					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}
	
	private List<Transaction> extractTransactionsFederal_1_2(String fileName, String accountNo) throws IOException {
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
				if (data.length == 8 && !data[0].equals("Date") && !data[0].equals("")) {

					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat));
					transaction.setDescription(data[2]);
					
					if (data[5] != null && !data[5].equals("") && !data[5].equals("-")) {
						transaction.setAmount(data[5]);
						transaction.setTxnType("DEBIT");
						transaction.setDebit(data[5]);
						transaction.setCredit("");
					} else {
						transaction.setAmount(data[6]);
						transaction.setTxnType("CREDIT");
						transaction.setCredit(data[6]);
						transaction.setDebit("");
					}
					transaction.setAccNo(accountNo);
					transaction.setBalance(data[7]);

					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}
	
	private List<Transaction> extractTransactionsFederal_2(String fileName, String accountNo, String dateFormat)
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
				if (data.length == 9 && !data[0].equals("Date") && !data[0].equals("")) {

					Transaction transaction = new Transaction();
					transaction.setsNo(Integer.toString(serialNoCount++));
					transaction.setTxnDate(CommonUtils.dateFormatter(data[0], dateFormat));
					transaction.setValueDate(CommonUtils.dateFormatter(data[1], dateFormat));
					transaction.setDescription(data[2]);
					transaction.setDebit(data[5]);
					transaction.setCredit(data[6]);
					if (data[8].equalsIgnoreCase("Dr")) {
						transaction.setAmount(data[5]);
						transaction.setTxnType("DEBIT");
					} else if(data[8].equalsIgnoreCase("Cr")){
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

}