package aditazz.poc.validator;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import aditazz.poc.constants.AditazzConstants;
import aditazz.poc.dto.PlanEquipment;
import aditazz.poc.enums.JsonFields;
import aditazz.poc.service.EquipmentService;
import aditazz.poc.util.DistanceUtil;

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
	 * @param equimentLib
	 * @return : Map<String,Boolean>
	 *
	 */
	public Map<String,Boolean> validatePlanAndPfd(JsonObject pfdObject,JsonObject planObject,JsonObject equimentLib) {
		Map<String, Boolean> result=null;
		try {
			result=new HashMap<>();
			JsonObject planPayloadObject=getPayloadFromPlan(planObject);
	    	JsonObject pfdPayloadObject=pfdObject.getAsJsonObject(JsonFields.PAYLOAD.getValue());
	    	
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
	    	boolean isValidDistance=false;
	    	if(isEquipmentEqual && isLinesEqual) {
	    		isValidDistance=validateDistance(equimentLib, planPayloadObject,pfdPayloadObject);
	    		if(isValidDistance)
	    			logger.info("Valid distance found between source and target");
	    		else
	    			logger.info("Invalid distance found between source and target");
	    	}
	    	result.put(AditazzConstants.LINES_EQUAL, isLinesEqual);
	    	result.put(AditazzConstants.EQUIPMENT_EQUAL, isEquipmentEqual);
	    	result.put(AditazzConstants.VALID_DISTANCE, isValidDistance);
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
			for (Entry<String, JsonElement> pfdEntry : pfdChildset) {
				JsonObject lineObject=pfdEntry.getValue().getAsJsonObject();
				String lineID=lineObject.get(JsonFields.ID.getValue()).getAsString();
				boolean isExistLine=false;
				for (Entry<String, JsonElement> planEntry : planChildset) {
					if(lineID.equals(planEntry.getKey())) {
						logger.info("Pfd line id :: {} and Plan line id  ::{}",lineID,planEntry.getKey());
						isExistLine=true;
						break;
					}
				}
				if(!isExistLine) {
					logger.info("Line is not exists in plan with id  :: {}",lineID);
					isEqual=false;
					break;
				}
			}
		} catch (Exception e) {
			logger.error("Exception occurred while validating lines "+e.getMessage(),e);
			isEqual=false;
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
	    	Set<Map.Entry<String,JsonElement>> pfdChildset=pfdEquipmentObject.entrySet();
			// Equipment Size comparison
			logger.info("Number of equipments of plan is :: {}",planChildset.size());
			logger.info("Number of equipments of pfd is :: {}",pfdChildset.size());
			
				
			for (Entry<String, JsonElement> pfdEquipment: pfdChildset) {
				String pfdEquipmentId=pfdEquipment.getValue().getAsJsonObject().get(JsonFields.ID.getValue()).getAsString();
				boolean isExists=false;
				for (Entry<String, JsonElement> planEntry : planChildset) {
					if(pfdEquipmentId.equals(planEntry.getKey())) {
						logger.info("Pfd equipment id :: {} and Plan equipment id  ::{}",pfdEquipmentId,planEntry.getKey());
						isExists=true;
						break;
					}
				}
				if(!isExists) {
					logger.info("Equipment is not exists in plan with id  ::{}",pfdEquipmentId);
					isValid=false;
					break;
				}
				
			}
		}catch (Exception e) {
			logger.error("Exception occurred while comparing equipments and plan "+e.getMessage(),e);
			isValid=false;
		}
		return isValid;
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
	
    /**
     * 
     * @name : validateDistance
     * @description : The Method "validateDistance" is used for validating space between two nodes.
     * @date : 06-Dec-2018 3:21:46 PM
     * @param equipmentLib
     * @param planPayload
     * @param pfdObject
     * @return
     * @throws IOException
     * @return : boolean
     *
     */
    private boolean validateDistance(JsonObject equipmentLib,JsonObject planPayload,JsonObject pfdObject) throws IOException {
    	ObjectMapper objectMapper = new ObjectMapper();
    	Gson gson=new Gson();
    	boolean isValid=true;
    	EquipmentService equipmentService=new EquipmentService();
    	JsonObject planEquipments=planPayload.getAsJsonObject(JsonFields.EQUIPMENT.getValue());
    	JsonObject pfdEquipment=pfdObject.getAsJsonObject(JsonFields.EQUIPMENT.getValue());
    	Map<String,JsonObject> spacingTable=equipmentService.getSpacingTableFromLib(equipmentLib);
    	DistanceUtil distanceUtil=new DistanceUtil();
    	Map<String,String> equipments=getListOfEquipmentTypes(pfdEquipment);
    	List<String>  equipmentTypes=new LinkedList<>(equipments.keySet());
    	//A -> B or B -> A same distance should be there. In spacing table consider either A->B or B->A.
    	for(int i=0;i<equipmentTypes.size();i++) {
    		String sourceType=equipmentTypes.get(i);
    		PlanEquipment sourceEquip=objectMapper.readValue(gson.toJson(planEquipments.get(equipments.get(sourceType))),PlanEquipment.class);
    		for (int j=i;j<equipmentTypes.size();j++) {
    			String targetType=equipmentTypes.get(j);
    			PlanEquipment targetEquip=objectMapper.readValue(gson.toJson(planEquipments.get(equipments.get(targetType))),PlanEquipment.class);
    			if(spacingTable.containsKey(sourceType) || spacingTable.containsKey(targetType)) {
        			double shortestDistance=distanceUtil.getShortestDistance(sourceType, targetType, spacingTable);
        			Map<String,Double> distances=distanceUtil.calculateDistance(sourceEquip, targetEquip);
        			double x=distances.get(JsonFields.X.getValue());
        			double y=distances.get(JsonFields.Y.getValue());
        			double z=distances.get(JsonFields.Z.getValue());
        			logger.info("Source type :: {} to target type :: {} spacing table distance is :: {} and plan distance X is :: {} Y is ::{} and Z is ::{} ",sourceType,targetType,shortestDistance,x,y,z);
        			
        			if(x < shortestDistance && y < shortestDistance && z < shortestDistance) {    			
        				logger.info("Invalid distance found between source type :: {} and target type :: {}",sourceType,targetType);
        				isValid=false;
        				break;
        			}
        		}else {
        			logger.info("Invalid distance found between source type :: {} and target type :: {}",sourceType,targetType);
    				isValid=false;
    				break;
        		}
    		}
    	}
    	return isValid;
    }
    
    
    /**
     * 
     * @name : getListOfEquipmentTypes
     * @description : The Method "getListOfEquipmentTypes" is used for getting equipment Types and ids.
     * @date : 11-Dec-2018 1:07:29 PM
     * @param jsonObject
     * @return
     * @return : Map<String,String>
     *
     */
    private Map<String,String> getListOfEquipmentTypes(JsonObject jsonObject){
    	Map<String,String> types=new HashMap<>();
    	Set<Map.Entry<String,JsonElement>> planChildset=jsonObject.entrySet();
    	for(Entry<String , JsonElement> entry : planChildset) {
    		JsonObject equipment=entry.getValue().getAsJsonObject();
    		types.put(equipment.get(JsonFields.TYPE.getValue()).getAsString(),equipment.get(JsonFields.ID.getValue()).getAsString());
    	}
    	return types;
    }
    
    
}
