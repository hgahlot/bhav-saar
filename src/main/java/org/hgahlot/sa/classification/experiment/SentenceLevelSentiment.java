/**
 * 
 */
package org.hgahlot.sa.classification.experiment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import cc.mallet.classify.Classifier;
import cc.mallet.classify.Trial;
import cc.mallet.types.InstanceList;
import org.hgahlot.sa.classification.mallet.Importer;
import org.hgahlot.sa.classification.mallet.MalletClassifier;
import org.hgahlot.sa.coref.bart.BARTCorefResolver;
import org.hgahlot.sa.coref.bart.BARTCorefText;
import org.hgahlot.sa.coref.bart.BARTCorefText.BARTCorefPhrase;
import org.hgahlot.sa.coref.bart.BARTCorefText.BARTSentence;
import org.hgahlot.sa.coref.bart.BARTCorefText.BARTWord;
import org.hgahlot.sa.util.FileUtil;

/**
 * @author hgahlot
 *
 */
public class SentenceLevelSentiment {
	private static final String TARGET_ENTITY = "windows 8";
	private static final String CONTENT_DIR = "data/sentiment/windows8DataBill/windows8_contentDir";
	private static final String TRAIN_FILE = "data/sentiment/windows8DataBill/sentence-polarity.train";
	private static final String MALLET_TRAIN_FILE = "data/sentiment/windows8DataBill/sentence-polarity_withNeg.train.mallet";
	private static final String MODEL_FILE = "data/sentiment/windows8DataBill/sentence-polarity_withNeg.model.60_52_64_66";
	private static final String TEST_FILE = "data/sentiment/windows8DataBill/sentence-polarity_withNeg.test";
	private static final String TEST_OUT_FILE = "data/sentiment/windows8DataBill/sentence-polarity_withNeg.test.out";
	private static final String ONLP_SD_MODEL = "data/models/opennlp/EnglishSD.bin.gz";

	private static final String NEG_SENT_FILE = "data/sentiment/windows8DataBill/sentence-polarity.neg.sent";
	private static final String NEG_WORDS_FILE = "data/sentiment/windows8DataBill/negativeWordList.txt";

	private Map<String, ArrayList<BARTCorefPhrase>> reverseCorefMap;

	public static void main(String a[]){
		SentenceLevelSentiment sls = new SentenceLevelSentiment(); 
		//sls.createTrainingData(NEG_SENT_FILE, false, true);
		//sls.runClassifier();
		sls.predictLabel(MODEL_FILE, TEST_FILE, TEST_OUT_FILE);
	}

