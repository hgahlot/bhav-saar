/**
 * 
 */
package org.hgahlot.sa.coref.bart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hgahlot
 *
 */
public class BARTCorefText {
	private ArrayList<BARTSentence> sentenceList;
	private Map<String, ArrayList<BARTCorefPhrase>> corefMap;
	private Map<String, ArrayList<BARTCorefPhrase>> reverseCorefMap;
	
	public BARTCorefText(){}
	
	public BARTCorefText(ArrayList<BARTSentence> sentenceList, 
			Map<String, ArrayList<BARTCorefPhrase>> corefMap, 
			Map<String, ArrayList<BARTCorefPhrase>> reverseCorefMap) {
		super();
		this.sentenceList = sentenceList;
		this.corefMap = corefMap;
		this.reverseCorefMap = reverseCorefMap;
	}

	public ArrayList<BARTSentence> getSentenceList() {
		return sentenceList;
	}

	public void setSentenceList(ArrayList<BARTSentence> sentenceList) {
		this.sentenceList = sentenceList;
	}
	
	public Map<String, ArrayList<BARTCorefPhrase>> getCorefMap() {
		return corefMap;
	}

	public void setCorefMap(Map<String, ArrayList<BARTCorefPhrase>> corefMap) {
		this.corefMap = corefMap;
	}
	
	public Map<String, ArrayList<BARTCorefPhrase>> getReverseCorefMap() {
		return reverseCorefMap;
	}

	public void setReverseCorefMap(
			Map<String, ArrayList<BARTCorefPhrase>> reverseCorefMap) {
		this.reverseCorefMap = reverseCorefMap;
	}

	
	public class BARTSentence{
		private ArrayList<BARTWord> words;
		private int sentenceIndex;
		
		public BARTSentence(){}
		
		public BARTSentence(ArrayList<BARTWord> words, int sentenceIndex) {
			super();
			this.words = words;
			this.sentenceIndex = sentenceIndex;
		}

		public ArrayList<BARTWord> getWords() {
			return words;
		}

		public void setWords(ArrayList<BARTWord> words) {
			this.words = words;
		}

		public int getSentenceIndex() {
			return sentenceIndex;
		}

		public void setSentenceIndex(int sentenceIndex) {
			this.sentenceIndex = sentenceIndex;
		}
		
		@Override
		public String toString(){
			StringBuffer sentence = new StringBuffer();
			int numWords = this.words.size();
			int i=0;
			for(; i<numWords-1; i++){
				sentence.append(this.words.get(i).getWordText()+" ");
			}
			sentence.append(this.words.get(i).getWordText());
			return sentence.toString();
		}
	}
	
	public class BARTWord{
		private String wordText;
		private String posTag;
		private int position;
		private boolean isCoref;
		
		public BARTWord(){}

		public BARTWord(String wordText, String posTag, int position, boolean isCoref) {
			super();
			this.wordText = wordText;
			this.posTag = posTag;
			this.position = position;
			this.isCoref = isCoref;
		}

		public String getWordText() {
			return wordText;
		}

		public void setWordText(String wordText) {
			this.wordText = wordText;
		}

		public String getPosTag() {
			return posTag;
		}

		public void setPosTag(String posTag) {
			this.posTag = posTag;
		}

		public int getPosition() {
			return position;
		}

		public void setPosition(int position) {
			this.position = position;
		}

		public boolean isCoref() {
			return isCoref;
		}

		public void setCoref(boolean isCoref) {
			this.isCoref = isCoref;
		}
		
		@Override
		public String toString(){
			return this.wordText+"/"+this.posTag+"/"+this.position+"/"+this.isCoref;
		}
	}
	
	public class BARTCorefPhrase{
		private String setId;
		private ArrayList<BARTWord> corefWordList;
		private int sentenceIndex;
		
		public BARTCorefPhrase() {
			super();
		}

		public BARTCorefPhrase(String setId, ArrayList<BARTWord> corefWordList,
				int sentenceIndex) {
			super();
			this.setId = setId;
			this.corefWordList = corefWordList;
			this.sentenceIndex = sentenceIndex;
		}

		public String getSetId() {
			return setId;
		}

		public void setSetId(String setId) {
			this.setId = setId;
		}

		public ArrayList<BARTWord> getCorefWordList() {
			return corefWordList;
		}

		public void setCorefWordList(ArrayList<BARTWord> corefWordList) {
			this.corefWordList = corefWordList;
		}

		public int getSentenceIndex() {
			return sentenceIndex;
		}

		public void setSentenceIndex(int sentenceIndex) {
			this.sentenceIndex = sentenceIndex;
		}
		
		@Override
		public String toString(){
			int i=0;
			int numWords = this.corefWordList.size();
			StringBuffer sb = new StringBuffer();
			for(; i<numWords-1; i++){
				sb.append(this.corefWordList.get(i).getWordText()+" ");
			}
			sb.append(this.corefWordList.get(i).getWordText());
			return sb.toString();
		}
	}
	
	
}
