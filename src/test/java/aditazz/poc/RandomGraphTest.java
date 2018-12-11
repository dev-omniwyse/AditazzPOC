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
import aditazz.poc.dto.Aditazz;
import aditazz.poc.enums.JsonFields;
import aditazz.poc.service.AditazzService;
import aditazz.poc.service.AuthenticationService;
import aditazz.poc.service.EquipmentService;
import aditazz.poc.service.RandomGraphGenerator;
import aditazz.poc.util.FileUtil;
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
public class RandomGraphTest {
	private static final Logger logger = LoggerFactory.getLogger(RandomGraphTest.class);
	
	static AditazzService aditazzService;
	static Validator validator;
	static Properties optionIds=new Properties();
	static RandomGraphGenerator randomGraphGenerator;
	static Aditazz aditazz;
	static EquipmentService equipmentService;
	static JsonObject equipmentLib;
	static FileUtil fileUtil;
	static String path;
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
		aditazz=new Aditazz();
		Properties appProps=new Properties();
		appProps.load(TestCase.class.getClassLoader().getResourceAsStream("application.properties"));
		AuthenticationService authenticationService=new AuthenticationService();
		aditazz.setAuthToken(authenticationService.getAuthenticationToken(appProps.getProperty("username"), appProps.getProperty("password")));
		aditazz.setProjectId(appProps.getProperty("projectid"));
		optionIds.load(TestCase.class.getClassLoader().getResourceAsStream("option_plan.properties"));
		aditazzService=new AditazzService();
		validator=new Validator();
		randomGraphGenerator=new RandomGraphGenerator();
		equipmentService=new EquipmentService();
		fileUtil=new FileUtil();
		path=appProps.getProperty("filepath");
		logger.info("Project id ::{} ",aditazz.getProjectId());
	}
	
	
	/**
	 * 
	 * @name : validateRandomGraph
	 * @description : The Method "validateRandomGraph" is used for validating pfd and plan payload. 
	 * @date : 10-Dec-2018 10:58:20 AM
	 * @throws IOException
	 * @return : void
	 *
	 */
	@Test
	public void validateRandomGraph() throws IOException {
		for(Entry<Object, Object> entry : optionIds.entrySet()) {
			logger.info("Process started with Option id :: {}\t ",entry.getKey());
			aditazz.setOptionId(entry.getKey().toString());
			aditazz=aditazzService.getLibraryIds(aditazz);
			aditazz=aditazzService.getPlanAndOptionId(aditazz);
			JsonObject pfdObject=aditazzService.getPfdObject(aditazz);
			//logger.info("Before generating random graph pfd json is ::{}",pfdObject);
			logger.info("Generating random graph.........!");
			equipmentLib=equipmentService.getEquipments(aditazz);
			//logger.info("Before updating equipment library json is :: {}",equipmentLib);
			JsonObject payloadObj=randomGraphGenerator.generateRandomGraph(aditazz, 3, 2);
			JsonObject updatedLib=equipmentService.getEquipments(aditazz);
			//logger.info("After updating equipment library json is :: {}",updatedLib);
			fileUtil.createFile(path, updatedLib.toString(), "updated_equipment_library");
			int revision=equipmentService.getRevisonNumber(aditazz);
			logger.info("Updating equipment revison in projects :: {}",revision);
			assertEquals(true, aditazzService.updateProjectEquipLibRevison(revision, aditazz));
			
			
			pfdObject.add(JsonFields.PAYLOAD.getValue(), payloadObj);
			//logger.info("After generating random graph pfd json is :: {}",payloadObj);
			
			
			logger.info("Updating random graph pfd json :: {}",aditazz.getPfdId());
			assertEquals(true, aditazzService.updatePFD(pfdObject, aditazz) != null);
			fileUtil.createFile(path, pfdObject.toString(), "updated_pfd");
			
			int revison=new JsonReader().getPfdRevision(pfdObject)+1;
			logger.info("Updating option with latest revision of pfd :: {}",revison);
			assertEquals(true,aditazzService.updateOptionRevison(aditazz, revison) != null);
			
			logger.info("Generating plan for id :: {}",aditazz.getPlanId());
			assertEquals(AditazzConstants.COMPLETED_STATUS, aditazzService.generatePlan(UrlConstants.PLAN_PUT_URL+"&project_id="+aditazz.getProjectId(), aditazz.getAuthToken(),aditazz.getOptionId()) );
			logger.info("Getting plan for id :: {}",aditazz.getPlanId());
			JsonObject planObject=aditazzService.getPlan(aditazz);
			fileUtil.createFile(path, planObject.toString(), "new_plan");
			logger.info("Validating plan and pfd");
			Map<String,Boolean> result=validator.validatePlanAndPfd(pfdObject, planObject, updatedLib);
			assertEquals("Equipments are equal.",true,result.get(AditazzConstants.EQUIPMENT_EQUAL).booleanValue() );
			assertEquals("Lines are equal.",true,result.get(AditazzConstants.LINES_EQUAL).booleanValue() );
			assertEquals("Valid space exists.",true,result.get(AditazzConstants.VALID_DISTANCE).booleanValue() );
			
			System.out.println("Completed pfd and plan validation..........");
			logger.info("Process ended with Option id :: {}\t ",entry.getKey());
			
		}
		
		
	}
	/*@AfterClass
	public static void revertChanges() { 
		logger.info("Reverting spacing changes in equipment library {}",equipmentLib);
		equipmentService.updateEquipmentLibrary(aditazz, equipmentLib);
	}*/
	
	
	
}
