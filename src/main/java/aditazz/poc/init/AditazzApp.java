package aditazz.poc.init;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aditazz.poc.dto.Aditazz;
import aditazz.poc.service.AditazzService;
import aditazz.poc.service.AuthenticationService;

/**
 * 
 * @author      : Sreekhar Reddy.K
 * @version     : Java 1.8 
 * @createdOn   : 26-Nov-2018 11:07:50 AM
 * @description : The class AditazzApp.java used to initialize the process.
 */
public class AditazzApp {
	private static final Logger logger = LoggerFactory.getLogger(AditazzApp.class);
	
	
	public static void main(String[] args) throws IOException, InterruptedException {
		Properties appProps=new Properties();
		appProps.load(AditazzApp.class.getClassLoader().getResourceAsStream("application.properties"));
		AditazzService aditazzService=new AditazzService();
		AuthenticationService authenticationService=new AuthenticationService();
		String authToken=authenticationService.getAuthenticationToken(appProps.getProperty("username"), appProps.getProperty("password"));
		String projectId=appProps.getProperty("projectid");
		Aditazz aditazz=new Aditazz();
		aditazz.setAuthToken(authToken);
		aditazz.setProjectId(projectId);
		aditazz=aditazzService.getLibraryIds(aditazz);
		Properties properties=new Properties();
		
		properties.load(AditazzApp.class.getClassLoader().getResourceAsStream("option_plan.properties"));
		for(Entry<Object, Object> entry : properties.entrySet()) {
			aditazz.setOptionId(entry.getKey().toString());
			logger.info("Process started with Option id :: {}\t ",entry.getKey());
			aditazzService.processRandomGraph(aditazz);
			logger.info("Process ended with Option id :: {}\t ",entry.getKey());
			Thread.sleep(5000);
		}
		logger.info("Completed Successfully.......................!");
		
	}
}
