package aditazz.poc.service;
/**
 * 
 * @author      : Sreekhar Reddy.K
 * @version     : Java 1.8 
 * @createdOn   : 03-Dec-2018 11:43:32 AM
 * @description : The class RandomGraphGenerator.java used for
 */

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

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

import aditazz.poc.dto.Line;
import aditazz.poc.dto.PfdEquipment;
import aditazz.poc.enums.JsonFields;

public class RandomGraphGenerator {
	private static final Logger logger = LoggerFactory.getLogger(AditazzService.class);
	private static final AtomicLong seedUniquifier = new AtomicLong(8682522807148012L);
	Map<String,String> listEquipments=new LinkedHashMap<>() ;
	List<String> keySet=new LinkedList<>(Arrays.asList("P0","C0","HE0"));
	
	public RandomGraphGenerator() {
		listEquipments.put("P0", "P");
		listEquipments.put("C0", "C");
		
		listEquipments.put("HE0", "HE");
	}
	/**
	 * 
	 * @name : generateRandomGraph
	 * @description : The Method "generateRandomGraph" is used for generate random graph and prepare the valid equipments and lines. 
	 * @date : 03-Dec-2018 3:08:01 PM
	 * @param numberOfNodes
	 * @param numberOfEdges
	 * @return
	 * @return : JsonObject
	 *
	 */
	public JsonObject generateRandomGraph(int numberOfNodes,int numberOfEdges) {
		JsonObject jsonObject=new JsonObject();
		LineService lineService=new LineService();
		EquipmentService equipmentService=new EquipmentService();
		Gson gson=new Gson();
		long seed=seedUniquifier() ^ System.nanoTime();
		GraphGenerator<Integer, DefaultEdge, Integer> gen =  new GnmRandomGraphGenerator<>(numberOfNodes, numberOfEdges,seed,false,false);
		Graph<Integer, DefaultEdge> graph = new DirectedPseudograph<>(SupplierUtil.createIntegerSupplier(1), SupplierUtil.createDefaultEdgeSupplier(), false);
		gen.generateGraph(graph);
		
		JsonObject equipmentJson=new JsonObject();
		JsonObject lineJson=new JsonObject();
		int counter=0;
		LinkedHashMap<String, String> uuids=new LinkedHashMap<>();
		String lineId="L000";
		JsonParser parser = new JsonParser();
		for (DefaultEdge edge: graph.edgeSet()) {
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
				PfdEquipment pfdEquipment=new PfdEquipment();
				pfdEquipment=equipmentService.preparePfdEquipment(pfdEquipment, listEquipments.get(keySet.get(Integer.parseInt(edges[0])-1)), sourceUuid, keySet.get(Integer.parseInt(edges[0])-1), "left");
				uuids.put(edges[0], sourceUuid);
				equipmentJson.add(sourceUuid, parser.parse(gson.toJson(pfdEquipment)).getAsJsonObject());
			}else {
				sourceUuid=uuids.get(edges[0]);
			}
			if(!uuids.containsKey(edges[1])) {
				PfdEquipment pfdEquipment=new PfdEquipment();
				pfdEquipment=equipmentService.preparePfdEquipment(pfdEquipment, listEquipments.get(keySet.get(Integer.parseInt(edges[1])-1)), targetUuid, keySet.get(Integer.parseInt(edges[1])-1), "right");
				uuids.put(edges[1], targetUuid);
				
				equipmentJson.add(targetUuid, parser.parse(gson.toJson(pfdEquipment)).getAsJsonObject());
			}else {
				targetUuid=uuids.get(edges[1]);
			}
			
			String lineUuid=UUID.randomUUID().toString();
			Line line=new Line();
			line.setId(lineId+counter);
			line=lineService.preparePfdLine(line, lineUuid, sourceSide, targetSide, sourceUuid, targetUuid);
			lineJson.add(lineUuid,  parser.parse(gson.toJson(line)).getAsJsonObject());
			
		}
		logger.info("Graph is :: {} ",graph);	
		jsonObject.add(JsonFields.EQUIPMENT.getValue(), equipmentJson);
		jsonObject.add(JsonFields.LINES.getValue(), lineJson);
		return jsonObject;
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
