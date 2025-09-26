package in.fl.vault.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

@Component
public class CommonUtils {

	private final static Logger log = Logger.getLogger(CommonUtils.class);

	@Value("${spring.profiles.active}")
	private String activeProfileValue;

	private static String activeProfile;

	@PostConstruct
	private void init() {
		activeProfile = activeProfileValue;
	}

	public static String extractTextFromPdf(String filePath, String layout) throws IOException, InterruptedException, FileNotFoundException {
		log.info("CommonUtils extractTextFromPdf FilePath : " + filePath + ", Layout : " + layout);
		File pdfFile = new File(filePath);
		float charWidth = Float.parseFloat(layout);
		PDDocument doc = null;
		String pdfText = null;
		try {
			doc = PDDocument.load(pdfFile);
			LayoutTextStripper stripper = new LayoutTextStripper();
			stripper.setSortByPosition(true);
			stripper.setFixedCharWidth(charWidth);

			pdfText = stripper.getText(doc);

			pdfText = pdfText.replaceAll("\n[ \t\n]\n", "\n"); // remove all empty lines
			if (!activeProfile.equalsIgnoreCase("prod")) {
				Files.write(Paths.get("/home/vishnu/Documents/test.txt"), pdfText.getBytes());
			}
		} catch (FileNotFoundException e) {
			log.error("File Not Found!");
		} catch (Exception e) {
			log.error("Error while parsing pdf: ", e);
		} finally {
			if (doc != null) {
				doc.close();
			}
		}
		return pdfText;
	}

	public static List<String[]> tabulaExtraction(String fileName) throws IOException {
		InputStream in = new FileInputStream(fileName);
		PDDocument document = PDDocument.load(in);
		List<String[]> pdfTxnRows = new ArrayList<>();
		try (ObjectExtractor extractor = new ObjectExtractor(document)) {
			SpreadsheetExtractionAlgorithm sea = new SpreadsheetExtractionAlgorithm();
			Iterator<Page> pages = extractor.extract();

			while (pages.hasNext()) {
				Page page = pages.next();
				List<String[]> pageRow = sea.extract(page).stream().map(table -> table.getRows()).flatMap(List::stream).map(row -> row.stream().map(cell -> cell.getText()).toArray(String[]::new))
						.toList();

				for (String[] eachRow : pageRow) {
					pdfTxnRows.add(eachRow);
				}
			}
		} catch (Exception e) {
			log.error("Error in tabulaExtraction Method: " + e);
		}
		document.close();
		return pdfTxnRows;
	}

	public static String extractField(String text, String regex) {
		return extractField(text, regex, 0);
	}

	public static String extractField(String text, String regex, int flags) {
		Pattern pattern = Pattern.compile(regex, flags);
		Matcher matcher = pattern.matcher(text);
		if (matcher.find()) {
			return matcher.group(1).trim();
		}
		return "";
	}

	public static String[] extractMultiGroupArray(String text, String regex) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(text);
		int groupCount = matcher.groupCount();
		String[] result = null;

