package com.finder.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.finder.BadDataException;
import com.finder.enumer.Keyword;
import com.finder.graph.Node;

public class Json {

	/**
	 * based on given data, generates output
	 * @param returnCode
	 * @param msg
	 * @return
	 */
	public  static String composeMsg(int returnCode, String msg) {
		return "HTTP Response Code: " + returnCode + "\n" +
				"Response: {\"msg\": \"" + msg + "\"}";
	}
	
	/**
	 * list all nodes in json format
	 * device type and name are printed by default
	 * if isDetail is true, strength and adjacent nodes are also printed
	 * @param nodes
	 * @param isDetail
	 * @return
	 */
	public static String composeDeviceList(List<Node> nodes, boolean isDetail) {
		StringBuffer sb = new StringBuffer("");
		
		sb.append("HTTP Response Code: 200\n");
		sb.append("Response:\n{\n");
		
		boolean isFirstNode = true;
		
		for (Node node : nodes) {
			
			
			Map<String, String> wordMap = new LinkedHashMap<>();
			wordMap.put("type", "'" + node.getNodeType().toString());
			wordMap.put("name", node.getNodeName());
			
			if(isDetail) {
				wordMap.put("strength", node.getStrength() + "");
				StringBuffer neiSb = new StringBuffer("[");
				
				for (Node neiNode : node.getNeighbours().values()) {
					neiSb.append(neiSb.equals("[") ? "" : ", ");
					neiSb.append("'" + neiNode.getNodeName() + "'");
				}
				
				neiSb.append("]");
				wordMap.put("neighbours", neiSb.toString());
			}
			
			

			sb.append(String.format("%s\t{\n", 
					isFirstNode ? "" : ",\n"));
			isFirstNode = false;
			
			boolean isFirstData = true;
			
			for (String key : wordMap.keySet()) {
				sb.append(isFirstData ? "" : ",\n");
				sb.append(String.format("\t\t'%s' : '%s'", key, wordMap.get(key)));
				isFirstData = false;
			}
			
			
			sb.append("\n\t}\n");
		}
		
		sb.append("}");
		return sb.toString();
	}
	

	/**
	 * based on the given input, generate a parsed data
	 * 
	 * Here are the data placed in output map
	 * 
	 * _COMMAND = Command enum type
	 * _OPERAND = list of operands (delimited by / in input str)
	 * _OPERAND_PARAM = key value pair map
	 * 
	 * other json name value pair (placed after header) are also placed in this map
	 * 
	 * @param inputList
	 * @return
	 * @throws BadDataException 
	 */
	public  Map<String, Object> parse(List<String> inputList) throws BadDataException {
		
		if(inputList == null || inputList.size() <= 0) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Empty input");
		}
		
		Map<String, Object> dataMap = new HashMap<>();
		int lineNum = 0;
		
		String firstLine = inputList.get(lineNum);
		
