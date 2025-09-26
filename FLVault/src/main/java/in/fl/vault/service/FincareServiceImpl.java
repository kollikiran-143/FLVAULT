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
public class FincareServiceImpl implements FincareService{
	
	private final static Logger log = Logger.getLogger(IndusIndServiceImpl.class);
	
	@Override
	public BSInfo parseFincare1(ParseBankStmtRequestDTO request) throws IOException, InterruptedException {
		long startTimeInMillis = System.currentTimeMillis();
		log.info("Entering FincareServiceImpl parseFincare1 with request: " + request);
		
		BSInfo bankStatementInfo = new BSInfo();
		String filepath = request.getFileName();
		try {
			String text = CommonUtils.extractTextFromPdf(filepath, "4");
			String accountNo = CommonUtils.extractField(text, "ACCOUNT\\s*NO.\\s*:\\s*(\\d*)");
			bankStatementInfo.setAccountNo(accountNo);
			bankStatementInfo.setBranch(CommonUtils.extractField(text, "BRANCH\\s*CODE\\s*:\\s*(\\d*)"));
			bankStatementInfo.setName(CommonUtils.extractField(text, "HOLDER\\s*NAME\\s*:\\s*(.*)BRANCH").replaceAll("\\s+", " "));
			bankStatementInfo.setIfsc(CommonUtils.extractField(text, "IFSC\\s*:\\s*(FSFB\\w*)"));
			String dateFormat = "dd-MMM-yyyy";
			String[] addressInLines = CommonUtils.extractField(text, "ADDRESS\\s*:(.*?)(?=CUSTOMER\\s*ID)", Pattern.DOTALL).split("\n");
			String[] regexForEachLine = { "(.*)IFSC", "(.*)" };
			bankStatementInfo.setAddress(CommonUtils.extractFromMultiLinesAndRegex(addressInLines, regexForEachLine).replaceAll("\\s+", " ").trim());
			
			String[] period = CommonUtils.extractMultiGroupArray(text, "STATEMENT\\s*PERIOD\\s*:\\s*(\\d{2}-[A-Za-z]{3}-\\d{4})\\s*-\\s*(\\d{2}-[A-Za-z]{3}-\\d{4})");
            if (period != null && period.length>1) {
            	bankStatementInfo.setStartDate(CommonUtils.dateFormatter(period[0], dateFormat));
            	bankStatementInfo.setEnDate(CommonUtils.dateFormatter(period[1], dateFormat));
            }
			
			List<Transaction> transactions = extractTransactionsFincare1(text, accountNo, dateFormat);
			bankStatementInfo.setTransactions(transactions);
			
		} catch (Exception e) {
//			e.printStackTrace();
        	log.error("Error in FincareServiceImpl parseFincare1: "+e);
        }
        
        log.info("Exiting FincareServiceImpl parseFincare1: " + bankStatementInfo.printWithoutTrxs());
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Time Taken for FincareServiceImpl parseFincare1 is ==>" + timeTaken);
        return bankStatementInfo;
	}
	
	private List<Transaction> extractTransactionsFincare1(String pdfText, String accountNo, String dateFormat) {
		List<Transaction> transactions = new ArrayList<>();
		pdfText = pdfText.replaceAll(".*?Opening\\s*Balance.*\\n", "");

		Pattern pattern = Pattern.compile(
				"(\\d{2}-\\w{3}-\\d{4})\\s*(.{70})\\s*([\\d,\\.]+)(\\s+)([\\d,\\.]+)([\\s\\S]*?)(?=((\\d{2}-\\w{3}-\\d{4})|Account))");
		Matcher matcher = pattern.matcher(pdfText);
		int serialNoCount = 1;

		while (matcher.find()) {
			Transaction transaction = new Transaction();
			transaction.setsNo(String.valueOf(serialNoCount++));
			transaction.setTxnDate(CommonUtils.dateFormatter(matcher.group(1), dateFormat));
			transaction.setDescription((matcher.group(2) + " " + matcher.group(6)).replaceAll("\\n", " ").replaceAll("\\s+", " ").trim());

			transaction.setAmount(matcher.group(3));
			if (matcher.group(4).length() > 30) { // debit
				transaction.setDebit(transaction.getAmount());
				transaction.setCredit("");
				transaction.setTxnType("DEBIT");
			} else {
				transaction.setCredit(transaction.getAmount());
				transaction.setDebit("");
				transaction.setTxnType("CREDIT");
			}
			transaction.setBalance(matcher.group(5));
			transaction.setAccNo(accountNo);
			transactions.add(transaction);
		}
		return transactions;
	}
}