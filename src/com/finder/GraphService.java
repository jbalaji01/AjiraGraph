package com.finder;

import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.servlet.http.HttpServletResponse;

import com.finder.enumer.CommandType;
import com.finder.enumer.Keyword;
import com.finder.enumer.NodeType;
import com.finder.enumer.Operand;
import com.finder.graph.Graph;
import com.finder.graph.Node;
import com.finder.json.Json;

/**
 * logic of the application resides here all commands from user are executed
 * 
 * @author jbalaji
 *
 */
public class GraphService {
	/**
	 * The input command is parsed, commands and data are extracted appropriate
	 * module called.
	 * 
	 * @param inputList
	 * @return
	 * @throws BadDataException
	 */
	public String processCommand(List<String> inputList) throws BadDataException {
		Json json = new Json();
		Map<String, Object> dataMap = json.parse(inputList);
		printMap(dataMap);

		CommandType commandType = fetchCommandType(dataMap);
		Operand operand = fetchOperand(dataMap);

//		System.out.println("Given command is : " + commandType);
		switch (commandType) {
		case CREATE:

			if (operand == null) {
				throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Invalid command null ");
			}

			if (operand == Operand.devices)
				return createDevices(dataMap);

			if (operand == Operand.connections)
				return createConnections(dataMap);

			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Invalid command '" + operand + "'");

		case FETCH:
			return fetch(operand, dataMap);

		case MODIFY:
			return modify(dataMap, operand);

		case RESET:
			return reset();
		}

		throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Invalid command '" + commandType + "'");
	}

	/**
	 * add neighbours to given nodes
	 * since it is a undirected graph, add two connections for source and target pair
	 * 
	 * @param dataMap
	 * @return
	 * @throws BadDataException 
	 */
	private String createConnections(Map<String, Object> dataMap) throws BadDataException {
		
		Graph graph = Graph.getInstance();
		
		try {
			if(((List)dataMap.get("source")).get(0).toString().trim().equals("")) 
				throw new Exception();
		} catch (Exception e) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Invalid command, error in  source");
		}
		
