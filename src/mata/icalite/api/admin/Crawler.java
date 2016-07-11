package mata.icalite.api.admin;

import mata.icalite.api.util.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.google.common.net.InternetDomainName;
import com.sun.jersey.api.view.Viewable;

@Path("/crawler")
public class Crawler {
	private String caxHome = null;
	private String collectionHome = null;
	private FileManager fm = null;
	private SystemControl sc = null;
	private Json json = null;
	private int CRAWLER_DELETE_RETRY = 3;
	private int SLEEP_DELETE_RETRY_MS = 2000;
	private String username = null;
	private List<String> errorMessages = null;
	private List<String> errorDetails = null;
	private Logger sysLogger = null;

	public Crawler(){
		caxHome = System.getenv("SOLR_HOME");
		collectionHome = caxHome + "\\example\\solr";
		
		sc = new SystemControl();
		json = new Json();
		fm = new FileManager();
		
		errorMessages = new ArrayList<String>();
		errorDetails = new ArrayList<String>();
		
		sysLogger = Logger.getLogger(Crawler.class);
	}
	
	@GET
	@Produces("application/xml")
	public Viewable getParam(@QueryParam("method") String method,
				@QueryParam("collectionId") String collectionId,
				@QueryParam("crawlerId") String crawlerId,
				@QueryParam("template") String template,
				@QueryParam("type") String type,
				@QueryParam("encodedUrl") String encodedUrl,
				@QueryParam("sessionId") String session
			) {
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		//List<Object> error = new ArrayList<Object>();
		
		Security secure = new Security();
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
					if (!collectionId.toLowerCase().contains("colgroup")){
						username = secure.getUser(session);
						String group = secure.getGroupDerby(username);
						collectionId = group + "-" + collectionId;
					}
				}
				catch(Exception e){
					errorMessages.add(e.toString());
					errorDetails.add(sc.getStackTrace(e));
					
					writeLog(e, "error");
					
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			/*Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);*/
			errorMessages.add(e.toString());
			errorDetails.add(sc.getStackTrace(e));
			
			writeLog(e, "error");
			
			e.printStackTrace();
		}
		
		if(method.equals("getList")){
			return getList(collectionId);
		}else if(method.equals("getState")){
			return getState(collectionId, crawlerId);
		}else if(method.equals("delete")){
			return delete(collectionId, crawlerId, session);
		}else if(method.equals("start")){
			return start(collectionId, crawlerId);
		}else if(method.equals("stop")){
			return stop(collectionId, crawlerId);
		}else if(method.equals("fullRecrawl")){
			return fullRecrawl(collectionId, crawlerId);
		}else if(method.equals("getRecentlyCrawled")){
			return getRecentlyCrawled(collectionId, crawlerId);
		}else if(method.equals("getAllCrawled")){
			return getAllCrawled(collectionId, crawlerId);
		}else if(method.equals("deleteUrlCache")){
			return deleteUrlCache(collectionId, crawlerId);
		}else if(method.equals("getTotalDocument")){
			return getTotalDocument(collectionId, crawlerId);
		}else if(method.equals("getListTemplate")){
			return getListTemplate(type);
		}else if(method.equals("checkIfUrlIsOnList")){
			return checkIfUrlIsOnList(encodedUrl);
		}else if(method.equals("getTemplate")){
			return getTemplate(template,type);
		}else{
			/*Map<String,Object> errorProperty = new HashMap<String,Object>();
			
			errorProperty.put("code", "405");
			errorProperty.put("message", "Method Not Allowed");
			errorProperty.put("detail", 
					"The REST service does not support the operation implied by the HTTP "
					+ "method for the resource that is addressed by the URI that is "
					+ "passed in");*/
			
			errorMessages.add("Method Not Allowed");
			errorDetails.add("The REST service does not support the operation implied by the HTTP "
					+ "method for the resource that is addressed by the URI that is "
					+ "passed in");
			
			writeLog("Method not allowed: " + method, "error");
		}
		
		Map<String,Object> error = new HashMap<String,Object>();
		
		String messages = "";
		for(String message : errorMessages){
			messages += message + "\n";
		}
		
		error.put("message", messages);
		
		String details = "";
		for(String detail : errorDetails){
			details += detail + "\n";
		}
		
		error.put("detail", details);
		error.put("value", "1");
		
		apiResponse.put("items", error);
		
