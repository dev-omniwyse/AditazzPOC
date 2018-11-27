package aditazz.poc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Base64Utils;

import com.google.gson.JsonObject;

import aditazz.poc.enums.JsonFields;
import aditazz.poc.util.RestUtil;

/**
 * 
 * @author      : Sreekhar Reddy.K
 * @version     : Java 1.8 
 * @createdOn   : 26-Nov-2018 11:09:20 AM
 * @description : The class AuthenticationService.java used for
 */
public class AuthenticationService {
	
	private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
	
	/**
	 * 
	 * @name : getAuthenticationToken
	 * @description : The Method "getAuthenticationToken" is used for generating authentication token based on username and password.
	 * @date : 26-Nov-2018 11:09:11 AM
	 * @param username
	 * @param password
	 * @return
	 * @return : String
	 *
	 */
	public String getAuthenticationToken(String username,String password) {
		String key=new String(Base64Utils.encode((username+":"+password).getBytes()));
		JsonObject jsonObject=RestUtil.getAuthenticationObject("Basic "+key);
		logger.info("Authentication object :: {}",jsonObject);
		return jsonObject.get(JsonFields.ACCESS_TOKEN.getValue()).getAsString();
	}
}
