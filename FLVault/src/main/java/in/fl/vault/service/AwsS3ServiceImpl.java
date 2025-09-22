package in.fl.vault.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

@Service
public class AwsS3ServiceImpl implements AwsS3Service{

	@Value("${s3.bucket.name}")
	private String s3BucketName;

	@Value("${s3.access.key.id}")
	private String s3AccessKeyID;

	@Value("${s3.secret.access.key}")
	private String s3SecretAccessKey;

	
	private final Logger log = Logger.getLogger(AwsS3ServiceImpl.class);
	
	public String getFileFromS3(String filepath, String custId) {
		Date now = new Date();		
		SimpleDateFormat sdfDate2 = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
		String strDate = sdfDate2.format(now);
		
		int customerId = Integer.parseInt(custId);
		String userFilePath = String.format("%03d", customerId % 1000) + "/" + customerId + "/";
		//String filePathNew = "/FRNDLN/ImageFiles_EBS/" + userFilePath;
		String filePathNew = "/FRNDLN/ImageFiles/" + userFilePath;
		Path pathTemp = Paths.get(filePathNew);
		if (!Files.exists(pathTemp)) {
			try {
				Files.createDirectories(pathTemp);
			} catch (IOException e) {}
		}
		
		String s3FilePath = filePathNew+custId+"_"+strDate+".pdf";

		Regions clientRegion = Regions.AP_SOUTH_1;
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(s3AccessKeyID, s3SecretAccessKey);
		final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(clientRegion)
				.withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
		try {
		    S3Object o = s3.getObject(s3BucketName, filepath);
		    S3ObjectInputStream s3is = o.getObjectContent();
		    File file = new File(s3FilePath);
		    s3FilePath = file.getAbsolutePath();
            file.setReadable(true, false);
            file.setExecutable(true, false);
            FileOutputStream fos = new FileOutputStream(file);
            
		    byte[] read_buf = new byte[1024];
		    int read_len = 0;
		    while ((read_len = s3is.read(read_buf)) > 0) {
		        fos.write(read_buf, 0, read_len);
		    }
		    s3is.close();
		    fos.close();
		    
		} catch (AmazonServiceException e) {
			log.error("getFileFromS3 AmazonServiceException :",e);
		} catch (FileNotFoundException e) {
			log.error("getFileFromS3 FileNotFoundException ",e);
		} catch (IOException e) {
			log.error("getFileFromS3 IOException ",e);
		}
		
		return s3FilePath;
	}
	
	public String getFileBase64FromS3(String fileName, String custId) {
		log.info("Entering getFileFromS3 :: Bucket NAME: " + s3BucketName + " | ID: " + s3AccessKeyID + " | KEY: "
				+ s3SecretAccessKey);
		
		int customerId = Integer.parseInt(custId);
		String userFilePath = "PARTNER/"+String.format("%03d", customerId % 1000) + "/" + customerId + "/" + fileName;
		
		String base64String = "";

		Regions clientRegion = Regions.AP_SOUTH_1;
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(s3AccessKeyID, s3SecretAccessKey);
		final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(clientRegion)
				.withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
		try (
		    S3Object o = s3.getObject(s3BucketName, userFilePath);
		    S3ObjectInputStream s3is = o.getObjectContent();
		    ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = s3is.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            byte[] fileBytes = outputStream.toByteArray();
            base64String = Base64.getEncoder().encodeToString(fileBytes);
		    
		} catch (IOException e) {
			log.error("getFileFromS3 IOException ",e);
		}
		
		return base64String;
	}

	public boolean saveFileToS3(String keyName, byte[] fileContents, String extension) {
		log.info("entering saveFileToS3 ::keyName:"+keyName+" extension:"+extension);
		long startTimeInMillis = System.currentTimeMillis();
		boolean success = false;
		String contentType = getContentType(extension);
		Regions clientRegion = Regions.AP_SOUTH_1;
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(s3AccessKeyID, s3SecretAccessKey);
		final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(clientRegion)
				.withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();

		InputStream stream = new ByteArrayInputStream(fileContents);
		ObjectMetadata meta = new ObjectMetadata();
		meta.setContentLength(fileContents.length);
		if(contentType != null) {
			meta.setContentType(contentType);
		}
		try {
			s3.putObject(s3BucketName, keyName, stream, meta );
			success = true;
			log.info("saved file : "+keyName+" to S3 bucket "+s3BucketName+"...");
		} catch (AmazonServiceException e) {
			log.error("error saving file on s3 :",e);
		}
		long timeTaken = System.currentTimeMillis() - startTimeInMillis;
		log.info("Exiting saveFileToS3 : Time Taken:"+timeTaken);
		return success;
	}
	
	public String saveFileToDisk(byte[] fileContents, String fileType, String customerId, String extension)
			throws IOException {
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date now = new Date();
		String strDate = sdfDate.format(now);
		String fileDetails = "";
		String s3FileName = "";
		if (fileContents != null && fileContents.length > 0) {

			strDate = sdfDate.format(now);
			int custId = Integer.parseInt(customerId);
			String userFilePath = String.format("%03d", custId % 1000) + "/" + custId + "/";
			String s3Folder = userFilePath;
			fileDetails = custId + "_" + fileType + "_" + strDate + "." + extension;

			s3FileName = s3Folder + fileDetails;
			saveFileToS3(s3FileName, fileContents, extension);

		}
		return s3FileName;
	}

	private String getContentType(String extension) {
		extension = extension.toLowerCase();
		String contentType = null;
		switch (extension) {
		case "json" :
			contentType = "application/json";
			 break;
		case "png" :
			contentType = "image/png";
			 break;
		case "jpg" :
			contentType = "image/jpeg";
			 break;
		case "jpeg" :
			contentType = "image/jpeg";
			 break;
		case "txt" :
			contentType = "text/plain";
			 break;
		case "zip" :
			contentType = "application/zip";
			 break;
		case "pdf" :
			contentType = "application/pdf";
			 break;
		case "mp4" :
			contentType = "video/mp4";
			 break;
		case "xml" :
			contentType = "application/xml";
			 break;
		}
		log.info("Exiting getContentType : extension:"+extension +" contentType:"+contentType);
		return contentType;
	}

}