		if(firstLine == null || "".equals(firstLine.trim())) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Invalid command");
		}
		
		int spacePos = firstLine.indexOf(' ');
		
		if(spacePos < 0) {
			dataMap.put(Keyword._COMMAND.toString(), firstLine.trim());
			return dataMap;
		}
		
		String cmdStr = firstLine.substring(0, spacePos).trim();
		
		if(cmdStr == null || "".equals(cmdStr.trim())) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Invalid command");
		}
		dataMap.put(Keyword._COMMAND.toString(), cmdStr.trim());
		
		// System.out.println("cmdStr=" + cmdStr + " in the map=" + dataMap.get(Keyword._COMMAND.toString()) + " firstLine=" + firstLine);
		
		parseOperand(dataMap, firstLine.substring(spacePos + 1));
		
		lineNum++;
		
		if(lineNum >= inputList.size()) {
			return dataMap;
		}
		
		String header = inputList.get(lineNum);
		if(header == null || !header.matches("^\\s*content-type\\s*:\\s*application/json\\s*$")) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Error in header - " + header);
		}
		
		lineNum++;
		
		if(lineNum >= inputList.size()) {
			return dataMap;
		}
		
		String emptyLine = inputList.get(lineNum);
		if(emptyLine == null || !"".equals(emptyLine.trim())) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Error expected empty line after  header");
		}
		
		// merge the rest of the lines to get the json data part
		String jsonStr = "";
		for(int i = lineNum+1; i < inputList.size(); i++) {
			jsonStr += inputList.get(i);
		}
		
		parseJsonData(dataMap, jsonStr);
		
		return dataMap;
	}

	/**
	 * the json data is the last part of the input.
	 * store those data into dataMap
	 * 
	 * json may be in this format 
	 * 
	 * {"source" : "A1", "targets" : ["A2", "A3"]}
	 * 
	 * 
	 * @param dataMap
	 * @param jsonStr
	 * @throws BadDataException 
	 */
	private  void parseJsonData(Map<String, Object> dataMap, String jsonStr) throws BadDataException {
		
		if(jsonStr == null)
			return;
		
		// dangerous as replaces all {
		// we are ok as we have only one object in all requests
		jsonStr  = jsonStr.replace('{', ' ');
		jsonStr  = jsonStr.replace('}', ' ');
		
		
		// observation - there are either one key value pair or two
		// using this info, we split the string
		
		while(!"".equals(jsonStr.trim())) {
			int colonPos = jsonStr.indexOf(':');
			if(colonPos <= 0) {
				throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Invalid json data");
			}
			
			String name = stripQuotes(jsonStr.substring(0,  colonPos)).trim();
			if(name == null || "".equals(name)) {
				throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Invalid json data.  Name empty");
			}
			
			if(colonPos+1 >= jsonStr.length()) {
				throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Invalid json data. No value given for '" + name + "'");
			}
			
			jsonStr = jsonStr.substring(colonPos + 1);
			int squareStartPos = jsonStr.indexOf('[');
			
			List<String> jsonDataList = new ArrayList<>();
			int commaPos = jsonStr.indexOf(',');
			
			// regular singular data
			if(squareStartPos < 0 ||   // if there is no square box
					(commaPos >= 0 && commaPos < squareStartPos) || // if there is comma and it is ahead of square box
					(commaPos < 0 && squareStartPos < 0)  // if both comma and squares are missing
					) {
								
				// end of the data list
				if(commaPos < 0) {
					jsonDataList.add(stripQuotes(jsonStr));
				} else {
					jsonDataList.add(stripQuotes(jsonStr.substring(0,  commaPos)));
				}
			} 
			else
			{
				// deal with array of values inside square brackets
				int squareEndPos = jsonStr.indexOf(']');
				if(squareEndPos < 0) {
					throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Invalid json data. missing ] ");
				}
				
				String dataMala = jsonStr.substring(squareStartPos + 1, squareEndPos);
				String[] words = dataMala.split(",");
				for(String word : words) {
					jsonDataList.add(stripQuotes(word));
				}
				
				commaPos = jsonStr.indexOf(',', squareEndPos + 1);
			}
			
			
			
			dataMap.put(name, jsonDataList);
			
			if(commaPos < 0 || commaPos >= jsonStr.length()) {
				jsonStr = "";
			} else {
				jsonStr = jsonStr.substring(commaPos + 1);
			}
		}
		
	}

	/**
	 * if the string has double quotes, remove them
	 * @param substring
	 * @return
	 */
	private  String stripQuotes(String str) {
		if(str == null)
			return null;
		
		return str.replace("\"", "");
	}

	/**
	 * extract operand data, operand parameters 
	 * data could be like this :
	 * 
	 * /devices
	 * /info-routes?from=A1&to=A2
	 * /devices/A1/strength
	 * 
	 * @param dataMap
	 * @param substring
	 * @throws BadDataException 
	 */
	private  void parseOperand(Map<String, Object> dataMap, String givenString) throws BadDataException {
		if(givenString == null) {
			return;
		}
		
		String[] words = givenString.split("/");
		if(words.length <= 1) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Invalid command parameter - " + givenString);
		}
		
		List<String> wordList = new ArrayList<>(Arrays.asList(words));
		if("".equals(wordList.get(0).trim())) {
			wordList.remove(0);
		}
		
		String lastWord = new String(wordList.get(wordList.size() - 1));
		if(lastWord == null || "".equals(lastWord.trim())) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Invalid command parameter.  Last param missing - " + givenString);
		}
		
		// check if last word has key value pair
		int questionPos = lastWord.indexOf('?');
		if(questionPos == 0) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Invalid command parameter.  param missing before question - " + givenString);
		}
		
		if(questionPos + 1 >= lastWord.length()) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Invalid command parameter.  param missing after question - " + givenString);
		}
		
		if(questionPos > 0) {
			// the last word should have only the part before the question
			wordList.set(wordList.size() - 1, lastWord.substring(0, questionPos));
			
			// extract operand param
			Map<String, String> operandParamMap = new HashMap<>();
			
			String[] keyValues = lastWord.substring(questionPos + 1).split("&");
			
			for(String keyValue : keyValues) {
				if(keyValue == null || "".equals(keyValue.trim())) {
					throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Invalid command parameter.  key value missing - " + givenString);
				}
				
				String[] tokens = keyValue.split("=");
				if(tokens[0] == null || tokens[0].trim().equals("") ||
					tokens[1] == null || tokens[1].trim().equals("")) {
					throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Invalid command parameter.  tokens missing - " + givenString);
				}
				
				operandParamMap.put(tokens[0], tokens[1]);
			}
			
			dataMap.put(Keyword._OPERAND_PARAM.toString(), operandParamMap);
		}
		
		
		dataMap.put(Keyword._OPERAND.toString(), wordList);
	}



}
