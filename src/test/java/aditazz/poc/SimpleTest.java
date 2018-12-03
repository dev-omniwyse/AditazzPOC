package aditazz.poc;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Map;
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
import aditazz.poc.service.AditazzService;
import aditazz.poc.service.AuthenticationService;
import aditazz.poc.util.JsonReader;
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
public class SimpleTest {
	private static final Logger logger = LoggerFactory.getLogger(SimpleTest.class);
	
	String authenticationToken;
	
	static String authToken;
	static String optionId;
	static String planId;
	static AditazzService aditazzService;
	static String pfdId;
	static Validator validator;
	static String projectId;
	
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
		optionId="00000000-5bf3-9ef8-797f-070010c84b2b";
		planId="00000000-5bf3-9ef8-797f-070010c84b2a";
		pfdId="00000000-5bf3-9ef8-797f-070010c84b28";
		aditazzService=new AditazzService();
		validator=new Validator();
		logger.info("Project id ::{} Option id ::{}",projectId,optionId);
	}
	
	/**
	 * 
	 * @name : test2_IsLinesEqual
	 * @description : The Method "test2_IsLinesEqual" is used for validating lines of pfd and plan are equal or not.
	 * @date : 27-Nov-2018 3:16:39 PM
	 * @return : void
	 *
	 */
	@Test
    public void validateExistingPfdAndPlan() {
		
		logger.info("Getting existing pfd for the id :: {}",pfdId);
		JsonObject pfdObject=aditazzService.getPfdObject(authToken, pfdId);
		logger.info("Getting existing plan for the id :: {}",planId);
		JsonObject planObject=aditazzService.getPlan(authToken, planId);
		logger.info("Validating existing plan and pfd");
		Map<String,Boolean> result=validator.validatePlanAndPfd(pfdObject, planObject);
		assertEquals("Equipments are equal.",true,result.get(AditazzConstants.EQUIPMENT_EQUAL).booleanValue() );
    	assertEquals("Lines are equal.",true,result.get(AditazzConstants.LINES_EQUAL).booleanValue() );
    	System.out.println("Completed existing pfd and plan validation..........");
    }
	
	@Test
	public void validateNewPfdAndPlan() {
		JsonReader jsonReader=new JsonReader();
		logger.info("Reading new pfd json from file ");
		JsonObject newPfdObject=jsonReader.getPfdObject("/"+optionId+"_newpfd.json");
		logger.info("Updating new pfd json :: {}",pfdId);
		assertEquals(true, aditazzService.updatePFD(authToken, newPfdObject, pfdId) != null);
		logger.info("Generating plan for id :: {}",planId);
		assertEquals(AditazzConstants.COMPLETED_STATUS, aditazzService.generatePlan(UrlConstants.PLAN_PUT_URL+"&project_id="+projectId, authToken, optionId) );
		logger.info("Getting plan for id :: {}",planId);
		JsonObject planObject=aditazzService.getPlan(authToken, planId);
		logger.info("Validating plan and pfd");
		Map<String,Boolean> result=validator.validatePlanAndPfd(newPfdObject, planObject);
		assertEquals("Equipments are equal.",true,result.get(AditazzConstants.EQUIPMENT_EQUAL).booleanValue() );
		assertEquals("Lines are equal.",true,result.get(AditazzConstants.LINES_EQUAL).booleanValue() );
		System.out.println("Completed new pfd and plan validation..........");
	}
	
	
	
}
