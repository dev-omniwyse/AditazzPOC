package aditazz.poc.enums;

/**
 * 
 * @author      : Sreekhar Reddy.K
 * @version     : Java 1.8 
 * @createdOn   : 26-Nov-2018 11:05:09 AM
 * @description : The enum JsonFields.java used to declare json fields. 
 */
public enum JsonFields {
	ID("id"),
	STATUS("status"),
	PFDS("pfds"),
	PLANS("plans"),
	PAYLOAD("payload"),
	LINES("lines"),
	PATHS("paths"),
	ACCESS_TOKEN("accessToken"),  
    EQUIPMENT("equipment") ;
	

    private final String value;

    private JsonFields(String value) {
        this.value = value;
    }

	public String getValue() {
		return value;
	}
}
