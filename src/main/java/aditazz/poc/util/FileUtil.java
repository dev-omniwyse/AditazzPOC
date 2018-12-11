package aditazz.poc.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aditazz.poc.validator.Validator;

public class FileUtil {
	private static final Logger logger = LoggerFactory.getLogger(Validator.class);
	
	public void createFile(String path,String json,String fileName) throws IOException {
		Date date = new Date();  
	    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy_hh:mm:ss");  
	    String strDate= formatter.format(date);  
		 try (BufferedWriter writer = new BufferedWriter(new FileWriter(new StringBuilder().append(path).append(File.separator).append(fileName).append("_").append(strDate+".txt").toString()))){
			writer.write(json);
		    
		}catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
	}
	public static void main(String[] args) {  
	    Date date = new Date();  
	    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy_hh:mm:ss");  
	    String strDate= formatter.format(date);  
	    System.out.println(strDate);  
	}  
}
