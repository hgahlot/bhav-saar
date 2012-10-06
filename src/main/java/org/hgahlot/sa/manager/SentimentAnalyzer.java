package org.hgahlot.sa.manager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jsoup.Jsoup;

import cc.mallet.classify.Classifier;
import cc.mallet.types.Instance;
import cc.mallet.types.Label;
import cc.mallet.types.Labeling;
import org.hgahlot.sa.bean.AnalyzedSentimentInstance;
import org.hgahlot.sa.classification.mallet.MalletClassifier;
import org.hgahlot.sa.coref.bart.BARTCorefResolver;
import org.hgahlot.sa.coref.bart.BARTCorefText;
import org.hgahlot.sa.coref.bart.BARTCorefText.BARTCorefPhrase;
import org.hgahlot.sa.coref.bart.BARTCorefText.BARTSentence;
import org.hgahlot.sa.coref.bart.BARTCorefText.BARTWord;
import org.hgahlot.sa.parse.DependencyParseUtils;
import org.hgahlot.sa.parse.RelationRulesParser;
import org.hgahlot.sa.util.FileUtil;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.objectbank.TokenizerFactory;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;

public class SentimentAnalyzer {
	//public static final String TEXT_DIR = "data/sentiment/test3_iphone";
	public static String TEXT_DIR = "data/sentiment/test1";
	//public static final String SENTIMENT_ANALYSIS_OUT_FILE = "data/sentiment/sentences_test3Iphone.txt";
	public static String SENTIMENT_ANALYSIS_OUT_FILE = "data/sentiment/test1_sentiOut.txt";
	public static String STANFORD_PARSER_MODEL = "data/models/stanford/englishPCFG.ser.gz";
	public static String ONLP_SD_MODEL="data/models/opennlp/EnglishSD.bin.gz";
	public static String SENTIMENT_DICT = "data/sentiFiles/AFINN-111_modified.txt";
	public static String EMPHATICS_DICT = "data/sentiFiles/emphaticsDict.txt";
	public static String ENT_SENTI_RULES_FILE = "data/sentiFiles/entity_sentiment_relation.rules";
	public static String NEGATION_RULES_FILE = "data/sentiFiles/negation.rules";

	//public static final String ENTITY = "windows 8";
	public static String ENTITIES[] = new String[]{"windows 8", "microsoft", "bill gates"};
	public static String SENTENCE_LEVEL_MODEL = "data/models/sentiment/sentence-polarity_withNeg.model.80_70_82_88";

	public static boolean resolveCoref = false;

	private DependencyParseUtils dparseUtil;
	private GrammaticalStructureFactory gsf;
	private HashMap<String, Float> sentimentPhrases;
	private HashMap<String, Float> emphaticPhrases;
	private LexicalizedParser lp;
	private Map<String, ArrayList<BARTCorefPhrase>> reverseCorefMap;

	private Logger log;

	public SentimentAnalyzer(){
		log = Logger.getLogger("SentimentAnalyzer");
		log.setLevel(Level.ALL);
		log.info("Initializing...");

		dparseUtil = new DependencyParseUtils();
		TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		gsf = tlp.grammaticalStructureFactory();
		lp = new LexicalizedParser(STANFORD_PARSER_MODEL);

		sentimentPhrases = getSentimentPhrases();
		emphaticPhrases = getEmphaticPhrases();
	}

	public static void main(String a[]){
		SentimentAnalyzer sa = new SentimentAnalyzer();
		sa.process("text", "I do not really hate windows 8.", "data/sentiment/sa.out");
	}

