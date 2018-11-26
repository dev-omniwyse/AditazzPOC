package aditaazz.poc;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.JsonObject;

import aditaazz.poc.constants.AditaazzConstants;
import aditaazz.poc.service.AditaazzService;
import aditaazz.poc.service.AuthenticationService;
import aditaazz.poc.service.JsonReader;
import aditaazz.poc.service.Validator;
import junit.framework.TestCase;

/**
 * 
 * @author      : Sreekhar Reddy.K
 * @version     : Java 1.8 
 * @createdOn   : 26-Nov-2018 11:36:23 AM
 * @description : The class AppTest.java used for test pfd and plan.
 */
public class AppTest {
	static JsonObject pfdObject;
	static JsonObject planObject;
	String authenticationToken;
	static Map<String,Boolean> result=null;
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
		AditaazzService aditaazzService=new AditaazzService();
		AuthenticationService authenticationService=new AuthenticationService();
		String authToken=authenticationService.getAuthenticationToken(appProps.getProperty("username"), appProps.getProperty("password"));
		String optionId="00000000-5bf3-9ef8-797f-070010c84b2b";
		String planId="00000000-5bf3-9ef8-797f-070010c84b2a";
		JsonReader jsonReader=new JsonReader();
		pfdObject=jsonReader.getPfdObject("/"+optionId+"_pfd.json");
		planObject=aditaazzService.getPlan(authToken, planId);
		Validator validator=new Validator();
    	result=validator.validatePlanAndPfd(pfdObject, planObject);
    }
	/**
	 * 
	 * @name : testPdfAndPlan
	 * @description : The Method "testPdfAndPlan" is used for test objects 
	 * @date : 26-Nov-2018 12:06:24 PM
	 * @return : void
	 *
	 */
	@Test
    public void isLinesEqual() {
    	assertEquals("Lines are equal.",true,result.get(AditaazzConstants.LINES_EQUAL).booleanValue() );
    }
	
	@Test
	public void isEquipmentsEqual() {
		assertEquals("Equipments are equal.",true,result.get(AditaazzConstants.EQUIPMENT_EQUAL).booleanValue() );
	}
}