		if (matcher.find()) {
			result = new String[groupCount];
			for (int i = 1; i <= groupCount; i++) {
				result[i - 1] = matcher.group(i);
			}
		}
		return result;
	}

	public static String extractMultiGroupConcatenate(String text, String regex) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(text);
		int groupCount = matcher.groupCount();
		String result = "";
		if (matcher.find()) {
			for (int i = 1; i <= groupCount; i++) {
				String temp = matcher.group(i).replaceAll("\\s+", " ").trim();
				result += temp + " ";
			}
		}
		return result.trim();
	}

	public static String extractFromMultiLinesAndRegex(String[] text, String[] regex) {
//		System.out.println("length "+ text.length + " " +regex.length);
		String resultString = "";
		if (text.length == regex.length) {
			String[] result = new String[text.length];
			for (int i = 0; i < text.length; i++) {
				if (!regex[i].equalsIgnoreCase("")) {
					result[i] = extractField(text[i], regex[i]);
					resultString += result[i] + " ";
//					System.out.println("result is: "+text[i]+"   "+regex[i]+"   "+result[i]);
				}
			}
		}
		return resultString;
	}

	public static String extractMultiLinesField(String pdfText, String regex, int maxSize) {
		return extractMultiLinesField(pdfText, regex, 0, maxSize);
	}

	public static String extractMultiLinesField(String pdfText, String regex, int startIndex, int maxSize) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(pdfText);

		if (matcher.find()) {
			String[] addressInLines = matcher.group(1).split("\n");
			String result = "";

			for (String eachLine : addressInLines) {
				if (eachLine.length() < startIndex) {
					continue;
				}
				eachLine = eachLine.substring(startIndex);
				if (eachLine.length() > maxSize) {
					result += (eachLine.substring(0, maxSize - 1) + " ");
				} else {
					result += (eachLine + " ");
				}
			}
			return result.replaceAll("A\\s*d\\s*d\\s*r\\s*e\\s*s\\s*s\\s*:?", "").replaceAll("\\s+", " ").trim();
		}
		return "";
	}

	public static String dateFormatter(String value, String format) {
		if (value == null || value.equalsIgnoreCase("")) {
			return "";
		}
		value = value.replaceAll("\\s+", " ");
		SimpleDateFormat reqFormat = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat format1 = new SimpleDateFormat(format);
		try {
			value = reqFormat.format(format1.parse(value));
			return value;
		} catch (ParseException e) {
			log.info("Date format ParseException: Unrecognized date format==> " + format + "-----------Value==> " + value);
		}
		return "";
	}

	public static String getBankName(String bankFormat) {

		String bankCode = bankFormat.substring(0, bankFormat.length() - 2);
		switch (bankCode) {
		case "SBI": {
			return "State Bank of India";
		}
		case "CANARA": {
			return "Canara Bank";
		}
		case "AXIS": {
			return "Axis Bank Ltd";
		}
		case "BANDHAN": {
			return "Bandhan Bank";
		}
		case "BOB": {
			return "Bank of Baroda";
		}
		case "BOI": {
			return "Bank of India";
		}
		case "MAHB": {
			return "Bank of Maharashtra";
		}
		case "CNTB": {
			return "Central Bank of India";
		}
		case "CSB": {
			return "Catholic Syrian Bank";
		}
		case "CUB": {
			return "City Union Bank";
		}
		case "DBS": {
			return "DBS Bank";
		}
		case "EQUITAS": {
			return "Equitas Bank";
		}
		case "FEDERAL": {
			return "Federal Bank";
		}
		case "FINCARE": {
			return "Fincare Bank";
		}
		case "HDFC": {
			return "HDFC Bank";
		}
		case "ICICI": {
			return "ICICI Bank";
		}
		case "IDBI": {
			return "IDBI Bank Ltd";
		}
		case "IDFC": {
			return "IDFC Bank";
		}
		case "INDALH": {
			return "Indian Allahabad Bank";
		}
		case "INDIAN": {
			return "Indian Bank";
		}
		case "INDUS": {
			return "IndusInd Bank";
		}
		case "IOB": {
			return "Indian Overseas Bank";
		}
		case "KNB": {
			return "Karnataka Bank";
		}
		case "KOTAK": {
			return "Kotak Mahindra Bank";
		}
		case "KVB": {
			return "Karur Vysya Bank";
		}
		case "PNB": {
			return "Punjab National Bank";
		}
		case "RBL": {
			return "Ratnakar Bank Ltd";
		}
		case "SIB": {
			return "South Indian Bank";
		}
		case "STND": {
			return "Standard Chartered";
		}
		case "TMB": {
			return "Tamilnad Mercantile Bank";
		}
		case "UCO": {
			return "United Commercial Bank";
		}
		case "UNION": {
			return "Union Bank of India";
		}
		case "AUBL": {
			return "AU small Finance";
		}
		case "DEUT": {
			return "Deutsche Bank";
		}
		case "DLXB": {
			return "Dhanalaxmi BANK";
		}
		case "SURY": {
			return "Suryoday Small Finance Bank";
		}
		case "YESB": {
			return "Yes Bank Ltd";
		}
		case "UJVN": {
			return "Ujjivan Small Finance Bank Ltd";
		}
		case "PSIB": {
			return "Punjab & Sind Bank";
		}
		case "ESMF": {
			return "ESAF Small Finance Bank";
		}
		default: {
			log.warn("CommonUtils getBankName: Bankformat Key Doesn't Exist. " + bankFormat);
			return "";
		}
		}
	}

	public static boolean mdcPut(String key, String value) {
		if (value != null) {
			MDC.put(key, value);
		} else {
			return false;
		}
		return true;
	}

	public static int cleanAmountStringToInt(String raw) {
		int cleanedVal = 0;
		if (!(raw == null || raw.equalsIgnoreCase(""))) {
			String amt = raw.replaceAll("[^0-9.]", "");
			cleanedVal = Integer.parseInt(amt);
		}
		return cleanedVal;
	}

	public static float cleanAmountStringToFloat(String raw) {
		float cleanedVal = 0;
		if (!(raw == null || raw.equalsIgnoreCase(""))) {
			String amt = raw.replaceAll("[^0-9.]", "");
			cleanedVal = Float.parseFloat(amt);
		}
		return cleanedVal;
	}

	public static String cleanAmountString(String raw) {
		String amtStr = "";
		if (!(raw == null || raw.equalsIgnoreCase(""))) {
			amtStr = raw.replaceAll("[^0-9.,]", "");
		}
		return amtStr;
	}
}
