/**
 * 
 */
package org.hgahlot.sa.parse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;

/**
 * @author hgahlot
 *
 */
public class DependencyParseUtils {
	
	public ArrayList<Tree> getLowestNP(Tree root){
		ArrayList<Tree> terminalNPList = new ArrayList<Tree>();
		Iterator<Tree> allNodes = root.iterator();
		allNodes.next();
		while(allNodes.hasNext()){
			boolean isTerminalNode = true;
			Tree node = allNodes.next();
			if(node.isPreTerminal() || node.isLeaf())
				continue;
			if(!node.value().equals("NP")){
				continue;
			} else {
				Iterator<Tree> npNodeItr = node.iterator();
				npNodeItr.next();
				while(npNodeItr.hasNext()){
					Tree npNode = npNodeItr.next();
					if(npNode.isPreTerminal() || npNode.isLeaf())
						continue;
					if(npNode.value().equals("NP")){
						isTerminalNode = false;
					}
				}
				if(isTerminalNode == true){
					terminalNPList.add(node);
				}
			}
		}
		
		return terminalNPList;
	}
	
	
	public String getRelation(int firstIndex, int secondIndex, List<TypedDependency> tdl){
		Iterator<TypedDependency> tdItr = tdl.iterator();
		while(tdItr.hasNext()){
			TypedDependency td = tdItr.next();
			int govInd = td.gov().index();
			int depInd = td.dep().index();
			if(((govInd == firstIndex) && (depInd == secondIndex)) || ((depInd == firstIndex) && (govInd == secondIndex))){
				return td.reln().getShortName();
			}
		}
		return null;
	}
	
	public HashMap<Integer, GrammaticalRelation> getDepedencies(int index, List<TypedDependency> tdl){
		Iterator<TypedDependency> tdItr = tdl.iterator();
		HashMap<Integer, GrammaticalRelation> depList = new HashMap<Integer, GrammaticalRelation>();
		while(tdItr.hasNext()){
			TypedDependency td = tdItr.next();
			int govInd = td.gov().index();
			int depInd = td.dep().index();
			GrammaticalRelation reln = td.reln();
			if((govInd == index)){
				depList.put(depInd, reln);
			} else if(depInd == index){
				depList.put(govInd, reln);
			}
		}
		return depList;
	}

	public HashMap<Integer, ArrayList<Integer>> getDepGraph(List<TypedDependency> tdl){
		HashMap<Integer, ArrayList<Integer>> graph = new HashMap<Integer, ArrayList<Integer>>();
		Iterator<TypedDependency> tdItr = tdl.iterator();
		while(tdItr.hasNext()){
			TypedDependency td = tdItr.next();
			int govInd = td.gov().index();
			int depInd = td.dep().index();

			if(!graph.containsKey(govInd)){
				ArrayList<Integer> newList = new ArrayList<Integer>();
				newList.add(depInd);
				graph.put(govInd, newList);
			} else {
				ArrayList<Integer> adjVert = graph.get(govInd);
				adjVert.add(depInd);
				graph.put(govInd, adjVert);
			}
			if(!graph.containsKey(depInd)){
				ArrayList<Integer> newList = new ArrayList<Integer>();
				newList.add(govInd);
				graph.put(depInd, newList);
			} else {
				ArrayList<Integer> adjVert = graph.get(depInd);
				adjVert.add(govInd);
				graph.put(depInd, adjVert);
			}
		}

		return graph;
	}

	
	//This method is another way of implementing the getShortestPath() method, probably 
	//more efficient but has lesser exception handling. Hence, use getShortestPath().
	public List<Integer> getPath(HashMap<Integer, ArrayList<Integer>> graph, int start, 
			int end){
	    //Initialization.
	    Map<Integer, Integer> nextNodeMap = new HashMap<Integer, Integer>();
	    Integer currentNode = start;

	    //Queue
	    Queue<Integer> queue = new LinkedList<Integer>();
	    queue.add(currentNode);

	    /*
	     * HashSet is fast for add and lookup, if configured properly.
	     */
	    Set<Integer> visitedNodes = new HashSet<Integer>();
	    visitedNodes.add(currentNode);

	    //Search.
	    while (!queue.isEmpty()) {
	        currentNode = queue.remove();
	        
	        if (currentNode.equals(end)) {
	            break;
	        } else {
	        	List<Integer> adjNodes = graph.get(currentNode);
	        	if(adjNodes == null){
	        		break;
	        	}
	            for (Integer nextNode : adjNodes) {
	            	if (!visitedNodes.contains(nextNode)) {
	                    queue.add(nextNode);
	                    visitedNodes.add(nextNode);

	                    //Look up of next node instead of previous.
	                    nextNodeMap.put(currentNode, nextNode);
	                }
	            }
	        }
	    }

	    //If all nodes are explored and the destination node hasn't been found.
	    if (!currentNode.equals(end)) {
	        //throw new RuntimeException("No feasible path.");
	    	//System.out.println("No feasible path between entity and sentiment.");
	    	return null;
	    }

	    //Reconstruct path. No need to reverse.
	    List<Integer> directions = new LinkedList<Integer>();
	    for (Integer node = start; node != null; node = nextNodeMap.get(node)) {
	        directions.add(node);
	    }

	    return directions;
	}
	
	
	public List<Integer> getShortestPath(HashMap<Integer, ArrayList<Integer>> graph, int start, 
			int end){
		try {
			HashSet<Integer> visited = new HashSet<Integer>();
			List<Integer> visitedNodes = new ArrayList<Integer>();
			List<Integer> preceding = new ArrayList<Integer>();

			Queue<Integer> queue = new LinkedList<Integer>();
			queue.add(start);
			visitedNodes.add(start);
			preceding.add(0);

			int count = -1;
			int matchedNodeIndex = -1;
			while((queue.size() != 0) || (visited.size() != graph.size())){
				Integer node = queue.poll();

				count++;
				if(visited.contains(node)){
					continue;
				}
				visited.add(node);

				if(node == end){
					matchedNodeIndex = count;
					break;
				}
				ArrayList<Integer> adjNodes = graph.get(node);
				if(adjNodes != null){
					queue.addAll(adjNodes);

					for(Integer adjNode: adjNodes){
						visitedNodes.add(adjNode);
						preceding.add(count);
					}
				} else {
					//System.out.println("No feasible path between entity and sentiment.");
			    	return null;
				}
			}

			ArrayList<Integer> path = new ArrayList<Integer>();
			int index = matchedNodeIndex;
			if(index == -1){
				//System.out.println("No feasible path between entity and sentiment.");
		    	return null;
			}
			path.add(visitedNodes.get(index));
			while(index != 0){
				path.add(visitedNodes.get(preceding.get(index)));
				index = preceding.get(index);
			}

			int size = path.size();
			ArrayList<Integer> reversePath = new ArrayList<Integer>();
			for(int i=size-1; i>=0; i--){
				reversePath.add(path.get(i));
			}

			return reversePath;
		} catch(Exception e){
			//System.out.println("No feasible path between entity and sentiment.");
			e.printStackTrace();
			return null;
		}
	}
}
