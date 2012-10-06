/**
 * 
 */
package org.hgahlot.sa.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

/**
 * @author hgahlot
 *
 */
public class FileUtil {
	/**
	 * Reads in the content of a file into a String
	 * @param file
	 * @return
	 */
	public static String getFileContent(File file){
		StringBuffer content = new StringBuffer();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			while(br.ready()){
				content.append(br.readLine()+" ");
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
		return content.toString();
	}
	
	/**
	 * Uses the opennlp sentence model to break content into sentences.
	 * @param content
	 * @return Array of sentences
	 */
	public static String[] getSentences(String content, String onlpSdModel){
		InputStream modelIn = null;
		try {
			modelIn = new FileInputStream(onlpSdModel);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		SentenceModel model = null;
		try {
		  model = new SentenceModel(modelIn);
		}
		catch (IOException e) {
		  e.printStackTrace();
		}
		finally {
		  if (modelIn != null) {
		    try {
		      modelIn.close();
		    }
		    catch (IOException e) {
		    }
		  }
		}
		
		SentenceDetectorME sd = new SentenceDetectorME(model);
		String sentences[] = null;
		sentences = sd.sentDetect(content);
		return sentences;
	}
}
