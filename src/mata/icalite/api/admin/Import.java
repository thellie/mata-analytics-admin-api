package mata.icalite.api.admin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import com.sun.jersey.api.view.Viewable;

import mata.icalite.api.util.FileManager;
import mata.icalite.api.util.Json;
import mata.icalite.api.util.Security;
import mata.icalite.api.util.SystemControl;

@Path("/import")
public class Import {
	private SystemControl sc = null;
	private Json json = null;
	private String caxHome = null;
	private String collectionHome = null;
	
	public Import(){
		caxHome = System.getenv("SOLR_HOME");
		collectionHome = caxHome + "\\example\\solr";
		
		sc = new SystemControl();
		json  = new Json();
	}

	@GET
	@Produces("application/xml")
	public Viewable getParam(@QueryParam("method") String method,
				@QueryParam("collectionId") String collectionId,
				@QueryParam("crawlerId") String crawlerId,
				@QueryParam("sessionId") String session,
				@QueryParam("username") String username
			) {
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		
		Security secure = new Security();
		try {
			if(!new Security().derbyCheck(session)){
				Map<String,Object> property = new HashMap<String,Object>();
				
				property.put("message", "session expired");
				property.put("value", "1");
				
				apiResponse.put("items", property);
				return new Viewable("/general/ack", apiResponse);
			}
			else {
				try{
					if (!collectionId.toLowerCase().contains("colgroup")){
						username = secure.getUser(session);
						String groups = secure.getGroupDerby(username);
						collectionId = groups+"-"+collectionId;
					}
				}
				catch(Exception e){
					Map<String,Object> errorProperty = new HashMap<String,Object>();
					errorProperty.put("code", "500");
					errorProperty.put("message", e.toString());
					errorProperty.put("detail", sc.getStackTrace(e));
					
					error.add(errorProperty);
					
					e.printStackTrace();
				}
			}
		}
		catch(Exception e){
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);
			
			e.printStackTrace();
		}
		
