package org.hgahlot.sa.parse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.hgahlot.sa.util.FileUtil;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.objectbank.TokenizerFactory;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;

public class RuleExtractorTrainingGenerator {
	private static final String TEXT_DIR = "data/sentiment/iphoneTestData";
	private static final String OPENNLP_SENT_MODEL = "data/models/opennlp/EnglishSD.bin.gz";
	private static final String STANFORD_PARSER_MODEL = "data/models/stanford/englishPCFG.ser.gz";
	private static final String SENTIMENT_DICT = "data/sentiment/AFINN-111.txt";
	private static final String REL_EXT_TRAIN_FILE = "data/sentiment/relExtTraining.txt";

	public static void main(String a[]){
		RuleExtractorTrainingGenerator retg = new RuleExtractorTrainingGenerator();
		retg.process();
	}

	public void process(){
		HashSet<String> sentimentPhrases = getSentimentPhrases();
		LexicalizedParser lp = new LexicalizedParser(STANFORD_PARSER_MODEL);
		PrintWriter pw = null;
		
		try {
			pw = new PrintWriter(REL_EXT_TRAIN_FILE);

			File[] files = (new File(TEXT_DIR)).listFiles();
			int fileCount = -1;
			for(File file: files){
				String fileName = file.getName();
				fileCount++;
				System.out.println("File processed: "+fileCount+" out of: "+files.length);
				StringBuffer content = new StringBuffer();
				BufferedReader br = new BufferedReader(new FileReader(file));
				while(br.ready()){
					content.append(br.readLine());
				}
				br.close();

				String sentences[] = getSentences(content.toString());

				int i=0;
				for(String sentence: sentences){
					i++;
					System.out.println("Processing file: "+fileName+"; Sentence number: "+i);
					ArrayList<RuleExtInstance> reiList = parseSentence(lp, sentence, sentimentPhrases, fileName);
					
					for(RuleExtInstance rei: reiList){
						//System.out.println(rei.getSentence()+"|"+rei.getEntity()+"|"+rei.getSentiment());
						if((rei.getEntity() == null) || (rei.getSentiment() == null))
							continue;
						pw.println(rei.getDocId().replaceAll("\\|", ";")+"|"+rei.getSentence().replaceAll("\\|", ";")+"|"+
								rei.getEntity().replaceAll("\\|", ";")+"|"+rei.getSentiment().replaceAll("\\|", ";")+"|"+
								rei.getTreePath().replaceAll("\\|", ";")+"|"+rei.getDepPath().replaceAll("\\|", ";")+"|"+"0");
					}
				}


			}
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception ex){
			ex.printStackTrace();
			pw.close();
		}
	}

