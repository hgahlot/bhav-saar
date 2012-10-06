/**
 * 
 */
package org.hgahlot.sa.bean;

import java.util.ArrayList;

/**
 * @author hgahlot
 *
 */
public class AnalyzedSentimentInstance {
	private String docId;
	private String sentence;
	private String entity;
	private String sentiment;
	private String polarity;
	private Float score;
	private Float sentenceScore;
	private ArrayList<String> corefPhrases;
	
	public AnalyzedSentimentInstance() {}

	public AnalyzedSentimentInstance(String docId, String sentence, String entity,
			String sentiment, String polarity, Float score, Float sentenceScore, 
			ArrayList<String> corefPhrases) {
		super();
		this.docId = docId;
		this.sentence = sentence;
		this.entity = entity;
		this.sentiment = sentiment;
		this.polarity = polarity;
		this.score = score;
		this.sentenceScore = sentenceScore;
		this.corefPhrases = corefPhrases;
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

	public String getPolarity() {
		return polarity;
	}

	public void setPolarity(String polarity) {
		this.polarity = polarity;
	}

	public Float getScore() {
		return score;
	}

	public void setScore(Float score) {
		this.score = score;
	}

	public Float getSentenceScore() {
		return sentenceScore;
	}

	public void setSentenceScore(Float sentenceScore) {
		this.sentenceScore = sentenceScore;
	}

	public ArrayList<String> getCorefPhrases() {
		return corefPhrases;
	}

	public void setCorefPhrases(ArrayList<String> corefPhrases) {
		this.corefPhrases = corefPhrases;
	}
}
