package aditazz.poc.service;

import java.io.IOException;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import aditazz.poc.constants.AditazzConstants;
import aditazz.poc.constants.UrlConstants;
import aditazz.poc.dto.Aditazz;
import aditazz.poc.enums.JsonFields;
import aditazz.poc.util.JsonReader;
import aditazz.poc.util.RestUtil;
import aditazz.poc.validator.Validator;

/**
 * 
 * @author      : Sreekhar Reddy.K
 * @version     : Java 1.8 
 * @createdOn   : 26-Nov-2018 11:01:04 AM
 * @description : The class AditazzService.java used for process pfd and plan
 */
public class AditazzService {
	private static final Logger logger = LoggerFactory.getLogger(AditazzService.class);
	
	/**
	 * 
	 * @name : processPFD
	 * @description : The Method "processPFD" is used for update pfd and validate pfd.
	 * @date : 27-Nov-2018 4:54:49 PM
	 * @param aditazz
	 * @return : void
	 *
	 */
	public void processPFD(Aditazz aditazz) {
		try {
			EquipmentService equipmentService=new EquipmentService();
			JsonReader jsonReader=new JsonReader();
			logger.info("Authentication token : {}" , aditazz.getAuthToken());
			aditazz=getPlanAndOptionId(aditazz);
			
			logger.info("Pfd id :: {} \t Plan id :: {}",aditazz.getPfdId(),aditazz.getPlanId());
			JsonObject equimentLib=equipmentService.getEquipments(aditazz);
			JsonObject pfdObject=getPfdObject(aditazz);
			processPFDAndPlan(aditazz, pfdObject,equimentLib);
		
			logger.info("Validation Completed without updating pfd.............................!");
			JsonObject newPfdObject=jsonReader.getPfdObject("/"+aditazz.getOptionId()+"_newpfd.json");
			logger.info("Updating pfd :::: {}",newPfdObject);
			JsonObject jsonRes=updatePFD( newPfdObject,aditazz);
			logger.info("Update pfd response is :: {}",jsonRes);
			processPFDAndPlan(aditazz, newPfdObject,equimentLib);

		}  catch (Exception e) {
			logger.error("Exception occured due to :: "+e.getMessage(),e);
		}
	}
	/**
	 * 
	 * @name : processPFDAndPlan
	 * @description : The Method "processPFDAndPlan" is used for validate plan and pfd.
	 * @date : 27-Nov-2018 4:55:21 PM
	 * @param aditazz
	 * @param pfdObject
	 * @param equipmentLib
	 * @return : void
	 *
	 */
	public void processPFDAndPlan(Aditazz aditazz,JsonObject pfdObject,JsonObject equipmentLib) {
		String status = generatePlan(UrlConstants.PLAN_PUT_URL+"&project_id="+aditazz.getProjectId(), aditazz.getAuthToken(),aditazz.getOptionId());
		Validator validator=new Validator();
		if (AditazzConstants.COMPLETED_STATUS.equalsIgnoreCase(status)){
			JsonObject planObject = getPlan(aditazz);
			logger.info("Plan Object : {} " , planObject);
			validator.validatePlanAndPfd(pfdObject, planObject,equipmentLib);
		}
	}
	
	/**
	 * 
	 * @name : getPlan
	 * @description : The Method "getPlan" is used for getting plan based on plan id. 
	 * @date : 26-Nov-2018 11:02:23 AM
	 * @param aditazz
	 * @return
	 * @return : JsonObject
	 *
	 */
	public JsonObject getPlan(Aditazz aditazz) {
		return RestUtil.getObject(aditazz.getAuthToken(), null, UrlConstants.PLAN_URL+aditazz.getPlanId());
	}
	
