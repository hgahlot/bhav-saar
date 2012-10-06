package org.hgahlot.sa.xml;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XMLToSentimentFiles {
	private static final String XML_FILE = "data/sentiment/intel.xml";
	private static final String TEST_DATA_DIR = "data/sentiment/intelTestData";
	
	public static void main(String a[]){
		XMLToSentimentFiles conv = new XMLToSentimentFiles();
		conv.convert(XML_FILE);
	}
	
	public void convert(String file){
		StringBuffer content = new StringBuffer();
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(file));
			while(br.ready()){
				content.append(br.readLine());
			}
			br.close();
			
			String text = content.toString().replaceAll("\\n", " ");
			Pattern patt = Pattern.compile("<UGCSearchEntry.*?(</UGCSearchEntry>)");
			Matcher matcher = patt.matcher(text);
			int count = 0;
			HTMLParser hp = new HTMLParser();
			while(matcher.find()){
				String ugcContent = matcher.group();
				count++;
				int assetTypeStartIndex = ugcContent.indexOf("assetTypeId") + 13;
				int assetTypeEndIndex = ugcContent.indexOf("\"", assetTypeStartIndex);
				String assetTypeId = ugcContent.substring(assetTypeStartIndex, assetTypeEndIndex);
				System.out.println(assetTypeId);
				if(!assetTypeId.equals("94")){
					continue;
				}
				
				int idStartIndex = ugcContent.indexOf("idInSource") + 12;
				int idEndIndex = ugcContent.indexOf("\"", idStartIndex);
				if((idStartIndex == -1) || (idEndIndex == -1))
					continue;
				
				String id = ugcContent.substring(idStartIndex, idEndIndex);
				
				int titleStartIndex = ugcContent.indexOf("<Title>") + 7;
				int titleEndIndex = ugcContent.indexOf("</Title>", titleStartIndex);
				if((titleStartIndex == -1) || (titleEndIndex == -1))
					continue;
				String title = ugcContent.substring(titleStartIndex, titleEndIndex);
				
				int descStartIndex = ugcContent.indexOf("<Description>") + 13;
				int descEndIndex = ugcContent.indexOf("</Description>", descStartIndex);
				if((descStartIndex == -1) || (descEndIndex == -1))
					continue;
				String desc = ugcContent.substring(descStartIndex, descEndIndex);
				
				System.out.println(id+"\t"+title+"\t"+desc);
				
				String htmlText = title+".\n"+desc;
				
				
				String textContent = hp.getFullText(htmlText, true);
				if(textContent.contains("intel") || textContent.contains("Intel")){
					PrintWriter pw = new PrintWriter(TEST_DATA_DIR+"/"+id+".txt");
					pw.println(textContent);
					pw.close();
				}
			}
			System.out.println("count="+count);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
