package mata.icalite.api.admin;

import mata.icalite.api.util.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.solr.client.solrj.SolrServer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.jersey.api.view.Viewable;

@Path("/pear")
public class Pear {
	private String caxHome = null;
	private String collectionHome = null;
	private SystemControl sc = null;
	private FileManager fm = null;
//	private String zLoc = null;
	private String collectionDir = null;
	
	public static SolrServer server = null;
	
	public Pear(){
		caxHome = System.getenv("SOLR_HOME");
//		zLoc = System.getenv("ZIP_HOME");
		
		collectionHome = caxHome + "\\example\\solr";
		sc = new SystemControl();
		fm = new FileManager();
	}
	
	@GET
	@Produces("application/xml")
	public Viewable getParam(@QueryParam("method") String method,
				@QueryParam("collectionId") String collectionId,
				@QueryParam("pearName") String pearName,
				@QueryParam("sessionId") String session
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
		
		if(method.equals("pear")){
			return installPear(collectionId,pearName);
		}else if(method.equals("currentPear")){
			return currentPear(collectionId);
		}else if(method.equals("uninstall")){
			return uninstallPear(collectionId);
		}else if(method.equals("getStatusInstallPear")){
			return getStatusInstallPear(collectionId);
		}else if(method.equals("getPearList")){
			return getPearList(collectionId);
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
	
	private Viewable uninstallPear(String collectionId){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		
		System.out.println("Uninstalling "+collectionId+" PEAR");
		
		String collectionDir = collectionHome+"\\"+collectionId;
		String collectionStatus = collectionHome+"\\"+collectionId+"\\status.collection";
		
		if(!new File(collectionStatus).exists()){
			try {
				new FileManager().fileWriter(collectionStatus, "status=idle", false);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		Properties prop = new Properties();
		FileInputStream in;
		try {
			in = new FileInputStream(collectionStatus);
			prop.load(in);
			String status = (prop.getProperty("status"));
			
			if(!status.equalsIgnoreCase("unloaded")){
				if(status.equalsIgnoreCase("idle")){
//					System.out.println("state idle");
				}
				else{
					System.out.println("Collection busy..");
					
					System.out.println(collectionId + " Collection busy..");
					Map<String,Object> property = new HashMap<String,Object>();
					
					property.put("message", "failed, collection busy");
					property.put("value", "2");
					
					apiResponse.put("items", property);
					return new Viewable("/general/ack", apiResponse);
					
				}
			}
		} catch (Exception e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}
		
		if(new File(collectionDir + "\\PearManager.jar").exists()){
			
			String goRun =  "java -jar \"" + collectionDir + "\\PearManager.jar\" uninstall "+collectionId+"\r\nexit";
			try {
				fm.fileWriter(collectionDir+"\\StartPearManager.bat", goRun, false);
				Runtime.getRuntime().exec("cmd /c start "+collectionDir+"\\StartPearManager.bat");
			} catch (Exception e) {
				
				Map<String,Object> errorProperty = new HashMap<String,Object>();
				errorProperty.put("code", "500");
				errorProperty.put("message", e.toString());
				errorProperty.put("detail", sc.getStackTrace(e));
				
				error.add(errorProperty);
				
				e.printStackTrace();
			}
		}else{
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", "Failed");
			errorProperty.put("detail", "Internal Server Error");
			
			error.add(errorProperty);
		}
		
//		
//		String collectionStatus = collectionHome+"\\"+collectionId+"\\status.collection";
//		String installingPercentage = collectionHome+"\\"+collectionId+"\\installpercentage.state";
//		
//		collectionDir = collectionHome + "\\" + collectionId;
//		String pear_temp = collectionDir + "\\peartemp";
//		String pear_dest = collectionDir + "\\pearinstall";
//		String lib = collectionDir + "\\lib";
//		String desc = collectionDir + "\\desc";
//		String data = collectionDir + "\\data";
//		String conf = collectionDir + "\\conf";
//		
//		try {
//			fm.fileWriter(installingPercentage, "1", false);
//		} catch (Exception e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		
//		String schemaLoc = conf+"\\schema.xml";
//		
//		Properties prop = new Properties();
//		FileInputStream in;
//		try {
//			in = new FileInputStream(collectionStatus);
//			prop.load(in);
//			String status = (prop.getProperty("status"));
//			
//			if(!status.equalsIgnoreCase("unloaded")){
//				if(status.equalsIgnoreCase("idle")){
//					System.out.println("state idle");
//				}
//				else{
//					System.out.println("Collection busy..");
//					Map<String,Object> property = new HashMap<String,Object>();
//					
//					property.put("message", "failed, collection busy");
//					property.put("value", "2");
//					
//					apiResponse.put("items", property);
//					return new Viewable("/general/ack", apiResponse);
//				}
//			}
//			
//		} catch (Exception e3) {
//			// TODO Auto-generated catch block
//			e3.printStackTrace();
//		}
//		
//		try {
//			System.out.println("preparing states..");
//			fm.fileWriter(collectionStatus, "status=uninstallingpear", false);
//			fm.copyFile(schemaLoc, collectionDir+"\\schema.xml.bak");
//			fm.fileWriter(installingPercentage, "10", false);
//		} catch (Exception e2) {
//			e2.printStackTrace();
//		}
//		
//		try {
//			CoreAdminRequest.unloadCore(collectionId, false, false, server);
//			fm.fileWriter(installingPercentage, "20", false);
//		} catch (Exception e) {
//			System.out.println("failed to unloaded, maybe already unloaded");
//		}
//		
//		File collectionProperties = new File(collectionHome + "\\" + collectionId + 
//				"\\core.properties");
//		
//		if(collectionProperties.isFile()){
//			try {
//				System.out.println("deleting file..");
//				fm.deleteFile(collectionProperties.getAbsolutePath(), true);
//				fm.deleteFile(pear_temp, true);
//				fm.fileWriter(installingPercentage, "29", false);
//				fm.deleteFile(pear_dest, true);
//				fm.fileWriter(installingPercentage, "32", false);
//				fm.deleteFile(lib, true);
//				fm.deleteFile(desc, true);
//				fm.deleteFile(data, true);
//				fm.deleteFile(conf, true);
//				fm.fileWriter(installingPercentage, "39", false);
////				FileDeleteStrategy.FORCE.delete(collectionProperties);
//			} catch (Exception e) {
//				Map<String,Object> errorProperty = new HashMap<String,Object>();
//				errorProperty.put("code", "500");
//				errorProperty.put("message", e.toString());
//				errorProperty.put("detail", sc.getStackTrace(e));
//				
//				error.add(errorProperty);
//				
//				e.printStackTrace();
//			}
//		}
//		try{
//			fm.createDir(lib);
//			fm.createDir(desc);
//			fm.createDir(data);
//			fm.createDir(conf);
//			fm.fileWriter(installingPercentage, "49", false);
//			FileUtils.copyDirectory(new File(configResourceDir), new File(collectionDir+"\\conf"));
//			fm.fileWriter(installingPercentage, "66", false);
//			fm.copyFile(collectionDir+"\\schema.xml.bak", desc+"\\schema.xml");
//		}
//		catch(Exception e){
//			Map<String,Object> errorProperty = new HashMap<String,Object>();
//			errorProperty.put("code", "500");
//			errorProperty.put("message", e.toString());
//			errorProperty.put("detail", sc.getStackTrace(e));
//			
//			error.add(errorProperty);
//			
//			e.printStackTrace();
//		}
//
//        try {
//			CoreAdminRequest.createCore(collectionId, collectionId, server);
//			fm.fileWriter(installingPercentage, "72", false);
//		} catch (Exception e) {
//			Map<String,Object> errorProperty = new HashMap<String,Object>();
//			errorProperty.put("code", "500");
//			errorProperty.put("message", e.toString());
//			errorProperty.put("detail", sc.getStackTrace(e));
//			
//			error.add(errorProperty);
//			
//			e.printStackTrace();
//		}
//        
//        try{
//        	String currentPear = collectionHome + "\\" + collectionId+ "\\currentpear.state";
//        	fm.deleteFile(currentPear, true);
//        	fm.fileWriter(installingPercentage, "95", false);
//        }
//        catch(Exception e){
//        	e.printStackTrace();
//        }
//        try {
//			fm.fileWriter(installingPercentage, "100", false);
//		} catch (Exception e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
		
		
		if(error.size() > 0){
			apiResponse.put("items", error);
			try {
				fm.fileWriter(collectionStatus, "status=idle", false);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return new Viewable("/exception/error", apiResponse);
		}else{
//			System.out.println("PEAR Uninstallation Done.");
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", "successful");
			property.put("value", "0");
			
			apiResponse.put("items", property);
			try {
				fm.fileWriter(collectionStatus, "status=idle", false);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return new Viewable("/general/ack", apiResponse);
		}
	}
			
	public Viewable installPear(String collectionId,String pearName){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		
		System.out.println("Installing "+pearName+" to "+collectionId);
		
		String collectionDir = collectionHome+"\\"+collectionId;
		String collectionStatus = collectionHome+"\\"+collectionId+"\\status.collection";
		
		if(!new File(collectionStatus).exists()){
			try {
				new FileManager().fileWriter(collectionStatus, "status=idle", false);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		Properties prop = new Properties();
		FileInputStream in;
		try {
			in = new FileInputStream(collectionStatus);
			prop.load(in);
			String status = (prop.getProperty("status"));
			
			if(!status.equalsIgnoreCase("unloaded")){
				if(status.equalsIgnoreCase("idle")){
//					System.out.println("state idle");
				}
				else{
					System.out.println("Collection busy..");
					
					System.out.println(collectionId + " Collection busy..");
					Map<String,Object> property = new HashMap<String,Object>();
					
					property.put("message", "failed, collection busy");
					property.put("value", "2");
					
					apiResponse.put("items", property);
					return new Viewable("/general/ack", apiResponse);
					
				}
			}
		}
		 catch (Exception e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}

		if(new File(collectionDir + "\\PearManager.jar").exists()){
			
			String goRun =  "java -jar \"" + collectionDir + "\\PearManager.jar\" install "+collectionId+" "+pearName+"\r\nexit";
			try {
				fm.fileWriter(collectionDir+"\\StartPearManager.bat", goRun, false);
				Runtime.getRuntime().exec("cmd /c start "+collectionDir+"\\StartPearManager.bat");
			} catch (Exception e) {
				
				Map<String,Object> errorProperty = new HashMap<String,Object>();
				errorProperty.put("code", "500");
				errorProperty.put("message", e.toString());
				errorProperty.put("detail", sc.getStackTrace(e));
				
				error.add(errorProperty);
				
				e.printStackTrace();
			}
		}else{
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", "Failed");
			errorProperty.put("detail", "Internal Server Error");
			
			error.add(errorProperty);
		}
		
		
//		String collectionStatus = collectionHome+"\\"+collectionId+"\\status.collection";
//		String installingPercentage = collectionHome+"\\"+collectionId+"\\installpercentage.state";
//		
//		Properties prop = new Properties();
//		FileInputStream in;
//		try {
//			in = new FileInputStream(collectionStatus);
//			prop.load(in);
//			String status = (prop.getProperty("status"));
//			
//			if(!status.equalsIgnoreCase("unloaded")){
//				if(status.equalsIgnoreCase("idle")){
//					System.out.println("state idle");
//				}
//				else{
//					System.out.println("Collection busy..");
//					Map<String,Object> property = new HashMap<String,Object>();
//					
//					property.put("message", "failed, collection busy");
//					property.put("value", "2");
//					
//					apiResponse.put("items", property);
//					return new Viewable("/general/ack", apiResponse);
//				}
//			}
//			
//		} catch (Exception e3) {
//			// TODO Auto-generated catch block
//			e3.printStackTrace();
//		}
//		
////		Crawler crawler = new Crawler();
////		
////		boolean isAnyCrawlerRun = crawler.isAnyCrawlerRun(collectionId);
////		
////		if(isAnyCrawlerRun){
////			Map<String,Object> errorProperty = new HashMap<String,Object>();
////			errorProperty.put("code", "500");
////			String message = "Unable to proceed because of running crawler.";
////			String detail = "Stop all the crawlers and try again.";
////			
////			errorProperty.put("message", message);
////			errorProperty.put("detail", detail);
////			
////			error.add(errorProperty);
////			
//////			System.out.println(message + "\n" + detail);
////			
////			apiResponse.put("items", error);
////			return new Viewable("/exception/error", apiResponse);
////		}
//
//		server = new HttpSolrServer(URL);
//		((HttpSolrServer) server).setParser(new XMLResponseParser());
//		
//		collectionDir = collectionHome + "\\" + collectionId;
//		String collectionDirConf = collectionDir+"\\conf";
//		String pear_src = collectionDir+"\\"+pearName+".pear";
//		String pear_temp = collectionDir + "\\peartemp";
//		String pear_dest = collectionDir + "\\pearinstall";
//		File resource = new File(resourceHome);
//		File destFolder = new File(pear_dest);
//		
//		String defaultSolrConfig = collectionDirConf+"\\solrconfig_uima.xml";
//		String solrConfig = collectionDirConf+"\\solrconfig.xml";
//		
//		try {
//			System.out.println("preparing states..");
//			fm.fileWriter(collectionStatus, "status=installingpear", false);
//			fm.fileWriter(installingPercentage, "1", false);
//		} catch (Exception e2) {
//			e2.printStackTrace();
//		}
//		
//		try {
//			fm.copyFile(defaultSolrConfig, solrConfig);
//			fm.fileWriter(installingPercentage, "5", false);
//		} catch (Exception e1) {
//			Map<String,Object> errorProperty = new HashMap<String,Object>();
//			errorProperty.put("code", "500");
//			errorProperty.put("message", e1.toString());
//			errorProperty.put("detail", sc.getStackTrace(e1));
//			
//			error.add(errorProperty);
//			
//			e1.printStackTrace();
//		}
//		
//		try {
//			CoreAdminRequest.unloadCore(collectionId, false, false, server);
//			fm.fileWriter(installingPercentage, "9", false);
//		} catch (Exception e) {
//			System.out.println("failed to unloaded, maybe already unloaded");
//		}
//		
//		File collectionProperties = new File(collectionHome + "\\" + collectionId + 
//				"\\core.properties");
//		
//		if(collectionProperties.isFile()){
//			try {
//				System.out.println("deleting "+collectionProperties.getAbsolutePath()+" file");
//				fm.deleteFile(collectionProperties.getAbsolutePath(), true);
//				fm.fileWriter(installingPercentage, "13", false);
////				FileDeleteStrategy.FORCE.delete(collectionProperties);
//			} catch (Exception e) {
//				Map<String,Object> errorProperty = new HashMap<String,Object>();
//				errorProperty.put("code", "500");
//				errorProperty.put("message", e.toString());
//				errorProperty.put("detail", sc.getStackTrace(e));
//				
//				error.add(errorProperty);
//				
//				e.printStackTrace();
//			}
//		}
//		
//		try{
//			Thread.sleep(3000);
//		}
//		catch(Exception e){}
//		
//		try{
////			List<File> listDelete = (List<File>) FileUtils.listFiles(new File(pear_temp), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
////			System.gc();
////			Thread.sleep(2000);
////			for(File file : listDelete){
////				FileDeleteStrategy.FORCE.delete(file);
////			}
//			System.out.println("deleting "+pear_temp+"");
//			fm.deleteFile(pear_temp, true);
//			fm.fileWriter(installingPercentage, "17", false);
////			sc.runExec("unlocker \""+pear_temp+"\" /S /D");
//		}
//		catch(Exception e){
//			e.printStackTrace();
//			Map<String,Object> errorProperty = new HashMap<String,Object>();
//			errorProperty.put("code", "500");
//			errorProperty.put("message", e.toString());
//			errorProperty.put("detail", sc.getStackTrace(e));
//			
//			error.add(errorProperty);
//			
//			apiResponse.put("items", error);
//			return new Viewable("/exception/error", apiResponse);
//		}
//		
//		try{
////			List<File> listDelete = (List<File>) FileUtils.listFiles(new File(pear_dest), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
////			System.gc();
////			Thread.sleep(2000);
////			for(File file : listDelete){
////				FileDeleteStrategy.FORCE.delete(file);
////			}
//			System.out.println("deleting "+pear_dest+"");
//			fm.deleteFile(pear_dest, true);
//			fm.fileWriter(installingPercentage, "21", false);
//			
////			sc.runExec("unlocker \""+pear_dest+"\" /S /D");
//		}
//		catch(Exception e){
//			e.printStackTrace();
//			Map<String,Object> errorProperty = new HashMap<String,Object>();
//			errorProperty.put("code", "500");
//			errorProperty.put("message", e.toString());
//			errorProperty.put("detail", sc.getStackTrace(e));
//			
//			error.add(errorProperty);
//			
//			apiResponse.put("items", error);
//			return new Viewable("/exception/error", apiResponse);
//		}
//		
//		try{
//			System.gc();
//			Thread.sleep(1000);
//			System.out.println("deleting "+collectionDir+"\\pear.jar"+" file");
//			fm.deleteFile(collectionDir+"\\pear.jar", true);
////			sc.runExec("unlocker \""+collectionDir+"\\pear.jar\" /S /D");
////			FileDeleteStrategy.FORCE.delete(new File(collectionDir+"\\pear.jar"));
//			System.gc();
//			Thread.sleep(2000);
//			System.out.println("deleting "+collectionDir+"\\lib\\pear.jar"+" file");
//			fm.deleteFile(collectionDir+"\\lib\\pear.jar", true);
////			sc.runExec("unlocker \""+collectionDir+"\\lib\\pear.jar\" /S /D");
////			FileDeleteStrategy.FORCE.delete(new File(collectionDir+"\\lib\\pear.jar"));
//			System.gc();
//			Thread.sleep(1000);
//			System.out.println("deleting "+collectionDir+"\\desc\\aeDescriptor.xml"+" file");
//			fm.deleteFile(collectionDir+"\\desc\\aeDescriptor.xml", true);
////			sc.runExec("unlocker \""+collectionDir+"\\desc\\aeDescriptor.xml\" /S /D");
////			FileDeleteStrategy.FORCE.delete(new File(collectionDir+"\\desc\\aeDescriptor.xml"));
//			fm.fileWriter(installingPercentage, "25", false);
//		}
//		catch(Exception e){
//			e.printStackTrace();
//			Map<String,Object> errorProperty = new HashMap<String,Object>();
//			errorProperty.put("code", "500");
//			errorProperty.put("message", e.toString());
//			errorProperty.put("detail", sc.getStackTrace(e));
//			
//			error.add(errorProperty);
//			
//			apiResponse.put("items", error);
//			return new Viewable("/exception/error", apiResponse);
//		}
//		
//		try {
//			FileUtils.copyDirectory(resource, destFolder);
//			fm.fileWriter(installingPercentage, "29", false);
//		} catch (Exception e) {
//			Map<String,Object> errorProperty = new HashMap<String,Object>();
//			errorProperty.put("code", "500");
//			errorProperty.put("message", e.toString());
//			errorProperty.put("detail", sc.getStackTrace(e));
//			
//			error.add(errorProperty);
//			
//			e.printStackTrace();
//		}
//
//		try{
//			fm.createDir(collectionDir);
//			fm.createDir(pear_temp);
//			fm.createDir(pear_dest);
//			fm.fileWriter(installingPercentage, "33", false);
//		}
//		catch(Exception e){
//			Map<String,Object> errorProperty = new HashMap<String,Object>();
//			errorProperty.put("code", "500");
//			errorProperty.put("message", e.toString());
//			errorProperty.put("detail", sc.getStackTrace(e));
//			
//			error.add(errorProperty);
//			
//			e.printStackTrace();
//		}
//		
//		PearInstaller pear = new PearInstaller(caxHome,zLoc,collectionDir);	
//		
//		try {
//			pear.installPear(pear_src, pear_temp);
//			fm.fileWriter(installingPercentage, "37", false);
//		} catch (Exception e) {
//			Map<String,Object> errorProperty = new HashMap<String,Object>();
//			errorProperty.put("code", "500");
//			errorProperty.put("message", e.toString());
//			errorProperty.put("detail", sc.getStackTrace(e));
//			
//			error.add(errorProperty);
//			
//			e.printStackTrace();
//		}
//		
//		while(true){
//			if(fm.listFolder(pear_temp).size()>0){
//				System.out.println("try to break");
//				break;
//			}
//			try {
//				System.out.println("sleep now");
//				try{
//					Process process = Runtime.getRuntime().exec("start /wait cmd /c "+collectionDir+"\\install.bat");
//					process.waitFor();
//				}
//				catch(Exception e){
//					
//				}
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {}
//		}
//		System.out.println("extracting source: "+pear_src+" to: "+pear_temp);
//		
//		try {
//			pear.extractPear(pear_src, pear_temp);
//			fm.fileWriter(installingPercentage, "41", false);
//		} catch (Exception e) {
//			Map<String,Object> errorProperty = new HashMap<String,Object>();
//			errorProperty.put("code", "500");
//			errorProperty.put("message", e.toString());
//			errorProperty.put("detail", sc.getStackTrace(e));
//			
//			error.add(errorProperty);
//			
//			e.printStackTrace();
//		}
//		
//		System.out.println("getting pear id from: "+pear_temp);
//		String pearId = null;
//		
//		try {
//			pearId = (pear.getPearId(pear_temp));
//			fm.fileWriter(installingPercentage, "45", false);
//		} catch (Exception e) {
//			Map<String,Object> errorProperty = new HashMap<String,Object>();
//			errorProperty.put("code", "500");
//			errorProperty.put("message", e.toString());
//			errorProperty.put("detail", sc.getStackTrace(e));
//			
//			error.add(errorProperty);
//			
//			e.printStackTrace();
//		}
//		
//		String pearDescFile = pear_temp + "\\" + pearId + "\\" + pearId + "_pear.xml";
//		System.out.println("location: "+pearDescFile);
//		
//		String tt_file_path = pear_temp + "\\" + pearId + "\\" + "desc" + "\\" + "minimal_tt-ts.xml";
//		
//		try {
//			fm.fileWriter(installingPercentage, "49", false);
//			File tt_file = new File(tt_file_path);
//			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
//			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
//			Document doc = docBuilder.parse(tt_file);
//			
//			XPathFactory xpathFactory = XPathFactory.newInstance();
//			// XPath to find empty text nodes.
//			XPathExpression xpathExp = xpathFactory.newXPath().compile(
//			    	"//text()[normalize-space(.) = '']");  
//			NodeList emptyTextNodes = (NodeList) 
//			        xpathExp.evaluate(doc, XPathConstants.NODESET);
//			// Remove each empty text node from document.
//			for (int i = 0; i < emptyTextNodes.getLength(); i++) {
//			    Node emptyTextNode = emptyTextNodes.item(i);
//			    emptyTextNode.getParentNode().removeChild(emptyTextNode);
//			}
//			
//			NodeList nList = doc.getElementsByTagName("typeDescription");
//			ArrayList<String> typeNamesList = new ArrayList<String>();
//			for (int i=0;i<nList.getLength();i++) {
//				NodeList typeProperties = nList.item(i).getChildNodes();
//					for (int j=0;j<typeProperties.getLength();j++) {
//						if (typeProperties.item(j).getNodeName().equals("name")) {
//							typeNamesList.add(typeProperties.item(j).getTextContent());
//							break;
//						}
//					}
//			}
//			
//			String[] typeNames = typeNamesList.toArray(new String[typeNamesList.size()]);
//			
//			File aeDescFile = new File(pear_dest + "\\" + "desc" + "\\" + "aeDescriptor.xml");
//			doc = docBuilder.parse(aeDescFile);
//			xpathFactory = XPathFactory.newInstance();
//			// XPath to find empty text nodes.
//			xpathExp = xpathFactory.newXPath().compile(
//			    	"//text()[normalize-space(.) = '']");  
//			emptyTextNodes = (NodeList) 
//			        xpathExp.evaluate(doc, XPathConstants.NODESET);
//			// Remove each empty text node from document.
//			for (int i = 0; i < emptyTextNodes.getLength(); i++) {
//			    Node emptyTextNode = emptyTextNodes.item(i);
//			    emptyTextNode.getParentNode().removeChild(emptyTextNode);
//			}
//			
//			Node delegateAE = doc.getElementsByTagName("delegateAnalysisEngine").item(0);
//			NamedNodeMap attr = delegateAE.getAttributes();
//			Node nodeAttr = attr.getNamedItem("key");
//			nodeAttr.setTextContent(pearId + "_pear");
//			
//			Node importLoc = doc.getElementsByTagName("import").item(0);
//			attr = importLoc.getAttributes();
//			nodeAttr = attr.getNamedItem("location");
//			nodeAttr.setTextContent("file:/" + pearDescFile.replace("\\", "/"));
//			
//			Node fixedFlow = doc.getElementsByTagName("fixedFlow").item(0);
//			NodeList fixedFlowChild = fixedFlow.getChildNodes();
//			for (int i=0;i<fixedFlowChild.getLength();i++) {
//				if (fixedFlowChild.item(i).getNodeName().equals("node")) {
//					fixedFlowChild.item(i).setTextContent(pearId + "_pear");
//					break;
//				}
//			}
//			
//			Node outputs = doc.getElementsByTagName("outputs").item(0);
//			
//			for (int i=0;i<typeNames.length;i++) {
//				Element type = doc.createElement("type");
//				type.setAttribute("allAnnotatorFeatures", "true");
//				type.appendChild(doc.createTextNode(typeNames[i]));
//				outputs.appendChild(type);
//			}
//			
//			TransformerFactory transformerFactory = TransformerFactory.newInstance();
//			Transformer transformer = transformerFactory.newTransformer();
//			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
//			DOMSource source = new DOMSource(doc);
//			StreamResult result = new StreamResult(new File(pear_dest + "\\" + "desc" + "\\" + "aeDescriptor.xml"));
//			transformer.transform(source, result);
//			fm.fileWriter(installingPercentage, "53", false);
//			
//		}catch (Exception e) {
//			Map<String,Object> errorProperty = new HashMap<String,Object>();
//			errorProperty.put("code", "500");
//			errorProperty.put("message", e.toString());
//			errorProperty.put("detail", sc.getStackTrace(e));
//			
//			error.add(errorProperty);
//			
//			e.printStackTrace();
//		}
//		
//		try {
//			fm.fileWriter(installingPercentage, "57", false);
//			pear.jCasGenRun(pear_dest);
//			while(true){
//				if(!new File(pear_dest+"\\src\\org\\apache\\uima\\jcas\\tcas\\DocumentAnnotation.java").exists()){
//					Thread.sleep(1000);
//					System.out.println("Waiting for PEAR Extraction..");
//				}
//				else{
//					Thread.sleep(2000);
//					break;
//				}
//			}
//			
//			fm.fileWriter(pear_dest+"\\tobin.bat", "javac -cp "+pear_dest+"\\lib\\* "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\Alphabetic.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\Alphabetic_Type.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\Arabic.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\Arabic_Type.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\ChineseNumeral.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\ChineseNumeral_Type.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\ClauseEndingPunctuation.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\ClauseEndingPunctuation_Type.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\Han.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\Hangul.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\Hangul_Type.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\Han_Type.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\Hebrew.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\Hebrew_Type.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\Hiragana.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\Hiragana_Type.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\Ideographic.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\Ideographic_Type.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\Katakana.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\Katakana_Type.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\LowercaseAlphabetic.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\LowercaseAlphabetic_Type.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\Numeric.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\Numeric_Type.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\Punctuation.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\Punctuation_Type.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\Syllabic.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\Syllabic_Type.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\TitlecaseAlphabetic.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\TitlecaseAlphabetic_Type.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\UppercaseAlphabetic.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\UppercaseAlphabetic_Type.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\WordLikeToken.java "+pear_dest+"\\src\\com\\ibm\\langware\\uimatypes\\WordLikeToken_Type.java "+pear_dest+"\\src\\org\\apache\\uima\\jcas\\tcas\\DocumentAnnotation.java "+pear_dest+"\\src\\org\\apache\\uima\\jcas\\tcas\\DocumentAnnotation_Type.java "+pear_dest+"\\src\\uima\\tt\\CompPartAnnotation.java "+pear_dest+"\\src\\uima\\tt\\CompPartAnnotation_Type.java "+pear_dest+"\\src\\uima\\tt\\DictionaryEntryAnnotation.java "+pear_dest+"\\src\\uima\\tt\\DictionaryEntryAnnotation_Type.java "+pear_dest+"\\src\\uima\\tt\\DocStructureAnnotation.java "+pear_dest+"\\src\\uima\\tt\\DocStructureAnnotation_Type.java "+pear_dest+"\\src\\uima\\tt\\KeyStringEntry.java "+pear_dest+"\\src\\uima\\tt\\KeyStringEntry_Type.java "+pear_dest+"\\src\\uima\\tt\\LanguageConfidencePair.java "+pear_dest+"\\src\\uima\\tt\\LanguageConfidencePair_Type.java "+pear_dest+"\\src\\uima\\tt\\Lemma.java "+pear_dest+"\\src\\uima\\tt\\Lemma_Type.java "+pear_dest+"\\src\\uima\\tt\\LexicalAnnotation.java "+pear_dest+"\\src\\uima\\tt\\LexicalAnnotation_Type.java "+pear_dest+"\\src\\uima\\tt\\ParagraphAnnotation.java "+pear_dest+"\\src\\uima\\tt\\ParagraphAnnotation_Type.java "+pear_dest+"\\src\\uima\\tt\\SentenceAnnotation.java "+pear_dest+"\\src\\uima\\tt\\SentenceAnnotation_Type.java "+pear_dest+"\\src\\uima\\tt\\TokenAnnotation.java "+pear_dest+"\\src\\uima\\tt\\TokenAnnotation_Type.java "+pear_dest+"\\src\\uima\\tt\\TokenLikeAnnotation.java "+pear_dest+"\\src\\uima\\tt\\TokenLikeAnnotation_Type.java "+pear_dest+"\\src\\uima\\tt\\TTAnnotation.java "+pear_dest+"\\src\\uima\\tt\\TTAnnotation_Type.java -d "+pear_dest+"\\bin\r\nexit", false);
//			fm.fileWriter(pear_dest+"\\installpear.bat","c:\r\ncd "+pear_dest+"\r\njava -jar "+pear_dest+"\\pearinstall.jar\r\nexit\r\n" , false);
//			
//			while(true){
//				Thread.sleep(1000);
//				if(new File(pear_dest+"\\tobin.bat").exists()){
//					break;
//				}
//			}
//			
//			final Process process = Runtime.getRuntime().exec("cmd /c start /wait "+pear_dest+"\\tobin.bat");
//			process.waitFor();
//			
//			fm.fileWriter(installingPercentage, "61", false);
//			while(true){
//				Thread.sleep(1000);
//				if(new File(pear_dest+"\\bin\\org\\apache\\uima\\jcas\\tcas\\DocumentAnnotation.class").exists()){
//					break;
//				}
//			}
//			
//			sc.runExec("cmd /c start "+pear_dest+"\\installpear.bat");
//			fm.fileWriter(installingPercentage, "65", false);
//		} catch (Exception e) {
//			Map<String,Object> errorProperty = new HashMap<String,Object>();
//			errorProperty.put("code", "500");
//			errorProperty.put("message", e.toString());
//			errorProperty.put("detail", sc.getStackTrace(e));
//			
//			error.add(errorProperty);
//			
//			e.printStackTrace();
//		}
//		
//		try{
//			if(new File(pear_dest+"\\pear.jar").exists()){
//				fm.copyFile(pear_dest+"\\pear.jar", collectionDir+"\\pear.jar");
//				fm.fileWriter(installingPercentage, "69", false);
//			}
//			
//		}catch(Exception e){
//			Map<String,Object> errorProperty = new HashMap<String,Object>();
//			errorProperty.put("code", "500");
//			errorProperty.put("message", e.toString());
//			errorProperty.put("detail", sc.getStackTrace(e));
//			
//			error.add(errorProperty);
//			
//			e.printStackTrace();
//		}
//		
//		try {
//			fm.copyFile(pear_dest+"\\desc\\aeDescriptor.xml", collectionDir+"\\desc\\aeDescriptor.xml");
//			fm.copyFile(pear_dest+"\\desc\\aeDescriptor.xml", collectionDir+"\\desc\\aeDescriptor.xml.bak");
//			fm.fileWriter(installingPercentage, "73", false);
//		} catch (Exception e) {
//			Map<String,Object> errorProperty = new HashMap<String,Object>();
//			errorProperty.put("code", "500");
//			errorProperty.put("message", e.toString());
//			errorProperty.put("detail", sc.getStackTrace(e));
//			
//			error.add(errorProperty);
//			
//			e.printStackTrace();
//		}
//		
//		try {
//			configXmlOut(collectionDirConf+"\\solrconfig_field.xml",
//					collectionDirConf+"\\solrconfig_ae.xml", 
//					collectionDirConf+"\\solrconfig.xml", 
//					tt_file_path, collectionId);
//			fm.fileWriter(installingPercentage, "77", false);
//		} catch (Exception e) {
//			Map<String,Object> errorProperty = new HashMap<String,Object>();
//			errorProperty.put("code", "500");
//			errorProperty.put("message", e.toString());
//			errorProperty.put("detail", sc.getStackTrace(e));
//			
//			error.add(errorProperty);
//			
//			e.printStackTrace();
//		}
//		
//		try {
//			FileUtils.copyFile(new File(collectionDir+"\\pear.jar"), 
//					new File(collectionDir+"\\lib\\pear.jar"));
//			fm.fileWriter(installingPercentage, "81", false);
//		} catch (Exception e) {
//			Map<String,Object> errorProperty = new HashMap<String,Object>();
//			errorProperty.put("code", "500");
//			errorProperty.put("message", e.toString());
//			errorProperty.put("detail", sc.getStackTrace(e));
//			
//			error.add(errorProperty);
//			
//			e.printStackTrace();
//		}
//
//		try {			
//			CoreAdminRequest.createCore(collectionId, collectionId, server);
//			fm.fileWriter(installingPercentage, "85", false);
//		} catch (Exception e) {
//			Map<String,Object> errorProperty = new HashMap<String,Object>();
//			errorProperty.put("code", "500");
//			errorProperty.put("message", e.toString());
//			errorProperty.put("detail", sc.getStackTrace(e));
//			
//			error.add(errorProperty);
//			
//			e.printStackTrace();
//		}
//		
//		try {
//			fm.fileWriter(collectionDir+"\\currentpear.state", "currentPear="+pearName+"", false);
//			fm.fileWriter(installingPercentage, "89", false);
//		} catch (Exception e) {
//			Map<String,Object> errorProperty = new HashMap<String,Object>();
//			errorProperty.put("code", "500");
//			errorProperty.put("message", e.toString());
//			errorProperty.put("detail", sc.getStackTrace(e));
//			
//			error.add(errorProperty);
//			
//			e.printStackTrace();
//		}
//		
//		try {
//			fm.fileWriter(collectionStatus, "status=idle", false);
//			fm.fileWriter(installingPercentage, "100", false);
//		} catch (Exception e2) {
//			e2.printStackTrace();
//		}
        
		if(error.size() > 0){
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}else{
//			System.out.println("PEAR Installation Done.");
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", "successful");
			property.put("value", "0");
			
			apiResponse.put("items", property);
			return new Viewable("/general/ack", apiResponse);
		}
	}
			
	@SuppressWarnings("unused")
	private void configXmlOut(String locTemp, String template, String finalConfigLoc, 
			String targetSource, String collectionId) throws Exception{
    	
		System.out.println("deleting loctemp file");
		fm.deleteFile(locTemp,true);
    	
    	String nameXml = "			<lst name=\"type\">\r\n				<str name=\"name\">CHANGE_HERE</str>";
    	String featureXml = "				<lst name=\"mapping\">\r\n					<str name=\"feature\">FEATURE_CHANGE_HERE</str>\r\n					<str name=\"field\">NAME_CHANGE_HERE</str>\r\n				</lst>";
    	String endingXml = "		</lst>\r\n		</lst>\r\n      </processor>\r\n      <processor class=\"solr.LogUpdateProcessorFactory\" />\r\n      <processor class=\"solr.RunUpdateProcessorFactory\" />\r\n  </updateRequestProcessorChain>\r\n\r\n</config>";
    	String[] banFeatures = {"language"};
    	String[] banNames = {"uima.tcas.DocumentAnnotation"};
    	
    	File tt_file = new File(targetSource);
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(tt_file);
		
		XPathFactory xpathFactory = XPathFactory.newInstance();
		// XPath to find empty text nodes.
		XPathExpression xpathExp = xpathFactory.newXPath().compile(
		    	"//text()[normalize-space(.) = '']");  
		NodeList emptyTextNodes = (NodeList) 
		        xpathExp.evaluate(doc, XPathConstants.NODESET);
		// Remove each empty text node from document.
		for (int i = 0; i < emptyTextNodes.getLength(); i++) {
		    Node emptyTextNode = emptyTextNodes.item(i);
		    emptyTextNode.getParentNode().removeChild(emptyTextNode);
		}
		
		NodeList nList = doc.getElementsByTagName("typeDescription");
		ArrayList<String> listFieldNames = new ArrayList<String>();
		
		for (int i=0;i<nList.getLength();i++) {
			NodeList typeProperties = nList.item(i).getChildNodes();
			boolean breakLoop = false;
				for (int j=0;j<typeProperties.getLength();j++) {
					boolean noClose = true;
					if(typeProperties.item(j).getNodeName().equals("name") && typeProperties.getLength()>3){
						if(Arrays.asList(banNames).contains(typeProperties.item(j).getTextContent())){
							fm.fileWriter(locTemp, nameXml.replace("CHANGE_HERE", typeProperties.item(j).getTextContent()), true);
							noClose = false;
							System.out.println("name annot found");
						}
						else{
							continue;
						}
					}
					if (typeProperties.item(j).getNodeName().equals("features")) {
						NodeList featureDescription = typeProperties.item(3).getChildNodes();
						for (int k=0;k<featureDescription.getLength();k++) {
							Node features = featureDescription.item(k).getFirstChild();
							if(Arrays.asList(banFeatures).contains(features.getTextContent())){
								fm.fileWriter(locTemp, featureXml.replace("FEATURE_CHANGE_HERE", features.getTextContent()).replace("NAME_CHANGE_HERE", "_TEMP_"+features.getTextContent()), true);
								listFieldNames.add("_TEMP_"+features.getTextContent());
								noClose = false;
								break;
							}
						}
						if(!noClose){
							noClose = true;
							breakLoop = true;
							fm.fileWriter(locTemp," 		 	</lst>", true);
							break;
						}
					}
					if(breakLoop){
						break;
					}
				}
				if(breakLoop){
					break;
				}
		}
		fm.fileWriter(locTemp,endingXml, true);
    	
    	String solrtempconfig = fm.readData(locTemp);
    	String solrconfig = fm.readData(template);
    	
    	String finalConfig = solrconfig.replace("CHANGE_HERE", solrtempconfig);
    	finalConfig = finalConfig.replace("AELOCATION_CHANGE_ME", collectionDir+"\\desc\\aeDescriptor.xml");
    	fm.fileWriter(finalConfigLoc, finalConfig, false);
	}
	
	private Viewable currentPear(String collectionId){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		List<String> listFolders = new ArrayList<String>();
		
		String pearTempDir = collectionHome + "\\" + collectionId+ "\\peartemp";
		String currentPear = collectionHome + "\\" + collectionId+ "\\currentpear.state";
		
		if(new File(pearTempDir).exists()){
			listFolders = fm.listFolder(pearTempDir);
			for(String folder : listFolders){
				if(folder.contains(".")){
					try {
						int TRY = 0;
						while(true){
							if(new File(currentPear).exists()||TRY > 5){
								break;
							}
							else{
								Thread.sleep(1000);
							}
							TRY++;
						}
						String out = fm.readData(currentPear);
						out = out.trim();
						out = out.replaceAll("^(.*?)=", "");
						
						Map<String,Object> property = new HashMap<String,Object>();
						
						property.put("message", out+" : "+folder);
						property.put("value", "0");
						apiResponse.put("items", property);
						
						return new Viewable("/general/ack", apiResponse);
					} catch (Exception e) {
						Map<String,Object> errorProperty = new HashMap<String,Object>();
						errorProperty.put("code", "500");
						errorProperty.put("message", e.toString());
						errorProperty.put("detail", sc.getStackTrace(e));
						
						error.add(errorProperty);
						
						e.printStackTrace();
					}
				}
			}
		}
		
		if(error.size() > 0){
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", "No Pear Installed");
			property.put("value", "0");
			
			apiResponse.put("items", property);
			return new Viewable("/general/ack", apiResponse);
		}
	}
	
	private Viewable getPearList(String collectionId){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		List<String> listFile = new ArrayList<String>();
		List<String> listFilePear = new ArrayList<String>();
		
		String collectionDir = collectionHome + "\\" + collectionId;
		
		if(new File(collectionDir).exists()){
			
			listFile = fm.listFile(collectionDir);
			
			for(String file : listFile){
				if(file.endsWith(".pear")){
					listFilePear.add(file);
				}
			}
			
		}
		
		if(error.size() > 0){
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("pear", listFilePear.toString());
			property.put("value", "0");
			
			apiResponse.put("items", property);
			return new Viewable("/general/ack", apiResponse);
		}
	}
	
	private Viewable getStatusInstallPear(String collectionId){
		
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		
		List<Object> error = new ArrayList<Object>();
		String status = "idle";
		String installStatus = collectionHome+"\\"+collectionId+"\\installpercentage.state";
		
		if(!new File(installStatus).exists()){
			try {
				fm.fileWriter(installStatus, "100", false);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		try{
			status = fm.readData(installStatus).trim();
		}
		catch(Exception e){
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
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", status);
			property.put("value", "0");
			
			apiResponse.put("items", property);
			return new Viewable("/general/ack", apiResponse);
		}
		
	}
}
