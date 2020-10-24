package com.finder.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.servlet.http.HttpServletResponse;

import com.finder.BadDataException;
import com.finder.enumer.NodeType;

/**
 * Handles a set of devices.
 * 
 * @author jbalaji
 *
 */
public class Graph {

	private static Graph graph;

	Map<String, Node> nodeMap = new LinkedHashMap<>();

	private Graph() {

	}

	public static Graph getInstance() {
		if (graph == null) {
			graph = new Graph();
		}

		return graph;
	}

	/**
	 * if needed, wipe off the graph data
	 */
	public static void reset() {
		graph = null;
	}

	public boolean isNodeExist(String nodeName) {
		return nodeMap.containsKey(nodeName);
	}

	/**
	 * place the given node into the graph error thrown in case of invalid or if
	 * node already exist
	 * 
	 * @param node
	 * @return success message
	 * @throws BadDataException
	 */
	public String addNode(Node node) throws BadDataException {
		if (node == null) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "Empty node cannot be added");
		}

		if (isNodeExist(node.getNodeName())) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST,
					"Device '" + node.getNodeName().trim() + "' already exists");
		}

		nodeMap.put(node.getNodeName(), node);

		return "Successfully added " + node.getNodeName();
	}

	/**
	 * returns all the nodes available in the graph
	 * 
	 * @return
	 */
	public List<Node> fetchAllNodes() {
		List<Node> nodes = new ArrayList<>(nodeMap.values());
		return nodes;
	}

	public Node fetchNode(String nodeName) {
		return nodeMap.get(nodeName);
	}

	/**
	 * find route between two nodes the strength should be monitored
	 * 
	 * @param fromNode
	 * @param toNode
	 * @return
	 * @throws BadDataException
	 */
	/*
	 * public Stack<Node> findRoute(Node fromNode, Node toNode) throws
	 * BadDataException { if (fromNode == null) { throw new
	 * BadDataException(HttpServletResponse.SC_BAD_REQUEST, "from Node is null"); }
	 * 
	 * if (toNode == null) { throw new
	 * BadDataException(HttpServletResponse.SC_BAD_REQUEST, "to node is null"); }
	 * 
	 * Stack<Node> stack = new Stack<>(); Map<String, Boolean> visited = new
	 * HashMap<>();
	 * 
	 * int strength = fromNode.getStrength(); stack.push(fromNode);
	 * visited.put(fromNode.getNodeName(), true);
	 * 
	 * // from and two are same nodes if
	 * (fromNode.getNodeName().equals(toNode.getNodeName())) { stack.push(toNode);
	 * return stack; }
	 * 
	 * while (stack.size() > 0) { Node curNode = stack.peek();
	 * 
	 * if (curNode == null) { stack.pop(); continue; }
	 * 
	 * // if strength is too weak, backtrack if (strength <= 0) { continue; }
	 * 
	 * // found the route. push things into stack and return it if
	 * (curNode.isNeighbour(toNode)) { stack.push(toNode); return stack; }
	 * 
	 * strength--; if(curNode.getNodeType() == NodeType.REPEATER) strength *= 2;
	 * 
	 * Map<String, Node> neighbourNodes = curNode.getNeighbours();
	 * 
	 * for() }
	 * 
	 * return null; }
	 */

	/**
	 * recursive logic to find route between two nodes the strength should be monitored
	 * returns true if path found
	 * 
	 * @param fromNode
	 * @param toNode
	 * @param strength
	 * @param shortestStack
	 * @param transientStack 
	 * @param visited
	 * @throws BadDataException
	 */
	public boolean findRoute(Node fromNode, Node toNode, int strength, Stack<Node> shortestStack, Stack<Node> transientStack, Map<String, Boolean> visited) throws BadDataException {
		if (fromNode == null) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "from Node is null");
		}

		if (toNode == null) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "to node is null");
		}

		// if strength is too weak, backtrack
		if (strength <= 0) {
			return false;
		}
		
		transientStack.push(fromNode);
		visited.put(fromNode.getNodeName(), true);


		if(fromNode.getNodeType() == NodeType.REPEATER)
			strength *= 2;

		


		// found the destination or from and to are same node
		if(fromNode.getNodeName().equals(toNode.getNodeName()) || fromNode.isNeighbour(toNode)) {
			transientStack.push(toNode);
			
			// copy trasientStack into shortestStack, if the current stack size is small
			if(shortestStack.size() <= 0 || shortestStack.size() > transientStack.size()) {
				shortestStack.clear();
				shortestStack.addAll(transientStack);
			}
			
			
			transientStack.pop(); // remove toNode
			transientStack.pop(); // remove fromNode
			visited.remove(fromNode.getNodeName());
			
			return true;
		}


		Map<String, Node> neighbourNodes = fromNode.getNeighbours();

		for(Node node : neighbourNodes.values()) {
			if(visited.containsKey(node.getNodeName()))
				continue;
			
			findRoute(node, toNode, 
					strength - (fromNode.getNodeType() == NodeType.REPEATER || 
							node.getNodeType() == NodeType.REPEATER ? 0 : 1), 
					shortestStack, transientStack, visited);
		}

		// back track - remove fromNode
		transientStack.pop();
		visited.remove(fromNode.getNodeName());
		
		return shortestStack.size() > 0;
	}
	
	/**
	 * find route between two nodes
	 * 
	 * @param fromNode
	 * @param toNode
	 * @return 
	 * @return
	 * @throws BadDataException
	 */
	public Stack<Node> findRoute(Node fromNode, Node toNode) throws BadDataException {
		if (fromNode == null) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "from Node is null");
		}

		if (toNode == null) {
			throw new BadDataException(HttpServletResponse.SC_BAD_REQUEST, "to node is null");
		}
		
		Stack<Node> shortestStack = new Stack<>();
		Stack<Node> transientStack = new Stack<>();
		Map<String, Boolean> visited = new HashMap<>();
		
		return findRoute(fromNode, toNode, fromNode.getStrength(), shortestStack, transientStack, visited) ? shortestStack : null;
	}
}
