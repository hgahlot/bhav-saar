/**
 * 
 */
package org.hgahlot.sa.manager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.hgahlot.sa.coref.bart.BARTCorefResolver;

/**
 * @author hgahlot
 *
 */
public class SentimentAnalyzerMain {
	public static void main(String a[]){
		if(a.length < 1){
			System.out.println("Wrong arguments!\nUsage:\n$ java SentimentAnalyzerMain <config-file-path> " +
					"[comma-separated-list-of-entities] [input-type-text-or-file-or-dir] [input-text-or-filename-or-directory]");
			System.exit(0);
		}

		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream(a[0]));
			
			String entityString = "";
			if(a.length > 1){
				entityString = a[1];
			} else {
				entityString = prop.getProperty("sa.entities");
			}
			
			String inputType = "";
			if(a.length > 2){
				inputType = a[2];
			} else {
				inputType = prop.getProperty("sa.input.type");
			}
			
			String input = "";
			if(a.length > 3){
				input = a[3];
			} else {
				input = prop.getProperty("sa.input");
			}
			
			String outFilePath = prop.getProperty("sa.output.filepath");
			
			String stanParserModel = prop.getProperty("sa.model.parser.stanford");
			String onlpSdModel = prop.getProperty("sa.model.sd.opennlp");
			String sentLevelSentiModel = prop.getProperty("sa.model.sentence-level-sentiment");
			
			String sentimentsDict = prop.getProperty("sa.dict.sentiments");
			String emphaticsDict = prop.getProperty("sa.dict.emphatics");
			
			String entSentiRules = prop.getProperty("sa.rules.ent-senti-relation");
			String negationRules = prop.getProperty("sa.rules.negation");
			
			String entities[] = entityString.split(",");
			
			Boolean resolveCoref = Boolean.parseBoolean(prop.getProperty("sa.coref.resolve"));
			
			String bartServerUrl = prop.getProperty("sa.coref.bart.url");
			
			SentimentAnalyzer.SENTIMENT_ANALYSIS_OUT_FILE = outFilePath;
			SentimentAnalyzer.STANFORD_PARSER_MODEL = stanParserModel;
			SentimentAnalyzer.ONLP_SD_MODEL = onlpSdModel;
			SentimentAnalyzer.SENTENCE_LEVEL_MODEL = sentLevelSentiModel;
			SentimentAnalyzer.SENTIMENT_DICT = sentimentsDict;
			SentimentAnalyzer.EMPHATICS_DICT = emphaticsDict;
			SentimentAnalyzer.ENT_SENTI_RULES_FILE = entSentiRules;
			SentimentAnalyzer.NEGATION_RULES_FILE = negationRules;
			SentimentAnalyzer.ENTITIES = entities;
			SentimentAnalyzer.resolveCoref = resolveCoref;
			BARTCorefResolver.BART_SERVER_URL = bartServerUrl;
			
			SentimentAnalyzer sa = new SentimentAnalyzer();
			sa.process(inputType, input, outFilePath);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
