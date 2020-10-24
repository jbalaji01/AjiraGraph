package com.finder.graph;

import java.util.LinkedHashMap;
import java.util.Map;

import com.finder.enumer.NodeType;

/**
 * a single device, which can be computer or repeater
 * strength of nodes are stored here.
 * 
 * each node may have many neighbouring nodes
 * 
 * @author jbalaji
 *
 */
public class Node {
	private String nodeName;
	private NodeType nodeType;
	
	private int strength = 5; // default strength
		
	private Map<String, Node> neighbours = new LinkedHashMap<>();
	
	public Node(String nodeName, NodeType nodeType) {
		this.nodeName = nodeName;
		this.nodeType = nodeType;
	}
	
	public void addNeighbour(Node neighbour) {
		neighbours.put(neighbour.getNodeName(), neighbour);
	}

	/**
	 * check if given node is a neighbour of this node
	 * @param targetNode
	 * @return
	 */
	public boolean isNeighbour(Node targetNode) {
		if(targetNode == null || targetNode.getNodeName() == null)
			return false;
		
		return neighbours.containsKey(targetNode.getNodeName());
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public int getStrength() {
		return strength;
	}

	public void setStrength(int strength) {
		this.strength = strength;
	}


	public NodeType getNodeType() {
		return nodeType;
	}

	public void setNodeType(NodeType nodeType) {
		this.nodeType = nodeType;
	}

	public Map<String, Node> getNeighbours() {
		return neighbours;
	}

	public void setNeighbours(Map<String, Node> neighbours) {
		this.neighbours = neighbours;
	}


}
