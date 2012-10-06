package org.hgahlot.sa.parse;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hgahlot.sa.util.MapUtil;

public class ExtractedRulesAnalyzer {
	private static final String LABELED_RULES_FILE = "data/sentiment/relExtLabel1_1535.csv";
	private static final String RANKED_DEP_RULES = "data/sentiment/rankedDepRules1_1535.csv";
	private static final String RANKED_TREE_RULES = "data/sentiment/rankedTreeRules1_1535.csv";
	
	public static void main(String a[]){
		ExtractedRulesAnalyzer era = new ExtractedRulesAnalyzer();
		era.process();
	}
	
	public void process(){
		HashMap<String, Integer> depRules = new HashMap<String, Integer>();
		HashMap<String, Integer> treeRules = new HashMap<String, Integer>();
		
		HashMap<String, Integer> relatedDepRules = new HashMap<String, Integer>();
		HashMap<String, Integer> relatedTreeRules = new HashMap<String, Integer>();
		
		BufferedReader br;
		try{
			br = new BufferedReader(new FileReader(LABELED_RULES_FILE));
			while(br.ready()){
				String line = br.readLine();
				String tokens[] = line.split("\\|");
				if(tokens.length != 7){
					continue;
				}
				String depRule = tokens[5];
				Integer related = Integer.parseInt(tokens[6]);
				
				if(depRules.containsKey(depRule)){
					depRules.put(depRule, depRules.get(depRule)+1);
					relatedDepRules.put(depRule, relatedDepRules.get(depRule) + related);
				} else {
					depRules.put(depRule, 1);
					relatedDepRules.put(depRule, related);
				}
				
				String treeRule = tokens[4];
				if(treeRules.containsKey(treeRule)){
					treeRules.put(treeRule, treeRules.get(treeRule)+1);
					relatedTreeRules.put(treeRule, relatedTreeRules.get(treeRule) + related);
				} else {
					treeRules.put(treeRule, 1);
					relatedTreeRules.put(treeRule, related);
				}
			}
			br.close();
			
			Map<String, Integer> sortedDepRules = MapUtil.sortByValue(depRules);
			Map<String, Integer> sortedTreeRules = MapUtil.sortByValue(treeRules);
			
			PrintWriter depPw = new PrintWriter(RANKED_DEP_RULES);
			PrintWriter treePw = new PrintWriter(RANKED_TREE_RULES);
			
			Iterator<String> depItr = sortedDepRules.keySet().iterator();
			Iterator<String> treeItr = sortedTreeRules.keySet().iterator();
			
			while(depItr.hasNext()){
				String key = depItr.next();
				Integer total = sortedDepRules.get(key);
				depPw.println(key+"|"+total+"|"+relatedDepRules.get(key)+"|"+ (total-relatedDepRules.get(key)));
			}
			depPw.close();
			
			while(treeItr.hasNext()){
				String key = treeItr.next();
				Integer total = sortedTreeRules.get(key);
				treePw.println(key+"|"+total+"|"+relatedTreeRules.get(key)+"|"+ (total-relatedTreeRules.get(key)));
			}
			treePw.close();
			
		} catch(FileNotFoundException fe){
			fe.printStackTrace();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	

}
