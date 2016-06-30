package mata.icalite.api.admin;

import mata.icalite.api.util.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.common.params.CoreAdminParams.CoreAdminAction;
import org.apache.solr.common.util.NamedList;
import org.json.JSONObject;

import com.sun.jersey.api.view.Viewable;

@Path("/collection")
public class Collection {
	private String caxHome = null;
	private String collectionHome = null;
	private String configResourceDir = null;
	private String pearResourceDir = null;
	private String indexRebuilderResource = null;
	private String PearManagerResource = "";
	private String DashboardResource = "";
	private static final String URL = "http://127.0.0.1:8983/solr";
	private SystemControl sc = null;
	private Json json = null;
	private int COLLECTION_DELETE_RETRY = 3;
	private int SLEEP_DELETE_RETRY_MS = 2000;
	ArrayList<String> collectionLists = null;
	private String groupName = null;
	private Logger sysLogger = null;
	private String username = null;
	
	public Collection(){
		caxHome = System.getenv("SOLR_HOME");
		collectionHome = caxHome + "\\example\\solr";

		configResourceDir = caxHome + "\\example\\resources\\config_template\\collection\\conf";
		pearResourceDir = caxHome + "\\example\\resources\\config_template\\collection\\";
		indexRebuilderResource = caxHome + "\\example\\resources\\config_template\\collection\\indexRebuilder.jar";
		PearManagerResource = caxHome + "\\example\\resources\\config_template\\collection\\PearManager.jar";
		DashboardResource = caxHome + "\\example\\resources\\config_template\\collection\\dashboard.json";
		
		sc = new SystemControl();
		json = new Json();
		
		sysLogger = Logger.getLogger(Collection.class);
	}
	
	@GET
	@Produces("application/xml")
	public Viewable getParam(@QueryParam("method") String method,
				@QueryParam("collectionId") String collectionId,
				@QueryParam("sessionId") String session
				) {
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		
		Security secure = new Security();
		collectionLists = new ArrayList<String>();

		try {
			if(!new Security().derbyCheck(session)){
				Map<String,Object> property = new HashMap<String,Object>();
				
				property.put("message", "session expired");
				property.put("value", "1");
				
				apiResponse.put("items", property);
				
				writeLog("Session expired. token: " + session, "info");
				
				return new Viewable("/general/ack", apiResponse);
			}
			else {
				try{
					username = secure.getUser(session);
					groupName = secure.getGroupDerby(username);
					
					if (collectionId != null && 
							!collectionId.isEmpty() &&
							!collectionId.toLowerCase().contains("colgroup")){
						collectionId = groupName + "-" + collectionId;
					}
				}
				catch(Exception e){
					Map<String,Object> errorProperty = new HashMap<String,Object>();
					errorProperty.put("code", "500");
					errorProperty.put("message", e.toString());
					errorProperty.put("detail", sc.getStackTrace(e));
					
					error.add(errorProperty);
					
					writeLog(e, "error");
					
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);
			
			writeLog(e, "error");
			
			e.printStackTrace();
		}
		
		try {
			collectionLists = secure.checkPrivilage(session);
		} catch (Exception e) {
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);
			
			writeLog(e, "error");
			
			e.printStackTrace();
		}
		
		if(error.size() > 0){
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}
		
		if(method.equals("delete")){
			return delete(collectionId,session);
		}else if(method.equals("getList")){
			return getList();
		}else if(method.equals("getLocationCollection")){
			return getLocationCollection(collectionId);
		}else if(method.equals("getStatusCollection")){
			return getStatusCollection(collectionId);
		}else{
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			
			errorProperty.put("code", "405");
			errorProperty.put("message", "Method Not Allowed");
			errorProperty.put("detail", 
					"The REST service does not support the operation implied by the HTTP "
					+ "method for the resource that is addressed by the URI that is "
					+ "passed in");
			
			apiResponse.put("items", errorProperty);
			
			writeLog("Method not allowed: " + method, "error");
			
			return new Viewable("/exception/error", apiResponse);
		}
	}
	
