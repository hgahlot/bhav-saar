/**
 * 
 */
package org.hgahlot.sa.example.windows8;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.jsoup.Jsoup;
import org.springframework.jdbc.core.JdbcTemplate;

import org.hgahlot.sa.jdbc.TemplateAccessor;

/**
 * @author hgahlot
 *
 */
public class DataAggregator {
	private static final String BEAN_ID = "mysqlBlogsapidb";
	private static final String WINDOWS_ASSET_ID_FILE = "data/sentiment/windows8NewData/windows8NewAssetIds.txt";
	private static final String DIR = "data/sentiment/windows8NewData/newData";
	private static final String HTML_REGEX = "<(.|\n)*?>";

	JdbcTemplate template;

	public DataAggregator(){
		template = TemplateAccessor.getJdbcTemplateForBean(BEAN_ID);
	}

	public static void main(String a[]){
		DataAggregator da = new DataAggregator();
		da.fetchAllDocuments();
	}

	public void fetchAllDocuments(){
		BufferedReader br = null;
		PrintWriter pw = null;
		try {
			br = new BufferedReader(new FileReader(WINDOWS_ASSET_ID_FILE));
			StringBuffer commaSeparatedList = new StringBuffer();
			while(br.ready()){
				String line = br.readLine();
				commaSeparatedList.append(line.trim()).append(",");
			}
			String csl = commaSeparatedList.substring(0, commaSeparatedList.length()-1);

			int batchSize = 50;
			int tableRowCount = template.queryForInt("SELECT COUNT(*) FROM Post WHERE postId in ("+csl+")");
			System.out.println("tableRowCount="+tableRowCount);
			for(int i=0; i<tableRowCount; i+=batchSize){
				System.out.println("Fetching rows "+i+" to "+(i+batchSize)+". Total rows = "+tableRowCount);
				String query = "SELECT postId, head, body FROM Post WHERE postId in ("+csl+") " +
				"LIMIT "+batchSize+" OFFSET "+i;

				List<LinkedHashMap<String, Object>> list = (List<LinkedHashMap<String, Object>>)template.queryForList(query);
				
				Iterator<LinkedHashMap<String, Object>> itr = list.iterator();
				while(itr.hasNext()){
					LinkedHashMap<String, Object> mapObj = itr.next();
					//				for(String key: mapObj.keySet()){
					//					Object value = mapObj.get(key);
					//					if(value instanceof String){
					//						System.out.print(" value="+parser.getFullText(String.valueOf(value), true));
					//					} else {
					//						System.out.print(" value="+value);
					//					}
					//				}

					String fileName = (Integer)mapObj.get("postId")+".txt";
					String head = (String)mapObj.get("head");
					head = head.replaceAll(HTML_REGEX, "");
					head = Jsoup.parse(head).text();
					String body = (String)mapObj.get("body");
					body = body.replaceAll(HTML_REGEX, "");
					body = Jsoup.parse(body).text();
					
					pw = new PrintWriter(DIR+"/"+fileName);
					pw.println(head+"\n"+body);
					pw.close();
				}
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
	}
}