		try {
			if(((List)dataMap.get("targets")).size() <= 0) 
				throw new Exception();
		} catch (Exception e) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Invalid command, error in  target");
		}
		
		
		String sourceStr = ((List)dataMap.get("source")).get(0).toString().trim();
		List<String> targetList = (List<String>)dataMap.get("targets");
		
		if(!graph.isNodeExist(sourceStr)) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Node '" + sourceStr + "' not found");
		}
		
		Node sourceNode = graph.fetchNode(sourceStr);
		
		if(sourceNode == null) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Node '" + sourceStr + "' is empty");
		}
		
		for (String targetStr : targetList) {
			if(targetStr == null || targetStr.trim().equals("")) {
				throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "target Node empty");
			}
			
			targetStr = targetStr.trim();
			
			if(sourceStr.equals(targetStr)) {
				throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Cannot connect device to itself");
			}
			
			if(!graph.isNodeExist(targetStr)) {
				throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Node '" + targetStr + "' not found");
			}
			
			Node targetNode = graph.fetchNode(targetStr);
			
			if(targetNode == null) {
				throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Node '" + targetStr + "' is null");
			}
			
			if(sourceNode.isNeighbour(targetNode)) {
				throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Devices are already connected");
			}
			
			sourceNode.addNeighbour(targetNode);;
			targetNode.addNeighbour(sourceNode);
		}
		
		return Json.composeMsg(HttpServletResponse.SC_OK, "Successfully connected");
	}

	/**
	 * add the given device into the graph
	 * 
	 * @param dataMap
	 * @return
	 * @throws Exception 
	 */
	private String createDevices(Map<String, Object> dataMap) throws BadDataException {
		Graph graph = Graph.getInstance();
		
		if (
				(!dataMap.containsKey("type")) || 
				(dataMap.get("type") == null) ||
				(!(dataMap.get("type") instanceof List)) ||
				(((List)dataMap.get("type")).get(0) == null) ||
				(((List)dataMap.get("type")).get(0).toString().trim().equals("")) ||
				(!dataMap.containsKey("name")) || 
				(dataMap.get("name") == null) ||
				(!(dataMap.get("name") instanceof List)) ||
				(((List)dataMap.get("name")).get(0) == null) ||
				(((List)dataMap.get("name")).get(0).toString().trim().equals("")) 
		) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST,
					"Invalid command - valid device type and name required.");
		}

		String nodeName = ((List)dataMap.get("name")).get(0).toString().trim();
		String nodeTypeStr = ((List)dataMap.get("type")).get(0).toString().trim();
		NodeType nodeType = null;
		try {
			nodeType = NodeType.valueOf(nodeTypeStr);
		} catch (Exception e) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST,
					"Type '" + nodeTypeStr + "' is not supported");
		}

		if (graph.isNodeExist(nodeName)) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Device '" + nodeName + "' already exists");
		}
		
		Node newNode = new Node(nodeName, nodeType);
		String result = graph.addNode(newNode);

		return Json.composeMsg(HttpServletResponse.SC_OK, result);
	}

	/**
	 * fetch device list or route path
	 * 
	 * @param operand
	 * @param dataMap
	 * @return
	 * @throws BadDataException
	 */
	private String fetch(Operand operand, Map<String, Object> dataMap) throws BadDataException {
		if (operand == null) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Invalid command - null ");
		}

		Graph graph = Graph.getInstance();
		List<String> wordList = (List<String>) dataMap.get(Keyword._OPERAND.toString());

		switch (operand) {
		case devices:
			List<Node> nodes = graph.fetchAllNodes();
			return Json.composeDeviceList(nodes, wordList.size() > 1 && "detail".equals(wordList.get(1)));
			
		case infoRoutes:
			return fetchRoute(dataMap);
		}

		throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST,
				"Invalid command.  fetch failed with '" + operand + "'");

	}

	

	private void printMap(Map<String, Object> dataMap) {
		if (dataMap == null) {
			System.out.println("dataMap is null");
			return;
		}

		System.out.println("dataMap has following data");
		for (String key : dataMap.keySet()) {
			System.out.print(key + " => ");

			Object value = dataMap.get(key);
			if (value == null) {
				System.out.println("value is null");
				continue;
			}

			if (value instanceof String) {
				System.out.println(value);
				continue;
			}

			if (value instanceof List) {
				System.out.print("[");
				for (Object o : (List) value) {
					System.out.print(o.toString() + ", ");
				}
				System.out.println("]");
				continue;
			}

			if (value instanceof Map) {
				System.out.print("{");
				for (Object o : ((Map) value).keySet()) {
					System.out.print(o.toString() + " => " + ((Map) value).get(o).toString() + ", ");
				}
				System.out.println("}");
				continue;
			}
		}
		
		System.out.println("");
	}

	/**
	 * if operand is present, return it if not return null, no error thrown
	 * 
	 * @param dataMap
	 * @return
	 * @throws BadDataException
	 */
	private Operand fetchOperand(Map<String, Object> dataMap) throws BadDataException {
		if (!dataMap.containsKey(Keyword._OPERAND.toString())) {
			return null;
		}

		List<String> wordList;

		try {
			wordList = (List<String>) dataMap.get(Keyword._OPERAND.toString());
		} catch (Exception e) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Invalid command");
		}

		if (wordList == null || wordList.size() <= 0 || wordList.get(0) == null) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Invalid command. It is empty");
		}

//		System.out.println("operand received is - " + wordList.get(0));

		try {
			Operand operand = "info-routes".equals(wordList.get(0)) ? Operand.infoRoutes
					: Operand.valueOf(wordList.get(0));
			return operand;
		} catch (Exception ex) {

		}

		return null;
	}

	/**
	 * extract commandType from the data map. If not found, throws error
	 * 
	 * @param dataMap
	 * @return
	 * @throws BadDataException
	 */
	private CommandType fetchCommandType(Map<String, Object> dataMap) throws BadDataException {
		CommandType commandType = null;
		try {
			commandType = CommandType.valueOf((String) dataMap.get(Keyword._COMMAND.toString()));
		} catch (Exception e) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST,
					"Invalid command '" + dataMap.get(Keyword._COMMAND.toString()) + "'");
		}

		if (commandType == null) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST,
					"Invalid command '" + dataMap.get(Keyword._COMMAND.toString()) + "'");
		}
		return commandType;
	}

	/**
	 * erases all the data in the memory
	 * 
	 * @return
	 */
	private String reset() {
		Graph.reset();
		return Json.composeMsg(HttpServletResponse.SC_OK, "Cleaned the graph data");
	}

	/**
	 * change the strength of the given node
	 * 
	 * @param dataMap
	 * @param operand
	 * @return
	 * @throws BadDataException
	 */
	private String modify(Map<String, Object> dataMap, Operand operand) throws BadDataException {
		
		Graph graph = Graph.getInstance();

		List<String> wordList = (List<String>) dataMap.get(Keyword._OPERAND.toString());

		if (wordList.size() < 3) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST,
					"Insufficient paramaters in '" + dataMap.get(Keyword._COMMAND.toString()) + "'");
		}