	@POST
	@Produces("application/xml")
	public Viewable postParam(@QueryParam("method") String method,
			@QueryParam("sessionId") String session,
			@QueryParam("collectionId") String collectionId,
			String body) {
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();	
		
		Security secure = new Security();
		JSONObject jsonobj = null;
		
		try {
			if(!new Security().derbyCheck(session)){
				Map<String,Object> property = new HashMap<String,Object>();
				
				property.put("message", "session expired");
				property.put("value", "1");
				
				apiResponse.put("items", property);
				
				writeLog("Session expired. token: " + session, "info");
				
				return new Viewable("/general/ack", apiResponse);
			}
			else {
				try {
					jsonobj = new JSONObject(body);
					collectionId = jsonobj.getString("collectionId");
				} catch (Exception e) {
					Map<String,Object> errorProperty = new HashMap<String,Object>();
					errorProperty.put("code", "500");
					errorProperty.put("message", e.toString());
					errorProperty.put("detail", sc.getStackTrace(e));
					
					error.add(errorProperty);
					
					writeLog(e, "error");
					
					e.printStackTrace();
				}
				
				try{
					username = secure.getUser(session);
					groupName = secure.getGroupDerby(username);
					
					if (collectionId != null && 
							!collectionId.isEmpty() &&
							!collectionId.toLowerCase().contains("colgroup")){
						collectionId = groupName + "-" + collectionId;
					}
				}
				catch(Exception e){
					Map<String,Object> errorProperty = new HashMap<String,Object>();
					errorProperty.put("code", "500");
					errorProperty.put("message", e.toString());
					errorProperty.put("detail", sc.getStackTrace(e));
					
					error.add(errorProperty);
					
					writeLog(e, "error");
					
					e.printStackTrace();
				}
				
				try{
					jsonobj.remove("collectionId");
					jsonobj.put("collectionId", collectionId);
					body = jsonobj.toString();
				}
				catch(Exception e){
					Map<String,Object> errorProperty = new HashMap<String,Object>();
					errorProperty.put("code", "500");
					errorProperty.put("message", e.toString());
					errorProperty.put("detail", sc.getStackTrace(e));
					
					error.add(errorProperty);
					
					writeLog(e, "error");
					
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);
			
			writeLog(e, "error");
			
			e.printStackTrace();
		}
		
		if(error.size() > 0){
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}
				
		if(method.equalsIgnoreCase("create")){
			return create(body,session);
		}else if(method.equalsIgnoreCase("createV2")){
			return createV2(body,session);
		}else if(method.equalsIgnoreCase("edit")){
			return edit(body);
		}else{
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			
			errorProperty.put("code", "405");
			errorProperty.put("message", "Method Not Allowed");
			errorProperty.put("detail", 
					"The REST service does not support the operation implied by the HTTP "
					+ "method for the resource that is addressed by the URI that is "
					+ "passed in");
			
			apiResponse.put("items", errorProperty);
			
			writeLog("Method not allowed: " + method, "error");
			
			return new Viewable("/exception/error", apiResponse);
		}
	}
	
	private Viewable createV2(String body,String session){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		
		List<Object> error = new ArrayList<Object>();
		Map<String, String> jsonElements = null;	
		Security secure = new Security();
		
		String configResourceDirV2 = caxHome + 
				"\\example\\resources\\config_template\\collectionV2\\";
		String collectionId = null;
		
		try{
			username = secure.getUser(session);	
			if(secure.derbyCheckCollection(username)){
				try {
					jsonElements = json.parse(body);
				} catch (Exception e) {
					Map<String,Object> errorProperty = new HashMap<String,Object>();
					errorProperty.put("code", "500");
					errorProperty.put("message", e.toString());
					errorProperty.put("detail", sc.getStackTrace(e));
					
					error.add(errorProperty);
					
					writeLog(e, "error");
					
					e.printStackTrace();
				}
				
				collectionId = jsonElements.get("collectionId");
				String collectionDir = collectionHome + "\\" + collectionId;
	
		        SolrServer server = new HttpSolrServer(URL);
		        ((HttpSolrServer) server).setParser(new XMLResponseParser());
		        
		        File collectionDirFile = new File(collectionDir);
		        
		        if(collectionDirFile.isDirectory()){
		        	Map<String,Object> errorProperty = new HashMap<String,Object>();
					errorProperty.put("code", "500");
					
					String message = "Collection ID: " + collectionId + " is already exist.";
					String detail = "Choose different collection ID.";
					
					errorProperty.put("message", message);
					errorProperty.put("detail", detail);
					
					error.add(errorProperty);
					
					writeLog(message + detail, "error");
					
					System.out.println(message + detail);
		        }else{
		        	collectionDirFile.mkdirs();
		        	ArrayList<String> listPear = new ArrayList<String>();
		        	FileManager fman = new FileManager();
		        	listPear = fman.listFile(pearResourceDir);
		        	for(String pear : listPear){
		        		if(pear.contains(".pear")){
		        			fman.copyFile(pearResourceDir+pear, collectionDir+"\\"+pear);
		        		}
		        	}	
		        }
		       
		        try {
		        	FileUtils.copyDirectory(new File(configResourceDirV2), 
		        			collectionDirFile);
				} catch (Exception e) {
					Map<String,Object> errorProperty = new HashMap<String,Object>();
					errorProperty.put("code", "500");
					errorProperty.put("message", e.toString());
					errorProperty.put("detail", sc.getStackTrace(e));
					
					error.add(errorProperty);
					
					writeLog(e, "error");
					
					e.printStackTrace();
				}
		        
		        //String collectionId = jsonElements.get("collectionId").replace(" ", "_");
	//			System.out.println("coldir" +collectionDir);
				try {
					//changing MasterTemplate to current collectoinId
					ArrayList<String> listFileToChange = new ArrayList<String>();
					listFileToChange = getListFileToChange(collectionDir);
					FileManager fm = new FileManager();
					for(String fileToChange : listFileToChange){
						String content = fm.readData(fileToChange);
						content = content.replace("COLGROUP-DEV-MasterTemplate", 
								collectionId);
						fm.fileWriter(fileToChange, content, false);
					}
					new FileManager().fileWriter(collectionDir+"\\status.collection", 
							"status=unloaded", false);
				} catch (Exception e) {
					Map<String,Object> errorProperty = new HashMap<String,Object>();
					errorProperty.put("code", "500");
					errorProperty.put("message", e.toString());
					errorProperty.put("detail", sc.getStackTrace(e));
					
					error.add(errorProperty);
					
					writeLog(e, "error");
					
					e.printStackTrace();
				}
		        
		        if(new File(collectionHome + "\\" + collectionId).isDirectory()){
		        	Pattern pattern = Pattern.compile("[0-9]*$");
		        	Matcher matcher = pattern.matcher(collectionId);
		        	if(matcher.matches()){
		        		String num = null;
		        		while(matcher.find()) {
		        		    if(matcher.hitEnd()){
		        		    	num = matcher.group(1);
		        		    }
		        		}
		        		collectionId = collectionId.replace(num, 
		        				(Integer.toString(Integer.parseInt(num) + 1)));
		        	}
		        }
		        
		        try {
					CoreAdminRequest.createCore(collectionId, collectionId, server);
					new FileManager().fileWriter(collectionDir+"\\status.collection", 
							"status=idle", false);
				} catch (Exception e) {
					Map<String,Object> errorProperty = new HashMap<String,Object>();
					errorProperty.put("code", "500");
					errorProperty.put("message", e.toString());
					errorProperty.put("detail", sc.getStackTrace(e));
					
					error.add(errorProperty);
					
					writeLog(e, "error");
					
					e.printStackTrace();
				}
				
				if(error.size() > 0){
					apiResponse.put("items", error);
					return new Viewable("/exception/error", apiResponse);
				}else{
					System.out.println("User : "+username+" creating collection : " + 
							collectionId+"!");
					secure.addPrivilage(session, collectionId);
					secure.addAdminPrivilage(collectionId);
					secure.insertLimitCollection(username);
					secure.createLimitCrawler(collectionId, username);
					
					Map<String,Object> property = new HashMap<String,Object>();
					
					property.put("message", "successful");
					property.put("value", "0");
					
					apiResponse.put("items", property);
					
					writeLog("Collection ID: " + collectionId + " was created successfully.", "info");
					
					return new Viewable("/general/ack", apiResponse);
				}
			}
			else{
				Map<String,Object> property = new HashMap<String,Object>();
				
				String message = "Collection limit was reached."
						+ "Unable create new collection";
				
				property.put("message", message);
				property.put("value", "2");
				
				apiResponse.put("items", property);
				
				writeLog(message, "info");
				
				return new Viewable("/general/ack", apiResponse);
			}
		}
		catch(Exception e){
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);
			
			writeLog(e, "error");
			
			e.printStackTrace();
		}
		
		if(error.size() > 0){
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", "successful");
			property.put("value", "0");
			
			apiResponse.put("items", property);
			
			writeLog("Collection ID: " + collectionId + " was created successfully.", "info");
			
			return new Viewable("/general/ack", apiResponse);
		}
	}
	
