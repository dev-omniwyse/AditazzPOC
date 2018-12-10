package aditazz.poc.service;

import java.util.LinkedList;
import java.util.List;

import com.google.gson.JsonObject;

import aditazz.poc.constants.UrlConstants;
import aditazz.poc.dto.Aditazz;
import aditazz.poc.enums.JsonFields;
import aditazz.poc.util.RestUtil;

/**
 * 
 * @author      : Sreekhar Reddy.K
 * @version     : Java 1.8 
 * @createdOn   : 05-Dec-2018 9:36:21 AM
 * @description : The class PipeService.java used for
 */
public class PipeService {
	/**
	 * 
	 * @name : getListOfPipes
	 * @description : The Method "getListOfPipes" is used for getting the list of pipes from pipe library. 
	 * @date : 05-Dec-2018 11:39:05 AM
	 * @param aditazz
	 * @return
	 * @return : List<String>
	 *
	 */
	public List<String> getListOfPipes(Aditazz aditazz) {
		JsonObject jsonObject=RestUtil.getObject(aditazz.getAuthToken(), null, UrlConstants.PIPE_LIB_URL+aditazz.getPipeLibId());
		JsonObject payloadObj=jsonObject.get(JsonFields.PIPE_LIBRARIES.getValue()).getAsJsonArray().get(0).getAsJsonObject().get(JsonFields.PAYLOAD.getValue()).getAsJsonObject();
		JsonObject pipeJson=payloadObj.get(JsonFields.PIPES.getValue()).getAsJsonObject();
		return new LinkedList<>(pipeJson.keySet());
	}

}
