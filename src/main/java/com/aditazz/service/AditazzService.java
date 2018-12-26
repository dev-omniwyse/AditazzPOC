package com.aditazz.service;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.aditazz.constants.AditazzConstants;
import com.aditazz.constants.UrlConstants;
import com.aditazz.dto.AditazzStatsDTO;
import com.aditazz.dto.InputDTO;
import com.aditazz.enums.JsonFields;
import com.aditazz.model.Aditazz;
import com.aditazz.util.FileUtil;
import com.aditazz.util.JsonReader;
import com.aditazz.util.RestUtil;
import com.aditazz.validator.Validator;
import com.google.gson.Gson;
import com.google.gson.JsonObject;



/**
 * 
 * @author      : Sreekhar Reddy.K
 * @version     : Java 1.8 
 * @createdOn   : 26-Nov-2018 11:01:04 AM
 * @description : The class AditazzService.java used for process pfd and plan
 */
@Service
public class AditazzService {
	@Autowired
	Environment environment;
	
	@Autowired
	EquipmentService equipmentService;
	
	@Autowired 
	RandomGraphGenerator randomGraphGenerator;
	
	private static DecimalFormat decimalFormat = new DecimalFormat(".##");
	
	
	private static final Logger logger = LoggerFactory.getLogger(AditazzService.class);
	