//		String msg = "Got command " + CommandType.MODIFY.toString() + ", operand = " + operand;
//		msg += " change " + wordList.get(2) + " on " + wordList.get(1);
//
//		System.out.println(msg);
		
		if(operand == null || operand != Operand.devices) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "'devices' command missing");
		}
		
		if(!"strength".equals(wordList.get(2))) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Last portion of command should be 'strength'");
		}
		
		String nodeStr = wordList.get(1);
		
		if(nodeStr == null || nodeStr.trim().equals("")) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Incorrect device name");
		}

		if(!graph.isNodeExist(nodeStr)) {
			throw new BadDataException(HttpServletResponse.SC_NOT_FOUND, "Device Not Found");
		}
		
		Node node = graph.fetchNode(nodeStr);
		
		if(node == null) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Device is null");
		}
		
		if(node.getNodeType() == NodeType.REPEATER) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Repeater cannot set strength");
		}
		
		try {
			if(((List)dataMap.get("value")).get(0).toString().trim().equals("")) 
				throw new Exception();
			
			String valueStr = ((List)dataMap.get("value")).get(0).toString().trim();
			int value = Integer.parseInt(valueStr);
			
			if(value < 0)
				throw new Exception();
			
			node.setStrength(value);
			
		} catch (Exception e) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "value should be a positive integer");
		}
		
		return Json.composeMsg(HttpServletResponse.SC_OK, "Successfully defined strength");
	}
	
	/**
	 * find the route between given two nodes
	 * @param dataMap
	 * @return
	 * @throws BadDataException 
	 */
	private String fetchRoute(Map<String, Object> dataMap) throws BadDataException {
		Graph graph = Graph.getInstance();

		Map<String, String> operandParamMap  = (Map<String, String>) dataMap.get(Keyword._OPERAND_PARAM.toString());
	
		if(operandParamMap == null) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Invalid Request - Missing route parameters.");
		}
		
		if(!operandParamMap.containsKey("from") || 
				operandParamMap.get("from") == null ||
				"".equals(operandParamMap.get("from").trim())) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Invalid Request - Missing 'from' route parameters.");
		}
		
		if(!operandParamMap.containsKey("to") || 
				operandParamMap.get("to") == null ||
				"".equals(operandParamMap.get("to").trim())) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Invalid Request - Missing 'to' route parameters.");
		}
		
		String fromStr = operandParamMap.get("from").trim();
		String toStr = operandParamMap.get("to").trim();
		
		if(!graph.isNodeExist(fromStr)) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Node '" + fromStr + "' not found");
		}
		
		if(!graph.isNodeExist(toStr)) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Node '" + toStr + "' not found");
		}
		
		Node fromNode = graph.fetchNode(fromStr);
		Node toNode = graph.fetchNode(toStr);
		
		if(fromNode == null) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Node '" + fromStr + "' is null");
		}
		
		if(toNode == null) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Node '" + toStr + "' is null");
		}
		
		if(fromNode.getNodeType() == NodeType.REPEATER ||
				toNode.getNodeType() == NodeType.REPEATER) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Route cannot be calculated with repeater");
		}
		
//		if(fromStr.equals(toStr)) {
//			return Json.composeMsg(HttpServletResponse.SC_OK, "Route is " + fromStr + "->" + toStr);
//		}
		
		Stack<Node> routeNodes = graph.findRoute(fromNode, toNode);
		
		if(routeNodes == null || routeNodes.size() <= 0) {
			throw new BadDataException(HttpServletResponse.SC_NOT_FOUND, "Route not found");
		}
		
		StringBuffer sb = new StringBuffer("");
		boolean isFirst = true;
		for(Node node : routeNodes) {
			if(node == null) {
				throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Invalid route node - null");
			}
			
			sb.append(isFirst ? "" : "->");
			sb.append(node.getNodeName());
			isFirst = false;
		}
		
		return Json.composeMsg(HttpServletResponse.SC_OK, "Route is " + sb.toString());
		
	}
}