	private Viewable create(String body,String session){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		
		List<Object> error = new ArrayList<Object>();
		Map<String, String> jsonElements = null;	
		Security secure = new Security();
		
		String collectionId = null;
		
		try{
			username = secure.getUser(session);	
			if(secure.derbyCheckCollection(username)){
				try {
					jsonElements = json.parse(body);
				} catch (Exception e) {
					Map<String,Object> errorProperty = new HashMap<String,Object>();
					errorProperty.put("code", "500");
					errorProperty.put("message", e.toString());
					errorProperty.put("detail", sc.getStackTrace(e));
					
					error.add(errorProperty);
					
					writeLog(e, "error");
					
					e.printStackTrace();
				}
				
				collectionId = jsonElements.get("collectionId");
				String collectionDir = collectionHome + "\\" + collectionId;
				String configCollectionDir = collectionHome + "\\" + jsonElements.get("collectionId") +
						"\\conf";
				String jarCollectionDir = collectionHome + "\\" + jsonElements.get("collectionId") +
						"\\lib";
				String descCollectionDir = collectionHome + "\\" + jsonElements.get("collectionId") +
						"\\desc";
	
		        SolrServer server = new HttpSolrServer(URL);
		        ((HttpSolrServer) server).setParser(new XMLResponseParser());
		        
		        File collectionDirFile = new File(collectionDir);
		        File configCollectionDirFile = new File(configCollectionDir);
		        File jarCollectionDirFile = new File(jarCollectionDir);
		        File descCollectionDirFile = new File(descCollectionDir);
		        FileManager fman = new FileManager();
		        
		        if(collectionDirFile.isDirectory()){
		        	Map<String,Object> errorProperty = new HashMap<String,Object>();
					errorProperty.put("code", "500");
					
					String message = "Collection ID: " + collectionId + " already exist.";
					String detail = "Choose different collection ID.";
					
					errorProperty.put("message", message);
					errorProperty.put("detail", detail);
					
					writeLog(message + detail, "error");
					
					error.add(errorProperty);
		        }else{
		        	collectionDirFile.mkdirs();
		        	configCollectionDirFile.mkdirs();
		        	jarCollectionDirFile.mkdirs();
		        	descCollectionDirFile.mkdirs();
		        	ArrayList<String> listPear = new ArrayList<String>();
		        	listPear = fman.listFile(pearResourceDir);
		        	for(String pear : listPear){
		        		if(pear.contains(".pear")){
		        			fman.copyFile(pearResourceDir+pear, collectionDir+"\\"+pear);
		        		}
		        	}	
		        	fman.copyFile(indexRebuilderResource, collectionDir+"\\IndexRebuilder.jar");
		        	fman.copyFile(PearManagerResource, collectionDir+"\\PearManager.jar");
		        	fman.copyFile(DashboardResource, collectionDir+"\\dashboard.json");
		        }
		       
		        try {
		        	FileUtils.copyDirectory(new File(configResourceDir), configCollectionDirFile);
				} catch (Exception e) {
					Map<String,Object> errorProperty = new HashMap<String,Object>();
					errorProperty.put("code", "500");
					errorProperty.put("message", e.toString());
					errorProperty.put("detail", sc.getStackTrace(e));
					
					error.add(errorProperty);
					
					writeLog(e, "error");
					
					e.printStackTrace();
				}
		        
	//			System.out.println("coldir" +collectionDir);
				try {
					new FileManager().fileWriter(collectionDir+"\\status.collection", "status=idle", false);
				} catch (Exception e) {
					Map<String,Object> errorProperty = new HashMap<String,Object>();
					errorProperty.put("code", "500");
					errorProperty.put("message", e.toString());
					errorProperty.put("detail", sc.getStackTrace(e));
					
					error.add(errorProperty);
					
					writeLog(e, "error");
					
					e.printStackTrace();
				}
				
		        //String collectionId = jsonElements.get("collectionId").replace(" ", "_");
		        
		        if(new File(collectionHome + "\\" + collectionId).isDirectory()){
		        	Pattern pattern = Pattern.compile("[0-9]*$");
		        	Matcher matcher = pattern.matcher(collectionId);
		        	if(matcher.matches()){
		        		String num = null;
		        		while(matcher.find()) {
		        		    if(matcher.hitEnd()){
		        		    	num = matcher.group(1);
		        		    }
		        		}
		        		collectionId = collectionId.replace(num, 
		        				(Integer.toString(Integer.parseInt(num) + 1)));
		        	}
		        }
		        
				try {
					new Pear().installPear(collectionId, "GenAnnoV1_5_1_1");
					
					String schemaXmlLoc = caxHome + "\\example\\resources\\config_template\\collectionV2\\conf\\schema.xml";
					String solrXmlLoc = caxHome + "\\example\\resources\\config_template\\collectionV2\\conf\\solrconfig.xml";
					fman.copyFile(schemaXmlLoc, collectionDir+"\\conf\\schema.xml");
					fman.copyFile(solrXmlLoc, collectionDir+"\\conf\\solrconfig.xml");
					
					CoreAdminRequest.unloadCore(collectionId, false, false, server);
					Thread.sleep(3000);
					CoreAdminRequest.createCore(collectionId, collectionId, server);
				} catch (Exception e) {
					Map<String,Object> errorProperty = new HashMap<String,Object>();
					errorProperty.put("code", "500");
					errorProperty.put("message", e.toString());
					errorProperty.put("detail", sc.getStackTrace(e));
					
					error.add(errorProperty);
					
					writeLog(e, "error");
					
					e.printStackTrace();
				}
				
				if(error.size() > 0){
					apiResponse.put("items", error);
					return new Viewable("/exception/error", apiResponse);
				}else{
					System.out.println("User : "+username+" creating collection : "+collectionId+"!");
					secure.addPrivilage(session, collectionId);
					secure.addAdminPrivilage(collectionId);
					secure.insertLimitCollection(username);
					secure.createLimitCrawler(collectionId, username);
					
					Map<String,Object> property = new HashMap<String,Object>();
					
					property.put("message", "successful");
					property.put("value", "0");
					
					apiResponse.put("items", property);
					
					writeLog("Collection ID: " + collectionId + " was created successfully.", "info");
					
					return new Viewable("/general/ack", apiResponse);
				}
			}
			else{
				Map<String,Object> property = new HashMap<String,Object>();
				
				String message = "Collection limit was reached."
						+ "Unable create new collection";
				
				property.put("message", message);
				property.put("value", "2");
				
				apiResponse.put("items", property);
				
				writeLog(message, "info");
				
				return new Viewable("/general/ack", apiResponse);
			}
		}
		catch(Exception e){
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);
			
			writeLog(e, "error");
			
			e.printStackTrace();
		}
		