	/**
	 * 
	 * @name : generatePlan
	 * @description : The Method "generatePlan" is used for generating plan for specific option. 
	 * @date : 26-Nov-2018 11:02:44 AM
	 * @param url
	 * @param authToken
	 * @param optionId
	 * @return
	 * @return : String
	 *
	 */
	public String generatePlan(String url, String authToken,String optionId) {
		JsonObject emptyEquipment=getEmptyEquipment();
		JsonObject output = RestUtil.putObject(authToken,emptyEquipment,url+"&option_id="+optionId);
		String ticketId=output.get(JsonFields.ID.getValue()).getAsString();
		logger.info("Ticket Id : {} " , ticketId);
		String status = null;
		while (true) {
			status = getStatusByTicketId(ticketId, authToken);
			if (AditazzConstants.COMPLETED_STATUS.equalsIgnoreCase(status)) {
				break;
			}
			logger.info("Ticket id ::{} and Status :: {} " ,ticketId,status);
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				logger.error("Thread exception occured due to "+e.getMessage(),e);
			}
		}
		logger.info("Ticket Status : {} " , status);
		return status;
	}
	/**
	 * 
	 * @name : getTicketObeject
	 * @description : The Method "getTicketObeject" is used for getting status based on ticket id. 
	 * @date : 26-Nov-2018 11:03:25 AM
	 * @param ticketId
	 * @param authToken
	 * @return
	 * @return : JsonObject
	 *
	 */
	private String getStatusByTicketId(String ticketId, String authToken){
		JsonObject jsonObject=RestUtil.getObject(authToken, null, UrlConstants.TICKET_URL+ticketId);
		return jsonObject.get(JsonFields.STATUS.getValue()).getAsString();
	}

	/**
	 * 
	 * @name : getEmptyEquipment
	 * @description : The Method "getEmptyEquipment" is used to creating empty equipment. 
	 * @date : 26-Nov-2018 11:03:50 AM
	 * @return
	 * @return : JsonObject
	 *
	 */
	private JsonObject getEmptyEquipment() {
		JsonObject jsonObject=new JsonObject();
		jsonObject.add(JsonFields.EQUIPMENT.getValue(), new JsonObject());
		return jsonObject;
	}
	/**
	 * 
	 * @name : updatePFD
	 * @description : The Method "updatePFD" is used for update pfd based on pfdId 
	 * @date : 27-Nov-2018 12:38:59 PM
	 * @param aditazz
	 * @param pfdObject
	 * @param pfdId
	 * @return
	 * @return : JsonObject
	 *
	 */
	public JsonObject updatePFD(JsonObject pfdObject,Aditazz aditazz) {
		return RestUtil.putObject(aditazz.getAuthToken(), pfdObject, UrlConstants.PFD_URL+aditazz.getPfdId());
	}
	/**
	 * 
	 * @name : getPlanAndOptionId
	 * @description : The Method "getPlanAndOptionId" is used for getting plan id and pfd id. 
	 * @date : 27-Nov-2018 12:54:50 PM
	 * @param aditazz
	 * @return
	 * @return : JsonObject
	 *
	 */
	public Aditazz getPlanAndOptionId(Aditazz aditazz) {
		JsonObject responseObject=RestUtil.getObject(aditazz.getAuthToken(), null, UrlConstants.OPTIONS_URL+aditazz.getOptionId());
		JsonObject optionObject=responseObject.get(JsonFields.OPTIONS.getValue()).getAsJsonArray().get(0).getAsJsonObject();
		JsonObject payloadObj=optionObject.get(JsonFields.PAYLOAD.getValue()).getAsJsonObject();
		JsonObject pfdObj=payloadObj.get(JsonFields.PFD.getValue()).getAsJsonObject();
		aditazz.setPfdId(pfdObj.get(JsonFields.ID.getValue()).getAsString());
		JsonObject planObj=payloadObj.get(JsonFields.PLAN.getValue()).getAsJsonObject();
		aditazz.setPlanId(planObj.get(JsonFields.ID.getValue()).getAsString());
		return aditazz;
	}
	/**
	 * 
	 * @name : processRandomGraph
	 * @description : The Method "processRandomGraph" is used for generate random raph and update the pfd
	 * @date : 03-Dec-2018 3:07:22 PM
	 * @param aditazz
	 * @return : void
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 *
	 */
	public void processRandomGraph(Aditazz aditazz) throws  IOException {
		EquipmentService equipmentService=new EquipmentService();
		JsonReader jsonReader=new JsonReader();
		RandomGraphGenerator randomGraphGenerator=new RandomGraphGenerator();
		logger.info("Authentication token : {}" , aditazz.getAuthToken());
		aditazz=getPlanAndOptionId(aditazz);
		logger.info("Pfd id :: {} \t Plan id :: {}",aditazz.getPfdId(),aditazz.getPlanId());
		JsonObject equipmentLib=equipmentService.getEquipments(aditazz);
		
		logger.info("Before updating spacing the json is :: {}",equipmentLib);
		JsonObject payloadObj=randomGraphGenerator.generateRandomGraph(aditazz,3,2);
		
		JsonObject newEquipmentLib=equipmentService.getEquipments(aditazz);
		logger.info("After updating spacing the json is :: {}",newEquipmentLib);
		int revision=equipmentService.getRevisonNumber(aditazz);
		logger.info("Updating equipment revison in projects :: {}",revision);
		updateProjectEquipLibRevison(revision, aditazz);
		
		JsonObject pfdObject=getPfdObject(aditazz);
		pfdObject.add(JsonFields.PAYLOAD.getValue(), payloadObj);
		logger.info("After generating random graph pfd json is :: {}",pfdObject);
		int revison=jsonReader.getPfdRevision(pfdObject)+1;
		logger.info("Pfd revison number :: {}",revison);
		JsonObject jsonRes=updatePFD( pfdObject, aditazz);
		logger.info("Update pfd response is :: {}",jsonRes);
		updateOptionRevison(aditazz, revison);
		
		
		processPFDAndPlan(aditazz, pfdObject,newEquipmentLib);
		
		/*logger.info("Reverting spacing changes in equipment library ");
		equipmentService.updateEquipmentLibrary(aditazz, equipmentLib);
		revision=equipmentService.getRevisonNumber(aditazz);
		logger.info("Updating equipment revison in projects :: {}",revision);
		updateProjectEquipLibRevison(revision, aditazz);*/
		
	}
	
	
	
	/**
	 * 
	 * @name : getPfdObject
	 * @description : The Method "getPfdObject" is used for getting pfd object
	 * @date : 27-Nov-2018 1:52:03 PM
	 * @param aditazz
	 * @return
	 * @return : JsonObject
	 *
	 */
	public JsonObject getPfdObject(Aditazz aditazz) {
		JsonObject jsonObject=RestUtil.getObject(aditazz.getAuthToken(), null, UrlConstants.PFD_URL+aditazz.getPfdId());
		return jsonObject.get(JsonFields.PFDS.getValue()).getAsJsonArray().get(0).getAsJsonObject();
	}
	/**
	 * 
	 * @name : getLibraryIds
	 * @description : The Method "getLibraryIds" is used for getting library ids
	 * @date : 04-Dec-2018 2:30:41 PM
	 * @param aditazz
	 * @return
	 * @return : Aditazz
	 *
	 */
	public Aditazz getLibraryIds(Aditazz aditazz) {
		JsonObject jsonObject=RestUtil.getObject(aditazz.getAuthToken(), null, UrlConstants.PROJECT_URL+aditazz.getProjectId());
		JsonObject projectJson=jsonObject.get(JsonFields.PROJECTS.getValue()).getAsJsonArray().get(0).getAsJsonObject().get(JsonFields.PAYLOAD.getValue()).getAsJsonObject();
		JsonObject equipmentJson=projectJson.get(JsonFields.EQUIPMENT_LIBRARY.getValue()).getAsJsonObject();
		aditazz.setEquipLibId(equipmentJson.get(JsonFields.ID.getValue()).getAsString());
		JsonObject pipeLibJson=projectJson.get(JsonFields.PIPE_LIBRARY.getValue()).getAsJsonObject();
		aditazz.setPipeLibId(pipeLibJson.get(JsonFields.ID.getValue()).getAsString());
		return aditazz;
	}
	
	/**
	 * 
	 * @name : updateOptionRevison
	 * @description : The Method "updateOptionRevison" is used for updating option revision number. 
	 * @date : 06-Dec-2018 10:00:13 AM
	 * @param aditazz
	 * @param number
	 * @return : void
	 *
	 */
	public JsonObject updateOptionRevison(Aditazz aditazz,int number) {
		JsonObject jsonObject=RestUtil.getObject(aditazz.getAuthToken(), null, UrlConstants.OPTIONS_URL+aditazz.getOptionId());
		JsonObject optionJson=jsonObject.get(JsonFields.OPTIONS.getValue()).getAsJsonArray().get(0).getAsJsonObject();
		JsonObject payloadObj=optionJson.get(JsonFields.PAYLOAD.getValue()).getAsJsonObject();
		JsonObject pfdJson=payloadObj.get(JsonFields.PFD.getValue()).getAsJsonObject();
		JsonObject revison=pfdJson.get(JsonFields.REVISON.getValue()).getAsJsonObject();
		revison.add(JsonFields.ID.getValue(), new Gson().toJsonTree(number));
		pfdJson.add(JsonFields.REVISON.getValue(), revison);
		payloadObj.add(JsonFields.PFD.getValue(), pfdJson);
		optionJson.add(JsonFields.PAYLOAD.getValue(), payloadObj);
		//logger.info("Updated option json :: {}",optionJson);
		JsonObject response=RestUtil.putObject(aditazz.getAuthToken(), optionJson, UrlConstants.OPTIONS_URL+aditazz.getOptionId());
		//logger.info("After updating option the response is :: {}",response);
		return response;
	}
	
	/**
	 * 
	 * @name : updateProjectEquipLibRevison
	 * @description : The Method "updateProjectEquipLibRevison" is used for updating equipment library revision number in projects.
	 * @date : 06-Dec-2018 4:10:09 PM
	 * @param number
	 * @param aditazz
	 * @return
	 * @return : boolean
	 *
	 */
	public boolean updateProjectEquipLibRevison(int number,Aditazz aditazz) {
		JsonObject jsonObject=RestUtil.getObject(aditazz.getAuthToken(), null, UrlConstants.PROJECT_URL+aditazz.getProjectId());
		JsonObject projectJson=jsonObject.get(JsonFields.PROJECTS.getValue()).getAsJsonArray().get(0).getAsJsonObject();
		JsonObject payloadObj=projectJson.get(JsonFields.PAYLOAD.getValue()).getAsJsonObject();
		JsonObject equipmentJson=payloadObj.get(JsonFields.EQUIPMENT_LIBRARY.getValue()).getAsJsonObject();
		JsonObject revison=equipmentJson.get(JsonFields.REVISON.getValue()).getAsJsonObject();
		revison.add(JsonFields.ID.getValue(), new Gson().toJsonTree(number));
		JsonObject response=RestUtil.putObject(aditazz.getAuthToken(),projectJson, UrlConstants.PROJECT_URL+aditazz.getProjectId());
		//logger.info("After updating project json the response json is :: {}",response)
		return response != null;
		
	}
	
	
}