	public void process(String inputType, String input, String outFile){

		PrintWriter pw = null;

		try {
			pw = new PrintWriter(new FileWriter(outFile), true);

			File[] files = null;
			if(inputType.equals("dir")){
				files = (new File(input)).listFiles();
				log.info("Input dir: "+input);
				log.info("Total files: "+files.length+"\n");
				int fileCount = 0;

				//iterate over each file
				for(File file: files){
					String fileName = file.getName();
					fileCount++;

					//fetch content from file
					String fileContent = FileUtil.getFileContent(file);
					Double documentSentiScore = 0d;
					try {
						documentSentiScore = processText(fileName, fileContent);
					} catch (IllegalArgumentException e){
						e.printStackTrace();
						documentSentiScore = 1000d;
					}

					log.info("Files processed: "+fileCount+" out of: "+files.length+"\n");
					printOutput(pw, fileName, documentSentiScore);
				}
			} else if(inputType.equals("file")){
				log.info("Input file: "+input);
				//fetch content from file
				String fileContent = FileUtil.getFileContent(new File(input));
				Double documentSentiScore = 0d;
				try {
					documentSentiScore = processText(input, fileContent);
				} catch (IllegalArgumentException e){
					e.printStackTrace();
					documentSentiScore = 1000d;
				}
				printOutput(pw, input, documentSentiScore);
				
			} else {//the input is just some text
				//assign a dummy id
				String textId = "Text";
				Double documentSentiScore = 0d;
				try {
					documentSentiScore = processText(textId, input);
				} catch (IllegalArgumentException e){
					documentSentiScore = 1000d;
				}
				printOutput(pw, textId, documentSentiScore);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception ex){
			ex.printStackTrace();
			pw.close();
		} finally {
			pw.close();
		}
	}
	
	
	private void printOutput(PrintWriter pw, String textId, Double textScore){
		String sentimentString = "neutral";
		if(textScore == 1000d){
			sentimentString = "document too large";
		} else if(textScore>0){
			sentimentString = "positive";
		} else if(textScore<0){
			sentimentString = "negative";
		}
		pw.println(textId+"|"+textScore+"|"+sentimentString);
	}
	
	public Double processText(String textId, String text){
		//parse out html encoded chars (e.g. &nbsp;, &amp; etc.)
		text = Jsoup.parse(text).text();

		Double docSentiVal = 0d;
		String sentences[] = null;

		List<String> entityList = Arrays.asList(ENTITIES);

		log.info("Processing file: "+textId);

		if(!resolveCoref){//if co-references are not to be resolved
			sentences = FileUtil.getSentences(text, ONLP_SD_MODEL);//fetch sentences using OpenNLP model

			if(sentences.length > 70){
				throw new IllegalArgumentException(textId +" is too large to analyze. Number of sentences = "+sentences.length+". Skipping!");
			}

			int sentIdx = 0;
			for(String sentence: sentences){
				log.info("Processing file: "+textId+"; Sentence number: "+sentIdx);
				String tokens[] = sentence.split("\\s+");
				if(tokens.length > 70){//the Stanford parser would fail for long sentences, so skip them
					log.info("Sentence too long to process. Skipping.\n");
					sentIdx++;
					continue;
				}

				//check if this sentence contains our target entities
				boolean sentHasEntity = false;
				String lowerCaseSent = sentence.toLowerCase();
				for(String entityString: ENTITIES){
					if(lowerCaseSent.contains(entityString)){
						sentHasEntity = true;
					}
				}

				if(!sentHasEntity){
					//move ahead only if this sentence contains one of the target entities else continue
					log.info("Sentence: "+sentence);
					log.info("Sentence "+sentIdx+" does not contain entities: "+entityList+"\n");
					sentIdx++;
					continue;
				}

				HashSet<AnalyzedSentimentInstance> asiList = predictSentiment(sentence, textId, sentIdx, ENTITIES);
				Double sentiValue = getSentimentForSentence(asiList, textId, sentence, sentIdx);
				docSentiVal += sentiValue;
				sentIdx++;
			}

		} else {//co-references need to be resolved
			sentences = FileUtil.getSentences(text, ONLP_SD_MODEL);
			if(sentences.length > 70){
				throw new IllegalArgumentException(textId +" is too large to analyze. Number of sentences = "+sentences.length+". Skipping!");
			}

			BARTCorefResolver bcr = new BARTCorefResolver();//instantiate BARTCorefResolver
			log.info("Resolving coreferences...");
			//run BART coref resolver on file content
			String bartPreProcessedText = bcr.getBartPreProcessedText(text);
			//read BART coref resolved text into BARTCorefText object
			BARTCorefText bartCorefText = bcr.getBartProcessedText(bartPreProcessedText);

			//populate a map where key=<sentId-wordId> and value=<All co-references for this word>
			reverseCorefMap = bartCorefText.getReverseCorefMap();
			log.info("Coreferences resolved.\n");

			//fetch all sentences recognized by BART, thereby ignoring OpenNLP 
			//sentences (need to make this more efficient)
			ArrayList<BARTSentence> bartSentenceList = bartCorefText.getSentenceList();

			int sentIdx = 0;
			for(BARTSentence bartSentence: bartSentenceList){
				log.info("Processing file: "+textId+"; Sentence number: "+sentIdx);

				ArrayList<BARTWord> sentenceWords = bartSentence.getWords();
				if(sentenceWords.size() > 70){//ignore too long sentences
					log.info("Sentence: "+bartSentence.toString());
					log.info("Sentence too long to process. Skipping.\n");
					sentIdx++;
					continue;
				}

				//replace all coreferences of our target entities in a sentence with
				//their base form
				ArrayList<String> entityCorefedSentence = resolveEntityCorefInSentence(bartSentence, sentIdx, ENTITIES);

				//recreate sentence string from the ArrayList
				StringBuffer sentenceString = new StringBuffer();
				for(String token: entityCorefedSentence){
					sentenceString.append(token+" ");
				}
				String sentence = sentenceString.substring(0, sentenceString.length()-1).toString();
				String lowerCaseSent = sentence.toLowerCase();

				//check if this sentence contains our target entities
				boolean sentHasEntity = false;
				for(String entityString: ENTITIES){
					if(lowerCaseSent.contains(entityString)){
						sentHasEntity = true;
					}
				}

				if(!sentHasEntity){
					//move ahead only if this sentence contains one of the target entities else continue
					sentIdx++;
					log.info("Sentence: "+sentence);
					log.info("Sentence "+sentIdx+" does not contain entities: "+entityList+"\n");
					continue;
				}

				List<Word> stanfordWordList = new ArrayList<Word>();
				for(String token: entityCorefedSentence){
					Word stanfordWord = new Word(token);
					stanfordWordList.add(stanfordWord);
				}

				HashSet<AnalyzedSentimentInstance> asiList = predictSentiment(stanfordWordList, textId, sentIdx, ENTITIES);
				Double sentiValue = getSentimentForSentence(asiList, textId, sentence, sentIdx);
				docSentiVal += sentiValue;
				sentIdx++;
			}
		}


		log.info("Overall document sentiment: "+docSentiVal+"\n\n");
		return docSentiVal;
	}


	/**
	 * Checks the sentiments obtained using dependency parse method and if 
	 * no sentiment was present, uses the sentence-level model to predict
	 * the sentiment for the sentence and returns a sentiment score for the same.
	 * Explanation of scoring logic:
	 * Scores are combined from two sources - the dependency parse (DP) method and the 
	 * sentence-level trained model (SLTM). 
	 * DP method => The scores returned by the DP method lie 
	 * in the range 1 to 5 (positive) and -1 to -5 (negative), without boosting. If 
	 * a boost factor is applied then scores may grow more since the boost values 
	 * are added to them. The boost values range from -3 to +3. So, the scores 
	 * returned by DP method can range really high depending upon the boosting 
	 * tokens in the sentence but usually range between 1 to 5 (+ or -).  
	 * 
	 * SLTM method => The scores returned by the model are basically the confidence 
	 * score of the three classes predicted and lie between 0 and 1. For the best 
	 * predicted class the score may vary from 0.34 to 1. We use the score of the 
	 * best predicted class, subtract 0.34 from it and multiply the polarity (0 or
	 * 1 or -1):
	 * SLTM Score = (Best value - 0.34) * (0 or 1 or -1)
	 * This converts the score to a scale of 0 to 0.66.
	 * 
	 * We now need to convert the score from DP method to a similar scale. Hence, 
	 * we divide the DP score by 5 and multiply by 0.66 neglecting the boosted 
	 * scores (which may cause the score to be more than 0.66) i.e.;
	 * DP Score = (Score/5) * 0.66
	 * 
	 * Thus both methods return scores on similar scales and are now comparable.
	 * 
	 * @param asiList
	 * @param fileName
	 * @param sentence
	 * @param sentIdx
	 * @return sentiment score for the sentence using either dependency parse or model
	 */
	public Double getSentimentForSentence(HashSet<AnalyzedSentimentInstance> asiList, 
			String fileName, String sentence, int sentIdx){
		//check if any sentiment was found for the target entity using the dependency
		//parse method.
		Double sentiScoreForSentence = 0.0;
		boolean hasSentiment = false;
		for(AnalyzedSentimentInstance asi: asiList){
			sentiScoreForSentence += asi.getScore();
			log.info("entity: "+asi.getEntity()+" sentiment: "+asi.getScore());
			if((asi.getSentiment() != null) || (!asi.getSentiment().equals(""))){
				hasSentiment = true;
			}
		}
		
		if(hasSentiment){
			//this is the final score for this sentence
			log.info("Sentence: "+sentence);
			log.info("Sentiment: "+sentiScoreForSentence+"\n");
			sentiScoreForSentence = (sentiScoreForSentence/5.0)*0.66;
		} else {
			log.info("No sentiment found using dependency parse. Resorting " +
			"to sentence level model.");
			//no sentiment was found using dependency parse
			//use the sentence-level model to predict the sentiment
			Labeling labeling = predictSentimentUsingModel(SENTENCE_LEVEL_MODEL, fileName, sentIdx, 
					sentence);
			Label label = labeling.getLabelAtRank(0);
			double value = labeling.getValueAtRank(0);
			String senti = "";
			if(label.toString().equals("1")){
				senti = "positive";
				//sentiScoreForSentence = 1d;
				sentiScoreForSentence = (value - 0.34) * 1;
			} else if(label.toString().equals("0")){
				senti = "neutral";
				//sentiScoreForSentence = 0d;
				sentiScoreForSentence = (value - 0.34) * 0;
			} else {
				senti = "negative";
				//sentiScoreForSentence = -1d;
				sentiScoreForSentence = (value - 0.34) * -1;
			}
			log.info("Sentence no.: "+sentIdx);
			log.info("Sentence: "+sentence);
			log.info("Sentiment :"+senti+"\n");
		}
		return sentiScoreForSentence;
	}


	/**
	 * Uses the sentence-level trained model to predict sentiment for 
	 * a sentence
	 * @param modelFile
	 * @param fileName
	 * @param sentenceIdx
	 * @param sentence
	 * @return Labeling object which contains the sentiment prediction
	 */
	public Labeling predictSentimentUsingModel(String modelFile, String fileName, 
			int sentenceIdx, String sentence){
		MalletClassifier mc = new MalletClassifier();
		Labeling labeling = null;
		try {
			Classifier classifier = mc.loadClassifier(new File(modelFile));
			Instance instance = new Instance(sentence, "DUMMY", fileName+"-"+sentenceIdx, "");
			labeling = mc.predictLabel(classifier, instance);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return labeling;
	}

	/**
	 * Replaces co-references to target entities with the actual
	 * target entities
	 * @param bartSentence
	 * @param sentIdx
	 * @param entities
	 * @return List of tokens representing the coref resolved original sentence
	 */
	public ArrayList<String> resolveEntityCorefInSentence(BARTSentence bartSentence, int sentIdx, 
			String targetEntities[]){
		List<String> targetEntitiesList = Arrays.asList(targetEntities);
		ArrayList<BARTWord> bartWordList = bartSentence.getWords();
		ArrayList<String> sentence = new ArrayList<String>();

		int entityIdx = 0;
		boolean isTargetEntityInCorefPhrase = false;
		ArrayList<BARTCorefPhrase> prevTokenCorefPhrases = new ArrayList<BARTCorefPhrase>();

		for(BARTWord bartWord: bartWordList){
			String revCorefMapKey = sentIdx+"-"+entityIdx;
			String corefBartWord = bartWord.getWordText();
			ArrayList<BARTCorefPhrase> corefPhrasesForEntity = reverseCorefMap.get(revCorefMapKey);

			if((corefPhrasesForEntity != null)){
				//check to see if this was multi-worded token and the 
				//first word of this token has already been resolved.
				//This can be checked by seeing if the previous coref 
				//phrase was same as the current coref phrase. If not, 
				//then the current token refers to a different coref phrase.
				if(corefPhrasesForEntity.equals(prevTokenCorefPhrases)){
					prevTokenCorefPhrases = corefPhrasesForEntity;
					entityIdx++;
					continue;
				}

				isTargetEntityInCorefPhrase = false;

				for(BARTCorefPhrase corefPhrase: corefPhrasesForEntity){
					String corefPhraseString = corefPhrase.toString();
					if(targetEntitiesList.contains(corefPhraseString.toLowerCase())){
						corefBartWord = corefPhraseString;
						isTargetEntityInCorefPhrase = true;
						break;
					}
				}
			}

			//to allow other coreferenced entities (not referencing target entity) to 
			//print without intervention by their coreferences
			if(!isTargetEntityInCorefPhrase){
				prevTokenCorefPhrases = null;
			} else {
				prevTokenCorefPhrases = corefPhrasesForEntity;
			}

			//so that this '|' can be used as a delimiter for 
			//printing this sentence with other information in 
			//csv format
			sentence.add(corefBartWord.replaceAll("\\|", ";"));
			entityIdx++;
		}

		return sentence;
	}


	/**
	 * Reads the file containing sentiment phrases and 
	 * populates them in a hash map
	 * @return
	 */
	public HashMap<String, Float> getSentimentPhrases(){
		BufferedReader br;
		HashMap<String, Float> sentimentPhrases = new HashMap<String, Float>();
		try {
			br = new BufferedReader(new FileReader(SENTIMENT_DICT));
			while(br.ready()){
				String line = br.readLine();
				String tokens[] = line.split("\\t");
				if(tokens.length != 2)
					continue;

				String sentPhrase = "";
				for(int i=0; i<tokens.length-1; i++){
					sentPhrase += tokens[i]+" ";
				}

				String scoreString = tokens[tokens.length - 1];
				Float score = new Float(0);
				if((scoreString != null)){
					if(!scoreString.isEmpty()){
						score = Float.parseFloat(scoreString);
					}
				}

				sentimentPhrases.put(sentPhrase.substring(0, sentPhrase.length()-1), score);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sentimentPhrases;
	}

	/**
	 * Reads the file containing emphatic phrases and 
	 * populates them in a hash map
	 * @return
	 */
	public HashMap<String, Float> getEmphaticPhrases(){
		BufferedReader br;
		HashMap<String, Float> emphaticPhrases = new HashMap<String, Float>();
		try {
			br = new BufferedReader(new FileReader(EMPHATICS_DICT));
			while(br.ready()){
				String line = br.readLine();
				String tokens[] = line.split("\\t");
				if(tokens.length != 2)
					continue;

				String emphPhrase = "";
				for(int i=0; i<tokens.length-1; i++){
					emphPhrase += tokens[i]+" ";
				}

				String scoreString = tokens[tokens.length - 1];
				Float score = new Float(0);
				if((scoreString != null)){
					if(!scoreString.isEmpty()){
						score = Float.parseFloat(scoreString);
					}
				}

				emphaticPhrases.put(emphPhrase.substring(0, emphPhrase.length()-1), score);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return emphaticPhrases;
	}



	public HashSet<AnalyzedSentimentInstance> predictSentiment(String sentence, String docId, 
			int sentenceIndex, String entities[]){
		TokenizerFactory<CoreLabel> tokenizerFactory = 
			PTBTokenizer.factory(new CoreLabelTokenFactory(), "");
		List<CoreLabel> rawWords2 = 
			tokenizerFactory.getTokenizer(new StringReader(sentence)).tokenize();
		Tree parse = lp.apply(rawWords2);

		return predictSentiment(parse, sentence.toString(), docId, sentenceIndex, entities);
	}

	public HashSet<AnalyzedSentimentInstance> predictSentiment(List<Word> sentence, String docId, 
			int sentenceIndex, String entities[]){

		Tree parse = lp.apply(sentence);
		return predictSentiment(parse, sentence.toString(), docId, sentenceIndex, entities);
	}


	private HashSet<AnalyzedSentimentInstance> predictSentiment(Tree sentenceParse, String sentence, String docId, 
			int sentenceIndex, String[] targetEntities){
		HashSet<AnalyzedSentimentInstance> analyzedSentimentList = new HashSet<AnalyzedSentimentInstance>();
		for(Tree node: sentenceParse){
			if(node.isLeaf()){
				node.setValue(node.value().toLowerCase());
			}
		}
		try {
			GrammaticalStructure gs = gsf.newGrammaticalStructure(sentenceParse);
			//List<TypedDependency> tdl = (List<TypedDependency>)gs.typedDependenciesCollapsedTree();
			List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();

			ArrayList<Tree> entities = new ArrayList<Tree>();
			ArrayList<Tree> sentiments = new ArrayList<Tree>();

			ArrayList<Integer> entityNodeNum = new ArrayList<Integer>();
			ArrayList<Integer> sentiNodeNum = new ArrayList<Integer>();

			ArrayList<String> entityPhrases = new ArrayList<String>();

			Iterator<Tree> itr = sentenceParse.iterator();
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

			//populate entities
			ArrayList<Tree> terminalNPList = dparseUtil.getLowestNP(sentenceParse);
			for(Tree terminalNP: terminalNPList){
				List<Tree> leaves = terminalNP.getLeaves();
				int size = leaves.size();
				ArrayList<Tree> localEntities = new ArrayList<Tree>();
				ArrayList<Integer> localEntityNodeNum = new ArrayList<Integer>();

				String thisEntity = new String();
				if(size > 0){
					thisEntity = leaves.get(size-1).value();
					Tree lastLeafParent = leaves.get(size-1).ancestor(1, sentenceParse);
					localEntities.add(lastLeafParent);
					localEntityNodeNum.add(posTagNodes.get(lastLeafParent));
				} else {
					continue;
				}
				for(int entInd=(size-2); entInd>=0; entInd--){
					thisEntity = leaves.get(entInd).value() + " " + thisEntity;
					Tree leafParent = leaves.get(entInd).ancestor(1, sentenceParse);
					localEntities.add(leafParent);
					localEntityNodeNum.add(posTagNodes.get(leafParent));
				}

				int localEntSize = localEntities.size();
				for(String targetEntity: targetEntities){
					if(thisEntity.endsWith(targetEntity) || thisEntity.endsWith(targetEntity+" 's")){
						entities.addAll(localEntities);
						entityNodeNum.addAll(localEntityNodeNum);
						for(int locEntIdx=0; locEntIdx<localEntSize; locEntIdx++){
							entityPhrases.add(thisEntity);//add the same entity phrase for
							//each index corresponding to parts of this entity
						}
					}
				}
				
			}
			
			//TODO: this may not work when there are more than one entity 
			//in the sentence and one of them is identified while the other 
			//is not due to separation in two nodes. So, the condition 
			//shouldn't actually be (entities == null).
			if((entities == null) || entities.isEmpty()){
				//this means that our target entity(ies)
				//do not occur in a single noun phrase 
				//but are rather separated into two 
				//consecutive noun phrases
				List<String> sentTokens = new ArrayList<String>();
				int tokensSize = posTagNodes.size();
				for(int tokenIdx=1; tokenIdx<=tokensSize; tokenIdx++){
					Tree posNode = reverseMap.get(tokenIdx);
					sentTokens.add(posNode.getChild(0).value());
				}
				
				for(String targetEntity: targetEntities){
					String targetEntityTokens[] = targetEntity.split("\\s+");
					int matchIndex = Collections.indexOfSubList(sentTokens, Arrays.asList(targetEntityTokens));
					if(matchIndex != -1){
						int matchEnd = targetEntityTokens.length + matchIndex;
						for(int matchInd=matchIndex+1; matchInd<=matchEnd; matchInd++){
							entities.add(reverseMap.get(matchInd));
							entityNodeNum.add(matchInd);
							entityPhrases.add(targetEntity);
						}
					}
				}
			}
			
			//populate sentiments
			Iterator<Tree> itr2 = sentenceParse.iterator();
			while(itr2.hasNext()){
				Tree node = itr2.next();
				treeNodes.add(node);

				Tree children[] = node.children();

				if(node.isPreTerminal()){
					if(sentimentPhrases.containsKey(children[0].value())){
						sentiments.add(node);
						sentiNodeNum.add(posTagNodes.get(node));
					}
				}
			}

			//log.info(sentence);
			//log.info(tdl);

			Float sentenceScore = 0f;

			//iterate over all entities and sentiments and figure out their relationship.
			//If their relationship satisfies certain rules then mark them as related and calculate
			//the score of the sentiment expressed towards the entity.
			int entityIndex = -1;
			for(Tree entity: entities){
				entityIndex++;

				for(Tree sentiment: sentiments){

					//System.out.print("entity: "+entity+" | sentiment: "+sentiment+" => ");
					List<Tree> path = sentenceParse.pathNodeToNode(entity, sentiment);
					StringBuffer pathString = new StringBuffer();
					for(int k=0; k<path.size()-1; k++){
						String pathVal = path.get(k).value();
						//System.out.print(pathVal+" -> ");
						pathString.append(pathVal+" -> ");
					}
					//log.info(path.get(path.size()-1).value());
					pathString.append(path.get(path.size()-1).value());

					Integer entityIndexInSentence = posTagNodes.get(entity);
					Integer sentimentIndexInSentence = posTagNodes.get(sentiment);

					if(entityIndexInSentence == sentimentIndexInSentence){
						continue;
					}

					HashMap<Integer, ArrayList<Integer>> graph = dparseUtil.getDepGraph(tdl);
					
					//get path from entity to sentiment
					List<Integer> shortestPath = dparseUtil.getShortestPath(graph, entityIndexInSentence, 
							sentimentIndexInSentence);
					
					if(shortestPath == null)
						continue;

					String depPath = createDepPathString(shortestPath, reverseMap, tdl);
					String treePath = pathString.toString();
					
					//format the path according to entity-sentiment rules
					String depPathInRuleFormat = formatPathForRules(depPath);
					
					
					RelationRulesParser esrp = new RelationRulesParser(ENT_SENTI_RULES_FILE);
					//					if((depPathTokens.length == 5) && (depPathTokens[2].equals("dobj") || depPathTokens[2].equals("nsubj")) || 
					//							depPathTokens[2].equals("amod")){//the first dependency relation in the path
					if(esrp.matches(depPathInRuleFormat)){
						//if((DEP_RULES.contains(depPath)) || (TREE_RULES.contains(treePath))){
						//this is a valid entity-sentiment pair
						AnalyzedSentimentInstance asi = new AnalyzedSentimentInstance();
						asi.setDocId(docId);
						asi.setSentence(sentence);
						asi.setEntity(entityPhrases.get(entityIndex));
						String sentimentString = sentiment.getChild(0).value();

						//analyze polarity and score by applying more rules
						Float score = sentimentPhrases.get(sentimentString);
						boolean isPolarityReversed = false;
						Float emphaticsBooster = 0.1f;
						HashMap<Integer, GrammaticalRelation> sentiDepList = dparseUtil.getDepedencies(sentimentIndexInSentence, tdl);
						HashMap<Integer, GrammaticalRelation> entDepList = dparseUtil.getDepedencies(entityIndexInSentence, tdl);
						
						
						//create path for negations
						int negRelnGov = -1;
						int negRelnDep = -1;
						for(TypedDependency td: tdl){
							if(td.reln().getShortName().equals("neg")){
								negRelnGov = td.gov().index();
								negRelnDep = td.dep().index();
							}
						}
						List<Integer> negGovToEntityPath = dparseUtil.getShortestPath(graph, negRelnGov, entityIndexInSentence);
						List<Integer> negGovToSentiPath = dparseUtil.getShortestPath(graph, negRelnGov, sentimentIndexInSentence);
						List<Integer> negDepToEntityPath = dparseUtil.getShortestPath(graph, negRelnDep, entityIndexInSentence);
						List<Integer> negDepToSentiPath = dparseUtil.getShortestPath(graph, negRelnDep, sentimentIndexInSentence);
						
						String negGovToEntPathInRule = formatPathForRules(createDepPathString(negGovToEntityPath, reverseMap, tdl));
						String negGovToSentiPathInRule = formatPathForRules(createDepPathString(negGovToSentiPath, reverseMap, tdl));
						String negDepToEntPathInRule = formatPathForRules(createDepPathString(negDepToEntityPath, reverseMap, tdl));
						String negDepToSentiPathInRule = formatPathForRules(createDepPathString(negDepToSentiPath, reverseMap, tdl));
						
						RelationRulesParser negRuleParser = new RelationRulesParser(NEGATION_RULES_FILE);
						if((negRuleParser.matches(negGovToEntPathInRule) || negRuleParser.matches(negGovToSentiPathInRule) 
								|| negRuleParser.matches(negDepToEntPathInRule) || negRuleParser.matches(negDepToSentiPathInRule))
								&& !isPolarityReversed){
							isPolarityReversed = true;
						}
						
						//more negation rules
						//check if the tokens surrounding the sentiment are negations
						int from = sentimentIndexInSentence - 3;
						int to = sentimentIndexInSentence + 3;
						for(int neighbourIndex=from; neighbourIndex<=to; neighbourIndex++){
							Tree neighbourNode = reverseMap.get(neighbourIndex);
							if(neighbourNode != null){
								if(neighbourNode.getChild(0).value().toLowerCase().matches("no|not|n\'t") 
										&& !isPolarityReversed){
									isPolarityReversed = true;
									break;
								}
							}
						}
						
						
						//emphatics rules
						for(Integer ind: sentiDepList.keySet()){
							if(ind == 0){
								continue;
							}
							
							//rule for emphatics
							Tree treeNode = reverseMap.get(ind);
							if(treeNode == null)
								continue;
							String depString = treeNode.getChild(0).value();
							if(emphaticPhrases.containsKey(depString)){
								String relation = sentiDepList.get(ind).getShortName();
								if(relation.equals("amod") || relation.equals("advmod") || 
										relation.equals("nn")){
									emphaticsBooster = emphaticPhrases.get(depString);
									score += emphaticsBooster;
									log.info("boosting score for: "+sentimentString+"\nmodifier: "+depString);
									sentimentString += " (boosted)";
								}
							}
						}

//						for(Integer ind: entDepList.keySet()){
//							if(entDepList.get(ind).getShortName().equals("neg") && !isPolarityReversed){
//								isPolarityReversed = true;
//								continue;
//							}
//							if(entDepList.get(ind).getShortName().matches("nsubj|dobj") && !isPolarityReversed){
//								HashMap<Integer, GrammaticalRelation> relList = 
//									dparseUtil.getDepedencies(ind, tdl);
//								for(Integer idx: relList.keySet()){
//									if(relList.get(idx).getShortName().equals("neg")){
//										isPolarityReversed = true;
//									}
//								}
//							}
//						}

						if(isPolarityReversed){
							score *= -1;
							score -= (emphaticsBooster + emphaticsBooster);
							sentimentString = "(negation) " + sentimentString;
						}

						asi.setSentiment(sentimentString);
						asi.setScore(score);
						if(score > 0){
							asi.setPolarity("positive");
						} else if(score < 0) {
							asi.setPolarity("negative");
						} else {
							asi.setPolarity("neutral");
						}

						sentenceScore += score;

						analyzedSentimentList.add(asi);
					}
				}
			}

			for(AnalyzedSentimentInstance asi: analyzedSentimentList){
				asi.setSentenceScore(sentenceScore);
			}
		} catch (Exception ex){
			ex.printStackTrace();
		}
		return analyzedSentimentList;
	}		
	
	
	private String createDepPathString(List<Integer> shortestPath, HashMap<Integer, Tree> reverseMap, 
			List<TypedDependency> tdl){
		
		if(shortestPath == null){
			return "";
		}
		
		//combine the path from node1 to node2 with POS tags
		StringBuffer depPathString = new StringBuffer();
		int l=0;
		for(; l<shortestPath.size()-1; l++){
			String preRelation = "";
			String postRelation = "";
			String posTag = reverseMap.get(shortestPath.get(l)).value();
			if(l%2 != 0){
				preRelation = dparseUtil.getRelation(shortestPath.get(l-1), shortestPath.get(l), tdl);
				postRelation = dparseUtil.getRelation(shortestPath.get(l), shortestPath.get(l+1), tdl);
				//System.out.print(" -> "+preRelation+" -> "+posTag+" -> "+postRelation+" -> ");
				depPathString.append(" -> "+preRelation+" -> "+posTag+" -> "+postRelation+" -> ");
			} else {
				//System.out.print(posTag);
				depPathString.append(posTag);
			}
		}

		String posTag = reverseMap.get(shortestPath.get(l)).value();
		if(l%2 == 0){
			//log.info(posTag);
			depPathString.append(posTag);
		} else {
			String preReln = dparseUtil.getRelation(shortestPath.get(l-1), shortestPath.get(l), tdl);
			//log.info(" -> "+preReln+" -> "+posTag);
			depPathString.append(" -> "+preReln+" -> "+posTag);
		}
		
		return depPathString.toString();
	}
	
	public String formatPathForRules(String depPath){
		String depPathTokens[] = depPath.split("\\s");
		String depPathInRuleFormat = "";
		for(int r=0; r<depPathTokens.length; r++){
			if(r==0){
				depPathInRuleFormat += "START ";
			} else if(r==depPathTokens.length-1){
				depPathInRuleFormat += "END";
			} else if(r%4==0){
				depPathInRuleFormat += "INT ";
			} else {
				depPathInRuleFormat += depPathTokens[r]+" ";
			}
		}
		return depPathInRuleFormat;
	}
}
