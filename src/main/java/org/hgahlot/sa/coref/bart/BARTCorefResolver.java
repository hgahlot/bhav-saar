/**
 * 
 */
package org.hgahlot.sa.coref.bart;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.IOUtils;

import org.hgahlot.sa.coref.bart.BARTCorefText.BARTCorefPhrase;
import org.hgahlot.sa.coref.bart.BARTCorefText.BARTSentence;
import org.hgahlot.sa.coref.bart.BARTCorefText.BARTWord;
/**
 * @author hgahlot
 *
 */
public class BARTCorefResolver {
	public static String BART_SERVER_URL;
	
	public static void main(String a[]){
		BARTCorefResolver bcr = new BARTCorefResolver();
		//String preProcessedText = bcr.getBartPreProcessedText("This is the request body of an article. The article is about " +
		//		"a company called CBSi. This company is going strong.");
//		String preProcessedText = bcr.getBartPreProcessedText("The Iphone is not a very good choice. " +
//				"Iphone has a screen which is not very great. Negative Reviews CAN'T be from " +
//				"Actual iPhone Users. I don't think I would have liked the iPhone 3G because of lack " +
//				"of copy/paste as well as MMS but I don't understand the negative reviews with the new 3GS. " +
//				"Keep in mind I've only had it for 3 days and now I realize " +
//				"why there was a line wrapped around the mall the day this phone came out. It took me this " +
//				"long because of the negative reviews I've read on this website. But an review I read a few days ago " +
//				"convinced me to buy. It stated that most iPhone reviews are probably from people who don't actually have the " +
//				"phone and just repeat what others have said. Plus everyone that I've physically met who had an iPhone " +
//				"swears by it. So I took the chance and now I realize what all the hype was about. " +
//				"Now let's get down to the basics.Several reviews have stated that people can't get signals on their phones. " +
//				"Well the representative at the Apple store showed me a digital map of signal strength in the entire US. " +
//				"I absolutely love the iphone. It is an insanely great phone.");
		String preProcessedText = bcr.getBartPreProcessedText("Windows 8 beta: It's a whole new Windows First Take: " +
				"Microsoft pulled back much of the scaffolding and secrecy surrounding Windows 8 today at Mobile World Congress. " +
				"I've been using the Windows 8 beta (download), officially known as the Windows 8 Consumer Preview, for the past week, " +
				"and it's by far the most integrated and capable operating system Microsoft has ever put out. " +
				"The question is, will enough people care? There's a phenomenal amount of change here to discuss, but " +
				"if you're looking for a quick summary: Windows 8 is a breeze to use. It's tricked out with social " +
				"networking and synchronization, it's robust enough to handle Photoshop, it gracefully moves from touch to keyboard " +
				"and mouse, and it's got some top-notch security. And despite what Microsoft is calling strong interest from " +
				"hardware manufacturers and developers, its impact is still uncertain at best. Windows 8's predecessor could " +
				"be summarized in six words: Windows 7 is Vista done right. Windows 8 is a much harder sell for reasons that " +
				"stem from Microsoft itself, its hardware partners, and the whims of consumers. First, when Windows 8 " +
				"launches it will be the most ambitious operating system ever, with a workflow that's easy once you learn " +
				"it, but not necessarily obvious at first blush. Second, more of your Windows 8 experience will be dependent " +
				"on your hardware than ever before, because it will work on both desktops and laptops, and tablets. Last, and " +
				"this is the one that won't be resolved until Windows 8 starts shipping to consumers in the second half of " +
				"this year, there's no strong evidence indicating what it is consumers want next. Do people want tablets " +
				"that aren't made by Apple? Is the tablet more like a larger smartphone, or a thinner laptop? Is there " +
				"interest in one operating system that offers both casual touch and robust power modes? Logging on Windows 8 " +
				"offers some great log-on options. You can choose to create a local account, but the OS becomes infinitely " +
				"more useful when you use a Microsoft account. You'll be able to synchronize it to your Windows 8 settings, " +
				"including Internet Explorer history. This means that when you log in to any other Windows 8 machine with that " +
				"account, your data will sync, including background settings, address book, other accounts like Facebook and " +
				"Twitter, e-mail, and instant messaging. App syncing is planned for the Windows Store, too, while the " +
				"SkyDrive integration can be used for syncing files. Beyond sync, once you've logged on for the first " +
				"time you can change your log-in to a PIN or a picture log-in. The picture log-in is quite " +
				"cool, and lets you set a photo as your log-in background. You can then customize a quick series of " +
				"drawings on the picture, made up of a line, a circle, and a dot, to log you in. I was able to choose my " +
				"photo log-in from my Facebook photos, which I had synced using the native Photos app that comes with " +
				"Windows 8. The process was easy, and the photo picker tool in Settings connected through the Photos app to " +
				"provide access to my Facebook account. In drawing my log-in on the photo, there were times when it " +
				"worked on the first attempt, and other times that required multiple attempts. This appears to be more " +
				"related to the hardware than anything else. A killer feature that's missing is facial recognition log-ins. " +
				"The better of these apps have been proven to be resistant to printed photo hacking, and it would extremely " +
				"useful to have a Webcam recognize your face and log you in without having to physically touch the computer. " +
				"At least nobody else has this integrated into the operating system yet, but since third parties like KeyLemon " +
				"and FastAccess have been working on their versions for a while, expect it to arrive in the big players sooner " +
				"rather than later. Navigating Windows 8: Touch You can navigate around Windows 8 in two ways, and they work well " +
				"enough so that you can use them simultaneously if you're into that kind of torture. As we've all seen, Windows 8 is " +
				"highly grope-able. It wants you to touch it, and frankly touch is the easiest way to get around. " +
				"Unfortunately, at this point the Windows 8 beta doesn't come with a quick tutorial, and although the workflow " +
				"is easy, it's not necessarily obvious.");

		System.out.println("preprocessedtext=\n"+preProcessedText);
		HashMap<String, ArrayList<BARTCorefPhrase>> corefMap = 
			bcr.getBARTCorefPhrases(preProcessedText);
		
//		for(String key: corefMap.keySet()){
//			ArrayList<BARTCorefPhrase> phrases = corefMap.get(key);
//			System.out.println("setId="+key);
//			for(BARTCorefPhrase phrase: phrases){
//				ArrayList<BARTWord> words = phrase.getCorefWordList();
//				for(BARTWord word: words){
//					System.out.print(word.getWordText()+" ");
//				}
//				System.out.println();
//			}
//		}
		
		Map<String, ArrayList<BARTCorefPhrase>> reverseCorefMap = 
			bcr.createReverseCorefMap(corefMap);
		for(String key: reverseCorefMap.keySet()){
			ArrayList<BARTCorefPhrase> phrases = reverseCorefMap.get(key);
			System.out.println("sentId-wordId="+key);
			for(BARTCorefPhrase phrase: phrases){
				System.out.print(phrase+" | ");
			}
			System.out.println();
		}
	}
	
