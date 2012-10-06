package org.hgahlot.sa.xml;

import net.htmlparser.jericho.*;
import java.util.*;
import java.io.IOException;
import java.io.StringReader;
import java.net.*;

public class HTMLParser {
	private static Source source = null;
	
	public static void main(String a[]) {
		HTMLParser p = new HTMLParser();
		System.out.println(p.getFullText("http://jericho.htmlparser.net/docs/index.html", false));
	}
	
	private void init(String urlOrFileOrContent, boolean ifContent){
		try {	
			MicrosoftConditionalCommentTagTypes.register();
			PHPTagTypes.register();
			PHPTagTypes.PHP_SHORT.deregister(); // remove PHP short tags otherwise they override processing instructions
			MasonTagTypes.register();
			
			if(ifContent){
				source = new Source(new StringReader(urlOrFileOrContent));
			} else {
				source = new Source(new URL(urlOrFileOrContent));
			}
			
			source.fullSequentialParse();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public String getFullText(String urlOrFileName, boolean ifContent){
		init(urlOrFileName, ifContent);
		StringBuffer fullHtmlText = new StringBuffer();
		
		String title=getTitle(source);
		if(title != null){
			if(!title.equals("")){
				fullHtmlText.append(title+"\n");
			}
		}
		
		String description = getMetaValue(source,"description");
		if(description != null){
			if(!description.equals("")){
				fullHtmlText.append(description+"\n");
			}
		}
		
		String keywords = getMetaValue(source,"keywords");
		if(keywords != null){
			if(!keywords.equals("")){
				fullHtmlText.append(keywords+"\n");
			}
		}
		
		String body = getBody(source);
		if(body != null){
			if(!body.equals("")){
				fullHtmlText.append(body+"\n");
			}
		}
		
		return fullHtmlText.toString();
	}
	
	
	public String getBody(String urlOrFileName, boolean ifContent){
		init(urlOrFileName, ifContent);
		return getBody(source);
	}
	
	private String getBody(Source source){
		TextExtractor textExtractor=new TextExtractor(source) {
			public boolean excludeElement(StartTag startTag) {
				return startTag.getName()== HTMLElementName.P || "control".equalsIgnoreCase(startTag.getAttributeValue("class"));
			}
		};
		return textExtractor.setIncludeAttributes(true).toString();
	}
	
	public ArrayList<String> getAllLinks(String urlOrFileName, boolean ifContent){
		init(urlOrFileName, ifContent);
		ArrayList<String> links = new ArrayList<String>();
		List<Element> linkElements=source.getAllElements(HTMLElementName.A);
		for (Element linkElement : linkElements) {
			String href=linkElement.getAttributeValue("href");
			if (href==null) continue;
			// A element can contain other tags so need to extract the text from it:
			//String label=linkElement.getContent().getTextExtractor().toString();
			links.add(href);
		}
		return links;
	}
	
	public String getTitle(String urlOrFileName, boolean ifContent){
		init(urlOrFileName, ifContent);
		return getTitle(source);
	}
	
	
	private String getTitle(Source source){
		Element titleElement=source.getFirstElement(HTMLElementName.TITLE);
		if (titleElement==null) 
			return null;
		// TITLE element never contains other tags so just decode it collapsing whitespace:
		return CharacterReference.decodeCollapseWhiteSpace(titleElement.getContent());
	}
	
	public String getMetaValue(String urlOrFileName, String key, boolean ifContent){
		init(urlOrFileName, ifContent);
		return getMetaValue(source, key);
	}
	
	private String getMetaValue(Source source, String key){
		for (int pos=0; pos<source.length();) {
			StartTag startTag=source.getNextStartTag(pos,"name",key,false);
			if (startTag==null) return null;
			if (startTag.getName()==HTMLElementName.META)
				return startTag.getAttributeValue("content"); // Attribute values are automatically decoded
			pos=startTag.getEnd();
		}
		
		return null;
	}
}