	public Map<String,AditazzStatsDTO> generateStats(InputDTO inputDTO) throws IOException{
		List<AditazzStatsDTO> aditazzStatsDTOs=new ArrayList<>();
		logger.info("Minimum nodes :: {} Max nodes ::{} Increment size :: {}", inputDTO.getMinNodes(),inputDTO.getMaxNodes(),inputDTO.getIncrementSize());
		FileUtil fileUtil=new FileUtil();
		Validator validator=new Validator();
		Properties properties=new Properties();
		AuthenticationService authenticationService=new AuthenticationService();
		String authToken=authenticationService.getAuthenticationToken(environment.getProperty("aditazz.username"), environment.getProperty("aditazz.password"));
		String projectId=environment.getProperty("aditazz.projectid");
		
		Aditazz aditazz=new Aditazz();
		aditazz.setAuthToken(authToken);
		aditazz.setProjectId(projectId);
		String path=environment.getProperty("aditazz.path");
		
		properties.load(AditazzService.class.getClassLoader().getResourceAsStream("option_plan.properties"));
		aditazz=getLibraryIds(aditazz);
		Map<String,AditazzStatsDTO> response=new HashMap<>();
		int counter=0;
		for(Entry<Object, Object> entry : properties.entrySet()) {
			aditazz.setOptionId(entry.getKey().toString());
			logger.info("Process started with Option id :: {} ",entry.getKey());
			aditazz=getPlanAndPfdId(aditazz);
			for (int numberOfNodes=inputDTO.getMinNodes();numberOfNodes<inputDTO.getMaxNodes();) {
				counter++;
				AditazzStatsDTO aditazzStatsDTO=new AditazzStatsDTO();
				long start = System.currentTimeMillis( );
				if(counter != 1)
					numberOfNodes=numberOfNodes*inputDTO.getIncrementSize();
				
				if(numberOfNodes>inputDTO.getMaxNodes())
					numberOfNodes=inputDTO.getMaxNodes();
				aditazzStatsDTO.setEquipments(numberOfNodes);
				int numberOfEdges=numberOfNodes*2;
				aditazzStatsDTO.setLines(numberOfEdges);
				aditazzStatsDTO.setNumberOfObjects(numberOfNodes+numberOfEdges);
				
				JsonObject pfdObject=getPfdObject(aditazz);
				JsonObject equipPayload=equipmentService.getEquipments(aditazz).get(JsonFields.EQUIPMENT_LIBRARIES.getValue()).getAsJsonArray().get(0).getAsJsonObject().get(JsonFields.PAYLOAD.getValue()).getAsJsonObject();
				JsonObject payloadObj=randomGraphGenerator.generateRandomGraph(aditazz, numberOfNodes, numberOfEdges);
				JsonObject updatedEquipments=equipmentService.getEquipments(aditazz);
				JsonObject updatedLib=updatedEquipments.get(JsonFields.EQUIPMENT_LIBRARIES.getValue()).getAsJsonArray().get(0).getAsJsonObject().get(JsonFields.PAYLOAD.getValue()).getAsJsonObject();
				fileUtil.createFile(path, updatedLib.toString(), "updated_equipment_library");
				int revision=equipmentService.getRevisonNumber(aditazz);
				updateProjectEquipLibRevison(revision, aditazz);
				pfdObject.add(JsonFields.PAYLOAD.getValue(), payloadObj);
				updatePFD(pfdObject, aditazz) ;
				fileUtil.createFile(path, pfdObject.toString(), "updated_pfd");
				
				int revison=new JsonReader().getPfdRevision(pfdObject)+1;
				updateOptionRevison(aditazz, revison);
				//jsonObject.get(JsonFields.EQUIPMENT_LIBRARIES.getValue()).getAsJsonArray().get(0).getAsJsonObject().get(JsonFields.PAYLOAD.getValue()).getAsJsonObject();
				
				generatePlan(UrlConstants.PLAN_PUT_URL, aditazz,aditazzStatsDTO) ;
				JsonObject planObject=getPlan(aditazz);
				fileUtil.createFile(path, planObject.toString(), "new_plan");
				logger.info("Validating plan and pfd");
				long equivalencyStartTime=System.currentTimeMillis();
				Map<String,Boolean> result=validator.validatePlanAndPfd(pfdObject, planObject, updatedLib,aditazzStatsDTO);
				long equivalencyEndTime=System.currentTimeMillis();
				aditazzStatsDTO.setEquivalencyVerifiedTime((equivalencyEndTime-equivalencyStartTime)/AditazzConstants.MSSECONDS_PER_SECOND);
				
				
				if(result.get(AditazzConstants.EQUIPMENT_EQUAL) && result.get(AditazzConstants.VALID_DISTANCE) && result.get(AditazzConstants.LINES_EQUAL))
					aditazzStatsDTO.setEquivalency("Y");
				else
					aditazzStatsDTO.setEquivalency("N");
				
				aditazzStatsDTO.setIsTimedOut("N");
				
				logger.info("Reverting spacing changes in equipment library ");
				equipmentService.updateEquipmentLibrary(aditazz, equipPayload,updatedEquipments);
				revision=equipmentService.getRevisonNumber(aditazz);
				logger.info("Updating equipment revison in projects :: {}",revision);
				updateProjectEquipLibRevison(revision, aditazz);
				
				long end = System.currentTimeMillis( );
				aditazzStatsDTO.setTotalElpsedTime(Double.parseDouble(decimalFormat.format(((end-start)/AditazzConstants.MSSECONDS_PER_SECOND)/AditazzConstants.SECONDS_PER_MINUTE)));
				
				aditazzStatsDTOs.add(aditazzStatsDTO);
				response.put(String.valueOf(counter), aditazzStatsDTO);
			}
			logger.info("Process ends with Option id :: {} ",entry.getKey());
		}
		response.put(AditazzConstants.TOTAL, calculateTotalStats(aditazzStatsDTOs));
		return response;
	}
	/**
	 * 
	 * @name : calculateTotalStats
	 * @description : The Method "calculateTotalStats" is used for 
	 * @date : 19-Dec-2018 2:35:18 PM
	 * @param aditazzStatsDTOs
	 * @return
	 * @return : AditazzStatsDTO
	 *
	 */
	private AditazzStatsDTO calculateTotalStats(List<AditazzStatsDTO> aditazzStatsDTOs) {
		AditazzStatsDTO aditazzStatsDTO=new AditazzStatsDTO();
		int equipments=0;
		int lines=0;
		int numberOfObjects=0;
		int numberOfRulesChecked=0;
		double equipmentPlacementTime=0;//secs
		double pipeRouterTime=0;//secs
		double equivalencyVerifiedTime=0;//secs
		double totalElpsedTime=0;//minutes
		int throughput=0;
		for (AditazzStatsDTO aditazzStats : aditazzStatsDTOs) {
			equipments+=aditazzStats.getEquipments();
			lines+=aditazzStats.getLines();
			numberOfObjects+=aditazzStats.getNumberOfObjects();
			numberOfRulesChecked+=aditazzStats.getNumberOfRulesChecked();
			equipmentPlacementTime+=aditazzStats.getEquipmentPlacementTime();
			pipeRouterTime+=aditazzStats.getPipeRouterTime();
			equivalencyVerifiedTime+=aditazzStats.getEquivalencyVerifiedTime();
			totalElpsedTime+=aditazzStats.getTotalElpsedTime();
			throughput+=aditazzStats.getThroughput();
		}
		aditazzStatsDTO.setEquipments(equipments);
		aditazzStatsDTO.setLines(lines);
		aditazzStatsDTO.setNumberOfObjects(numberOfObjects);
		aditazzStatsDTO.setNumberOfRulesChecked(numberOfRulesChecked);
		aditazzStatsDTO.setEquipmentPlacementTime(equipmentPlacementTime);
		aditazzStatsDTO.setPipeRouterTime(pipeRouterTime);
		aditazzStatsDTO.setEquivalencyVerifiedTime(equivalencyVerifiedTime);
		aditazzStatsDTO.setTotalElpsedTime(totalElpsedTime);
		aditazzStatsDTO.setThroughput(throughput);
		return aditazzStatsDTO;
	}
	
	
	/**
	 * 
	 * @name : getPlan
	 * @description : The Method "getPlan" is used for getting plan based on plan id. 
	 * @date : 26-Nov-2018 11:02:23 AM
	 * @param aditazz
	 * @return : JsonObject
	 *
	 */
	public JsonObject getPlan(Aditazz aditazz) {
		logger.info("Getting plan for id :: {}",aditazz.getPlanId());
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
	 * @param aditazzStatsDTO
	 * @return
	 * @return : String
	 *
	 */
	public String generatePlan(String url, Aditazz aditazz,AditazzStatsDTO aditazzStatsDTO) {
		logger.info("Generating plan for option :: {}",aditazz.getOptionId());
		JsonObject emptyEquipment=getEmptyEquipment();
		String[] runTypes=new String[] {UrlConstants.PLACE,UrlConstants.ROUTE};
		String status = null;
		
		for (int i=0;i<runTypes.length;i++) {
			long startTime=System.currentTimeMillis();
			JsonObject output = RestUtil.putObject(aditazz.getAuthToken(),emptyEquipment,url+runTypes[i]+"&project_id="+aditazz.getProjectId()+"&option_id="+aditazz.getOptionId());
			String ticketId=output.get(JsonFields.ID.getValue()).getAsString();
			logger.info("Ticket Id : {} " , ticketId);
			while (true) {
				status = getStatusByTicketId(ticketId, aditazz.getAuthToken());
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
			long endTime=System.currentTimeMillis();
			if(UrlConstants.PLACE.equals(runTypes[i]))
				aditazzStatsDTO.setEquipmentPlacementTime((endTime-startTime)/AditazzConstants.MSSECONDS_PER_SECOND);
			else if(UrlConstants.ROUTE.equals(runTypes[i]))
				aditazzStatsDTO.setPipeRouterTime((endTime-startTime)/AditazzConstants.MSSECONDS_PER_SECOND);
			logger.info("Ticket Status : {} " , status);
		}
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
		logger.info("Updating random graph pfd json :: {}",aditazz.getPfdId());
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
	public Aditazz getPlanAndPfdId(Aditazz aditazz) {
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
	 * @param revisionNum
	 * @return : void
	 *
	 */
	public JsonObject updateOptionRevison(Aditazz aditazz,int revisionNum) {
		JsonObject jsonObject=RestUtil.getObject(aditazz.getAuthToken(), null, UrlConstants.OPTIONS_URL+aditazz.getOptionId());
		JsonObject optionJson=jsonObject.get(JsonFields.OPTIONS.getValue()).getAsJsonArray().get(0).getAsJsonObject();
		JsonObject payloadObj=optionJson.get(JsonFields.PAYLOAD.getValue()).getAsJsonObject();
		JsonObject pfdJson=payloadObj.get(JsonFields.PFD.getValue()).getAsJsonObject();
		JsonObject revison=pfdJson.get(JsonFields.REVISON.getValue()).getAsJsonObject();
		revison.add(JsonFields.ID.getValue(), new Gson().toJsonTree(revisionNum));
		pfdJson.add(JsonFields.REVISON.getValue(), revison);
		payloadObj.add(JsonFields.PFD.getValue(), pfdJson);
		optionJson.add(JsonFields.PAYLOAD.getValue(), payloadObj);
		logger.info("Updating option with latest revision of pfd :: {}",revisionNum);
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
	 * @param revisionNumber
	 * @param aditazz
	 * @return
	 * @return : boolean
	 *
	 */
	public boolean updateProjectEquipLibRevison(int revisionNumber,Aditazz aditazz) {
		JsonObject jsonObject=RestUtil.getObject(aditazz.getAuthToken(), null, UrlConstants.PROJECT_URL+aditazz.getProjectId());
		JsonObject projectJson=jsonObject.get(JsonFields.PROJECTS.getValue()).getAsJsonArray().get(0).getAsJsonObject();
		JsonObject payloadObj=projectJson.get(JsonFields.PAYLOAD.getValue()).getAsJsonObject();
		JsonObject equipmentJson=payloadObj.get(JsonFields.EQUIPMENT_LIBRARY.getValue()).getAsJsonObject();
		JsonObject revison=equipmentJson.get(JsonFields.REVISON.getValue()).getAsJsonObject();
		revison.add(JsonFields.ID.getValue(), new Gson().toJsonTree(revisionNumber));
		logger.info("Updating equipment revison in projects :: {}",revisionNumber);
		JsonObject response=RestUtil.putObject(aditazz.getAuthToken(),projectJson, UrlConstants.PROJECT_URL+aditazz.getProjectId());
		//logger.info("After updating project json the response json is :: {}",response)
		return response != null;
		
	}
	
	
}