		if(method.equalsIgnoreCase("readCSV")){
			return readCSV(collectionId,crawlerId);
		}else if(method.equalsIgnoreCase("get")){
			return get(collectionId,crawlerId);
		}else{
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			
			errorProperty.put("code", "405");
			errorProperty.put("message", "Method Not Allowed");
			errorProperty.put("detail", 
					"The REST service does not support the operation implied by the HTTP "
					+ "method for the resource that is addressed by the URI that is "
					+ "passed in");
			
			apiResponse.put("items", errorProperty);
			return new Viewable("/exception/error", apiResponse);
		}
	}
	
	@POST
	@Produces("application/xml")
	public Viewable postParam(@QueryParam("method") String method, 
			String body,
			@QueryParam("collectionId") String collectionId,
			@QueryParam("sessionId") String session,
			@QueryParam("crawlerId") String crawlerId,
			@QueryParam("crawlerId") String importDir)
	{
		
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		Security secure = new Security();
		
		try {
			if(!new Security().derbyCheck(session)){
				Map<String,Object> property = new HashMap<String,Object>();
				
				property.put("message", "session expired");
				property.put("value", "1");
				
				apiResponse.put("items", property);
				return new Viewable("/general/ack", apiResponse);
			}
			else {
				try{
					if (!collectionId.toLowerCase().contains("colgroup")){
						String username = secure.getUser(session);
						String groups = secure.getGroupDerby(username);
						collectionId = groups+"-"+collectionId;
					}
				}
				catch(Exception e){
					Map<String,Object> errorProperty = new HashMap<String,Object>();
					errorProperty.put("code", "500");
					errorProperty.put("message", e.toString());
					errorProperty.put("detail", sc.getStackTrace(e));
					
					error.add(errorProperty);
					
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);
			
			e.printStackTrace();
		}
		
		if(error.size() > 0){
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}
		
		if(method.equalsIgnoreCase("create")){
			return create(body,collectionId,importDir);
		}else if(method.equalsIgnoreCase("createImportFolder")){
			return createImportFolder(collectionId,session,body);
		}else{
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			
			errorProperty.put("code", "405");
			errorProperty.put("message", "Method Not Allowed");
			errorProperty.put("detail", 
					"The REST service does not support the operation implied by the HTTP "
					+ "method for the resource that is addressed by the URI that is "
					+ "passed in");
			
			apiResponse.put("items", errorProperty);
			return new Viewable("/exception/error", apiResponse);
		}
	}
	
	private Viewable create(String body, String collectionId, String crawlerId){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		
		Map<String, String> jsonElements = null;
		try {
			jsonElements = json.parse(body);
		} catch (Exception e) {
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);
			
			e.printStackTrace();
		}
		
		Properties configrunprops = new Properties();
		
		String importDir = collectionHome + "\\" + collectionId + "."+crawlerId;
		
		try{			
			OutputStream configrunout = new FileOutputStream(importDir + "\\configrun.cfg");
			configrunprops.setProperty("fieldnames", jsonElements.get("fieldnames"));
			configrunprops.setProperty("rowid", jsonElements.get("rowid"));
			configrunprops.setProperty("header", jsonElements.get("header"));
			configrunprops.store(configrunout, null);
			configrunout.close();
		}
		catch(Exception e){
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", "Failed");
			errorProperty.put("detail", "Internal Server Error");
			
			error.add(errorProperty);
		}
		
		if(error.size() > 0){
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", "successful");
			property.put("value", "0");
			
			apiResponse.put("items", property);
			return new Viewable("/general/ack", apiResponse);
		}
	}
	
	private Viewable get(String collectionId, String crawlerId){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		
		String fieldnames = "";
		String rowid = "";
		String header = "";
		
		String importDir = collectionHome + "\\" + collectionId + "."+crawlerId;
		System.out.println(importDir);
		if(!new File(importDir+"\\configrun.cfg").exists()){
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", "no configrun file found, using default value");
			property.put("value", "0");
			
			apiResponse.put("items", property);
			return new Viewable("/general/ack", apiResponse);
		}
		
		try{				
			
			InputStream configrunin = new FileInputStream(importDir + "\\configrun.cfg");
			Properties props = new Properties();
			props.load(configrunin);
			
			fieldnames = props.getProperty("fieldnames");
			rowid = props.getProperty("rowid");
			header = props.getProperty("header");
			
			configrunin.close();
		}
		catch(Exception e){
			e.printStackTrace();
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", "Failed");
			errorProperty.put("detail", "Internal Server Error");
			
			error.add(errorProperty);
		}
		
		if(error.size() > 0){
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("fieldname", fieldnames);
			property.put("rowid", rowid);
			property.put("header", header);
			property.put("value", "0");
			
			apiResponse.put("items", property);
			return new Viewable("/import/get", apiResponse);
		}
	}
	
	private Viewable createImportFolder(String collectionId, String session, String body){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();

		String username = "";
		Security secure = new Security();
		FileManager fm = new FileManager();
		String randomName = Integer.toString(randInt(10000, 99999));
		
		String jarResourceDir = caxHome + "\\example\\resources\\jar\\crawler";
		
		Map<String, String> jsonElements = null;
		try {
			jsonElements = json.parse(body);
		} catch (Exception e) {
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);
			
			e.printStackTrace();
		}

		try{
			username = secure.getUser(session);
			secure.updateLimitCrawler(collectionId, username);
			if(secure.derbyCheckCrawler(collectionId, username)){
				String importDir = collectionHome + "\\" + collectionId + ".IMPORT_" + randomName;
				File file = new File(importDir);
				while(file.exists()){
					randomName = Integer.toString(randInt(10000, 99999));
					importDir = collectionHome + "\\" + collectionId + ".IMPORT_" + randomName;
					file = new File(importDir);
				}

				if(!file.exists()){
					fm.createDir(importDir);
				}
				
				fm.copyFile(jarResourceDir + "\\csvimport.jar", importDir + "\\csvimport.jar");
				fm.fileWriter(importDir + "\\start.cfg", "startcrawl=stop\r\npid=", false);
				
				Properties configpropertiesprops = new Properties();
				OutputStream configpropertiesout = new FileOutputStream(importDir + "\\configproperties.cfg");
				configpropertiesprops.setProperty("displayname", jsonElements.get("displayname"));
				configpropertiesprops.setProperty("crawlerid", "IMPORT_" + randomName);
				configpropertiesprops.setProperty("type", jsonElements.get("type"));
				configpropertiesprops.store(configpropertiesout, null);
				configpropertiesout.close();	
			}
		}
		catch(Exception e){
			e.printStackTrace();
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", "Failed");
			errorProperty.put("detail", "Internal Server Error");

			error.add(errorProperty);
		}

		if(error.size() > 0){
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}else{
			try {
				secure.insertLimitCrawler(username, collectionId);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Map<String,Object> errorProperty = new HashMap<String,Object>();
				errorProperty.put("code", "500");
				errorProperty.put("message", "Failed");
				errorProperty.put("detail", "Internal Server Error");
				error.add(errorProperty);
				apiResponse.put("items", error);
				return new Viewable("/exception/error", apiResponse);
			}
		}
		Map<String,Object> property = new HashMap<String,Object>();

		property.put("message", "IMPORT_" + randomName);
		property.put("value", "0");

		apiResponse.put("items", property);
		return new Viewable("/general/ack", apiResponse);
	}

	
	private Viewable readCSV(String collectionId, String crawlerId){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		
		String importDir = collectionHome + "\\" + collectionId + "."+crawlerId;
		String csvFile = importDir+"\\importedcsv.csv";
		
		ArrayList<String> listHeaders = new ArrayList<String>();
		ArrayList<ArrayList<String>> allRecords = new ArrayList<ArrayList<String>>();
		
		try {
			CSVParser parser = CSVParser.parse(new File(csvFile), StandardCharsets.US_ASCII, CSVFormat.EXCEL.withHeader());
			Map<String, Integer> headers = parser.getHeaderMap();
			for (Map.Entry<String, Integer> entry : headers.entrySet())
			{
//			    System.out.println(entry.getKey());
			    listHeaders.add(entry.getKey());
			}
			allRecords.add(listHeaders);
			List<CSVRecord> records = parser.getRecords();
//			Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
			int countrecord = 0;
			for (CSVRecord record : records) {
				int column = 0;
				ArrayList<String> recordsPerLine = new ArrayList<String>();
				while(column < listHeaders.size()){
					recordsPerLine.add(record.get(column));
					column++;
				}
				countrecord++;
				allRecords.add(recordsPerLine);
				if(countrecord==10){
					break;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", "Failed");
			errorProperty.put("detail", "csv file not supported");
			error.add(errorProperty);
		}
		
		if(error.size() > 0){
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", allRecords);
			property.put("value", "0");
			
			apiResponse.put("items", property);
			return new Viewable("/general/ack", apiResponse);
		}
	}
	
	private int randInt(int min, int max) {
	    Random rand = new Random();

	    int randomNum = rand.nextInt((max - min) + 1) + min;

	    return randomNum;
	}
}
