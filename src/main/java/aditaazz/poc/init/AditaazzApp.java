package aditaazz.poc.init;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aditaazz.poc.service.AditaazzService;
import aditaazz.poc.service.AuthenticationService;
import aditaazz.poc.util.JsonReader;

/**
 * 
 * @author      : Sreekhar Reddy.K
 * @version     : Java 1.8 
 * @createdOn   : 26-Nov-2018 11:07:50 AM
 * @description : The class AditaazzApp.java used to initialize the process.
 */
public class AditaazzApp {
	private static final Logger logger = LoggerFactory.getLogger(JsonReader.class);
	
	
	public static void main(String[] args) throws IOException, InterruptedException {
		Properties appProps=new Properties();
		appProps.load(AditaazzApp.class.getClassLoader().getResourceAsStream("application.properties"));
		AditaazzService aditaazzService=new AditaazzService();
		AuthenticationService authenticationService=new AuthenticationService();
		String authToken=authenticationService.getAuthenticationToken(appProps.getProperty("username"), appProps.getProperty("password"));
		String projectId=appProps.getProperty("projectid");
		Properties properties=new Properties();
		properties.load(AditaazzApp.class.getClassLoader().getResourceAsStream("option_plan.properties"));
		for(Entry<Object, Object> entry : properties.entrySet()) {
			logger.info("Process started with Option id :: {}\t Plan id :: {} ",entry.getKey(),entry.getValue());
			aditaazzService.processPFD(projectId,entry.getKey().toString(),entry.getValue().toString(),authToken);
			logger.info("Process ended with Option id :: {}\t Plan id :: {} ",entry.getKey(),entry.getValue());
			Thread.sleep(5000);
		}
		logger.info("Completed Successfully.......................!");
		
	}
}