	public String getBartPreProcessedText(String text){
		return makeBARTPostRequest(text);
	}
	
	private String makeBARTPostRequest(String text){
		HttpClient client = new HttpClient( );
		// Create POST method
		PostMethod method = new PostMethod(BART_SERVER_URL);
		
		//create request
		RequestEntity requestText = null;
		try {
			requestText = new StringRequestEntity(text, "text/plain", "ISO-8859-1");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		
		// Set request body on POST 
		method.setRequestEntity(requestText);
		
		// Execute and print response
		String response = null;
		try {
			client.executeMethod( method );
			InputStream responseStream = method.getResponseBodyAsStream();
			response = IOUtils.toString(responseStream, "ISO-8859-1");
		} catch (HttpException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		method.releaseConnection();
		//System.out.println(response);
		return response;
	}
	
	
	public HashMap<String, ArrayList<BARTCorefPhrase>> getBARTCorefPhrases(String bartPreProcessedText){
		bartPreProcessedText = bartPreProcessedText.toLowerCase();
		Pattern pattern = Pattern.compile("<s>(.*?)</s>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(bartPreProcessedText);
		int index = 0;
		int sentenceIndex = 0;
		BARTCorefText bartCorefText = new BARTCorefText();
		ArrayList<BARTCorefPhrase> bartCorefPhrases = new ArrayList<BARTCorefPhrase>();
		HashMap<String, ArrayList<BARTCorefPhrase>> corefMap = 
			new HashMap<String, ArrayList<BARTCorefPhrase>>();
		
		while(matcher.find(index)){
			String match = matcher.group();
			
			index = matcher.end();
			String tokens[] = match.split("\\n+");
			
			BARTCorefPhrase bcp = null;
			
			boolean isCorefTagOpen = false;
			int wordIdx = 0;
			for(int i=0; i<tokens.length; i++){
				if(tokens[i].startsWith("<coref")){
					int firstIndexOfSetId = tokens[i].indexOf("set-id=\"")+8;
					int secondIndexOfSetId = tokens[i].indexOf("\"", firstIndexOfSetId);
					String setId = tokens[i].substring(firstIndexOfSetId,
							secondIndexOfSetId);
					isCorefTagOpen = true;
					bcp = bartCorefText.new BARTCorefPhrase();
					bcp.setSetId(setId);
					bcp.setSentenceIndex(sentenceIndex);
					bcp.setCorefWordList(new ArrayList<BARTWord>());
					
				} else if(tokens[i].startsWith("<w")){
					int firstIndexOfPos = tokens[i].indexOf("pos=\"")+5;
					int secondIndexOfPos = tokens[i].indexOf("\"", firstIndexOfPos);
					String pos = tokens[i].substring(firstIndexOfPos,
							secondIndexOfPos);
					
					int firstIndexOfWord = tokens[i].indexOf(">", secondIndexOfPos);
					int secondIndexOfWord = tokens[i].indexOf("<", firstIndexOfWord);
					String wordText = tokens[i].substring(firstIndexOfWord+1, 
							secondIndexOfWord);
					
					BARTWord bartWord = bartCorefText.new BARTWord();
					bartWord.setWordText(wordText);
					bartWord.setPosTag(pos);
					bartWord.setPosition(wordIdx);
					if(!isCorefTagOpen){
						bartWord.setCoref(false);
					} else {
						bartWord.setCoref(true);
						ArrayList<BARTWord> wordList = bcp.getCorefWordList();
						wordList.add(bartWord);
						bcp.setCorefWordList(wordList);
					}
					
					wordIdx++;
				} else if(tokens[i].startsWith("</coref>")){
					isCorefTagOpen = false;
					bartCorefPhrases.add(bcp);
				}
			}
			
			sentenceIndex++;
		}
		
		for(BARTCorefPhrase corefPhrase: bartCorefPhrases){
			String setId = corefPhrase.getSetId();
			
			ArrayList<BARTCorefPhrase> phraseList = 
				new ArrayList<BARTCorefPhrase>();
			
			if(corefMap.containsKey(setId)){
				phraseList = corefMap.get(setId);
			}
			
			phraseList.add(corefPhrase);
			corefMap.put(setId, phraseList);
		}
		
		return corefMap;
	}
	
	
	public BARTCorefText getBartProcessedText(String bartPreProcessedText){
		bartPreProcessedText = bartPreProcessedText.toLowerCase();
		Pattern pattern = Pattern.compile("<s>(.*?)</s>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(bartPreProcessedText);
		int index = 0;
		int sentenceIndex = 0;
		BARTCorefText bartCorefText = new BARTCorefText();
		ArrayList<BARTCorefPhrase> bartCorefPhrases = new ArrayList<BARTCorefPhrase>();
		HashMap<String, ArrayList<BARTCorefPhrase>> corefMap = 
			new HashMap<String, ArrayList<BARTCorefPhrase>>();
		
		ArrayList<BARTSentence> bartSentences = new ArrayList<BARTSentence>();
		
		while(matcher.find(index)){
			String match = matcher.group();
			
			BARTSentence sentence = bartCorefText.new BARTSentence();
			sentence.setSentenceIndex(sentenceIndex);
			ArrayList<BARTWord> sentWords = new ArrayList<BARTWord>();
			
			index = matcher.end();
			String tokens[] = match.split("\\n+");
			
			BARTCorefPhrase bcp = null;
			
			boolean isCorefTagOpen = false;
			int wordIdx = 0;
			for(int i=0; i<tokens.length; i++){
				if(tokens[i].startsWith("<coref")){
					int firstIndexOfSetId = tokens[i].indexOf("set-id=\"")+8;
					int secondIndexOfSetId = tokens[i].indexOf("\"", firstIndexOfSetId);
					String setId = tokens[i].substring(firstIndexOfSetId,
							secondIndexOfSetId);
					isCorefTagOpen = true;
					bcp = bartCorefText.new BARTCorefPhrase();
					bcp.setSetId(setId);
					bcp.setSentenceIndex(sentenceIndex);
					bcp.setCorefWordList(new ArrayList<BARTWord>());
					
				} else if(tokens[i].startsWith("<w")){
					int firstIndexOfPos = tokens[i].indexOf("pos=\"")+5;
					int secondIndexOfPos = tokens[i].indexOf("\"", firstIndexOfPos);
					String pos = tokens[i].substring(firstIndexOfPos,
							secondIndexOfPos);
					
					int firstIndexOfWord = tokens[i].indexOf(">", secondIndexOfPos);
					int secondIndexOfWord = tokens[i].indexOf("<", firstIndexOfWord);
					String wordText = tokens[i].substring(firstIndexOfWord+1, 
							secondIndexOfWord);
					
					BARTWord bartWord = bartCorefText.new BARTWord();
					bartWord.setWordText(wordText);
					bartWord.setPosTag(pos);
					bartWord.setPosition(wordIdx);
					if(!isCorefTagOpen){
						bartWord.setCoref(false);
					} else {
						bartWord.setCoref(true);
						ArrayList<BARTWord> wordList = bcp.getCorefWordList();
						wordList.add(bartWord);
						bcp.setCorefWordList(wordList);
					}
					
					sentWords.add(bartWord);
					wordIdx++;
				} else if(tokens[i].startsWith("</coref>")){
					isCorefTagOpen = false;
					bartCorefPhrases.add(bcp);
				}
				
			}
			sentence.setWords(sentWords);
			bartSentences.add(sentence);
			sentenceIndex++;
		}
		
		for(BARTCorefPhrase corefPhrase: bartCorefPhrases){
			String setId = corefPhrase.getSetId();
			
			ArrayList<BARTCorefPhrase> phraseList = 
				new ArrayList<BARTCorefPhrase>();
			
			if(corefMap.containsKey(setId)){
				phraseList = corefMap.get(setId);
			}
			
			phraseList.add(corefPhrase);
			corefMap.put(setId, phraseList);
		}
		
		bartCorefText.setSentenceList(bartSentences);
		bartCorefText.setCorefMap(corefMap);
		bartCorefText.setReverseCorefMap(createReverseCorefMap(corefMap));
		
		return bartCorefText;
	}
	
	
	/**
	 * Creates a map of coreference phrases for sentidx-wordidx as key. This 
	 * method will be helpful in fetching all the coreferences phrases 
	 * for a given word in a sentence using the word index and sentence index. The 
	 * key is a String of format 'sentenceIndex-wordIndex'.
	 * 
	 * @return Map with key='sentIdx-wordIdx' and value=<List of coreferenced phrases>
	 */
	public Map<String, ArrayList<BARTCorefPhrase>> createReverseCorefMap(Map<String, ArrayList<BARTCorefPhrase>> corefMap){
		Map<String, ArrayList<BARTCorefPhrase>> revCorefMap = new
			HashMap<String, ArrayList<BARTCorefPhrase>>();
		
		for(String setId: corefMap.keySet()){
			ArrayList<BARTCorefPhrase> corefPhrases = corefMap.get(setId);
			for(BARTCorefPhrase phrase: corefPhrases){
				int sentenceIdx = phrase.getSentenceIndex();
				ArrayList<BARTWord> corefWords = phrase.getCorefWordList();
				for(BARTWord corefWord: corefWords){
					int wordIdx = corefWord.getPosition();
					String sentWordIdxKey = sentenceIdx+"-"+wordIdx;
					ArrayList<BARTCorefPhrase> corefPhraseList = corefPhrases;
					revCorefMap.put(sentWordIdxKey, corefPhraseList);
				}
			}
		}
		return revCorefMap;
	}
	
}
