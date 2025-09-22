package in.fl.vault.service;

import java.io.IOException;

public interface AwsS3Service {
	
	public String getFileFromS3(String filepath, String custId);
	
	public boolean saveFileToS3(String keyName, byte[] fileContents, String extension);
	
	public String saveFileToDisk(byte[] fileContents, String fileType, String customerId, String extension) throws IOException;
}