	public HashSet<String> getSentimentPhrases(){
		BufferedReader br;
		HashSet<String> sentimentPhrases = new HashSet<String>();
		try {
			br = new BufferedReader(new FileReader(SENTIMENT_DICT));
			while(br.ready()){
				String line = br.readLine();
				String tokens[] = line.split("\\t");
				if(tokens.length < 2)
					continue;

				String sentPhrase = "";
				for(int i=0; i<tokens.length-1; i++){
					sentPhrase += tokens[i]+" ";
				}
				sentimentPhrases.add(sentPhrase.substring(0, sentPhrase.length()-1));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sentimentPhrases;
	}

	public String[] getSentences(String content){
		return FileUtil.getSentences(content, OPENNLP_SENT_MODEL);
	}

	public ArrayList<RuleExtInstance> parseSentence(LexicalizedParser lp, String sentence, 
			HashSet<String> sentimentPhrases, String docId){
		TokenizerFactory<CoreLabel> tokenizerFactory = 
			PTBTokenizer.factory(new CoreLabelTokenFactory(), "");
		List<CoreLabel> rawWords2 = 
			tokenizerFactory.getTokenizer(new StringReader(sentence)).tokenize();
		Tree parse = lp.apply(rawWords2);

		TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
		GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
		List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();

		HashMap<Tree, Integer> entities = new HashMap<Tree, Integer>();
		HashMap<Tree, Integer> sentiments = new HashMap<Tree, Integer>();

		Iterator<Tree> itr = parse.iterator();
		ArrayList<Tree> treeNodes = new ArrayList<Tree>();

		while(itr.hasNext()){
			Tree node = itr.next();
			treeNodes.add(node);
		}

		HashMap<Tree, Integer> posTagNodes = new HashMap<Tree, Integer>();
		HashMap<Integer, Tree> reverseMap = new HashMap<Integer, Tree>();
		int index = 1;
		for(Tree node: treeNodes){
			if(node.isPreTerminal()){
				posTagNodes.put(node, index);
				reverseMap.put(index++, node);
			}
		}

		Iterator<Tree> itr2 = parse.iterator();
		while(itr2.hasNext()){
			Tree node = itr2.next();
			treeNodes.add(node);

			Tree children[] = node.children();

			if(node.isPrePreTerminal() && node.value().equals("NP")){
				Tree lastChild = children[children.length-1]; //most probably a noun
				entities.put(lastChild, posTagNodes.get(lastChild));
			}

			if(node.isPreTerminal()){
				if(sentimentPhrases.contains(children[0].value())){
					sentiments.put(node, posTagNodes.get(node));
				}
			}
		}

		//System.out.println(sentence);
		//System.out.println(tdl);

		ArrayList<RuleExtInstance> ruleExtList = new ArrayList<RuleExtInstance>();

		for(Tree entity: entities.keySet()){
			for(Tree sentiment: sentiments.keySet()){

				//System.out.print("entity: "+entity+" | sentiment: "+sentiment+" => ");
				List<Tree> path = parse.pathNodeToNode(entity, sentiment);
				StringBuffer pathString = new StringBuffer();
				for(int k=0; k<path.size()-1; k++){
					String pathVal = path.get(k).value();
					//System.out.print(pathVal+" -> ");
					pathString.append(pathVal+" -> ");
				}
				//System.out.println(path.get(path.size()-1).value());
				pathString.append(path.get(path.size()-1).value());

				Integer entityIndexInSentence = posTagNodes.get(entity);
				Integer sentimentIndexInSentence = posTagNodes.get(sentiment);

				HashMap<Integer, ArrayList<Integer>> graph = getDepGraph(tdl);

				List<Integer> shortestPath = getShortestPath(graph, entityIndexInSentence, 
						sentimentIndexInSentence);
				
				if(shortestPath == null)
					continue;
				
				//combine it with POS tags
				StringBuffer depPathString = new StringBuffer();
				int l=0;
				for(; l<shortestPath.size()-1; l++){
					String preRelation = "";
					String postRelation = "";
					String posTag = reverseMap.get(shortestPath.get(l)).value();
					if(l%2 != 0){
						preRelation = getRelation(shortestPath.get(l-1), shortestPath.get(l), tdl);
						postRelation = getRelation(shortestPath.get(l), shortestPath.get(l+1), tdl);
						//System.out.print(" -> "+preRelation+" -> "+posTag+" -> "+postRelation+" -> ");
						depPathString.append(" -> "+preRelation+" -> "+posTag+" -> "+postRelation+" -> ");
					} else {
						//System.out.print(posTag);
						depPathString.append(posTag);
					}
				}

				String posTag = reverseMap.get(shortestPath.get(l)).value();
				if(l%2 == 0){
					//System.out.println(posTag);
					depPathString.append(posTag);
				} else {
					String preReln = getRelation(shortestPath.get(l-1), shortestPath.get(l), tdl);
					//System.out.println(" -> "+preReln+" -> "+posTag);
					depPathString.append(" -> "+preReln+" -> "+posTag);
				}

				RuleExtInstance ruleExtInstance = new RuleExtInstance();
				ruleExtInstance.setDocId(docId);
				ruleExtInstance.setSentence(sentence);
				ruleExtInstance.setEntity(entity.getChild(0).value());
				ruleExtInstance.setSentiment(sentiment.getChild(0).value());
				ruleExtInstance.setTreePath(pathString.toString());
				ruleExtInstance.setDepPath(depPathString.toString());

				ruleExtList.add(ruleExtInstance);
			}
		}
		return ruleExtList;
	}

	private String getRelation(int firstIndex, int secondIndex, List<TypedDependency> tdl){
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

	private HashMap<Integer, ArrayList<Integer>> getDepGraph(List<TypedDependency> tdl){
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
				}
			}
	
			ArrayList<Integer> path = new ArrayList<Integer>();
			int index = matchedNodeIndex;
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
			e.printStackTrace();
			return null;
		}
	}


	private class RuleExtInstance {
		private String docId;
		private String sentence;
		private String entity;
		private String sentiment;
		private String treePath;
		private String depPath;
		private String label;

		public RuleExtInstance() {}

		public RuleExtInstance(String docId, String sentence, String entity,
				String sentiment, String treePath, String depPath, String label) {
			super();
			this.docId = docId;
			this.sentence = sentence;
			this.entity = entity;
			this.sentiment = sentiment;
			this.treePath = treePath;
			this.depPath = depPath;
			this.label = label;
		}
		public String getDocId() {
			return docId;
		}
		public void setDocId(String docId) {
			this.docId = docId;
		}
		public String getSentence() {
			return sentence;
		}
		public void setSentence(String sentence) {
			this.sentence = sentence;
		}
		public String getEntity() {
			return entity;
		}
		public void setEntity(String entity) {
			this.entity = entity;
		}
		public String getSentiment() {
			return sentiment;
		}
		public void setSentiment(String sentiment) {
			this.sentiment = sentiment;
		}
		public String getTreePath() {
			return treePath;
		}
		public void setTreePath(String treePath) {
			this.treePath = treePath;
		}
		public String getDepPath() {
			return depPath;
		}
		public void setDepPath(String depPath) {
			this.depPath = depPath;
		}
		public String getLabel() {
			return label;
		}
		public void setLabel(String label) {
			this.label = label;
		}
	}
}
