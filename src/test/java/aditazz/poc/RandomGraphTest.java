package aditazz.poc;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import aditazz.poc.constants.AditazzConstants;
import aditazz.poc.constants.UrlConstants;
import aditazz.poc.enums.JsonFields;
import aditazz.poc.service.AditazzService;
import aditazz.poc.service.AuthenticationService;
import aditazz.poc.service.RandomGraphGenerator;
import aditazz.poc.validator.Validator;
import junit.framework.TestCase;

/**
 * 
 * @author      : Sreekhar Reddy.K
 * @version     : Java 1.8 
 * @createdOn   : 26-Nov-2018 11:36:23 AM
 * @description : The class AppTest.java used for test pfd and plan.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RandomGraphTest {
	private static final Logger logger = LoggerFactory.getLogger(RandomGraphTest.class);
	
	String authenticationToken;
	
	static String authToken;
	static String optionId;
	static String planId;
	static AditazzService aditazzService;
	static String pfdId;
	static Validator validator;
	static String projectId;
	static Properties optionIds=new Properties();
	static RandomGraphGenerator randomGraphGenerator;
	/**
	 * 
	 * @name : initialize
	 * @description : The Method "initialize" is used for initialize objects.
	 * @date : 26-Nov-2018 12:05:57 PM
	 * @throws IOException
	 * @return : void
	 *
	 */
	@BeforeClass
    public static void initialize() throws IOException {
		Properties appProps=new Properties();
		appProps.load(TestCase.class.getClassLoader().getResourceAsStream("application.properties"));
		AuthenticationService authenticationService=new AuthenticationService();
		authToken=authenticationService.getAuthenticationToken(appProps.getProperty("username"), appProps.getProperty("password"));
		projectId=appProps.getProperty("projectid");
		optionIds.load(TestCase.class.getClassLoader().getResourceAsStream("option_plan.properties"));
		aditazzService=new AditazzService();
		validator=new Validator();
		randomGraphGenerator=new RandomGraphGenerator();
		logger.info("Project id ::{} ",projectId);
	}
	
	
	
	@Test
	public void validateRandomGraph() {
		for(Entry<Object, Object> entry : optionIds.entrySet()) {
			logger.info("Process started with Option id :: {}\t ",entry.getKey());
			optionId=entry.getKey().toString();
			JsonObject optionJson=aditazzService.getPlanAndOptionId(authToken,optionId);
			pfdId=optionJson.get(JsonFields.PFD_ID.getValue()).getAsString();
			planId=optionJson.get(JsonFields.PLAN_ID.getValue()).getAsString();
			JsonObject pfdObject=aditazzService.getPfdObject(authToken, pfdId);
			logger.info("Before generating random graph pfd json is ::{}",pfdObject);
			logger.info("Generating random graph.........!");
			JsonObject payloadObj=randomGraphGenerator.generateRandomGraph(3,2);
			pfdObject.add(JsonFields.PAYLOAD.getValue(), payloadObj);
			logger.info("After generating random graph pfd json is :: {}",payloadObj);
			logger.info("Updating random graph pfd json :: {}",pfdId);
			assertEquals(true, aditazzService.updatePFD(authToken, pfdObject, pfdId) != null);
			logger.info("Generating plan for id :: {}",planId);
			assertEquals(AditazzConstants.COMPLETED_STATUS, aditazzService.generatePlan(UrlConstants.PLAN_PUT_URL+"&project_id="+projectId, authToken, optionId) );
			logger.info("Getting plan for id :: {}",planId);
			JsonObject planObject=aditazzService.getPlan(authToken, planId);
			logger.info("Validating plan and pfd");
			Map<String,Boolean> result=validator.validatePlanAndPfd(pfdObject, planObject);
			assertEquals("Equipments are equal.",true,result.get(AditazzConstants.EQUIPMENT_EQUAL).booleanValue() );
			assertEquals("Lines are equal.",true,result.get(AditazzConstants.LINES_EQUAL).booleanValue() );
			System.out.println("Completed new pfd and plan validation..........");
			logger.info("Process ended with Option id :: {}\t ",entry.getKey());
			
		}
	}
	
	
	
}
