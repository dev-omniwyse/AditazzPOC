package aditazz.poc.service;
/**
 * 
 * @author      : Sreekhar Reddy.K
 * @version     : Java 1.8 
 * @createdOn   : 03-Dec-2018 10:33:05 AM
 * @description : The class LineService.java used for
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aditazz.poc.dto.Line;
import aditazz.poc.dto.LineSource;
import aditazz.poc.dto.LineTarget;
import aditazz.poc.enums.JsonFields;


public class LineService {
	/**
	 * 
	 * @name : preparePfdLine
	 * @description : The Method "preparePfdLine" is used for pfd line objects.
	 * @date : 03-Dec-2018 3:10:44 PM
	 * @param line
	 * @param uuid
	 * @param sourceSide
	 * @param targetSide
	 * @param sourceUuid
	 * @param targetUuid
	 * @return
	 * @return : Line
	 *
	 */
	public Line preparePfdLine(Line line,String uuid,String sourceSide,String targetSide,String sourceUuid,String targetUuid) {
		line.setUuid(uuid);
		line.setStraight(false);
		Map<String,Object> errors=new HashMap<>();
		errors.put(JsonFields.IDERR.getValue(), false);
		line.setErrors(errors);
        LineSource lineSource=getLineSource(sourceUuid, sourceSide);
        line.setSource(lineSource);
        line.setDrawing(false);
        
        LineTarget lineTarget=getLineTarget(targetSide, targetUuid);
        line.setTarget(lineTarget);
        List<Integer[]> paths=new ArrayList<>();
        paths.add(new Integer[] {460,  423});
        paths.add(new Integer[] {484,423});
        paths.add(new Integer[] {484,436});
        paths.add(new Integer[] { 490,436});
        paths.add(new Integer[] {490,434});
        line.setPath(paths);
        line.setLength(46);
        line.setPipeID("PPXC39349");
		
		return line;
	}
	/**
	 * 
	 * @name : getLineSource
	 * @description : The Method "getLineSource" is used for preparing line source
	 * @date : 03-Dec-2018 3:35:21 PM
	 * @param sourceUuid
	 * @param sourceSide
	 * @return
	 * @return : LineSource
	 *
	 */
	private LineSource getLineSource(String sourceUuid,String sourceSide) {
		LineSource lineSource=new LineSource();
        lineSource.setUuid(sourceUuid);
        lineSource.setSide(sourceSide);
        lineSource.setType("equipment");
        lineSource.setPosition(new Double[] {1d,0.5});
        lineSource.setNozzle(sourceUuid+sourceSide+"1");
        return lineSource;
	}
	/**
	 * 
	 * @name : getLineTarget
	 * @description : The Method "getLineTarget" is used for preparing line target 
	 * @date : 03-Dec-2018 3:35:36 PM
	 * @param targetSide
	 * @param targetUuid
	 * @return
	 * @return : LineTarget
	 *
	 */
	private LineTarget getLineTarget(String targetSide,String targetUuid) {
		LineTarget lineTarget=new LineTarget();
		lineTarget.setUuid(targetUuid);
		lineTarget.setSide(targetSide);
		lineTarget.setType("equipment");
		lineTarget.setPosition(new Double[] {0.5,1d});
		lineTarget.setNozzle(targetUuid+targetSide+"1");
		return lineTarget;
		
	}
	
	
}