		if(error.size() > 0){
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", "successful");
			property.put("value", "0");
			
			apiResponse.put("items", property);
			
			writeLog("Collection ID: " + collectionId + " was created successfully.", "info");
			
			return new Viewable("/general/ack", apiResponse);
		}
	}
	
	private Viewable delete(String collectionId, String session){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		
		List<Object> error = new ArrayList<Object>();
		
		Security secure = new Security();
		
		try{
//			String username = secure.getUser(session);
			Crawler crawler = new Crawler();
			
			boolean isAnyCrawlerRun = crawler.isAnyCrawlerRun(collectionId);
			
			if(isAnyCrawlerRun){
				Map<String,Object> errorProperty = new HashMap<String,Object>();
				errorProperty.put("code", "500");
				String message = "Unable to delete collection: " + collectionId + 
						" because of its running crawler(s).";
				String detail = "Stop all the crawler(s) and try to delete collection again.";
				
				errorProperty.put("message", message);
				errorProperty.put("detail", detail);
				
				error.add(errorProperty);
				
//				System.out.println(message + "\n" + detail);
				
				apiResponse.put("items", error);
				
				writeLog(message + detail, "info");
				
				return new Viewable("/exception/error", apiResponse);
			}
			
			SolrServer server = new HttpSolrServer(URL);
			((HttpSolrServer) server).setParser(new XMLResponseParser());
			
			try {
				if(new Core().isCoreLoaded(collectionId)){
					try {
						CoreAdminRequest.unloadCore(collectionId, true, true, server);
					} catch (Exception e) {
						Map<String,Object> errorProperty = new HashMap<String,Object>();
						errorProperty.put("code", "500");
						errorProperty.put("message", e.toString());
						errorProperty.put("detail", sc.getStackTrace(e));
						
						error.add(errorProperty);
						
						writeLog(e, "error");
						
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				Map<String,Object> errorProperty = new HashMap<String,Object>();
				errorProperty.put("code", "500");
				errorProperty.put("message", e.toString());
				errorProperty.put("detail", sc.getStackTrace(e));
				
				error.add(errorProperty);
				
				writeLog(e, "error");
				
				e.printStackTrace();
			}
			
			File collectionHomeDir = new File(collectionHome);
			File[] collectionHomeFiles = collectionHomeDir.listFiles();
			
			for(File collectionHomeFile : collectionHomeFiles){
				if(collectionHomeFile.isDirectory() && 
						collectionHomeFile.getName().startsWith(collectionId)){
					
					try {
						FileUtils.deleteDirectory(collectionHomeFile);
					} catch (Exception e) {
						int retry = 0;
						
						while(true){
							if(collectionHomeFile.listFiles().length < 1){
								break;
							}
							
							if(retry > COLLECTION_DELETE_RETRY){
								break;
							}
							
							for (File file : collectionHomeFile.listFiles()) {
							    try {
									sc.runExec("unlocker \"" + file.getAbsolutePath() + "\" -S -D");
								} catch (Exception e1) {
									Map<String,Object> errorProperty = new HashMap<String,Object>();
									errorProperty.put("code", "500");
									errorProperty.put("message", e1.toString());
									errorProperty.put("detail", sc.getStackTrace(e1));
									
									error.add(errorProperty);
									
									writeLog(e1, "error");
									
									e1.printStackTrace();
								}
							}
							
							retry++;
							
							try {
								Thread.sleep(SLEEP_DELETE_RETRY_MS);
							} catch (InterruptedException e1) {
								Map<String,Object> errorProperty = new HashMap<String,Object>();
								errorProperty.put("code", "500");
								errorProperty.put("message", e1.toString());
								errorProperty.put("detail", sc.getStackTrace(e1));
								
								error.add(errorProperty);
								
								writeLog(e1, "error");
								
								e1.printStackTrace();
							}
						}
						
						try {
							FileUtils.deleteDirectory(collectionHomeFile);
						} catch (IOException e1) {
							Map<String,Object> errorProperty = new HashMap<String,Object>();
							errorProperty.put("code", "500");
							errorProperty.put("message", e1.toString());
							errorProperty.put("detail", sc.getStackTrace(e1));
							
							error.add(errorProperty);
							
							writeLog(e1, "error");
							
							e1.printStackTrace();
						}
						
						Map<String,Object> errorProperty = new HashMap<String,Object>();
						errorProperty.put("code", "500");
						errorProperty.put("message", e.toString());
						errorProperty.put("detail", sc.getStackTrace(e));
						
						error.add(errorProperty);
						
						writeLog(e, "error");
						
						e.printStackTrace();
					}
				}
			}
			
			try {
				System.out.println("collection : "+collectionId+ " deleted!");
				secure.removePrivilage(session, collectionId);
				secure.remAdminPrivilege(collectionId);
				secure.removeLimitCollection(collectionId);
				secure.deleteLimitCrawler(collectionId);
			} catch (Exception e) {
				Map<String,Object> errorProperty = new HashMap<String,Object>();
				errorProperty.put("code", "500");
				errorProperty.put("message", e.toString());
				errorProperty.put("detail", sc.getStackTrace(e));
				
				error.add(errorProperty);
				
				writeLog(e, "error");
				
				e.printStackTrace();
			}
			
			if(error.size() > 0){
				apiResponse.put("items", error);
				return new Viewable("/exception/error", apiResponse);
			}else{
				Map<String,Object> property = new HashMap<String,Object>();
				
				property.put("message", "successful");
				property.put("value", "0");
				
				apiResponse.put("items", property);
				
				writeLog("Collection ID: " + collectionId + " was deleted successfully.", "info");
				
				return new Viewable("/general/ack", apiResponse);
			}
		}
		catch (Exception e){
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);
			
			writeLog(e, "error");
			
			e.printStackTrace();
		}
		
		if(error.size() > 0){
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", "successful");
			property.put("value", "0");
			
			apiResponse.put("items", property);
			
			writeLog("Collection ID: " + collectionId + " was deleted successfully.", "info");
			
			return new Viewable("/general/ack", apiResponse);
		}
	}
	
	private Viewable edit(String body){
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
			
			writeLog(e, "error");
			
			e.printStackTrace();
		}
		
		SolrServer server = new HttpSolrServer(URL);
        ((HttpSolrServer) server).setParser(new XMLResponseParser());
        
        String collectionId = jsonElements.get("collectionId");
        
        try {
			CoreAdminRequest.renameCore(collectionId, 
					jsonElements.get("newCollectionId"), server);
		} catch (Exception e) {
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);
			
			writeLog(e, "error");
			
			e.printStackTrace();
		}
		
		if(error.size() > 0){
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", "successful");
			property.put("value", "0");
			
			apiResponse.put("items", property);			
			
			writeLog("Collection ID: " + collectionId + " was edited successfully.", "info");

			return new Viewable("/general/ack", apiResponse);
		}
	}
	
	private Viewable getLocationCollection(String collectionId){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		
		List<Object> error = new ArrayList<Object>();
		String location = "";
		try{
			location = collectionHome+"\\"+collectionId;
		}
		catch(Exception e){
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);
			
			writeLog(e, "error");
			
			e.printStackTrace();
		}
		
		if(error.size() > 0){
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", location);
			property.put("value", "0");
			
			apiResponse.put("items", property);
			
			writeLog("Collection ID: " + collectionId + " ,location: " + location, "info");
			
			return new Viewable("/general/ack", apiResponse);
		}
		
	}
	
	private Viewable getList(){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		
		List<Object> collectionDefinition = new ArrayList<Object>();
		List<Object> error = new ArrayList<Object>();
		ArrayList<String> existCollections = getExistCollection();
		
		SolrServer server = new HttpSolrServer(URL);
		((HttpSolrServer) server).setParser(new XMLResponseParser());
		
		CoreAdminRequest request = new CoreAdminRequest();
		CoreAdminResponse cores = null;
		
		try {
			request.setAction(CoreAdminAction.STATUS);
			cores = request.process(server);
		} catch (Exception e) {
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);
			
			writeLog(e, "error");
			
			e.printStackTrace();
		}
		
		if(cores != null){
			for(String existCollection : existCollections){
				if(collectionLists.contains(existCollection)){
					boolean isLoaded = false;
					
					for(int i = 0; i < cores.getCoreStatus().size(); i++){
						if(existCollection.equals(cores.getCoreStatus().getName(i))){
							isLoaded = true;
							break;
						}
					}
					
					Map<String,Object> collectionProperty = new HashMap<String,Object>();
					List<Object> keyValuePair = new ArrayList<Object>();
					
					if(isLoaded){
						NamedList<?> statusList = cores.getCoreStatus(existCollection);
						
						for(Object obj : statusList){
							String[] keyValueString = obj.toString().split("=");
							
							String key = keyValueString[0];
							
							if(key.equals("index")){
								String[] keyValueIndexString = obj.toString().split(",");
								for(int j=0; j < keyValueIndexString.length; j++){
									Map<String,Object> keyValue = new HashMap<String,Object>();
									String norm = keyValueIndexString[j].
											replaceAll("^[a-zA-Z]*$=\\{|\\}", "");
									
									String[] separated = norm.split("=");
		
									if(norm.startsWith("directory")){
										keyValue.put("key", separated[0].
												replaceAll("\\{|\\}", ""));
										String conc = "";
										for(int k = 1;k < separated.length; k++){
											conc = conc + "=" + separated[k];
										}
										keyValue.put("value", conc.substring(1));
									}else{
										keyValue.put("key", separated[separated.length-2].
												replaceAll("\\{|\\}", ""));
										keyValue.put("value", separated[separated.length-1].
												replaceAll("\\{|\\}", ""));
									}
									
									keyValuePair.add(keyValue);
								}
							}else if(key.equals("name")){
								Map<String,Object> keyValue = new HashMap<String,Object>();
								String value =  keyValueString[1];
								
								keyValue.put("key", key);
								keyValue.put("value", value.replace(groupName + "-", ""));
								
								keyValuePair.add(keyValue);
							}else{
								Map<String,Object> keyValue = new HashMap<String,Object>();
								String value =  keyValueString[1];
								
								keyValue.put("key", key);
								keyValue.put("value", value);
								
								keyValuePair.add(keyValue);
							}
						}
						
						//Add group node
						Map<String,Object> keyValue = new HashMap<String,Object>();
						
						keyValue.put("key", "group");
						keyValue.put("value", groupName);
						
						keyValuePair.add(keyValue);
					}else{
						String name = existCollection.replace(groupName + "-", "");
						
						String[] keys = {"name", "group"};
						String[] values = {name, groupName};
						
						for(int i = 0; i < keys.length; i++){
							Map<String,Object> keyValue = new HashMap<String,Object>();
							
							keyValue.put("key", keys[i]);
							keyValue.put("value", values[i]);
							
							keyValuePair.add(keyValue);
						}
					}
					
					collectionProperty.put("keyValuePair", keyValuePair);
					collectionDefinition.add(collectionProperty);
				}
			}
		}
		
		if(error.size() > 0){
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}else{
			apiResponse.put("items", collectionDefinition);
			
			writeLog("Collection list retrieved successfully.", "info");
			
			return new Viewable("/collection/getList", apiResponse);
		}
	}
	
	private ArrayList<String> getExistCollection(){
		ArrayList<String> collections = new ArrayList<String>();
		
		for(File colDir : new File(collectionHome).listFiles()){
			String colDirName = colDir.getName();
			if(!colDirName.contains(".") && !colDirName.contains("_")){
				collections.add(colDirName);
			}
		}
		
		return collections;
	}
	
	private Viewable getStatusCollection(String collectionId){
		
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		
		List<Object> error = new ArrayList<Object>();
		String status = "idle";
		String collectionStatus = collectionHome+"\\"+collectionId+"\\status.collection";
		
		if(!new File(collectionStatus).exists()){
			try {
				new FileManager().fileWriter(collectionStatus, "status=idle", false);
			} catch (Exception e) {
				Map<String,Object> errorProperty = new HashMap<String,Object>();
				errorProperty.put("code", "500");
				errorProperty.put("message", e.toString());
				errorProperty.put("detail", sc.getStackTrace(e));
				
				error.add(errorProperty);
				
				writeLog(e, "error");
				
				e.printStackTrace();
			}
		}
		
		try{
			Properties prop = new Properties();
			FileInputStream in = new FileInputStream(collectionStatus);
			prop.load(in);
			
			status = (prop.getProperty("status"));
		}
		catch(Exception e){
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);
			
			writeLog(e, "error");
			
			e.printStackTrace();			
		}
		
		if(error.size() > 0){
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", status);
			property.put("value", "0");
			
			apiResponse.put("items", property);
			
			writeLog("Collection ID: " + collectionId + " status retrieved successfully.", "info");
			
			return new Viewable("/general/ack", apiResponse);
		}
		
	}
	
	private ArrayList<String> getListFileToChange(String collectionDir){
		ArrayList<String> listFileToChange = new ArrayList<String>();
		
		listFileToChange.add(collectionDir+"\\conf\\solrconfig.xml");
		listFileToChange.add(collectionDir+"\\desc\\aeDescriptor.xml");
		listFileToChange.add(collectionDir+"\\desc\\aeDescriptor.xml.bak");
		listFileToChange.add(collectionDir+"\\install.bat");
		listFileToChange.add(collectionDir+"\\pearinstall\\desc\\aeDescriptor.xml");
		listFileToChange.add(collectionDir+"\\pearinstall\\installpear.bat");
		listFileToChange.add(collectionDir+"\\pearinstall\\tobin.bat");
		listFileToChange.add(collectionDir+"\\peartemp\\com.mata.genanno.MAXAnno\\com.mata.genanno.MAXAnno_pear.xml");
		listFileToChange.add(collectionDir+"\\peartemp\\com.mata.genanno.MAXAnno\\desc\\LangID0.xml");
		listFileToChange.add(collectionDir+"\\peartemp\\com.mata.genanno.MAXAnno\\desc\\LWAnnotator1.xml");
		listFileToChange.add(collectionDir+"\\peartemp\\com.mata.genanno.MAXAnno\\desc\\LWShallowParser3.xml");
		listFileToChange.add(collectionDir+"\\peartemp\\com.mata.genanno.MAXAnno\\metadata\\install.xml");
		listFileToChange.add(collectionDir+"\\peartemp\\com.mata.genanno.MAXAnno\\metadata\\PEAR.properties");
		listFileToChange.add(collectionDir+"\\peartemp\\com.mata.genanno.MAXAnno\\metadata\\setenv.txt");
		listFileToChange.add(collectionDir+"\\peartemp\\tobin.bat");
		listFileToChange.add(collectionDir+"\\StartPearManager.bat");		
		
		return listFileToChange;
	}
	
	private void writeLog(Object content, String type){
		content = "USER:" + username + " " + content;
		if(type.equals("debug")){
			sysLogger.debug(content);
			//userLogger.debug(content);
		}else if(type.equals("info")){
			sysLogger.info(content);
			//userLogger.info(content);
		}else if(type.equals("warn")){
			sysLogger.warn(content);
			//userLogger.warn(content);
		}else if(type.equals("error")){
			sysLogger.error(content);
			//userLogger.error(content);
		}else if(type.equals("fatal")){
			sysLogger.fatal(content);
			//userLogger.fatal(content);
		}
	}
}
