package aditaazz.poc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import aditaazz.poc.constants.AditaazzConstants;
import aditaazz.poc.constants.UrlConstants;
import aditaazz.poc.enums.JsonFields;
import aditaazz.poc.util.RestUtil;

/**
 * 
 * @author      : Sreekhar Reddy.K
 * @version     : Java 1.8 
 * @createdOn   : 26-Nov-2018 11:01:04 AM
 * @description : The class AditaazzService.java used for process pfd and plan
 */
public class AditaazzService {
	private static final Logger logger = LoggerFactory.getLogger(AditaazzService.class);
	
	/**
	 * 
	 * @name : processPFD
	 * @description : The Method "processPFD" is used for process pfd and generating plan 
	 * @date : 26-Nov-2018 11:01:44 AM
	 * @param optionId
	 * @param planId
	 * @param authToken
	 * @return : void
	 *
	 */
	public void processPFD(String projectId,String optionId,String planId,String authToken) {
		try {
			JsonReader jsonReader=new JsonReader();
			Validator validator=new Validator();
			logger.info("Authentication token : {}" , authToken);
			JsonObject jsonObject=jsonReader.getPfdObject("/"+optionId+"_pfd.json");
			String pfdResponce = generatePlan(UrlConstants.PFD_URL+"&project_id="+projectId, authToken,optionId);
			
			if (AditaazzConstants.COMPLETED_STATUS.equalsIgnoreCase(pfdResponce)){
				JsonObject planObject = getPlan(authToken,planId);
				logger.info("Plan Object : {} " , planObject);
				validator.validatePlanAndPfd(jsonObject, planObject);
			}

		}  catch (Exception e) {
			logger.error("Exception occured due to :: "+e.getMessage(),e);
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
		JsonObject ticketObject = new JsonObject();
		while (true) {
			ticketObject = getTicketObeject(output.get(JsonFields.ID.getValue()).getAsString(), authToken);
			if (AditaazzConstants.COMPLETED_STATUS.equalsIgnoreCase(ticketObject.get(JsonFields.STATUS.getValue()).getAsString())) {
				break;
			}
			logger.info("Ticket Status : {} " , output.get(JsonFields.STATUS.getValue()).getAsString());
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				logger.error("Thread exception occured due to "+e.getMessage(),e);
			}
		}
		logger.info("Ticket Status : {} " , ticketObject.get(JsonFields.STATUS.getValue()).getAsString());
		return ticketObject.get(JsonFields.STATUS.getValue()).getAsString();
	}
	/**
	 * 
	 * @name : getTicketObeject
	 * @description : The Method "getTicketObeject" is used for getting ticket based on ticket id. 
	 * @date : 26-Nov-2018 11:03:25 AM
	 * @param ticketId
	 * @param authToken
	 * @return
	 * @return : JsonObject
	 *
	 */
	private JsonObject getTicketObeject(String ticketId, String authToken){
		return RestUtil.getObject(authToken, null, UrlConstants.TICKET_URL+ticketId);
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
}