		return new Viewable("/exception/error", apiResponse);
	}
	
	@POST
	@Produces("application/xml")
	public Viewable postParam(@QueryParam("method") String method, 
			String body,
			@QueryParam("collectionId") String collectionId,
			@QueryParam("sessionId") String session,
			@QueryParam("crawlerId") String crawlerId) {
		
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		//List<Object> error = new ArrayList<Object>();
		Security secure = new Security();
		JSONObject jsonobj = null;
		String collectionIdTemp = "";
		
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
					collectionIdTemp = jsonobj.getString("collectionId");
					if(collectionIdTemp.length()>1){
						collectionId = collectionIdTemp;
					}
				} catch (Exception e) {
					/*Map<String,Object> errorProperty = new HashMap<String,Object>();
					errorProperty.put("code", "500");
					errorProperty.put("message", e.toString());
					errorProperty.put("detail", sc.getStackTrace(e));*/
					errorMessages.add(e.toString());
					errorDetails.add(sc.getStackTrace(e));
					
					writeLog(e, "error");
					
					e.printStackTrace();
					
//					error.add(errorProperty);
//					System.out.println("no body collection ID found");
//					e.printStackTrace();
				}
				
				try{
					if (!collectionId.toLowerCase().contains("colgroup")){
						String username = secure.getUser(session);
						String groups = secure.getGroupDerby(username);
						collectionId = groups+"-"+collectionId;
					}
				}catch(Exception e){
					/*Map<String,Object> errorProperty = new HashMap<String,Object>();
					errorProperty.put("code", "500");
					errorProperty.put("message", e.toString());
					errorProperty.put("detail", sc.getStackTrace(e));
					
					error.add(errorProperty);*/
					errorMessages.add(e.toString());
					errorDetails.add(sc.getStackTrace(e));
					
					writeLog(e, "error");
					
					e.printStackTrace();
				}
				
				try{
					collectionId = jsonobj.getString("collectionId");
					if(collectionId.length()>1){
						jsonobj.remove("collectionId");
						jsonobj.put("collectionId", collectionId);
						if(collectionIdTemp.length()>1){
							body = jsonobj.toString();
						}
						
					}
				}catch(Exception e){
					/*Map<String,Object> errorProperty = new HashMap<String,Object>();
					errorProperty.put("code", "500");
					errorProperty.put("message", e.toString());
					errorProperty.put("detail", sc.getStackTrace(e));*/
					errorMessages.add(e.toString());
					errorDetails.add(sc.getStackTrace(e));
					
					writeLog(e, "error");
					
//					System.out.println("no body collection ID found");
					
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			/*Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);*/
			errorMessages.add(e.toString());
			errorDetails.add(sc.getStackTrace(e));
			
			writeLog(e, "error");
			
			e.printStackTrace();
		}
		
		if(method.equalsIgnoreCase("create")){
			return create(body, collectionId, session);
		}else if(method.equalsIgnoreCase("edit")){
			return edit(body, collectionId, crawlerId);
		}else if(method.equalsIgnoreCase("createXmlConfig")){
			return createXmlConfig(collectionId, crawlerId, body);
		}else{
			/*Map<String,Object> errorProperty = new HashMap<String,Object>();
			
			errorProperty.put("code", "405");
			errorProperty.put("message", "Method Not Allowed");
			errorProperty.put("detail", 
					"The REST service does not support the operation implied by the HTTP "
					+ "method for the resource that is addressed by the URI that is "
					+ "passed in");
			
			apiResponse.put("items", errorProperty);
			return new Viewable("/exception/error", apiResponse);*/
			errorMessages.add("Method Not Allowed");
			errorDetails.add("The REST service does not support the operation implied by the HTTP "
					+ "method for the resource that is addressed by the URI that is "
					+ "passed in");
			
			writeLog("Method not allowed: " + method, "error");
		}
		
		Map<String,Object> error = new HashMap<String,Object>();
		
		String messages = "";
		for(String message : errorMessages){
			messages += message + "\n";
		}
		
		error.put("message", messages);
		
		String details = "";
		for(String detail : errorDetails){
			details += detail + "\n";
		}
		
		error.put("detail", details);
		error.put("value", "1");
		
		apiResponse.put("items", error);
		
		return new Viewable("/exception/error", apiResponse);
	}
	
	private Viewable getList(String collectionId){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		
		List<Object> crawlerDefinition = new ArrayList<Object>();
		//List<Object> error = new ArrayList<Object>();
		
		InputStream inputPropFile = null;
		
		File colHome = new File(collectionHome);
		
		try{
			for(File colHomeFile : colHome.listFiles()){
				if(colHomeFile.isDirectory()){
					String[] colHomeFileParts = colHomeFile.getName().split("\\.");
					if(colHomeFileParts.length > 1 && colHomeFileParts[0].equals(collectionId)){
						File colDir = new File(colHomeFile.getAbsolutePath());
	
						Map<String,Object> crawlerProperty = new HashMap<String,Object>();
						List<Object> keyValuePair = new ArrayList<Object>();
						
						for(File colFile : colDir.listFiles()){
							if(colFile.isFile() && colFile.getName().startsWith("config")){
								try {
									inputPropFile = new FileInputStream(
											colFile.getAbsolutePath());
							 
									Properties prop = new Properties();
									prop.load(inputPropFile);
									
									inputPropFile.close();
							 
									@SuppressWarnings("rawtypes")
									Enumeration em = prop.keys();
									
									while(em.hasMoreElements()){
										String key = (String) em.nextElement();
										String value = (String) prop.get(key);
										Map<String,Object> keyValue = new HashMap<String,Object>();
										keyValue.put("key", key);
										keyValue.put("value", value);
										
										keyValuePair.add(keyValue);
									}
									
									crawlerProperty.put("keyValuePair", keyValuePair);
								} catch (Exception e) {
									/*Map<String,Object> errorProperty = new HashMap<String,Object>();
									errorProperty.put("code", "500");
									errorProperty.put("message", e.toString());
									errorProperty.put("detail", sc.getStackTrace(e));
									
									error.add(errorProperty);*/
									errorMessages.add(e.toString());
									errorDetails.add(sc.getStackTrace(e));
									
									writeLog(e, "error");
									
									e.printStackTrace();
								} finally {
									if (inputPropFile != null) {
										try {
											inputPropFile.close();
										} catch (Exception e) {
											/*Map<String,Object> errorProperty = new HashMap<String,Object>();
											errorProperty.put("code", "500");
											errorProperty.put("message", e.toString());
											errorProperty.put("detail", sc.getStackTrace(e));
											
											error.add(errorProperty);*/
											errorMessages.add(e.toString());
											errorDetails.add(sc.getStackTrace(e));
											
											writeLog(e, "error");
											
											e.printStackTrace();
										}
									}
								}
							}else if(colFile.isFile() && colFile.getName().endsWith("config.xml")){
								Map<String,Object> keyValue = new HashMap<String,Object>();
								
								keyValue.put("key", "xpath");
								
								try {
									keyValue.put("value", FileUtils.readFileToString(colFile));
								} catch (Exception e) {
									errorMessages.add(e.toString());
									errorDetails.add(sc.getStackTrace(e));
									
									writeLog(e, "error");
									
									e.printStackTrace();
								}
								
								keyValuePair.add(keyValue);
								
								crawlerProperty.put("keyValuePair", keyValuePair);
							}
						}
						
						crawlerDefinition.add(crawlerProperty);
					}
				}
			}
		} catch (Exception e) {
			errorMessages.add(e.toString());
			errorDetails.add(sc.getStackTrace(e));
			
			writeLog(e, "error");
			
			e.printStackTrace();
		}
		
		if(errorMessages.size() > 0){
			Map<String,Object> error = new HashMap<String,Object>();
			String messages = "";
			for(String message : errorMessages){
				messages += message + "\n";
			}
			
			error.put("message", messages);
			
			String details = "";
			for(String detail : errorDetails){
				details += detail + "\n";
			}
			
			error.put("detail", details);
			error.put("value", "1");
			
			apiResponse.put("items", error);
			
			writeLog("Crawler list was failed to be retrieved.", "error");
			
			return new Viewable("/exception/error", apiResponse);
		} else {
			apiResponse.put("items", crawlerDefinition);
			
			writeLog("Crawler list retrieved successfully.", "info");
			
			return new Viewable("/crawler/getList", apiResponse);
		}
	}
	
	public boolean isAnyCrawlerRun (String collectionId) throws Exception{
		InputStream inputPropFile = null;
		
		File colHome = new File(collectionHome);
		
		for(File colHomeFile : colHome.listFiles()){
			if(colHomeFile.isDirectory()){
				String[] colHomeFileParts = colHomeFile.getName().split("\\.");
				if(colHomeFileParts.length > 1 && colHomeFileParts[0].equals(collectionId)){
					File colFile = new File(colHomeFile.getAbsolutePath() + "\\configproperties.cfg");

					try {
						inputPropFile = new FileInputStream(
								colFile.getAbsolutePath());
				 
						Properties prop = new Properties();
						prop.load(inputPropFile);
						
						String crawlerId = prop.getProperty("crawlerid");
						
						if(getStateOff(collectionId, crawlerId)){
							return true;
						}
						
						inputPropFile.close();
					} catch (Exception e) {
						errorMessages.add(e.toString());
						errorDetails.add(sc.getStackTrace(e));
						
						
						writeLog(e, "error");
						
						e.printStackTrace();
					} finally {
						if (inputPropFile != null) {
							try {
								inputPropFile.close();
							} catch (Exception e) {
								errorMessages.add(e.toString());
								errorDetails.add(sc.getStackTrace(e));
								
								writeLog(e, "error");
								
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
		
		return false;
	}
	
	private Viewable getState(String collectionId, String crawlerId){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		
		//List<Object> error = new ArrayList<Object>();
		
		String collectionDir = collectionHome + "\\" + collectionId + "." + crawlerId;
		
		SystemControl sc = new SystemControl();
		String pidString = "";
		String start = "";
		
		try {
			FileInputStream in = new FileInputStream(collectionDir + "\\start.cfg");
			Properties prop = new Properties();
			prop.load(in);
			
			pidString = prop.getProperty("pid");
			start = prop.getProperty("startcrawl");
			String[] pidStrings = pidString.split("@");
			pidString = pidStrings[0];
			
			in.close();
		} catch (Exception e) {
			/*Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);*/
			errorMessages.add(e.toString());
			errorDetails.add(sc.getStackTrace(e));
			
			writeLog(e, "error");
			
			e.printStackTrace();
		}
		
		String outCmd = "";
		String stateString = "not running";
		try {
			outCmd = sc.runExec("tasklist /FI \"PID eq " + pidString + "\"");
			if((outCmd.contains("No tasks are running") || outCmd.contains("ERROR")) && start.equalsIgnoreCase("stop")){
				stateString = "not running";
				pidString = "not found";
			}else if(!outCmd.isEmpty() && start.equalsIgnoreCase("stop")){
				stateString = "stopping";
			}else if((outCmd.contains("No tasks are running") || outCmd.contains("ERROR")) && start.equalsIgnoreCase("start")){
				stateString = "starting";
			}else if(!outCmd.isEmpty() && start.equalsIgnoreCase("start")){
				stateString = "running";
			}
		} catch (Exception e) {
			/*Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);*/
			errorMessages.add(e.toString());
			errorDetails.add(sc.getStackTrace(e));
			
			writeLog(e, "error");
			
			e.printStackTrace();
		}
		
		Map<String,Object> property = new HashMap<String,Object>();
		property.put("state", stateString);
		property.put("pid", pidString);
		
		if(errorMessages.size() > 0){
			Map<String,Object> error = new HashMap<String,Object>();
			String messages = "";
			for(String message : errorMessages){
				messages += message + "\n";
			}
			
			error.put("message", messages);
			
			String details = "";
			for(String detail : errorDetails){
				details += detail + "\n";
			}
			
			error.put("detail", details);
			error.put("value", "1");
			
			apiResponse.put("items", error);
			
			writeLog("Crawler ID: " + crawlerId + " status was failed to be retrieved.", "error");
			
			return new Viewable("/exception/error", apiResponse);
		}else{
			apiResponse.put("items", property);			
			
			writeLog("Crawler ID: " + crawlerId + " status retrieved successfully.", "info");

			return new Viewable("/crawler/getState", apiResponse);
		}
	}
	
	private boolean getStateOff(String collectionId, String crawlerId) throws Exception{
		boolean run = false;
		String collectionDir = collectionHome + "\\" + collectionId + "." + crawlerId;
		
		SystemControl sc = new SystemControl();
		String pidString = "";
		FileInputStream in = null;
		
		try {
			in = new FileInputStream(collectionDir + "\\start.cfg");
			Properties prop = new Properties();
			prop.load(in);
			
			pidString = prop.getProperty("pid");
			String[] pidStrings = pidString.split("@");
			pidString = pidStrings[0];
		} catch (Exception e) {
			errorMessages.add(e.toString());
			errorDetails.add(sc.getStackTrace(e));
			
			writeLog(e, "error");
			
			e.printStackTrace();
		} finally {
			if(in != null){
				in.close();
			}
		}
		
		try {
			String outCmd = sc.runExec("tasklist /FI \"PID eq " + pidString + "\"");
			if(outCmd.contains("No tasks are running") || outCmd.contains("ERROR")){
				run = false;
			}else if(!outCmd.isEmpty()){
				run = true;
			}
		} catch (Exception e) {
			errorMessages.add(e.toString());
			errorDetails.add(sc.getStackTrace(e));
			
			writeLog(e, "error");
			
			e.printStackTrace();
		}
		
		return run;
	}
	
	private Viewable create(String body, String collectionId, String sessionID){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		
		Map<String, String> jsonElements = null;
		//List<Object> error = new ArrayList<Object>();
		Security secure = new Security();
		boolean isOnList = false;
		String crawlerId = null;
		
		try {
			username = secure.getUser(sessionID);
			secure.updateLimitCrawler(collectionId, username);
			if(secure.derbyCheckCrawler(collectionId, username)){
				try {
					jsonElements = json.parse(body);
				} catch (Exception e) {
					/*Map<String,Object> errorProperty = new HashMap<String,Object>();
					errorProperty.put("code", "500");
					errorProperty.put("message", e.toString());
					errorProperty.put("detail", sc.getStackTrace(e));
					
					error.add(errorProperty);*/
					errorMessages.add(e.toString());
					errorDetails.add(sc.getStackTrace(e));
					
					writeLog(e, "error");
					
					e.printStackTrace();
				}
				
				String typeCrawler = "";
				String type = jsonElements.get("type");
				
				if(type.equals("general")){
					typeCrawler = "generalcrawler.jar";
				}
				else if(type.equals("news")){
					typeCrawler = "newscrawler.jar";
				}
				else if(type.equals("forum")){
					typeCrawler = "forumcrawler.jar";
				}
				else if(type.equals("facebook")){
					typeCrawler = "fbcrawler.jar";
				}
				else if(type.equals("twitter")){
					typeCrawler = "twcrawler.jar";
				}
				else if(type.equals("twitter_bluemix")){
					typeCrawler = "twbmcrawler.jar";
				}
				else if(type.equals("youtube")){
					typeCrawler = "ytcrawler.jar";
				}
				else if(type.equals("instagram")){
					typeCrawler = "igcrawler.jar";
				}
				
				String randomName = Integer.toString(randInt(10000, 99999));
				crawlerId = "WEB_" + randomName;
				String collectionDir = collectionHome + "\\" + collectionId + "." + crawlerId;
				String jarResourceDir = caxHome + "\\example\\resources\\jar\\crawler";
				String configResourceDir = caxHome + "\\example\\resources\\config_template\\crawler";
				
				File file = new File(collectionDir);
				
				while(file.exists()){
					randomName = Integer.toString(randInt(10000, 99999));
					collectionDir = collectionHome + "\\" + collectionId + ".WEB_" + randomName;
					file = new File(collectionDir);
				}
				
				if(!file.exists()){
					fm.createDir(collectionDir);
					
					if(new File(jarResourceDir + "\\"+typeCrawler).exists()){
						try {
							fm.copyFile(jarResourceDir + "\\"+typeCrawler, 
									collectionDir + "\\"+typeCrawler);
							fm.copyFile(jarResourceDir + "\\CrawlControl.jar", 
									collectionDir + "\\CrawlControl.jar");
							fm.copyFile(configResourceDir + "\\configproperties.cfg", 
									collectionDir + "\\configproperties.cfg");
							fm.copyFile(configResourceDir + "\\start.cfg", 
									collectionDir + "\\start.cfg");
							fm.copyFile(configResourceDir + "\\model.dat", 
									collectionDir + "\\model.dat");
							
							if(typeCrawler.equals("fbcrawler.jar")){
								fm.copyFile(configResourceDir + "\\facebook\\configrun.cfg", 
										collectionDir + "\\configrun.cfg");
							}else if(typeCrawler.equals("twcrawler.jar")){
								fm.copyFile(configResourceDir + "\\twitter\\configrun.cfg", 
										collectionDir + "\\configrun.cfg");
							}else if(typeCrawler.equals("twbmcrawler.jar")){
								fm.copyFile(configResourceDir + "\\twitter_bluemix\\configrun.cfg", 
										collectionDir + "\\configrun.cfg");
							}else if(typeCrawler.equals("ytcrawler.jar")){
								fm.copyFile(configResourceDir + "\\youtube\\configrun.cfg", 
										collectionDir + "\\configrun.cfg");
							}else if(typeCrawler.equals("igcrawler.jar")){
								fm.copyFile(configResourceDir + "\\instagram\\configrun.cfg", 
										collectionDir + "\\configrun.cfg");
							}else if(typeCrawler.equals("generalcrawler.jar")){
								fm.copyFile(configResourceDir + "\\configrun.cfg", 
										collectionDir + "\\configrun.cfg");
							}else{
								fm.copyFile(configResourceDir + "\\configrun.cfg", 
										collectionDir + "\\configrun.cfg");
								
								String domainUrl = "";
								String url = jsonElements.get("url");
								URL URI = new URL(url);
								domainUrl = URI.getHost();
								domainUrl = InternetDomainName.from(domainUrl).topPrivateDomain().name();
								
								String templateDir = caxHome + "\\example\\resources\\config_template\\crawler\\cfgfile\\"+type+"\\"+domainUrl;
								ArrayList<String> listTemplate = new ArrayList<String>();
								listTemplate = fm.listFile(templateDir);
								for(String template : listTemplate){
									if(template.toLowerCase().contains(domainUrl.toLowerCase())){
										isOnList = true;
										break;
									}
								}
								if(isOnList){
									fm.createDir(collectionDir+"\\"+domainUrl);
									FileUtils.copyDirectory(new File(templateDir), new File(collectionDir+"\\"+domainUrl));
								}
							}
						} catch (Exception e) {
							/*Map<String,Object> errorProperty = new HashMap<String,Object>();
							errorProperty.put("code", "500");
							errorProperty.put("message", e.toString());
							errorProperty.put("detail", sc.getStackTrace(e));
							
							error.add(errorProperty);*/
							errorMessages.add(e.toString());
							errorDetails.add(sc.getStackTrace(e));
							
							writeLog(e, "error");
							
							e.printStackTrace();
						}
					}
					
					FileInputStream in = null;
					FileOutputStream out = null;
					
					try {
						in = new FileInputStream(collectionDir + "\\configproperties.cfg");
						Properties props = new Properties();
						props.load(in);

						out = new FileOutputStream(collectionDir + "\\configproperties.cfg");
						props.setProperty("displayname", jsonElements.get("displayname"));
						props.setProperty("crawlerid", "WEB_" + randomName);
						props.setProperty("type", jsonElements.get("type"));
						props.store(out, null);
					} catch (Exception e) {
						/*Map<String,Object> errorProperty = new HashMap<String,Object>();
						errorProperty.put("code", "500");
						errorProperty.put("message", e.toString());
						errorProperty.put("detail", sc.getStackTrace(e));
						
						error.add(errorProperty);*/
						errorMessages.add(e.toString());
						errorDetails.add(sc.getStackTrace(e));
						
						writeLog(e, "error");
						
						e.printStackTrace();
					} finally {
						if(in != null){
							in.close();;
						}

						if(out != null){
							out.close();
						}
					}
					
					try {
						in = new FileInputStream(collectionDir + "\\configrun.cfg");
						Properties props = new Properties();
						props.load(in);
						
						String[] propertiesToWrite = {"useragent","depthlink","thread",
								"linktofollow","linktoforbid","forumconfigloc","newsconfigloc", "url", "keyword", "progressive"};
						String[] propertiesFbToWrite = {"query","since","until","token"};
						String[] propertiesTwToWrite = {"query","until","count","lang","token"};
						String[] propertiesTwBmToWrite = {"delaybetweenrequest","count","query","until","since","token"};
						String[] propertiesYtToWrite = {"videosearchtype","keyword","progressive","videokeywordascommentfilter","key"};
						String[] propertiesIgToWrite = {"delaybetweenrequest","searchtype","search","usesearchasfilter"};
						
						out = new FileOutputStream(collectionDir + "\\configrun.cfg");
						
						if(type.equals("facebook")){
							for(String property : propertiesFbToWrite){
								if(jsonElements.containsKey(property)){
									props.setProperty(property, jsonElements.get(property));
								}
							}
						}else if(type.equals("twitter")){
							for(String property : propertiesTwToWrite){
								if(jsonElements.containsKey(property)){
									props.setProperty(property, jsonElements.get(property));
								}
							}
						}else if(type.equals("twitter_bluemix")){
							for(String property : propertiesTwBmToWrite){
								if(jsonElements.containsKey(property)){
									props.setProperty(property, jsonElements.get(property));
								}
							}
						}else if(type.equals("youtube")){
							for(String property : propertiesYtToWrite){
								if(jsonElements.containsKey(property)){
									props.setProperty(property, jsonElements.get(property));
								}
							}
						}else if(type.equals("instagram")){
							for(String property : propertiesIgToWrite){
								if(jsonElements.containsKey(property)){
									props.setProperty(property, jsonElements.get(property));
								}
							}
						}else{
							for(String property : propertiesToWrite){
								if(jsonElements.containsKey(property)){
									props.setProperty(property, jsonElements.get(property));
								}
							}
							
						}
						
						props.store(out, null);
					} catch (Exception e) {
						/*Map<String,Object> errorProperty = new HashMap<String,Object>();
						errorProperty.put("code", "500");
						errorProperty.put("message", e.toString());
						errorProperty.put("detail", sc.getStackTrace(e));
						
						error.add(errorProperty);*/
						errorMessages.add(e.toString());
						errorDetails.add(sc.getStackTrace(e));
						
						writeLog(e, "error");
						
						e.printStackTrace();
					} finally {
						if(in != null){
							in.close();;
						}

						if(out != null){
							out.close();
						}
					}
					
					try{
//						secure.insertLimitCrawler(username, collectionId);
//						System.out.println("crawler : "+collectionId + ".WEB_" + randomName+" created!");
					}
					catch(Exception e){
						/*Map<String,Object> errorProperty = new HashMap<String,Object>();
						errorProperty.put("code", "500");
						errorProperty.put("message", e.toString());
						errorProperty.put("detail", sc.getStackTrace(e));
						
						error.add(errorProperty);
						*/
						errorMessages.add(e.toString());
						errorDetails.add(sc.getStackTrace(e));
						
						writeLog(e, "error");
						
						e.printStackTrace();
					}
				}else{
					/*Map<String,Object> errorProperty = new HashMap<String,Object>();
					errorProperty.put("code", "500");
					errorProperty.put("message", "Failed");
					errorProperty.put("detail", "Internal Server Error");
					
					error.add(errorProperty);*/
					String message = "Failed";
					String detail = "Internal Server Error";
					
					errorMessages.add(message);
					errorDetails.add(detail);
					
					writeLog(message + detail, "error");
				}
				
				/*if(errorMessages.size() > 0){
					Map<String,Object> error = new HashMap<String,Object>();
					String messages = "";
					for(String message : errorMessages){
						messages += message + "\n";
					}
					
					error.put("message", messages);
					
					String details = "";
					for(String detail : errorDetails){
						details += detail + "\n";
					}
					
					error.put("detail", details);
					error.put("value", "1");
					
					apiResponse.put("items", error);
					
					writeLog("Crawler ID: " + crawlerId + " was failed to be created.", "error");
					
					return new Viewable("/exception/error", apiResponse);
				}else{*/if(errorMessages.size() == 0){
					secure.insertLimitCrawler(username, collectionId);
					System.out.println("crawler : " + collectionId + "." + crawlerId + " created!");
					/*Map<String,Object> property = new HashMap<String,Object>();
					
					property.put("message", "successful");
					property.put("value", "0");
					
					apiResponse.put("items", property);
					return new Viewable("/general/ack", apiResponse);*/
				}
			} else {
				/*Map<String,Object> property = new HashMap<String,Object>();
				
				property.put("message", "crawler reach max");
				property.put("value", "2");
				
				apiResponse.put("items", property);
				return new Viewable("/general/ack", apiResponse);*/
				String message = "Unable to create new crawler.";
				String detail = "Crawler limit was reached.";
				
				errorMessages.add(message);
				errorDetails.add(detail);
				
				writeLog(message + detail, "error");
			}
		} catch (Exception e1) {
			/*Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", "Failed");
			errorProperty.put("detail", "Internal Server Error");
			
			error.add(errorProperty);*/
			errorMessages.add(e1.toString());
			errorDetails.add(sc.getStackTrace(e1));
			
			writeLog(e1, "error");
		}
		
		if(errorMessages.size() > 0){
			Map<String,Object> error = new HashMap<String,Object>();
			String messages = "";
			for(String message : errorMessages){
				messages += message + "\n";
			}
			
			error.put("message", messages);
			
			String details = "";
			for(String detail : errorDetails){
				details += detail + "\n";
			}
			
			error.put("detail", details);
			error.put("value", "1");
			
			apiResponse.put("items", error);
			
			writeLog("Crawler ID: " + crawlerId + " was failed to be created.", "error");
			
			return new Viewable("/exception/error", apiResponse);
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", "successful");
			property.put("value", "0");
			
			apiResponse.put("items", property);
			
			writeLog("Crawler ID: " + crawlerId + " was created successfully.", "info");
			
			return new Viewable("/general/ack", apiResponse);
		}
	}
	
	private Viewable createXmlConfig(String collectionId, String crawlerId, String body){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		
		//List<Object> error = new ArrayList<Object>();
		File collectionDir = new File(collectionHome + "\\" + collectionId + "." + crawlerId);	
		
		try{
			JSONObject json = new JSONObject(body);
			String xml = XML.toString(json);
			String typeConfig = "";
			
	//		System.out.println(body);
			
	    	xml = xml.replaceAll("<properties>", "<properties>\r\n");
	    	xml = xml.replaceAll("</properties>", "\r\n</properties>");
	    	xml = xml.replaceAll("<collectionId>(.*?)</collectionId>", "");  
	    	
	//    	System.out.println("xml : "+xml);
	 
	    	ArrayList<String> listFile = new ArrayList<String>();
	    	listFile = fm.listFile(collectionHome + "\\" + collectionId + "." + crawlerId);
	    	
	    	for(String file : listFile){
	    		if(file.contains("news")){
	    			typeConfig = "\\newsconfig.xml";
	    			break;
	    		}
	    		else if(file.contains("forum")){
	    			typeConfig = "\\forumconfig.xml";
	    			break;
	    		}
	    	}
			
			try {
				fm.fileWriter(collectionDir + typeConfig, xml, false);
			} catch (Exception e) {
				/*Map<String,Object> errorProperty = new HashMap<String,Object>();
				errorProperty.put("code", "500");
				errorProperty.put("message", e.toString());
				errorProperty.put("detail", sc.getStackTrace(e));
				
				error.add(errorProperty);*/
				errorMessages.add(e.toString());
				errorDetails.add(sc.getStackTrace(e));
				
				writeLog(e, "error");
				
				e.printStackTrace();
			}
		} catch (Exception e) {
			/*Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);*/
			errorMessages.add(e.toString());
			errorDetails.add(sc.getStackTrace(e));
			
			writeLog(e, "error");
			
			e.printStackTrace();
		}
		
		if(errorMessages.size() > 0){
			Map<String,Object> error = new HashMap<String,Object>();
			String messages = "";
			for(String message : errorMessages){
				messages += message + "\n";
			}
			
			error.put("message", messages);
			
			String details = "";
			for(String detail : errorDetails){
				details += detail + "\n";
			}
			
			error.put("detail", details);
			error.put("value", "1");
			
			apiResponse.put("items", error);
			
			writeLog("XML for crawler ID: " + crawlerId + " was failed to be created.", "error");
			
			return new Viewable("/exception/error", apiResponse);
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", "successful");
			property.put("value", "0");
			
			apiResponse.put("items", property);
			
			writeLog("XML for crawler ID: " + crawlerId + " was created successfully.", "info");
			
			return new Viewable("/general/ack", apiResponse);
		}
	}
	
	private Viewable delete(String collectionId, String crawlerId, String sessionID){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		
		//List<Object> error = new ArrayList<Object>();
		File collectionDir = new File(collectionHome + "\\" + collectionId + "." + crawlerId);	
			
		try {
			fm.deleteFile(collectionDir.getAbsolutePath(), true);
			FileUtils.deleteDirectory(collectionDir);
		} catch (Exception e) {
			int retry = 0;
			
			while(true){
				if(collectionDir.listFiles().length < 1){
					break;
				}
				
				if(retry > CRAWLER_DELETE_RETRY){
					break;
				}
				
				for (File file : collectionDir.listFiles()) {
				    try {
				    	fm.deleteFile(file.getAbsolutePath(), true);
//						FileDeleteStrategy.FORCE.delete(file);
					} catch (Exception e1) {
						/*Map<String,Object> errorProperty = new HashMap<String,Object>();
						errorProperty.put("code", "500");
						errorProperty.put("message", e1.toString());
						errorProperty.put("detail", sc.getStackTrace(e1));
						
						error.add(errorProperty);*/
						errorMessages.add(e1.toString());
						errorDetails.add(sc.getStackTrace(e1));
						
						writeLog(e1, "error");
						
						e1.printStackTrace();
					}
				}
				
				retry++;
				
				try {
					Thread.sleep(SLEEP_DELETE_RETRY_MS);
				} catch (InterruptedException e1) {
					errorMessages.add(e1.toString());
					errorDetails.add(sc.getStackTrace(e1));
					
					writeLog(e1, "error");
					
					e1.printStackTrace();
				}
			}
			
			try {
//				fm.deleteFile(collectionDir.getAbsolutePath(), true);
				FileUtils.deleteDirectory(collectionDir);
			} 
			catch (Exception e1) {
				/*Map<String,Object> errorProperty = new HashMap<String,Object>();
				errorProperty.put("code", "500");
				errorProperty.put("message", e1.toString());
				errorProperty.put("detail", sc.getStackTrace(e1));
				
				error.add(errorProperty);*/
				errorMessages.add(e1.toString());
				errorDetails.add(sc.getStackTrace(e1));
				
				writeLog(e1, "error");
				
				e1.printStackTrace();
			}
			
			/*Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);*/
			errorMessages.add(e.toString());
			errorDetails.add(sc.getStackTrace(e));
			
			writeLog(e, "error");
			
			e.printStackTrace();
		}
		
		try{
			Security secure = new Security();
			String username = secure.getUser(sessionID);
			secure.removeLimitCrawler(username, collectionId);
			System.out.println("crawler : "+collectionDir.getName()+" deleted!");
		}
		catch(Exception e){
			/*Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);*/
			errorMessages.add(e.toString());
			errorDetails.add(sc.getStackTrace(e));
			
			writeLog(e, "error");
			
			e.printStackTrace();
		}
		
		if(errorMessages.size() > 0){
			Map<String,Object> error = new HashMap<String,Object>();
			String messages = "";
			for(String message : errorMessages){
				messages += message + "\n";
			}
			
			error.put("message", messages);
			
			String details = "";
			for(String detail : errorDetails){
				details += detail + "\n";
			}
			
			error.put("detail", details);
			error.put("value", "1");
			
			apiResponse.put("items", error);
			
			writeLog("Crawler ID: " + crawlerId + " was failed to be deleted.", "error");
			
			return new Viewable("/exception/error", apiResponse);
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", "successful");
			property.put("value", "0");
			
			apiResponse.put("items", property);
			
			writeLog("Crawler ID: " + crawlerId + " was created successfully.", "info");
			
			return new Viewable("/general/ack", apiResponse);
		}
	}
	
	private Viewable edit(String body, String collectionId, String crawlerId){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		
		//List<Object> error = new ArrayList<Object>();
		String collectionDir = collectionHome + "\\" + collectionId + "." + crawlerId;
		
		String type = "news";
		
		Map<String, String> jsonElements = null;
		try {
			jsonElements = json.parse(body);
			System.out.println(body);
		} catch (Exception e) {
			/*Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);*/
			errorMessages.add(e.toString());
			errorDetails.add(sc.getStackTrace(e));
			
			writeLog(e, "error");
			
			e.printStackTrace();
		}
		
		List<File> files = (List<File>) FileUtils.listFiles(new File(collectionDir), 
				TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
		
		for(File file : files){
			if(file.getName().contains("config") && file.getName().endsWith("cfg")){
				FileInputStream in = null;
				FileOutputStream out = null;
				
				try{
					in = new FileInputStream(file.getAbsolutePath());
					Properties props = new Properties();
					props.load(in);
		
					out = new FileOutputStream(file.getAbsolutePath());
					
					for(String key : jsonElements.keySet()){
						if(props.containsKey(key)){
							props.setProperty(key, jsonElements.get(key));
						}
					}
					
					props.store(out, null);
				}catch(Exception e){
					/*Map<String,Object> errorProperty = new HashMap<String,Object>();
					errorProperty.put("code", "500");
					errorProperty.put("message", e.toString());
					errorProperty.put("detail", sc.getStackTrace(e));
					
					error.add(errorProperty);*/
					errorMessages.add(e.toString());
					errorDetails.add(sc.getStackTrace(e));
					
					writeLog(e, "error");
					
					e.printStackTrace();
				}finally{
					if(in != null){
						try {
							in.close();
						} catch (Exception e) {
							errorMessages.add(e.toString());
							errorDetails.add(sc.getStackTrace(e));
							
							writeLog(e, "error");
							
							e.printStackTrace();
						}
					}
					if(out != null){
						try {
							out.close();
						} catch (Exception e) {
							errorMessages.add(e.toString());
							errorDetails.add(sc.getStackTrace(e));
							
							writeLog(e, "error");
							
							e.printStackTrace();
						}
					}
				}
			}else if(file.getName().contains("config") && file.getName().endsWith("xml")){
				if(file.getName().contains("news")){
					type = "news";
				}
				else if(file.getName().contains("forum")){
					type = "forum";
				}
				
				DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
				Document doc = null;
				
				try {
					DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
					doc = docBuilder.parse(file.getAbsolutePath());
				} catch (Exception e) {
					errorMessages.add(e.toString());
					errorDetails.add(sc.getStackTrace(e));
					
					writeLog(e, "error");
					
					e.printStackTrace();
				} 
				
				for(String key : jsonElements.keySet()){
					Node nodeKey = null;
					try{
						nodeKey = doc.getElementsByTagName(key).item(0);
					}catch(Exception e){
						errorMessages.add(e.toString());
						errorDetails.add(sc.getStackTrace(e));
						
						writeLog(e, "error");
						
						e.printStackTrace();
					}
					
					if(nodeKey != null){
						nodeKey.setTextContent(jsonElements.get(key));
					}
				}
				
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = null;
				try {
					transformer = transformerFactory.newTransformer();
					transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
					transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
					transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				} catch (Exception e) {
					errorMessages.add(e.toString());
					errorDetails.add(sc.getStackTrace(e));
					
					writeLog(e, "error");
					
					e.printStackTrace();
				}
				
				DOMSource source = new DOMSource(doc);
				StreamResult result = new StreamResult(file);
				
				try {
					transformer.transform(source, result);
				} catch (Exception e) {
					errorMessages.add(e.toString());
					errorDetails.add(sc.getStackTrace(e));
					
					writeLog(e, "error");
					
					e.printStackTrace();
				}
			}
		}
		
		boolean isOnList = false;
		
		String domainUrl = "";
		String url = jsonElements.get("url");		
		try {
			URL URI = new URL(url);
			domainUrl = URI.getHost();
			domainUrl = InternetDomainName.from(domainUrl).topPrivateDomain().name();
			
			String templateDir = caxHome + "\\example\\resources\\config_template\\crawler\\cfgfile\\"+type+"\\"+domainUrl;
			ArrayList<String> listTemplate = new ArrayList<String>();
			listTemplate = fm.listFile(templateDir);
			for(String template : listTemplate){
				if(template.toLowerCase().contains(domainUrl.toLowerCase())){
					isOnList = true;
					break;
				}
			}
			if(isOnList){
				fm.createDir(collectionDir+"\\"+domainUrl);
				FileUtils.copyDirectory(new File(templateDir), new File(collectionDir+"\\"+domainUrl));
			}
		} catch (Exception e) {
			errorMessages.add(e.toString());
			errorDetails.add(sc.getStackTrace(e));
			
			writeLog(e, "error");
			
			e.printStackTrace();
		}
	
		
		if(errorMessages.size() > 0){
			Map<String,Object> error = new HashMap<String,Object>();
			String messages = "";
			for(String message : errorMessages){
				messages += message + "\n";
			}
			
			error.put("message", messages);
			
			String details = "";
			for(String detail : errorDetails){
				details += detail + "\n";
			}
			
			error.put("detail", details);
			error.put("value", "1");
			
			apiResponse.put("items", error);
			
			writeLog("Crawler ID: " + crawlerId + " was failed to be edited.", "error");
			
			return new Viewable("/exception/error", apiResponse);
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", "successful");
			property.put("value", "0");
			
			apiResponse.put("items", property);
			
			writeLog("Crawler ID: " + crawlerId + " was edited successfully.", "info");
			
			return new Viewable("/general/ack", apiResponse);
		}
	}
	
	private Viewable fullRecrawl(String collectionId, String crawlerId){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		
		//List<Object> error = new ArrayList<Object>();
		
		String pidString = "";
		
		String collectionDir = collectionHome + "\\" + collectionId + "." + crawlerId;
		String pendingFile = collectionDir+"\\pending_to_indexer.txt";
		
		System.out.println("full recrawling: "+collectionDir+"");
		
		String type = "";
		FileInputStream in = null;
		
		try {
			in = new FileInputStream(collectionDir + "\\configproperties.cfg");
			Properties props = new Properties();
			props.load(in);
			type = props.getProperty("type");
		} catch(Exception e){
			/*Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);*/
			errorMessages.add(e.toString());
			errorDetails.add(sc.getStackTrace(e));
			
			writeLog(e, "error");
			
			e.printStackTrace();
		} finally{
			if(in != null){
				try {
					in.close();
				} catch (IOException e) {
					errorMessages.add(e.toString());
					errorDetails.add(sc.getStackTrace(e));
					
					writeLog(e, "error");
					
					e.printStackTrace();
				}
			}
		}
		
		if(type.equals("general")){
			type = "generalcrawler.jar";
		}
		else if(type.equals("news")){
			type = "newscrawler.jar";
		}
		else if(type.equals("forum")){
			type = "forumcrawler.jar";
		}else if(type.equals("facebook")){
			type = "fbcrawler.jar";
		}else if(type.equals("twitter")){
			type = "twcrawler.jar";
		}else if(type.equals("twitter_bluemix")){
			type = "twbmcrawler.jar";
		}else if(type.equals("youtube")){
			type = "ytcrawler.jar";
		}else if(type.equals("instagram")){
			type = "igcrawler.jar";
		}
				
		try{
			if(new File(collectionDir + "\\start.cfg").exists()){
				FileInputStream in2 = null;
				
				try {
					in2 = new FileInputStream(collectionDir + "\\start.cfg");
					Properties prop = new Properties();	
					prop.load(in2);
					
					pidString = prop.getProperty("pid");
					String[] pidStrings = pidString.split("@");
					pidString = pidStrings[0];
					
					try{
						new SystemControl().runExec("taskkill /PID "+pidString+" /F");
					}
					catch(Exception e){
						System.out.println("Crawler already stop, continue..");
					}
				} catch (Exception e) {
					/*Map<String,Object> errorProperty = new HashMap<String,Object>();
					errorProperty.put("code", "500");
					errorProperty.put("message", e.toString());
					errorProperty.put("detail", sc.getStackTrace(e));
					
					error.add(errorProperty);*/
					errorMessages.add(e.toString());
					errorDetails.add(sc.getStackTrace(e));
					
					writeLog(e, "error");
					
					e.printStackTrace();
				} finally{
					if(in2 != null){
						in2.close();
					}
				}
			}	
		}catch(Exception e){
			/*Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);*/
			errorMessages.add(e.toString());
			errorDetails.add(sc.getStackTrace(e));
			
			writeLog(e, "error");
			
			e.printStackTrace();
		}
		
		File delDir = new File(collectionDir+"\\outputcrawler");	
		
		try {
			while(true){
				if(pidString.length()<1){
					break;
				}
				String out = sc.runExec("tasklist /FI \"PID eq "+pidString+"\"");
				if(out.contains("No tasks are running")){
					break;
				}
//				System.out.println("-");
				Thread.sleep(1000);
			}
			Thread.sleep(1000);
			
			System.out.println("deleting previous result");
			fm.deleteFile(delDir.getAbsolutePath(), true);
//			FileDeleteStrategy.FORCE.delete(delDir);
			File pendingFiles = new File(pendingFile);
			
			if(pendingFiles.exists()){
				fm.deleteFile(pendingFiles.getAbsolutePath(), true);
//				FileDeleteStrategy.FORCE.delete(pendingFiles);
			}
			
			File urlCrawledFile = new File(collectionDir + "\\urlcrawled.txt");
			File urlRecentlyCrawledFile = new File(collectionDir + "\\urlrecentlycrawled.txt");
			File crawledFile = new File(collectionDir + "\\crawled.txt");
			File recentlyCrawledFile = new File(collectionDir + "\\recentlycrawled.txt");
			
			if(urlCrawledFile.isFile()){
				fm.deleteFile(urlCrawledFile.getAbsolutePath(), true);
//				urlCrawledFile.delete();
			}
			
			if(urlRecentlyCrawledFile.isFile()){
				fm.deleteFile(urlRecentlyCrawledFile.getAbsolutePath(), true);
//				urlRecentlyCrawledFile.delete();
			}
			
			if(crawledFile.isFile()){
				fm.deleteFile(crawledFile.getAbsolutePath(), true);
//				crawledFile.delete();
			}
			
			if(recentlyCrawledFile.isFile()){
				fm.deleteFile(recentlyCrawledFile.getAbsolutePath(), true);
//				recentlyCrawledFile.delete();
			}

		} catch (Exception e) {
			/*Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty); */
			errorMessages.add(e.toString());
			errorDetails.add(sc.getStackTrace(e));
			
			writeLog(e, "error");
			
			e.printStackTrace();
		}
		
		try{
			if(new File(collectionDir + "\\"+type).exists()){
				
				System.out.println("re-crawling..");
				String goRun =  "java -jar \"" + collectionDir + "\\"+type+"\"\r\nexit";
				
				fm.fileWriter(collectionDir+"\\startcrawl.bat", goRun, false);
				Runtime.getRuntime().exec("cmd /c start "+collectionDir+"\\startcrawl.bat");
			}else{
				/*Map<String,Object> errorProperty = new HashMap<String,Object>();
				errorProperty.put("code", "500");
				errorProperty.put("message", "Failed");
				errorProperty.put("detail", "Internal Server Error");
				
				error.add(errorProperty);*/
				String message = "Failed";
				String detail = "Internal Server Error";
				errorMessages.add(message);
				errorDetails.add(detail);
				
				writeLog(message + detail, "error");
			}
		}catch(Exception e){
			/*Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);*/
			errorMessages.add(e.toString());
			errorDetails.add(sc.getStackTrace(e));
			
			writeLog(e, "error");
			
			e.printStackTrace();
		}
		
		if(errorMessages.size() > 0){
			Map<String,Object> error = new HashMap<String,Object>();
			String messages = "";
			for(String message : errorMessages){
				messages += message + "\n";
			}
			
			error.put("message", messages);
			
			String details = "";
			for(String detail : errorDetails){
				details += detail + "\n";
			}
			
			error.put("detail", details);
			error.put("value", "1");
			
			apiResponse.put("items", error);
			
			writeLog("Crawler ID: " + crawlerId + " was failed to be re-crawled.", "error");
			
			return new Viewable("/exception/error", apiResponse);
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", "successful");
			property.put("value", "0");
			
			apiResponse.put("items", property);
			
			writeLog("Crawler ID: " + crawlerId + " was re-crawled successfully.", "info");
			
			return new Viewable("/general/ack", apiResponse);
		}
	}
	
	private Viewable getRecentlyCrawled(String collectionId, String crawlerId){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		
		//List<Object> error = new ArrayList<Object>();
		
		File collectionDir = new File(collectionHome + "\\" + collectionId + "." + crawlerId);	
		
		String out = "";
		
		String urlFilePath = collectionDir + "\\urlrecentlycrawled.txt";
		String filePath = collectionDir + "\\recentlycrawled.txt";
		
		File urlFile = new File(urlFilePath);
		File file = new File(filePath);
		
		if(urlFile.exists()){
			try {
				out = fm.readData(urlFilePath);
				out = out.replace("&", "&amp;")
						.replace("\"", "&quot;")
						.replace("'", "&apos;")
						.replace("<", "&lt;")
						.replace(">", "&gt;")
						.replace("\n", "|");
			} catch (Exception e) {
				/*Map<String,Object> errorProperty = new HashMap<String,Object>();
				errorProperty.put("code", "500");
				errorProperty.put("message", e.toString());
				errorProperty.put("detail", sc.getStackTrace(e));
				
				error.add(errorProperty);*/
				errorMessages.add(e.toString());
				errorDetails.add(sc.getStackTrace(e));
				
				writeLog(e, "error");
				
				e.printStackTrace();
			}
		}else if(file.exists()){
			try {
				out = fm.readData(filePath);
				out = out.replace("&", "&amp;")
						.replace("\"", "&quot;")
						.replace("'", "&apos;")
						.replace("<", "&lt;")
						.replace(">", "&gt;")
						.replace("\n", "|");
			} catch (Exception e) {
				/*Map<String,Object> errorProperty = new HashMap<String,Object>();
				errorProperty.put("code", "500");
				errorProperty.put("message", e.toString());
				errorProperty.put("detail", sc.getStackTrace(e));
				
				error.add(errorProperty);*/
				errorMessages.add(e.toString());
				errorDetails.add(sc.getStackTrace(e));
				
				writeLog(e, "error");
				
				e.printStackTrace();
			}
		}

		if(errorMessages.size() > 0){
			Map<String,Object> error = new HashMap<String,Object>();
			String messages = "";
			for(String message : errorMessages){
				messages += message + "\n";
			}
			
			error.put("message", messages);
			
			String details = "";
			for(String detail : errorDetails){
				details += detail + "\n";
			}
			
			error.put("detail", details);
			error.put("value", "1");
			
			apiResponse.put("items", error);
			
			writeLog("Recently crawled list for crawler ID: " + crawlerId + " was failed to be retrieved.", "error");
			
			return new Viewable("/exception/error", apiResponse);
		}else{
			apiResponse.put("items", out);			
			
			writeLog("Recently crawled list for crawler ID: " + crawlerId + " was retrieved successfully.", "info");

			return new Viewable("/crawler/getUrl", apiResponse);
		}
	}
	
	private Viewable getAllCrawled(String collectionId, String crawlerId){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		
		//List<Object> error = new ArrayList<Object>();
		
		File collectionDir = new File(collectionHome + "\\" + collectionId + "." + crawlerId);	
			
		String out = "";
		
		String urlFilePath = collectionDir + "\\urlcrawled.txt";
		String filePath = collectionDir + "\\crawled.txt";
		
		File urlFile = new File(urlFilePath);
		File file = new File(filePath);
		
		if(urlFile.exists()){
			try {
				out = fm.readData(urlFilePath);
				out = out.replace("&", "&amp;")
						.replace("\"", "&quot;")
						.replace("'", "&apos;")
						.replace("<", "&lt;")
						.replace(">", "&gt;")
						.replace("\n", "|");
			} catch (Exception e) {
				/*Map<String,Object> errorProperty = new HashMap<String,Object>();
				errorProperty.put("code", "500");
				errorProperty.put("message", e.toString());
				errorProperty.put("detail", sc.getStackTrace(e));
				
				error.add(errorProperty);*/
				errorMessages.add(e.toString());
				errorDetails.add(sc.getStackTrace(e));
				
				writeLog(e, "error");
				
				e.printStackTrace();
			}
		}else if(file.exists()){
			try {
				out = fm.readData(filePath);
				out = out.replace("&", "&amp;")
						.replace("\"", "&quot;")
						.replace("'", "&apos;")
						.replace("<", "&lt;")
						.replace(">", "&gt;")
						.replace("\n", "|");
			} catch (Exception e) {
				/*Map<String,Object> errorProperty = new HashMap<String,Object>();
				errorProperty.put("code", "500");
				errorProperty.put("message", e.toString());
				errorProperty.put("detail", sc.getStackTrace(e));
				
				error.add(errorProperty);*/
				errorMessages.add(e.toString());
				errorDetails.add(sc.getStackTrace(e));
				
				writeLog(e, "error");
				
				e.printStackTrace();
			}
		}

		if(errorMessages.size() > 0){
			Map<String,Object> error = new HashMap<String,Object>();
			String messages = "";
			for(String message : errorMessages){
				messages += message + "\n";
			}
			
			error.put("message", messages);
			
			String details = "";
			for(String detail : errorDetails){
				details += detail + "\n";
			}
			
			error.put("detail", details);
			error.put("value", "1");
			
			apiResponse.put("items", error);
			
			writeLog("Crawled list for crawler ID: " + crawlerId + " was failed to be retrieved.", "error");
			
			return new Viewable("/exception/error", apiResponse);
		}else{
			apiResponse.put("items", out);
			
			writeLog("Crawled list for crawler ID: " + crawlerId + " was retrieved successfully.", "info");
			
			return new Viewable("/crawler/getUrl", apiResponse);
		}
	}
	
	private Viewable start(String collectionId, String crawlerId){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		
		//List<Object> error = new ArrayList<Object>();

		String collectionDir = collectionHome + "\\" + collectionId + "." + crawlerId;
		String type = "";
		
//		String collectionStatus =  collectionHome + "\\" + collectionId+"\\status.collection";
//		
//		try {
//			Properties prop = new Properties();
//			FileInputStream in = new FileInputStream(collectionStatus);
//			prop.load(in);
//			String status = (prop.getProperty("status"));
//			
//			if(!status.equalsIgnoreCase("idle")){
//				System.out.println("Collection busy..");
//				Map<String,Object> property = new HashMap<String,Object>();
//				
//				property.put("message", "failed, collection busy");
//				property.put("value", "2");
//				
//				apiResponse.put("items", property);
//				return new Viewable("/general/ack", apiResponse);
//			}
//			
//		} catch (Exception e3) {
//			// TODO Auto-generated catch block
//			e3.printStackTrace();
//		}
		FileInputStream in = null;
		
		try {
			in = new FileInputStream(collectionDir + "\\configproperties.cfg");
			Properties props = new Properties();
			props.load(in);
			type = props.getProperty("type");
		} catch(Exception e){
			/*Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);*/
			errorMessages.add(e.toString());
			errorDetails.add(sc.getStackTrace(e));
			
			writeLog(e, "error");
			
			e.printStackTrace();
		} finally{
			if(in != null){
				try {
					in.close();
				} catch (Exception e) {
					errorMessages.add(e.toString());
					errorDetails.add(sc.getStackTrace(e));
					
					writeLog(e, "error");
					
					e.printStackTrace();
				}
			}
		}
		
		if(type.equals("general")){
			type = "generalcrawler.jar";
		}
		else if(type.equals("news")){
			type = "newscrawler.jar";
		}
		else if(type.equals("forum")){
			type = "forumcrawler.jar";
		}
		else if(type.equals("facebook")){
			type = "fbcrawler.jar";
		}
		else if(type.equals("twitter")){
			type = "twcrawler.jar";
		}
		else if(type.equals("twitter_bluemix")){
			type = "twbmcrawler.jar";
		}
		else if(type.equals("youtube")){
			type = "ytcrawler.jar";
		}
		else if(type.equals("instagram")){
			type = "igcrawler.jar";
		}
		
		if(new File(collectionDir + "\\"+type).exists()){
			String goRun =  "java -jar \"" + collectionDir + "\\"+type+"\"\r\nexit";
			try {
				fm.fileWriter(collectionDir+"\\startcrawl.bat", goRun, false);
				Runtime.getRuntime().exec("cmd /c start /min "+collectionDir+"\\startcrawl.bat");
			} catch (Exception e) {
				/*Map<String,Object> errorProperty = new HashMap<String,Object>();
				errorProperty.put("code", "500");
				errorProperty.put("message", e.toString());
				errorProperty.put("detail", sc.getStackTrace(e));
				
				error.add(errorProperty);*/
				errorMessages.add(e.toString());
				errorDetails.add(sc.getStackTrace(e));
				
				writeLog(e, "error");
				
				e.printStackTrace();
			}
		}else{
			/*Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", "Failed");
			errorProperty.put("detail", "Internal Server Error");
			
			error.add(errorProperty);*/
			String message = "Failed";
			String detail = "Internal Server Error";
			
			errorMessages.add(message);
			errorDetails.add(detail);
			
			writeLog(message + detail, "error");
		}
		
		if(errorMessages.size() > 0){
			Map<String,Object> error = new HashMap<String,Object>();
			String messages = "";
			for(String message : errorMessages){
				messages += message + "\n";
			}
			
			error.put("message", messages);
			
			String details = "";
			for(String detail : errorDetails){
				details += detail + "\n";
			}
			
			error.put("detail", details);
			error.put("value", "1");
			
			apiResponse.put("items", error);
			
			writeLog("Crawler ID: " + crawlerId + " was failed to be started.", "error");
			
			return new Viewable("/exception/error", apiResponse);
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", "successful");
			property.put("value", "0");
			
			apiResponse.put("items", property);
			
			writeLog("Crawler ID: " + crawlerId + " was started successfully.", "info");
			
			return new Viewable("/general/ack", apiResponse);
		}
	}
	
	private Viewable stop(String collectionId, String crawlerId){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		
		//List<Object> error = new ArrayList<Object>();
		String collectionDir = collectionHome + "\\" + collectionId + "." + crawlerId;
		
		if(new File(collectionDir + "\\start.cfg").exists()){
			String pidString = "";
			FileInputStream in = null;
			FileOutputStream out = null;
			
			try {
				in = new FileInputStream(collectionDir + "\\start.cfg");
				Properties prop = new Properties();	
				prop.load(in);
				pidString = prop.getProperty("pid");
				String[] pidStrings = pidString.split("@");
				pidString = pidStrings[0];
				
				out = new FileOutputStream(collectionDir + "\\start.cfg");
				prop.setProperty("startcrawl", "stop");
				prop.store(out, null);
			} catch (Exception e) {
				/*Map<String,Object> errorProperty = new HashMap<String,Object>();
				errorProperty.put("code", "500");
				errorProperty.put("message", e.toString());
				errorProperty.put("detail", sc.getStackTrace(e));
				
				error.add(errorProperty);*/
				errorMessages.add(e.toString());
				errorDetails.add(sc.getStackTrace(e));
				
				writeLog(e, "error");
				
				e.printStackTrace();
			} finally {
				if(in != null){
					try {
						in.close();
					} catch (Exception e) {
						errorMessages.add(e.toString());
						errorDetails.add(sc.getStackTrace(e));
						
						writeLog(e, "error");
						
						e.printStackTrace();
					}
				}
				
				if(out != null){
					try {
						out.close();
					} catch (IOException e) {
						errorMessages.add(e.toString());
						errorDetails.add(sc.getStackTrace(e));
						
						writeLog(e, "error");
						
						e.printStackTrace();
					}
				}
			}
		}
		else{
			/*Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", "Failed");
			errorProperty.put("detail", "Internal Server Error");*/
			String message = "Failed";
			String detail = "Internal Server Error";
			
			errorMessages.add(message);
			errorDetails.add(detail);
			
			writeLog(message + detail, "error");
		}
		
		if(errorMessages.size() > 0){
			Map<String,Object> error = new HashMap<String,Object>();
			String messages = "";
			for(String message : errorMessages){
				messages += message + "\n";
			}
			
			error.put("message", messages);
			
			String details = "";
			for(String detail : errorDetails){
				details += detail + "\n";
			}
			
			error.put("detail", details);
			error.put("value", "1");
			
			apiResponse.put("items", error);
			
			writeLog("Crawler ID: " + crawlerId + " was failed to be stopped.", "error");
			
			return new Viewable("/exception/error", apiResponse);
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", "successful");
			property.put("value", "0");
			
			apiResponse.put("items", property);
			
			writeLog("Crawler ID: " + crawlerId + " was stopped successfully.", "info");
			
			return new Viewable("/general/ack", apiResponse);
		}
	}
	
//	private Viewable getTemplateV2(String template){
//		Map<String,Object> apiResponse = new HashMap<String,Object>();
//		
//		List<Object> error = new ArrayList<Object>();
//		String templateDir = caxHome + "\\example\\resources\\config_template\\crawler\\xmlconfig";
//		String templateString = "not found";
//		
//		try{
//			if(new File(templateDir + "\\"+template+".xml").exists()){
//				templateString = fm.readData(templateDir + "\\"+template+".xml");
//			}
//			else{
//				Map<String,Object> errorProperty = new HashMap<String,Object>();
//				errorProperty.put("code", "500");
//				errorProperty.put("message", "Failed");
//				errorProperty.put("detail", "Internal Server Error");
//			}
//		}
//		catch(Exception e){
//			Map<String,Object> errorProperty = new HashMap<String,Object>();
//			errorProperty.put("code", "500");
//			errorProperty.put("message", "Failed");
//			errorProperty.put("detail", "Internal Server Error");
//		}
//		
//		if(error.size() > 0){
//			apiResponse.put("items", error);
//			return new Viewable("/exception/error", apiResponse);
//		}else{
//			Map<String,Object> property = new HashMap<String,Object>();
//			
//			property.put("message", templateString);
//			property.put("value", "0");
//			
//			apiResponse.put("items", property);
//			return new Viewable("/general/ack", apiResponse);
//		}
//	}
	
	private Viewable getTemplate(String template, String type){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		
		//List<Object> error = new ArrayList<Object>();
		String templateDir = caxHome + "\\example\\resources\\config_template\\crawler\\cfgfile\\"+type;
		String templateString = "not found";
		
		try{
			if(new File(templateDir + "\\"+template).isDirectory()){
				templateString = fm.readData(templateDir + "\\"+template+"\\default_config.xml");
			}
			else{
				/*Map<String,Object> errorProperty = new HashMap<String,Object>();
				errorProperty.put("code", "500");
				errorProperty.put("message", "Failed");
				errorProperty.put("detail", "Internal Server Error");*/
				String message = "Failed";
				String detail = "Internal Server Error";
				
				errorMessages.add(message);
				errorDetails.add(detail);
				
				writeLog(message + detail, "error");
			}
		}
		catch(Exception e){
			/*Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", "Failed");
			errorProperty.put("detail", "Internal Server Error");*/
			errorMessages.add(e.toString());
			errorDetails.add(sc.getStackTrace(e));
			
			writeLog(e, "error");
			
			e.printStackTrace();
		}
		
		if(errorMessages.size() > 0){
			Map<String,Object> error = new HashMap<String,Object>();
			String messages = "";
			for(String message : errorMessages){
				messages += message + "\n";
			}
			
			error.put("message", messages);
			
			String details = "";
			for(String detail : errorDetails){
				details += detail + "\n";
			}
			
			error.put("detail", details);
			error.put("value", "1");
			
			apiResponse.put("items", error);
			
			writeLog("Template for crawler type: " + type + " was failed to be retrieved.", "error");
			
			return new Viewable("/exception/error", apiResponse);
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", templateString);
			property.put("value", "0");
			
			apiResponse.put("items", property);
			
			writeLog("Template for crawler type: " + type + " was retrieved successfully.", "info");
			
			return new Viewable("/general/ack", apiResponse);
		}
	}
	
//	private Viewable getListTemplateV2(String type){
//		Map<String,Object> apiResponse = new HashMap<String,Object>();
//		
//		List<Object> error = new ArrayList<Object>();
//		String templateDir = caxHome + "\\example\\resources\\config_template\\crawler\\xmlconfig";
//		ArrayList<String> listTemplate = new ArrayList<String>();
//		ArrayList<String> listTemplateFinal = new ArrayList<String>();
//		String xmlbuilder = "";
//		
//		try{
//			listTemplate = fm.listFile(templateDir);
//			for(String template : listTemplate){
//				if(template.contains(type)){
//					listTemplateFinal.add(template);
//				}
//			}
//			if(listTemplateFinal!=null){
//				for(String template : listTemplateFinal){
//					xmlbuilder = ""+xmlbuilder+"<template>"+template+"</template>\r\n";
//				}
//			}
//		}
//		catch(Exception e){
//			Map<String,Object> errorProperty = new HashMap<String,Object>();
//			errorProperty.put("code", "500");
//			errorProperty.put("message", "Failed");
//			errorProperty.put("detail", "Internal Server Error");
//		}
//		
//		if(error.size() > 0){
//			apiResponse.put("items", error);
//			return new Viewable("/exception/error", apiResponse);
//		}else{
//			Map<String,Object> property = new HashMap<String,Object>();
//			
//			property.put("message", xmlbuilder);
//			property.put("value", "0");
//			
//			apiResponse.put("items", property);
//			return new Viewable("/general/ack", apiResponse);
//		}
//	}
	
	private Viewable checkIfUrlIsOnList(String encodedUrl){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		
		//List<Object> error = new ArrayList<Object>();
		
		String templateDirNews = caxHome + "\\example\\resources\\config_template\\crawler\\cfgfile\\news";
		String templateDirForum = caxHome + "\\example\\resources\\config_template\\crawler\\cfgfile\\forum";
		
		ArrayList<String> listConfigFileTemp = new ArrayList<String>();
		ArrayList<String> listConfigFileAll = new ArrayList<String>();
		listConfigFileTemp = fm.listFolder(templateDirNews);
		listConfigFileAll = fm.listFolder(templateDirForum);
		listConfigFileAll.addAll(listConfigFileTemp);
		
		boolean isOnList = false;
		
		try {
			String url = java.net.URLDecoder.decode(encodedUrl, "UTF-8");
			URL URI = new URL(url);
			String domainUrl = URI.getHost();
			domainUrl = InternetDomainName.from(domainUrl).topPrivateDomain().name().toLowerCase();
			
			for(String configFile : listConfigFileAll){
				if(domainUrl.contains(configFile)){
					isOnList = true;
					break;
				}
			}
			
		} catch (Exception e) {
			/*Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);*/
			errorMessages.add(e.toString());
			errorDetails.add(sc.getStackTrace(e));
			
			writeLog(e, "error");
			
			e.printStackTrace();
		}
		
		if(errorMessages.size() > 0){
			Map<String,Object> error = new HashMap<String,Object>();
			String messages = "";
			for(String message : errorMessages){
				messages += message + "\n";
			}
			
			error.put("message", messages);
			
			String details = "";
			for(String detail : errorDetails){
				details += detail + "\n";
			}
			
			error.put("detail", details);
			error.put("value", "1");
			
			apiResponse.put("items", error);
			
			writeLog("Checking for url: " + encodedUrl + " was failed.", "error");
			
			return new Viewable("/exception/error", apiResponse);
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", isOnList);
			property.put("value", "0");
			
			apiResponse.put("items", property);			
			
			writeLog("Checking for url: " + encodedUrl + " was successfully.", "info");

			return new Viewable("/general/ack", apiResponse);
		}
	}
	
	private Viewable getListTemplate(String type){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		
		//List<Object> error = new ArrayList<Object>();
		String templateDir = caxHome + "\\example\\resources\\config_template\\crawler\\cfgfile\\"+type;
		ArrayList<String> listTemplate = new ArrayList<String>();
		ArrayList<String> listTemplateFinal = new ArrayList<String>();
		String xmlbuilder = "";
		
		try{
			listTemplate = fm.listFolder(templateDir);
			for(String template : listTemplate){
				listTemplateFinal.add(template);
			}
			if(listTemplateFinal!=null){
				for(String template : listTemplateFinal){
					xmlbuilder = ""+xmlbuilder+"<template>"+template+"</template>\r\n";
				}
			}
		} catch(Exception e){			
			/*Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);*/
			errorMessages.add(e.toString());
			errorDetails.add(sc.getStackTrace(e));
			
			writeLog(e, "error");
			
			e.printStackTrace();
		}
		
		/*if(error.size() > 0){
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", xmlbuilder);
			property.put("value", "0");
			
			apiResponse.put("items", property);
			return new Viewable("/general/ack", apiResponse);
		}*/
		if(errorMessages.size() > 0){
			Map<String,Object> error = new HashMap<String,Object>();
			String messages = "";
			for(String message : errorMessages){
				messages += message + "\n";
			}
			
			error.put("message", messages);
			
			String details = "";
			for(String detail : errorDetails){
				details += detail + "\n";
			}
			
			error.put("detail", details);
			error.put("value", "1");
			
			apiResponse.put("items", error);
			
			writeLog("Getting list template was failed to be retrieved.", "error");
			
			return new Viewable("/exception/error", apiResponse);
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", xmlbuilder);
			property.put("value", "0");
			
			apiResponse.put("items", property);
			
			writeLog("List template was retrieved successfully.", "info");
			
			return new Viewable("/general/ack", apiResponse);
		}
	}
	
	private Viewable deleteUrlCache(String collectionId, String crawlerId){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		
		//List<Object> error = new ArrayList<Object>();
		String collectionDir = collectionHome + "\\" + collectionId + "." + crawlerId;
		
		String pidString = "";
		FileInputStream in = null;
		FileOutputStream out = null;
		
		try {
			in = new FileInputStream(collectionDir + "\\start.cfg");
			Properties prop = new Properties();	
			prop.load(in);
			pidString = prop.getProperty("pid");
			String[] pidStrings = pidString.split("@");
			pidString = pidStrings[0];
			
			out = new FileOutputStream(collectionDir + "\\start.cfg");
			prop.setProperty("startcrawl", "stop");
			prop.store(out, null);
			
			Thread.sleep(5000);
		} catch (Exception e) {
			/*Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);*/
			errorMessages.add(e.toString());
			errorDetails.add(sc.getStackTrace(e));
			
			writeLog(e, "error");
			
			e.printStackTrace();
		} finally {
			if(in != null){
				try {
					in.close();
				} catch (Exception e) {
					errorMessages.add(e.toString());
					errorDetails.add(sc.getStackTrace(e));
					
					writeLog(e, "error");
					
					e.printStackTrace();
				}
			}

			if(out != null){
				try {
					out.close();
				} catch (Exception e) {
					errorMessages.add(e.toString());
					errorDetails.add(sc.getStackTrace(e));
					
					writeLog(e, "error");
					
					e.printStackTrace();
				}
			}
		}
		
		
		try{
			if(new File(collectionDir + "\\outputcrawler\\frontier").exists()){
				while(true){
					int retry = 0;
					
					if(retry > CRAWLER_DELETE_RETRY){
						break;
					}
					
					try{
//						FileDeleteStrategy.FORCE.delete(new File(collectionDir + "\\outputcrawler\\frontier"));
						fm.deleteFile(collectionDir + "\\outputcrawler\\frontier", true);
					}
					catch(Exception e){}
					
					retry++;
					
					try {
						Thread.sleep(SLEEP_DELETE_RETRY_MS);
					} catch (Exception e1) {}
				}
			}
		} catch(Exception e){
			/*Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", "no url cache");
			property.put("value", "0");
			
			apiResponse.put("items", property);
			return new Viewable("/general/ack", apiResponse);*/
			errorMessages.add(e.toString());
			errorDetails.add(sc.getStackTrace(e));
			
			writeLog(e, "error");
		
			e.printStackTrace();
		}
						
		try {
			Runtime.getRuntime().exec("cmd /c start "+collectionDir+"\\startcrawl.bat");
		} catch (Exception e) {
			/*Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
		
			error.add(errorProperty);*/
			errorMessages.add(e.toString());
			errorDetails.add(sc.getStackTrace(e));
			
			writeLog(e, "error");
		
			e.printStackTrace();
		}
		
		/*if(error.size() > 0){
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", "successful");
			property.put("value", "0");
			
			apiResponse.put("items", property);
			return new Viewable("/general/ack", apiResponse);
		}*/
		if(errorMessages.size() > 0){
			Map<String,Object> error = new HashMap<String,Object>();
			String messages = "";
			for(String message : errorMessages){
				messages += message + "\n";
			}
			
			error.put("message", messages);
			
			String details = "";
			for(String detail : errorDetails){
				details += detail + "\n";
			}
			
			error.put("detail", details);
			error.put("value", "1");
			
			apiResponse.put("items", error);
			
			writeLog("Deleting URL cache was failed to be created.", "error");
			
			return new Viewable("/exception/error", apiResponse);
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", "successful");
			property.put("value", "0");
			
			apiResponse.put("items", property);
			
			writeLog("URL cache was deleted successfully.", "info");
			
			return new Viewable("/general/ack", apiResponse);
		}
	}
	
	private Viewable getTotalDocument(String collectionId, String crawlerId){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		
		//List<Object> error = new ArrayList<Object>();
		String collectionDir = collectionHome + "\\" + collectionId + "." + crawlerId;
		
		int totalDoc = 0;
		
		try{
			List<File> filesDest = (List<File>) FileUtils.listFiles(new File(collectionDir+"\\outputcrawler"), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
			for(File file : filesDest){
				if(file.getName().contains(".txt")){
					totalDoc++;
				}
			}
		}
		catch(Exception e){
			errorMessages.add(e.toString());
			errorDetails.add(sc.getStackTrace(e));
			
			writeLog(e, "error");
		
			e.printStackTrace();
		}
		
		/*if(error.size() > 0){
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", "total document : "+totalDoc+"");
			property.put("value", "0");
			
			apiResponse.put("items", property);
			return new Viewable("/general/ack", apiResponse);
		}*/
		if(errorMessages.size() > 0){
			Map<String,Object> error = new HashMap<String,Object>();
			String messages = "";
			for(String message : errorMessages){
				messages += message + "\n";
			}
			
			error.put("message", messages);
			
			String details = "";
			for(String detail : errorDetails){
				details += detail + "\n";
			}
			
			error.put("detail", details);
			error.put("value", "1");
			
			apiResponse.put("items", error);
			
			writeLog("Getting total document was failed to be retrieved.", "error");
			
			return new Viewable("/exception/error", apiResponse);
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", "total document : " + totalDoc);
			property.put("value", "0");
			
			apiResponse.put("items", property);
			
			writeLog("Total document was retrieved successfully.", "info");
			
			return new Viewable("/general/ack", apiResponse);
		}
	}
	
	private int randInt(int min, int max) throws Exception {
	    Random rand = new Random();

	    int randomNum = rand.nextInt((max - min) + 1) + min;

	    return randomNum;
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
