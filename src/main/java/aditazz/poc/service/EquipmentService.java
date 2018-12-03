package aditazz.poc.service;
/**
 * 
 * @author      : Sreekhar Reddy.K
 * @version     : Java 1.8 
 * @createdOn   : 30-Nov-2018 5:00:50 PM
 * @description : The class EquipmentService.java used for
 */

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;

import aditazz.poc.dto.Grid;
import aditazz.poc.dto.PfdEquipment;
import aditazz.poc.enums.JsonFields;

public class EquipmentService {
	
	/**
	 * 
	 * @name : preparePfdEquipment
	 * @description : The Method "preparePfdEquipment" is used for preparing the equipment object.
	 * @date : 03-Dec-2018 3:10:00 PM
	 * @param pfdEquipment
	 * @param type
	 * @param uuid
	 * @param id
	 * @param location
	 * @return
	 * @return : PfdEquipment
	 *
	 */
	public PfdEquipment preparePfdEquipment(PfdEquipment pfdEquipment,String type,String uuid,String id,String location) {
		pfdEquipment.setType(type);
		pfdEquipment.setId(id);
		pfdEquipment.setDimensions(new Integer[] {2,2,6});
		pfdEquipment.setX(451);
		pfdEquipment.setY(418);
		Map<String,Object> errors=new HashMap<>();
		errors.put(JsonFields.IDERR.getValue(), false);
		pfdEquipment.setErrors(errors);
		pfdEquipment.setResizing(false);
		Grid grid=new Grid();
		grid.setDimensions(new int[] { 5,20});
		grid.setLastDimensions(new int[] { 5, 20});
		pfdEquipment.setGrid(grid);
		pfdEquipment.setDragging(false);
		pfdEquipment.setArea(0);
		pfdEquipment.setElevation(2);
		pfdEquipment.setGrouping(true);
		pfdEquipment.setNozzlesToReconciliate(new String[] {});
		pfdEquipment.setOccupiedNozzles(new String[] {uuid+location});
		return pfdEquipment;
	}
	

}