	public void createTrainingData(String outFile, boolean resolveCoref, boolean outputOnlyNegSent){
		File[] files = new File(CONTENT_DIR).listFiles();
		int fileCount = 0;
		int totalSentencesWritten = 0;
		PrintWriter pw = null;
		HashSet<String> negWordsSet = new HashSet<String>();
		if(outputOnlyNegSent){
			negWordsSet = getNegWords(NEG_WORDS_FILE);
		}
		try {
			pw = new PrintWriter(new FileWriter(outFile), true);
			for(File file: files){
				String fileName = file.getName();
				System.out.println("Processing file: "+fileName+"\nFiles processed: "+fileCount+" out of "+files.length);

				fileCount++;
				String fileContent = FileUtil.getFileContent(file);

				if(resolveCoref){
					BARTCorefResolver bcr = new BARTCorefResolver();
					System.out.println("Resolving coreferences...");
					String bartPreProcessedText = bcr.getBartPreProcessedText(fileContent);
					BARTCorefText bartCorefText = bcr.getBartProcessedText(bartPreProcessedText);
					reverseCorefMap = bartCorefText.getReverseCorefMap();
					System.out.println("Coreferences resolved.");


					ArrayList<BARTSentence> bartSentenceList = bartCorefText.getSentenceList();

					int sentIdx = 0;
					for(BARTSentence bartSentence: bartSentenceList){
						ArrayList<BARTWord> bartWordList = bartSentence.getWords();
						StringBuffer sentenceBuffer = new StringBuffer();
						int entityIdx = 0;
						boolean isTargetEntityInCorefPhrase = false;
						ArrayList<BARTCorefPhrase> prevTokenCorefPhrases = new ArrayList<BARTCorefPhrase>();
						for(BARTWord bartWord: bartWordList){
							String revCorefMapKey = sentIdx+"-"+entityIdx;
							String corefBartWord = bartWord.getWordText();
							ArrayList<BARTCorefPhrase> corefPhrasesForEntity = reverseCorefMap.get(revCorefMapKey);
							if((corefPhrasesForEntity != null)){
								if(corefPhrasesForEntity.equals(prevTokenCorefPhrases)){
									prevTokenCorefPhrases = corefPhrasesForEntity;
									entityIdx++;
									continue;
								}
								isTargetEntityInCorefPhrase = false;

								for(BARTCorefPhrase corefPhrase: corefPhrasesForEntity){
									String corefPhraseString = corefPhrase.toString().toLowerCase();
									if(corefPhraseString.equals(TARGET_ENTITY)){
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

							sentenceBuffer.append(corefBartWord+" ");
							entityIdx++;
						}
						String sentence = sentenceBuffer.substring(0, sentenceBuffer.length()-1)
						.toString().replaceAll("\\|", ";").toLowerCase();

						if(sentence.contains(TARGET_ENTITY)){
							if(!outputOnlyNegSent){
								pw.println(fileName+"-"+sentIdx+"||"+sentence);
								totalSentencesWritten++;
							} else {
								String tokens[] = sentence.split("\\s+");
								for(String token: tokens){
									if(negWordsSet.contains(token)){
										pw.println(fileName+"-"+sentIdx+"||"+sentence);
										totalSentencesWritten++;
										break;
									}
								}
							}
						}


						sentIdx++;
					}
					System.out.println("Total sentences written to file: "+totalSentencesWritten);
				} else {
					String sentences[] = FileUtil.getSentences(fileContent, ONLP_SD_MODEL);//fetch sentences using OpenNLP model

					int sentIdx = 0;
					for(String sentence: sentences){
						sentence = sentence.toLowerCase();
						System.out.println("Sentence number: "+sentIdx);
						if(sentence.contains(TARGET_ENTITY)){
							if(!outputOnlyNegSent){
								pw.println(fileName+"-"+sentIdx+"||"+sentence);
								totalSentencesWritten++;
							} else {
								String tokens[] = sentence.split("\\s+");
								for(String token: tokens){
									if(negWordsSet.contains(token)){
										pw.println(fileName+"-"+sentIdx+"||"+sentence);
										totalSentencesWritten++;
										break;
									}
								}
							}
						}
						sentIdx++;
					}
					System.out.println("Total sentences written to file: "+totalSentencesWritten);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			pw.close();
		}
	}

	private HashSet<String> getNegWords(String file){
		HashSet<String> negWordSet = new HashSet<String>();
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(file));
			while(br.ready()){
				String line = br.readLine();
				if(line != null){
					negWordSet.add(line.trim().toLowerCase());
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return negWordSet;
	}

	public void runClassifier(){
		Importer importer = new Importer();
		InstanceList instances = importer.readFile(new File(TRAIN_FILE));
		instances.save(new File(MALLET_TRAIN_FILE));

		MalletClassifier mc = new MalletClassifier();
		Trial sentLevelSentiTrial = mc.testTrainSplit(instances, 0.9, 0.1, 0);

		Classifier classifier = sentLevelSentiTrial.getClassifier();
		//save the classifer to disk
		try {
			mc.saveClassifier(classifier, new File(MODEL_FILE));
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Accuracy: "+sentLevelSentiTrial.getAccuracy());

		for(int i=0; i<3; i++){
			String sentiment = sentLevelSentiTrial.getClassifier().getLabelAlphabet().lookupLabel(i).toString();
			System.out.println("F1 for class '"+sentiment+"': " + 
					sentLevelSentiTrial.getF1(sentiment));
			System.out.println("Precision for class '" + sentiment + "': " +
					sentLevelSentiTrial.getPrecision(i));
		}
	}


	public void predictLabel(String modelFile, String testFile, String outFile){
		MalletClassifier mc = new MalletClassifier();
		try {
			Classifier classifier = mc.loadClassifier(new File(modelFile));
			mc.printLabelings(classifier, new File(testFile), new File(outFile), false);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

	}
}
