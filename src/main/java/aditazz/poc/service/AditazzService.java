package aditazz.poc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import aditazz.poc.constants.AditazzConstants;
import aditazz.poc.constants.UrlConstants;
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
	 * @name : process
	 * @description : The Method "process" is used for process the pfd and plan
	 * @date : 27-Nov-2018 1:51:12 PM
	 * @param projectId
	 * @param optionId
	 * @param authToken
	 * @return : void
	 *
	 */
	public void process(String projectId,String optionId,String authToken) {
		try {
			JsonReader jsonReader=new JsonReader();
			logger.info("Authentication token : {}" , authToken);
			JsonObject jsonObject=getPlanAndOptionId(authToken, optionId);
			String pfdId=jsonObject.get(JsonFields.PFD_ID.getValue()).getAsString();
			String planId=jsonObject.get(JsonFields.PLAN_ID.getValue()).getAsString();
			logger.info("Pfd id :: {} \t Plan id :: {}",pfdId,planId);
			JsonObject pfdObject=getPfdObject(authToken, pfdId);
			processPFD(authToken, planId, optionId, projectId, pfdObject,pfdId);
			
			
			JsonObject newPfdObject=jsonReader.getPfdObject("/"+optionId+"_newpfd.json");
			processPFD(authToken, planId, optionId, projectId, newPfdObject,pfdId);

		}  catch (Exception e) {
			logger.error("Exception occured due to :: "+e.getMessage(),e);
		}
	}
	/**
	 * 
	 * @name : processPFD
	 * @description : The Method "processPFD" is used for update and validate plan and pfd values.
	 * @date : 27-Nov-2018 1:51:17 PM
	 * @param authToken
	 * @param planId
	 * @param optionId
	 * @param projectId
	 * @param pfdObject
	 * @param pfdId
	 * @return : void
	 *
	 */
	public void processPFD(String authToken,String planId,String optionId,String projectId,JsonObject pfdObject,String pfdId) {
		JsonObject jsonObject=updatePFD(authToken, pfdObject, pfdId);
		logger.info("Update pfd response is :: {}",jsonObject);
		String status = generatePlan(UrlConstants.PLAN_PUT_URL+"&project_id="+projectId, authToken,optionId);
		Validator validator=new Validator();
		if (AditazzConstants.COMPLETED_STATUS.equalsIgnoreCase(status)){
			JsonObject planObject = getPlan(authToken,planId);
			logger.info("Plan Object : {} " , planObject);
			validator.validatePlanAndPfd(pfdObject, planObject);
		}
	}
	
	/**
	 * 
	 * @name : getPlan
	 * @description : The Method "getPlan" is used for getting plan based on plan id. 
	 * @date : 26-Nov-2018 11:02:23 AM
	 * @param authToken
	 * @param planId
	 * @return
	 * @return : JsonObject
	 *
	 */
	public JsonObject getPlan(String authToken,String planId) {
		return RestUtil.getObject(authToken, null, UrlConstants.PLAN_URL+planId);
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
		logger.info("Ticket Id : {} " , output.get(JsonFields.ID.getValue()).getAsString());
		String status = null;
		while (true) {
			status = getStatusByTicketId(output.get(JsonFields.ID.getValue()).getAsString(), authToken);
			if (AditazzConstants.COMPLETED_STATUS.equalsIgnoreCase(status)) {
				break;
			}
			logger.info("Ticket Status : {} " ,status);
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
	 * @param authToken
	 * @param pfdObject
	 * @param pfdId
	 * @return
	 * @return : JsonObject
	 *
	 */
	public JsonObject updatePFD(String authToken,JsonObject pfdObject,String pfdId) {
		return RestUtil.putObject(authToken, pfdObject, UrlConstants.PFD_URL+pfdId);
	}
	/**
	 * 
	 * @name : getPlanAndOptionId
	 * @description : The Method "getPlanAndOptionId" is used for getting plan id and pfd id. 
	 * @date : 27-Nov-2018 12:54:50 PM
	 * @param authToken
	 * @param optionId
	 * @return
	 * @return : JsonObject
	 *
	 */
	public JsonObject getPlanAndOptionId(String authToken,String optionId) {
		JsonObject jsonObject=new JsonObject();
		JsonObject responseObject=RestUtil.getObject(authToken, null, UrlConstants.OPTIONS_URL+optionId);
		logger.info("Option Json :: {}",responseObject);
		JsonObject optionObject=responseObject.get(JsonFields.OPTIONS.getValue()).getAsJsonArray().get(0).getAsJsonObject();
		JsonObject payloadObj=optionObject.get(JsonFields.PAYLOAD.getValue()).getAsJsonObject();
		JsonObject pfdObj=payloadObj.get(JsonFields.PFD.getValue()).getAsJsonObject();
		jsonObject.add(JsonFields.PFD_ID.getValue(), pfdObj.get(JsonFields.ID.getValue()));
		JsonObject planObj=payloadObj.get(JsonFields.PLAN.getValue()).getAsJsonObject();
		jsonObject.add(JsonFields.PLAN_ID.getValue(), planObj.get(JsonFields.ID.getValue()));
		return jsonObject;
	}
	
	/**
	 * 
	 * @name : getPfdObject
	 * @description : The Method "getPfdObject" is used for 
	 * @date : 27-Nov-2018 1:52:03 PM
	 * @param authToken
	 * @param pfdId
	 * @return
	 * @return : JsonObject
	 *
	 */
	public JsonObject getPfdObject(String authToken,String pfdId) {
		JsonObject jsonObject=RestUtil.getObject(authToken, null, UrlConstants.PFD_URL+pfdId);
		logger.info("Pfd json :: {} ",jsonObject);
		return jsonObject.get(JsonFields.PFDS.getValue()).getAsJsonArray().get(0).getAsJsonObject();
	}
}
