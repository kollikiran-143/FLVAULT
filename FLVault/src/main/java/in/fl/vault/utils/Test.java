package in.fl.vault.utils;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;


public class Test {
	
	private final static Logger log = LoggerFactory.getLogger(Test.class);
	public static void main(String[] args) throws ParseException, IOException {
		
		
		// 10 min before time
//		  Date now = new Date();
//        Calendar calendar = Calendar.getInstance();
//        calendar.setTime(now);
//        calendar.add(Calendar.MINUTE, -10);  //TODO make 10 after testing done
//        Date limitTime = calendar.getTime();
//        
//        System.out.println(now);
//        System.out.println(limitTime);
		
		  // Recieve response as String, Sometimes ObjectMapping generates Error
//		  DigitapMobileAPIRequestDTO digitapMobileAPIRequestDTO = new DigitapMobileAPIRequestDTO();
//        digitapMobileAPIRequestDTO.setClientRefNum(clientRefNum);
//        digitapMobileAPIRequestDTO.setMobileNo(mobileNo);
//        digitapMobileAPIRequestDTO.setNameLookup(Integer.parseInt(requestDTO.getNameLookup()));
//
//        String uri = digitapBaseUrl + "mobile_prefill/request";
//        
//        RestTemplate restTemplate = new RestTemplate();
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("content-type", "application/json");
//        headers.setBasicAuth(clientId, clientSecret);
//        log.info("getDigitapMobileData uri: " + uri);
//        log.info("getDigitapMobileData Request: " + digitapMobileAPIRequestDTO);
//        HttpEntity<Object> entity = new HttpEntity<>(digitapMobileAPIRequestDTO, headers);
//        String responseStr = restTemplate.postForObject(uri, entity,String.class);
//        log.info("getDigitapMobileData Response Str: " + responseStr);
	
		// formatting date to specific format, and parse Date from a specific format; we must provide both formats
//		SimpleDateFormat inputFormat = new SimpleDateFormat("dd-MM-yyyy");   // Recieving from API call
//		SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");  // Saving in DB
//		try {
//			System.out.println(outputFormat.format(inputFormat.parse("02-06-1991")));
//			System.out.println(outputFormat.format(outputFormat.parse("02-06-1991")));
//		} catch (ParseException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		// float->double || double->float   changes the decimal values
		// use String.format("%.2f",dwot)  to format double/float to String
//		float d = (float) 1.77;
//		double dwot = d/1.18f;
//		System.out.println(dwot);
//		System.out.println(String.format("%.2f",dwot) + "% + 18% GST");
		
		// get timeDiff in Mins
//		Date date1 = new Date(); // Current time
//      Date date2 = new Date(date1.getTime() + 5 * 60 * 60 * 1000); // 5 hours later
//      long diff = date2.getTime() - date1.getTime();
//      System.out.println(diff/(60*1000));
		
//		SimpleDateFormat dtFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//		SimpleDateFormat dtFmt2 = new SimpleDateFormat("yyyy-MM-dd");
//		
//		String d = dtFmt.format(new Date());
//		
//		System.out.println("d: "+ d);
//		
//		String formattedFromDate = dtFmt2.format(dtFmt.parse(d));
//      String formattedToDate = dtFmt.format(dtFmt.parse());
//		
//		System.out.println(formattedFromDate);
		
		// get Date and add 4 days and format 
//		Calendar calendar = Calendar.getInstance();
//        // Add 4 days
//		calendar.setTime(new Date());
//        calendar.add(Calendar.DAY_OF_MONTH, 4);
//        
//        // Get the updated date
//        Date newDate = calendar.getTime();
//        
//        // Format the date as dd-MM-yyyy
//        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
//        String formattedDate = sdf.format(newDate);
//        System.out.println(formattedDate);
        
			// Money in Indian Format String
//        String amt = "301,342.34";
//        double amtDouble = 336865243333.00;
////        System.out.println((float)amtDouble);
//        
//        float amtInFloat = Float.parseFloat(amt.replaceAll(",", ""));
////        System.out.println(amtInFloat);
//////        Format format = NumberFormat.getNumberInstance(new Locale("en", "IN"));
//////
//////        String moneyString = format.format(amtInFloat);
//////        System.out.println(moneyString);
//////        System.out.println(amtInFloat);
////        double dou = amtInFloat;
////        System.out.println(dou);
//        String formatted = String.format("%.2f", amtDouble);
//        System.out.println(commaSeperator("3673840.344"));
//        Object obj =  3673840.344;
//        
//        double d = new Double(obj.toString());
//        System.out.println(commaSeperator(formatted));
        
//        System.out.println(round(amtInFloat, 2));
		

		
//		Calendar calender = Calendar.getInstance();
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//		
//		Date prevDate = sdf.parse("2025-04-07");
//		calender.setTime(prevDate);
//		calender.add(Calendar.DAY_OF_YEAR, 120);
//		System.out.println(sdf.format(calender.getTime()));
//		calender.setTime(new Date());
//		calender.add(Calendar.DAY_OF_MONTH, 4);  // starting debit two days after
//		
//        List<Date> validDates = getValidWorkdays(calender.getTime(), 180);
//
//        for (Date d : validDates) {
//            System.out.println(sdf.format(d));
//        }
//
//        System.out.println("Total Valid Workdays: " + validDates.size());
		
		
//        Date now = new Date();
//        SimpleDateFormat sdfmm = new SimpleDateFormat("mm");
//        int currMin = Integer.parseInt(sdfmm.format(now));
//        
//        SimpleDateFormat sdfhr = new SimpleDateFormat("HH");
//        int currHr = Integer.parseInt(sdfhr.format(now));
//        
//        if((currHr >= 18 && currMin > 30) || (currHr <= 10 && currMin < 30)) {
//        	System.out.println("NO");
//        }else {
//        	System.out.println("YES");
//        }
//        double avgDisbAmt = 37499.5687654;
//        int roundOff25kMul = (int) Math.round(avgDisbAmt/25000);
//		avgDisbAmt = (roundOff25kMul * 25000);
//		System.out.println(avgDisbAmt);
//        System.out.println(0/avgDisbAmt);
		
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm");
//		String dt =  "2025-05-15 17:00:32";
//		
//		System.out.println(sdf.parse(dt));
//		
//		System.out.println(System.currentTimeMillis());
//		
		
//		System.out.println(commaSeperator(String.format("%.2f", -343.23)));
//		String s = "GOPINATH KARPAGAM, no:1438,3rd main road mathur mmda ch-68 ,1438, 3rd Main Rd, Manali, Chennai, Mathur, Tamil Nadu 600068, India - Landmark : govt High school near, null,,,";
//		s = s.replaceAll("null\s*,?", "").replaceAll("\s*,\s*", "").trim();
//		System.out.println(s);
//		if(s.lastIndexOf(",") == s.length()-1) {
//			s = s.substring(0, s.length()-1);
//		}
//		System.out.println(s);
//		
//		String s = "   Shubham   Kumar  S";
//		System.out.println(s.trim().split("\\s+")[0]);
		
//		System.out.println(new Date());
		
//		SimpleDateFormat sdfhr = new SimpleDateFormat("yyyy-MM-dd");
//		Calendar cal = Calendar.getInstance();
//        cal.setTime(new Date());
//        
//        cal.add(Calendar.DAY_OF_MONTH, 1); // next day
//		cal.set(Calendar.HOUR_OF_DAY, 0);
//		cal.set(Calendar.MINUTE, 0);
//		cal.set(Calendar.SECOND, 0);
//		cal.set(Calendar.MILLISECOND, 0);
//        
//        Date d = sdfhr.parse(sdfhr.format(new Date()));
//		System.out.println(isbefore(cal.getTime(), "9:30"));
//		System.out.println(isbefore(d, "9:30"));
		
//		int avlSlotWinDays = 25;
//		Date currDate = new Date();	
		
        
//        
//		// Date after avlSlotWinDays days
//        calendar.add(Calendar.DAY_OF_MONTH, avlSlotWinDays);
//        Date dateAfter25Days = calendar.getTime();
//        
//        Calendar cal = Calendar.getInstance();
//        calendar.setTime(currDate);
//        
//		// Date after avlSlotWinDays days
//        calendar.add(Calendar.DAY_OF_MONTH, 5);
//        Date lastCreatedagentSlot = calendar.getTime();
//        
//        long diffInMillies = dateAfter25Days.getTime() - lastCreatedagentSlot.getTime();
//		// get days diff
//		int daysDiff = (int) TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
//		
//		System.out.println(daysDiff);
		
		
//		System.out.println(getDaysCountIncludingSunday(25));
//		System.out.println(base64Creds);
		
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//		String fromDate="2024-12-13T00:00:00.000";
//		String toDate="2025-06-13T00:00:00.000";
//		
//		Calendar fromCal = Calendar.getInstance();
//		fromCal.setTime(sdf.parse(fromDate));
//
//		Calendar toCal = Calendar.getInstance();
//		toCal.setTime(sdf.parse(toDate));
//		
//		Calendar c = Calendar.getInstance();
//		System.out.println(sdf.format(c.getTime()));
//		c.add(Calendar.DAY_OF_MONTH, -180);
//		
//		System.out.println(sdf.format(c.getTime()));
//		
//		int monthDiff = toCal.get(Calendar.MONTH) - fromCal.get(Calendar.MONTH) + 12 * (toCal.get(Calendar.YEAR) - fromCal.get(Calendar.YEAR));
//		
//		int daysDiff = (int)((toCal.getTimeInMillis() - fromCal.getTimeInMillis())/(1000*60*60*24));
//		
//		System.out.println("MonthDiff: "+ monthDiff);
//		System.out.println("DaysDiff: "+ daysDiff);
        
//        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
//        Calendar cal = Calendar.getInstance();
//        cal.add(Calendar.MINUTE, 30);
//        String futureTime = sdf.format(cal.getTime());
//        System.out.println(futureTime);
//		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
//		//Consent Requset for 5 Years
//		Calendar calendar = Calendar.getInstance();
//		calendar.setTime(new Date());
////		calendar.add(Calendar.YEAR, 4); // adding 5 years to the current Date
//		String expiryDate = format.format(calendar.getTime());
//		
//		System.out.println("expiryDate: " + expiryDate);
//		
//		//From Date 
//		Calendar cal = Calendar.getInstance();
//		cal.setTime(new Date());
//		cal.add(Calendar.DAY_OF_YEAR, -179); // redue 1 year
//		String startDate = format.format(cal.getTime());
//		
//		System.out.println("startDate: " + startDate);
		
//		byte[] imageArr = Files.readAllBytes(Paths.get("/home/shubham/Downloads/shyam.jpg"));
//		System.out.println(imageArr.length);
//		float f = 0.1f;
//		if(imageArr.length > 102400) {
//			f = 102400/imageArr.length;
////			f = ((int) (f * 100)) / 100.0f;
//		}
//		
//		System.out.println(f);
//		
//		byte[] compressedImageBytes = compressImage(imageArr, 0.3f);
//
//		try {
//			Files.write(Paths.get("/home/shubham/Downloads/shyam_c.jpg"), compressedImageBytes);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		String s0 = "Cbt";
//		String s1 = "Cat";
//        String s2 = "Cat";
//        s2= s2.replace("a", "b");
//        String s3 = new String("Cat");
//        
//        System.out.println("s0 == s2 :"+(s0==s2));
//        System.out.println("s1 == s2 :"+(s1==s2));
//        System.out.println("s1 == s3 :"+(s1==s3));
		
//		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
//		Date currTime = null;
//		Date startTime = null;
//		Date endTime = null;
//		try {
//			startTime = sdf.parse("12:00");
//			endTime = sdf.parse("16:00");
//			currTime = sdf.parse("11:50");
//			log.info("startTime | endTime | currTime: "+ startTime +"|"+ endTime +"|"+ currTime);
//		} catch (ParseException e) {
//			// TODO Auto-generated catch block
//			log.info("time parse Error");
//		}
//		
//		System.out.println(currTime.after(endTime));
//		System.out.println(currTime.before(startTime));
//		Integer a = 6;
//		Integer b = 1;
//		a.intValue();
//		System.out.println(a.intValue());
//		
//		String s1 = "S1";
//		String s3 = new String("S1");
//		String s4 = new String("S1");
//		System.out.println(s1==s3);
//		System.out.println(s4==s3);
//		System.out.println(s1==s4);
//		System.out.println(s1.equals(s3));

      int currHr = Integer.parseInt(new SimpleDateFormat("HH").format(new Date()));
      if((currHr < 7)) {
      	System.out.println("YES");
      }else {
      	System.out.println("NO");
      }
      
      System.out.println(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
		
//		Random random = new Random();
//		double nu = Math.floor(random.nextDouble()*100)/100;
//		System.out.println(nu);

      

	}
	
	public static byte[] compressImage(byte[] originalImageBytes, float compressionQuality) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(originalImageBytes);
        BufferedImage bufferedImage = ImageIO.read(bais);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg"); // or "png"
        if (!writers.hasNext()) throw new IllegalStateException("No writers found for the specified format.");

        ImageWriter writer = writers.next();
        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);

        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(compressionQuality); // 0.0f to 1.0f
        }

        writer.write(null, new IIOImage(bufferedImage, null, null), param);
        ios.close();
        writer.dispose();

        return baos.toByteArray();
    }
	
	public static int getDaysCountIncludingSunday(int slotWin) {
		Calendar cal = Calendar.getInstance();
		int count = 0;
		
		for (int i = 1; i <= slotWin; i++) {
			count++;
			if(cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY){
				i--;
			}
			cal.add(Calendar.DAY_OF_MONTH, 1);
		}
		return count;
	}

	
	public static boolean isbefore(Date inputDate, String timeString) {

        // Extract hour and minute from Date
        Calendar cal = Calendar.getInstance();
        cal.setTime(inputDate);
        cal.add(Calendar.MINUTE, 30);
        
        System.out.println(cal.getTime());
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);

        // Parse hour and minute from timeString
        String[] parts = timeString.split(":");
        int targetHour = Integer.parseInt(parts[0]);
        int targetMinute = Integer.parseInt(parts[1]);

        boolean isbefore;
        if (hour < targetHour) {
            isbefore = true;
        } else if (hour == targetHour) {
        	isbefore = minute < targetMinute;
        } else {
        	isbefore = false;
        }
        return isbefore;
	}
	 
    public static String commaSepetator(float val){
		return NumberFormat.getNumberInstance(Locale.UK).format(val);
	}
    
    public static String formatIndianNumber(String number) {
        String[] parts = number.split("\\.");
        String intPart = parts[0];
        String decimalPart = parts[1]; 
        
        if(decimalPart.length() == 1) { 
        	decimalPart += "0";
        }

        int len = intPart.length();
        int count =0;
        String s = "";
        
        for (int i = len-1; i >=0; i--) {
			s+=intPart.charAt(i);
			count++;
			if(count == 3) {
				s+=',';
			}else if(count > 3 && (count-3)%2 == 0 && i != 0) {
				s+=',';
			}
		}
        String amt = new StringBuilder(s).reverse().toString() + "." + decimalPart;
//        System.out.println(amt + "."+decimalPart);
		return amt ;
    }
    
    private static String format(String pattern, Object value,String pointValue) {
        String value1=   new DecimalFormat(pattern).format(value);
        return value1+"."+pointValue;
    }
    public static String commaSeperator(String amount) {
    	if(!amount.contains(".")) {
    		amount = amount + ".00";
    	}
        String[] aar=amount.split("\\.");
        String pointValue=aar[1];
        double value =Double.parseDouble(aar[0]);
        if(value < 1000) {
            return format("###", value,pointValue);
        } else {
            double hundreds = value % 1000;
            int other = (int) (value / 1000);
            return new DecimalFormat(",##").format(other)+ ',' + format("000", hundreds,pointValue);
        }
    }
    
    
    private static float round(float value, int places) {
		if (places < 0)
			throw new IllegalArgumentException();

		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.floatValue();
	}

    public static List<Date> getValidWorkdays(Date startDate, int tenureDays) throws ParseException {
        List<Date> validDates = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        Map<String, Set<Integer>> lastTwoWorkingDaysCache = new HashMap<>();

        for (int i = 0; i < tenureDays; i++) {
            Calendar current = (Calendar) calendar.clone();
            int dayOfWeek = current.get(Calendar.DAY_OF_WEEK);
            int dayOfMonth = current.get(Calendar.DAY_OF_MONTH);
            int year = current.get(Calendar.YEAR);
            int month = current.get(Calendar.MONTH); // 0-based

            String yearMonthKey = year + "-" + month;

            // Sunday Case
            if (dayOfWeek == Calendar.SUNDAY) {
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                continue;
            }
            
            // Last two working days
            if (!lastTwoWorkingDaysCache.containsKey(yearMonthKey)) {
                Set<Integer> lastTwoWorkingDays = new HashSet<>(getLastTwoWorkingDays(year, month));
                lastTwoWorkingDaysCache.put(yearMonthKey, lastTwoWorkingDays);
            }

            if (!lastTwoWorkingDaysCache.get(yearMonthKey).contains(dayOfMonth)) {
                validDates.add(current.getTime());
            }

            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        return validDates;
    }

    private static List<Integer> getLastTwoWorkingDays(int year, int month) {
        // Calendar uses 0-based months
        Calendar calendar = new GregorianCalendar(year, month, 1);
        int lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        calendar.set(Calendar.DAY_OF_MONTH, lastDay);

        List<Integer> workingDays = new ArrayList<>();
        while (workingDays.size() < 2) {
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY) {
                workingDays.add(calendar.get(Calendar.DAY_OF_MONTH));
            }
            calendar.add(Calendar.DAY_OF_MONTH, -1);
        }
        return workingDays;
    }


}


