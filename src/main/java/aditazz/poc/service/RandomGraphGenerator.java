package aditazz.poc.service;
/**
 * 
 * @author      : Sreekhar Reddy.K
 * @version     : Java 1.8 
 * @createdOn   : 03-Dec-2018 11:43:32 AM
 * @description : The class RandomGraphGenerator.java used for
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.jgrapht.Graph;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedPseudograph;
import org.jgrapht.util.SupplierUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import aditazz.poc.dto.Aditazz;
import aditazz.poc.dto.Line;
import aditazz.poc.dto.PfdEquipment;
import aditazz.poc.enums.JsonFields;
import aditazz.poc.util.DistanceUtil;

public class RandomGraphGenerator {
	private static final Logger logger = LoggerFactory.getLogger(AditazzService.class);
	private static final AtomicLong seedUniquifier = new AtomicLong(8682522807148012L);
	
	
	
	
	/**
	 * 
	 * @name : generateRandomGraph
	 * @description : The Method "generateRandomGraph" is used for generate random graph and prepare the valid equipments and lines. 
	 * @date : 03-Dec-2018 3:08:01 PM
	 * @param aditazz
	 * @param numberOfNodes
	 * @param numberOfEdges
	 * @return
	 * @return : JsonObject
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 *
	 */
	public JsonObject generateRandomGraph(Aditazz aditazz,int numberOfNodes,int numberOfEdges) throws   IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		LineService lineService=new LineService();
		EquipmentService equipmentService=new EquipmentService();
		Gson gson=new Gson();
		DistanceUtil distanceUtil=new DistanceUtil();
		JsonObject equipmentJson=new JsonObject();
		JsonObject lineJson=new JsonObject();
		LinkedHashMap<String, String> uuids=new LinkedHashMap<>();
		JsonParser parser = new JsonParser();
		
		Graph<Integer, DefaultEdge> graph =gnerateGraph(numberOfNodes, numberOfEdges);
		int counter=0;
		String lineId="L000";
		JsonObject equipmentLib=equipmentService.getEquipments(aditazz);
		List<String> equipOrder=equipmentService.jsonStringToArray(equipmentLib);
		List<String> pipeIds=new PipeService().getListOfPipes(aditazz);
		int pipeCounter=0;
		int x=102;
		int y=112;
		Map<String,JsonObject> spacingTable=equipmentService.getSpacingTableFromLib(equipmentLib);
		for (DefaultEdge edge: graph.edgeSet()) {
			List<Integer[]> path=new ArrayList<>();
			PfdEquipment sourceEquipment=null;
			PfdEquipment targetEquipment=null;
			String[] edges=edge.toString().replaceAll("[^a-zA-Z0-9:]", "").trim().split(":");
			
			counter++;
			String sourceUuid=UUID.randomUUID().toString();
			String targetUuid=UUID.randomUUID().toString();
			String sourceSide="left";
			String targetSide="right";
			if(counter >= 10) {
				lineId="L00";
			}
			
			if(!uuids.containsKey(edges[0])) {
				x+=10;y+=10;
				sourceEquipment=new PfdEquipment();
				sourceEquipment=equipmentService.preparePfdEquipment(sourceEquipment, equipOrder.get(Integer.parseInt(edges[0])-1), sourceUuid, "left",x,y);
				uuids.put(edges[0], sourceUuid);
				equipmentJson.add(sourceUuid, parser.parse(gson.toJson(sourceEquipment)).getAsJsonObject());
			}else {
				sourceEquipment=objectMapper.readValue(gson.toJson(equipmentJson.get(uuids.get(edges[0]))), PfdEquipment.class);
				sourceUuid=uuids.get(edges[0]);
			}
			if(!uuids.containsKey(edges[1])) {
				x+=10;y+=10;
				targetEquipment=new PfdEquipment();
				targetEquipment=equipmentService.preparePfdEquipment(targetEquipment,  equipOrder.get(Integer.parseInt(edges[1])-1), targetUuid,  "right",x,y);
				uuids.put(edges[1], targetUuid);
				equipmentJson.add(targetUuid, parser.parse(gson.toJson(targetEquipment)).getAsJsonObject());
			}else {
				targetEquipment=objectMapper.readValue(gson.toJson(equipmentJson.get(uuids.get(edges[1]))), PfdEquipment.class);
				targetUuid=uuids.get(edges[1]);
			}
			path.add(new Integer[] {sourceEquipment.getX(),sourceEquipment.getY()});
			path.add(new Integer[] {targetEquipment.getX(),targetEquipment.getY()});
			if(pipeCounter >= pipeIds.size()) 
				pipeCounter=0;
			
			spacingTable=distanceUtil.updateShortestDistance(equipOrder.get(Integer.parseInt(edges[0])-1), equipOrder.get(Integer.parseInt(edges[1])-1), spacingTable);
			String lineUuid=UUID.randomUUID().toString();
			Line line=new Line();
			line.setId(lineId+counter);
			
			line=lineService.preparePfdLine(line, lineUuid, sourceSide, targetSide, sourceUuid, targetUuid,path);
			line.setPipeID(pipeIds.get(counter-1));
			lineJson.add(lineUuid,  parser.parse(gson.toJson(line)).getAsJsonObject());
			pipeCounter++;
			
		}
		equipmentLib.add(JsonFields.SPACING.getValue(), new Gson().toJsonTree(spacingTable));
		equipmentService.updateEquipmentLibrary(aditazz, equipmentLib);
		logger.info("Graph is :: {} ",graph);
		JsonObject jsonObject=new JsonObject();
		jsonObject.add(JsonFields.EQUIPMENT.getValue(), equipmentJson);
		jsonObject.add(JsonFields.LINES.getValue(), lineJson);
		return jsonObject;
	}
	/**
	 * 
	 * @name : gnerateGraph
	 * @description : The Method "gnerateGraph" is used for 
	 * @date : 04-Dec-2018 5:04:25 PM
	 * @param numberOfNodes
	 * @param numberOfEdges
	 * @return
	 * @return : Graph<Integer,DefaultEdge>
	 *
	 */
	private Graph<Integer, DefaultEdge> gnerateGraph(int numberOfNodes,int numberOfEdges) {
		long seed=seedUniquifier() ^ System.nanoTime();
		GraphGenerator<Integer, DefaultEdge, Integer> gen =  new GnmRandomGraphGenerator<>(numberOfNodes, numberOfEdges,seed,false,false);
		Graph<Integer, DefaultEdge> graph = new DirectedPseudograph<>(SupplierUtil.createIntegerSupplier(1), SupplierUtil.createDefaultEdgeSupplier(), false);
		gen.generateGraph(graph);
		return graph;
	}
	
	/**
	 * 
	 * @name : seedUniquifier
	 * @description : The Method "seedUniquifier" is used for generating some sequence to generate random graph 
	 * @date : 03-Dec-2018 3:09:21 PM
	 * @return
	 * @return : long
	 *
	 */
	private long seedUniquifier() {
        // L'Ecuyer, "Tables of Linear Congruential Generators of
        // Different Sizes and Good Lattice Structure", 1999
        for (;;) {
            long current = seedUniquifier.get();
            long next = current * 181783497276652981L;
            if (seedUniquifier.compareAndSet(current, next))
                return next;
        }
	 }
	
	
	
	
}
