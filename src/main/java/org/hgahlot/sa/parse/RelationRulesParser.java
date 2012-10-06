/**
 * 
 */
package org.hgahlot.sa.parse;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * @author hgahlot
 *
 */
public class RelationRulesParser {
	private static final String DEFAULT_RULES_FILE = "data/sentiment/entity_sentiment_relation.rules";
	private static HashSet<String> RULES = new HashSet<String>();

	private List<String> tempRules = new ArrayList<String>();
	
	public static void main(String a[]){
		RelationRulesParser esrp = new RelationRulesParser(DEFAULT_RULES_FILE);
		String string = "ENT -> dep -> INT_ENT -> prep_of -> INT_ENT -> amod -> SENTI";
		System.out.println(esrp.matches(string));
	}

	public RelationRulesParser(String rulesFilePath){
		if(rulesFilePath == null){
			rulesFilePath = DEFAULT_RULES_FILE;
		}
		RULES = parse(rulesFilePath);
	}

	/**
	 * Parses the rules file, reads rules not starting with a '#'
	 * and populates them in the RULES hash set.
	 * @param rulesFile
	 * @return
	 */
	private HashSet<String> parse(String rulesFile){
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(rulesFile));
			while(br.ready()){
				String rule = br.readLine();
				if((rule != null) && (!rule.startsWith("#"))){
					if(!rule.isEmpty()){
						RULES.add(rule.trim());
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return RULES;
	}


	public boolean matches(String string){
		normalizeRules();
		
		String stringTokens[] = string.split("\\s+");
		
		for(String rule: tempRules){
			String ruleTokens[] = rule.split("\\s+");
			if(ruleTokens.length != stringTokens.length){
				continue;
			}
			int idx = 0;
			while((idx<ruleTokens.length) && (idx<stringTokens.length)){
				//if it is a relation
				if(ruleTokens[idx].contains("|")){
					//iterate over all the '|' delimited relations
					//and check if the current string token is one of them
					String ruleRelnTokens[] = ruleTokens[idx].split("\\|");
					boolean relnMatched = false;
					for(int i=0; i<ruleRelnTokens.length; i++){
						if(stringTokens[idx].equals(ruleRelnTokens[i])){
							idx++;
							relnMatched = true;
							continue;
						}
					}
					if(!relnMatched){//go to the next rule by breaking out of 
						//this loop over current rule's tokens
						break;
					} else {//else continue with the current rule
						idx++;
					}
				} else {//current rule token is not a relation
					if(!ruleTokens[idx].equals(stringTokens[idx])){
						//rule token and string token do not match, so break 
						//out and go to the next rule
						break;
					} else {//continue with the current rule
						idx++;
					}
				}
			}
			if(idx == ruleTokens.length){
				//this means that the above loop has run fully without breaking 
				//out at any place and every token has matched.
				return true;
			}
		}
		//all rules have been compared and no match was found
		return false;
	}
	
	/**
	 * Reads all rules in the RULES HashSet and converts them to a 
	 * flat form by interpreting their wild cards. Example, this rule:
	 * ENT -> ( nsubj|dobj|nn|dep|amod|adjmod -> INT_ENT -> ) 2* nsubj|dobj|nn|dep|amod|adjmod -> SENTI
	 * is converted to the following rules:
	 * ENT -> nsubj|dobj|nn|dep|amod|adjmod -> SENTI
	 * ENT -> nsubj|dobj|nn|dep|amod|adjmod -> INT_ENT -> nsubj|dobj|nn|dep|amod|adjmod -> SENTI
	 * ENT -> nsubj|dobj|nn|dep|amod|adjmod -> INT_ENT -> nsubj|dobj|nn|dep|amod|adjmod -> INT_ENT -> nsubj|dobj|nn|dep|amod|adjmod -> SENTI
	 */
	private void normalizeRules(){
		//add the original rules to the tempRules ArrayList
		for(String rule: RULES){
			tempRules.add(rule);
		}
		int idx = 0;
		//the size of tempRules is varying
		int totalRules = tempRules.size();
		while(idx < totalRules){
			String listRule = tempRules.get(idx);
			String listRuleTokens[] = listRule.split("\\s+");
			int braceStartIdx = 0;
			int i=0;
			for(; i<listRuleTokens.length; i++){//iterate over all the space separated tokens in rule
				String token = listRuleTokens[i];
				if(token.equals("(")){
					braceStartIdx = i;//remember the index of the last brace start '('
					continue;
				} else if(token.equals(")")){//this means that one (most probably the most 
					//internal) brace has ended and hence we can process it by reading the 
					//wildcard occurring just after it
					int braceEndIdx = i;
					if(i<listRuleTokens.length-1){
						String nextToken = listRuleTokens[i+1];
						String preTokens = "";//tokens before braceStart
						String postTokens = "";//tokens after braceEnd
						String braceTokens = "";//tokens within the braces
						int j=0;
						for(; j<braceStartIdx-1; j++){
							preTokens += listRuleTokens[j]+" ";
						}
						preTokens += listRuleTokens[j];
						int k=braceEndIdx+2;
						for(; k<listRuleTokens.length-1; k++){
							postTokens += listRuleTokens[k]+" ";
						}
						postTokens += listRuleTokens[k];
						int l=braceStartIdx+1;
						for(; l<braceEndIdx-1; l++){
							braceTokens += listRuleTokens[l]+" ";
						}
						braceTokens += listRuleTokens[l];
						
						if(nextToken.equals("?")){//then add the braceTokens 0 and 1 times and
												//add these rules to the tempRules list
							tempRules.add(preTokens+" "+postTokens);
							tempRules.add(preTokens+" "+ braceTokens +" "+postTokens);
							break;
						} else if(nextToken.matches("\\d\\*")){
							//read the digit mentioned in the wildcard,
							//iterate as many times and add the braceTokens
							//as many times (0 or more)
							int digit = Integer.parseInt(nextToken.charAt(0)+"");
							for(int m=0; m<=digit; m++){
								String midString = "";
								for(int n=0; n<m; n++){
									midString += braceTokens+" ";
								}
								tempRules.add(preTokens+" "+midString+postTokens);
							}
							break;
						} else if(nextToken.matches("\\d\\+")){
							//read the digit mentioned in the wildcard,
							//iterate as many times and add the braceTokens
							//as many times (1 or more)
							int digit = Integer.parseInt(nextToken.charAt(0)+"");
							for(int m=1; m<=digit; m++){
								String midString = "";
								for(int n=0; n<m; n++){
									midString += braceTokens+" ";
								}
								tempRules.add(preTokens+" "+midString+postTokens);
							}
							break;
						}
					}
				}
			}
			
			if(i==listRuleTokens.length){
				//no new rule was added (break wasn't
				//executed even once) hence preserve
				//this rule and move on to the next one
				idx++;
			} else {
				//break was called somewhere and hence a new child rule was added,
				//so remove the current rule but do not increment idx since this 
				//index will now be occupied by the next rule in the list
				tempRules.remove(idx);
			}
			totalRules = tempRules.size();
		}
	}
	
}
