package aditazz.poc.validator;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import aditazz.poc.constants.AditazzConstants;
import aditazz.poc.enums.JsonFields;

/**
 * 
 * @author      : Sreekhar Reddy.K
 * @version     : Java 1.8 
 * @createdOn   : 26-Nov-2018 10:08:07 AM
 * @description : The class Validator.java used for validating pfd and plan
 */
public class Validator {
	
	private static final Logger logger = LoggerFactory.getLogger(Validator.class);
	
	/**
	 * 
	 * @name : validatePlanAndPfd
	 * @description : The Method "validatePlanAndPfd" is used for compare pfd and plan object values. 
	 * @date : 26-Nov-2018 9:52:40 AM
	 * @param pfdObject
	 * @param planObject
	 * @return : void
	 *
	 */
	public Map<String,Boolean> validatePlanAndPfd(JsonObject pfdObject,JsonObject planObject) {
		Map<String, Boolean> result=null;
		try {
			result=new HashMap<>();
			JsonObject planPayloadObject=getPayloadFromPlan(planObject);
	    	JsonObject pfdPayloadObject=getPayloadFromPfd(pfdObject);
	    	
	    	//Lines validation
	    	boolean isLinesEqual=validateLineObjects(pfdPayloadObject, planPayloadObject);
	    	if(isLinesEqual) {
				logger.info("Number of lines are equal");
			}else {
				logger.info("Number of lines are not equal");
			}
	    	
	    	//Equipment Validation
	    	boolean isEquipmentEqual=validateEquipments(pfdPayloadObject, planPayloadObject);
	    	if(isEquipmentEqual) {
				logger.info("Number of equipments are equal");
			}else {
				logger.info("Number of equipments are not equal");
			}
	    	
	    	result.put(AditazzConstants.LINES_EQUAL, isLinesEqual);
	    	result.put(AditazzConstants.EQUIPMENT_EQUAL, isEquipmentEqual);
		} catch (Exception e) {
			logger.error("Exception occurred while comparing pfd and plan "+e.getMessage(),e);
		}
		return result;
	}
	
	/**
	 * 
	 * @name : validateLineObjects
	 * @description : The Method "validateLineObjects" is used for compare pfd lines and plan lines.  
	 * @date : 26-Nov-2018 9:54:14 AM
	 * @param pfdPayload
	 * @param planPayload
	 * @return
	 * @return : boolean
	 *
	 */
	private boolean validateLineObjects(JsonObject pfdPayload,JsonObject planPayload) {
		boolean isEqual=true;
		try {
			JsonObject pfdLinesbject=pfdPayload.getAsJsonObject(JsonFields.LINES.getValue());
			JsonObject planPathsObject=planPayload.getAsJsonObject(JsonFields.PATHS.getValue());
			Set<Map.Entry<String,JsonElement>> pfdChildset=pfdLinesbject.entrySet();
			Set<Map.Entry<String,JsonElement>> planChildset=planPathsObject.entrySet();
			for (Entry<String, JsonElement> planEntry : planChildset) {
				boolean isExistLine=false;
				for (Entry<String, JsonElement> pfdEntry : pfdChildset) {
					JsonObject lineObject=pfdEntry.getValue().getAsJsonObject();
					if(lineObject.get(JsonFields.ID.getValue()).getAsString().equals(planEntry.getKey())) {
						isExistLine=true;
						break;
					}
				}
				if(!isExistLine) {
					isEqual=false;
					break;
				}
			}
		} catch (Exception e) {
			logger.error("Exception occurred while validating lines "+e.getMessage(),e);
		}
		return isEqual;
	}
	/**
	 * 
	 * @name : validateEquipments
	 * @description : The Method "validateEquipments" is used for compare pfd equipments and plan equipments. 
	 * @date : 26-Nov-2018 9:55:31 AM
	 * @param pfdPayloadObject
	 * @param planPayloadObject
	 * @return
	 * @return : boolean
	 *
	 */
	
	private boolean  validateEquipments(JsonObject pfdPayloadObject,JsonObject planPayloadObject) {
		boolean isValid=true;
		try {
			JsonObject planEquipmentObj=planPayloadObject.getAsJsonObject(JsonFields.EQUIPMENT.getValue());
	    	
	    	JsonObject pfdEquipmentObject=pfdPayloadObject.getAsJsonObject(JsonFields.EQUIPMENT.getValue());
			Set<Map.Entry<String,JsonElement>> planChildset=planEquipmentObj.entrySet();
			Set<Map.Entry<String,JsonElement>> equipmentChildset=pfdEquipmentObject.entrySet();
			// Equipment Size comparison
			if(planChildset.size() != equipmentChildset.size()) {
				isValid=false;
			}
				
			/*for (Map.Entry<String, JsonElement> planChild: planChildset) {
				logger.info("Equipment Name : {} " , planChild.getKey());
				boolean isExists=false;
				for (Map.Entry<String, JsonElement> equipmentChild: equipmentChildset) {
					
					// Equipment names comparison
					if(equipmentChild.getKey().equals(planChild.getKey())) {
						isExists=true;
						break;
					}
				}
				if(!isExists) {
					isValid=false;
					break;
				}
			}*/
		}catch (Exception e) {
			logger.error("Exception occurred while comparing equipments and plan "+e.getMessage(),e);
			isValid=false;
		}
		return isValid;
	}
	
	/**
	 * 
	 * @name : getPayloadFromPfd
	 * @description : The Method "getPayloadFromPfd" is used for getting payload object from pfd object.
	 * @date : 26-Nov-2018 9:56:22 AM
	 * @param pfdObject
	 * @return
	 * @return : JsonObject
	 *
	 */
	private JsonObject getPayloadFromPfd(JsonObject pfdObject) {
		JsonElement pfdInnerElement=pfdObject.getAsJsonArray(JsonFields.PFDS.getValue()).get(0);
    	JsonObject  pfdInnerObject = pfdInnerElement.getAsJsonObject();
    	return pfdInnerObject.getAsJsonObject(JsonFields.PAYLOAD.getValue());
	}
	/**
	 * 
	 * @name : getPayloadFromPlan
	 * @description : The Method "getPayloadFromPlan" is used for getting payload object from plan object.
	 * @date : 26-Nov-2018 9:59:14 AM
	 * @param planObject
	 * @return
	 * @return : JsonObject
	 *
	 */
	private JsonObject getPayloadFromPlan(JsonObject planObject) {
		JsonElement innerElement=planObject.getAsJsonArray(JsonFields.PLANS.getValue()).get(0);
    	JsonObject  innerObject = innerElement.getAsJsonObject();
    	return innerObject.getAsJsonObject(JsonFields.PAYLOAD.getValue());
	}
}
