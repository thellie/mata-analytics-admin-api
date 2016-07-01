package mata.icalite.api.admin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.request.CoreAdminRequest;

import mata.icalite.api.util.FileManager;
import mata.icalite.api.util.Security;
import mata.icalite.api.util.SystemControl;

import com.sun.jersey.api.view.Viewable;

@Path("/parsingindex")
public class ParsingIndex {
	private String caxHome = null;
	private String collectionHome = null;
	private SystemControl sc = null;
	private FileManager fm = null;
	private static final String URL = "http://127.0.0.1:8983/solr";
	

	public ParsingIndex(){
		caxHome = System.getenv("SOLR_HOME");
		collectionHome = caxHome + "\\example\\solr";
		fm = new FileManager();
		sc = new SystemControl();
	}
	
	@GET
	@Produces("application/xml")
	public Viewable getParam(@QueryParam("method") String method,
				@QueryParam("collectionId") String collectionId,
				@QueryParam("crawlerId") String crawlerId,
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
		
		if(method.equals("startParsing")){
			return startParsing(collectionId);
		}else if(method.equals("rebuildIndex")){
			return rebuildIndex(collectionId);
		}else if(method.equals("stopParsing")){
			return stopParsing(collectionId);
		}else if(method.equals("reloadParsing")){
			return reloadParsing(collectionId);
		}else if(method.equals("checkStatus")){
			return checkStatus(collectionId);
		}else if(method.equals("getStatusRebuild")){
			return getStatusRebuild(collectionId);
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
		
	private Viewable stopParsing(String collectionId){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		System.out.println("turning off : " +collectionId);
		
		String collectionStatus = collectionHome+"\\"+collectionId+"\\status.collection";
		
		if(!new File(collectionHome+"\\"+collectionId+"\\desc\\aeDescriptor.xml").exists()){
			try {
				if(new File(collectionHome+"\\"+collectionId+"\\pearinstall\\desc\\aeDescriptor.xml").exists()){
					fm.copyFile(collectionHome+"\\"+collectionId+"\\pearinstall\\desc\\aeDescriptor.xml", collectionHome+"\\"+collectionId+"\\desc\\aeDescriptor.xml");
				}
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
			
			if(!status.equalsIgnoreCase("idle")){
				if(!status.equalsIgnoreCase("unloaded")){
					System.out.println("Collection busy..");
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
		
//		Crawler crawler = new Crawler();
//		boolean isAnyCrawlerRun = crawler.isAnyCrawlerRun(collectionId);
//		
//		if(isAnyCrawlerRun){
//			System.out.println("Crawler Running..");
//			Map<String,Object> property = new HashMap<String,Object>();
//			
//			property.put("message", "Crawler(s) running, please turn it off first");
//			property.put("value", "3");
//			
//			apiResponse.put("items", property);
//			return new Viewable("/general/ack", apiResponse);
//		}
		
		SolrServer server = new HttpSolrServer(URL);
		((HttpSolrServer) server).setParser(new XMLResponseParser());
		
		try {
			CoreAdminRequest.unloadCore(collectionId, false, false, server);
			new FileManager().fileWriter(collectionStatus, "status=unloaded", false);
		} catch (Exception e) {
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);
			
			e.printStackTrace();
		}
		
		File collectionProperties = new File(collectionHome + "\\" + collectionId + 
				"\\core.properties");
		
		if(collectionProperties.isFile()){
			try {
				fm.deleteFile(collectionProperties.getAbsolutePath(), true);
//				FileDeleteStrategy.FORCE.delete(collectionProperties);
			} catch (Exception e) {
				Map<String,Object> errorProperty = new HashMap<String,Object>();
				errorProperty.put("code", "500");
				errorProperty.put("message", e.toString());
				errorProperty.put("detail", sc.getStackTrace(e));
				
				error.add(errorProperty);
				
				e.printStackTrace();
			}
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
	
	private Viewable reloadParsing(String collectionId){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		
		System.out.println("reloading collection :" + collectionId);
		String collectionStatus = collectionHome+"\\"+collectionId+"\\status.collection";
		
		if(!new File(collectionHome+"\\"+collectionId+"\\desc\\aeDescriptor.xml").exists()){
			try {
				if(new File(collectionHome+"\\"+collectionId+"\\pearinstall\\desc\\aeDescriptor.xml").exists()){
					fm.copyFile(collectionHome+"\\"+collectionId+"\\pearinstall\\desc\\aeDescriptor.xml", collectionHome+"\\"+collectionId+"\\desc\\aeDescriptor.xml");
				}
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
					System.out.println("state idle");
				}
				else{
					System.out.println("Collection busy..");
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

		SolrServer server = new HttpSolrServer(URL);
		((HttpSolrServer) server).setParser(new XMLResponseParser());
		
		try {
			new FileManager().fileWriter(collectionStatus, "status=reloading", false);
			CoreAdminRequest.unloadCore(collectionId, false, false, server);
		} catch (Exception e) {
			System.out.println("failed to unload, maybe already unloaded..");
		}
		
		File collectionProperties = new File(collectionHome + "\\" + collectionId + 
				"\\core.properties");
		
		if(collectionProperties.isFile()){
			try {
				fm.deleteFile(collectionProperties.getAbsolutePath(), true);
//				FileDeleteStrategy.FORCE.delete(collectionProperties);
			} catch (Exception e) {
				Map<String,Object> errorProperty = new HashMap<String,Object>();
				errorProperty.put("code", "500");
				errorProperty.put("message", e.toString());
				errorProperty.put("detail", sc.getStackTrace(e));
				
				error.add(errorProperty);
				
				e.printStackTrace();
				
			}
		}
		
		try {
			Thread.sleep(5000);
				CoreAdminRequest.createCore(collectionId, collectionId, server);
				System.out.println("done!");
			} catch (Exception e) {
				Map<String,Object> errorProperty = new HashMap<String,Object>();
				errorProperty.put("code", "500");
				errorProperty.put("message", e.toString());
				errorProperty.put("detail", sc.getStackTrace(e));
				
				error.add(errorProperty);
				
				e.printStackTrace();
		}
		
//		try{
//			testReloadedCollection(collectionId);
//		}
//		catch(Exception e){
//			Map<String,Object> errorProperty = new HashMap<String,Object>();
//			errorProperty.put("code", "500");
//			errorProperty.put("message", "Collection Critical Error - "+e.getMessage());
//			errorProperty.put("detail", "please ask administrator for troubleshoot - "+sc.getStackTrace(e));
//			
//			error.add(errorProperty);
//			
//			e.printStackTrace();
//		}
			
		
		if(error.size() > 0){
			apiResponse.put("items", error);
			try {
				new FileManager().fileWriter(collectionStatus, "status=idle", false);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return new Viewable("/exception/error", apiResponse);
		}else{
			
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", "successful");
			property.put("value", "0");
			
			apiResponse.put("items", property);
			try {
				new FileManager().fileWriter(collectionStatus, "status=idle", false);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return new Viewable("/general/ack", apiResponse);
		}
	}
	
//	private boolean testReloadedCollection(String collectionId) throws Exception{
//		String fileDir = collectionHome+"\\"+collectionId+"\\conf\\";
//		
//    	try {
//			Post postSolr = new Post(fileDir);
//			postSolr.run(fileDir+"sampleAdd.txt",true);
//			Thread.sleep(3000);
//			postSolr.run(fileDir+"sampleDelete.txt",false);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			new FileManager().fileWriter(fileDir+"\\log.txt", SystemControl.getCurrentTime()+" - FAILED TO PUSH TO SOLR - COLLECTION ERROR", true);
//			return false;
//		}
//		
//		return true;
//	}
	
	private Viewable startParsing(String collectionId){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		System.out.println("turning on : " +collectionId);
		
		String collectionStatus = collectionHome+"\\"+collectionId+"\\status.collection";
		
		if(!new File(collectionHome+"\\"+collectionId+"\\desc\\aeDescriptor.xml").exists()){
			try {
				if(new File(collectionHome+"\\"+collectionId+"\\pearinstall\\desc\\aeDescriptor.xml").exists()){
					fm.copyFile(collectionHome+"\\"+collectionId+"\\pearinstall\\desc\\aeDescriptor.xml", collectionHome+"\\"+collectionId+"\\desc\\aeDescriptor.xml");
				}
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
					System.out.println("state idle");
				}
				else{
					System.out.println("Collection busy..");
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
		
		File collectionProperties = new File(collectionHome + "\\" + collectionId + 
				"\\core.properties");
		
		if(collectionProperties.isFile()){
			try {
				fm.deleteFile(collectionProperties.getAbsolutePath(), true);
//				FileDeleteStrategy.FORCE.delete(collectionProperties);
			} catch (Exception e) {
				Map<String,Object> errorProperty = new HashMap<String,Object>();
				errorProperty.put("code", "500");
				errorProperty.put("message", e.toString());
				errorProperty.put("detail", sc.getStackTrace(e));
				
				error.add(errorProperty);
				
				e.printStackTrace();
			}
		}
		
		SolrServer server = new HttpSolrServer(URL);
		((HttpSolrServer) server).setParser(new XMLResponseParser());
		
        try {
			CoreAdminRequest.createCore(collectionId, collectionId, server);
			new FileManager().fileWriter(collectionStatus, "status=idle", false);
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
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", "successful");
			property.put("value", "0");
			
			apiResponse.put("items", property);
			return new Viewable("/general/ack", apiResponse);
		}
	}
	
	
	private Viewable rebuildIndex(String collectionId){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		String collectionDir = collectionHome+"\\"+collectionId;
		
		String collectionStatus = collectionHome+"\\"+collectionId+"\\status.collection";
		
		if(!new File(collectionHome+"\\"+collectionId+"\\desc\\aeDescriptor.xml").exists()){
			try {
				if(new File(collectionHome+"\\"+collectionId+"\\pearinstall\\desc\\aeDescriptor.xml").exists()){
					fm.copyFile(collectionHome+"\\"+collectionId+"\\pearinstall\\desc\\aeDescriptor.xml", collectionHome+"\\"+collectionId+"\\desc\\aeDescriptor.xml");
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
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
			
			if(!status.equalsIgnoreCase("idle")){
				System.out.println("Collection busy..");
				Map<String,Object> property = new HashMap<String,Object>();
				
				property.put("message", "failed, collection busy");
				property.put("value", "2");
				
				apiResponse.put("items", property);
				return new Viewable("/general/ack", apiResponse);
			}
			
		} catch (Exception e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}
		
//		Crawler crawler = new Crawler();
//		
//		boolean isAnyCrawlerRun = crawler.isAnyCrawlerRun(collectionId);
//		
//		if(isAnyCrawlerRun){
//			Map<String,Object> errorProperty = new HashMap<String,Object>();
//			errorProperty.put("code", "500");
//			String message = "Unable to proceed because of running crawler.";
//			String detail = "Stop all the crawlers and try again.";
//			
//			errorProperty.put("message", message);
//			errorProperty.put("detail", detail);
//			
//			error.add(errorProperty);
//			
//			System.out.println(message + "\n" + detail);
//			
//			apiResponse.put("items", error);
//			return new Viewable("/exception/error", apiResponse);
//		}
		
		if(new File(collectionDir + "\\IndexRebuilder.jar").exists()){
			
			String goRun =  "java -jar \"" + collectionDir + "\\IndexRebuilder.jar\" \""+collectionHome+"\" \""+collectionId+"\"\r\nexit";
			try {
				fm.fileWriter(collectionDir+"\\startRebuild.bat", goRun, false);
				Runtime.getRuntime().exec("cmd /c start "+collectionDir+"\\startRebuild.bat");
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
//		
//		ArrayList<String> listFolder = new ArrayList<String>();
//		List<File> files = null;
//		ArrayList<File> allFiles = new ArrayList<File>();
//		FileManager fman = new FileManager();
//		
//		try {
//			System.out.println("preparing states..");
//			new FileManager().fileWriter(collectionStatus, "status=rebuildindex", false);
//			FileUtils.deleteDirectory(new File(collectionHome+"\\"+collectionId+"\\_temprebuild_"));
//		} catch (Exception e2) {
//			Map<String,Object> errorProperty = new HashMap<String,Object>();
//			errorProperty.put("code", "500");
//			errorProperty.put("message", e2.toString());
//			errorProperty.put("detail", sc.getStackTrace(e2));
//			
//			error.add(errorProperty);
//			
//			e2.printStackTrace();
//		}
//		
//		SolrServer server = new HttpSolrServer(URL);
//		((HttpSolrServer) server).setParser(new XMLResponseParser());
//		
//		try {
//			CoreAdminRequest.unloadCore(collectionId, false, false, server);
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
//		File collectionProperties = new File(collectionHome + "\\" + collectionId + 
//				"\\core.properties");
//		String collectionDirConf = collectionHome+"\\"+collectionId+"\\data";
//		
//		if(collectionProperties.isFile()){
//			try {
//				System.out.println("forcing delete data files");
//				FileDeleteStrategy.FORCE.delete(collectionProperties);
//				FileUtils.deleteDirectory(new File(collectionDirConf));
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
//		try {
//			Thread.sleep(2000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		
//        try {
//			CoreAdminRequest.createCore(collectionId, collectionId, server);
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
//		listFolder = fman.listFolderFullPath(collectionHome);
//		fman.createDir(collectionHome+"\\"+collectionId+"\\_temprebuild_");
//		int x = 0;
//		
//		for(String folder:listFolder){
//			if(folder.contains(collectionId)&&folder.contains("WEB_")){
//				try {
//					System.out.println("copying crawler : "+folder+"");
//					fman.createDir(collectionHome+"\\"+collectionId+"\\_temprebuild_\\"+x);
//					FileUtils.copyDirectory(new File(folder), new File(collectionHome+"\\"+collectionId+"\\_temprebuild_\\"+x));
//					x++;
//				} catch (Exception e) {
//					Map<String,Object> errorProperty = new HashMap<String,Object>();
//					errorProperty.put("code", "500");
//					errorProperty.put("message", e.toString());
//					errorProperty.put("detail", sc.getStackTrace(e));
//					
//					error.add(errorProperty);
//					
//					e.printStackTrace();
//				}
//			}
//			
//		}
//		
//		System.out.println("put all files..");
//		files = (List<File>) FileUtils.listFiles(new File(collectionHome+"\\"+collectionId+"\\_temprebuild_"), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
//		allFiles.addAll(files);
//		
//		System.out.println("post all data to solr.. please wait..");
//		for(File file:allFiles){
//			String filename = file.getName();
//			String extension = filename.substring(filename.lastIndexOf(".") + 1, filename.length());
//			if(extension.equalsIgnoreCase("txt")&&!filename.equals("log.txt")&&!filename.equals(
//					"pending_to_indexer.txt")&&
//					!filename.contains("crawled.txt")&&
//					!filename.contains("recentlycrawled.txt")){
//				try {
//					new Post(file.getAbsolutePath(),collectionId).runRebuild(file.getAbsolutePath(),collectionId);
//				} catch (IOException e) {
//					try {
//						System.out.println("\r\nerror in posting : forcing state to idle..\r\n");
//						FileUtils.deleteDirectory(new File(collectionHome+"\\"+collectionId+"\\_temprebuild_"));
//						new FileManager().fileWriter(collectionStatus, "status=idle", false);
//					} catch (Exception e1) {
//						// TODO Auto-generated catch block
//						e1.printStackTrace();
//					}
//					Map<String,Object> errorProperty = new HashMap<String,Object>();
//					errorProperty.put("code", "500");
//					errorProperty.put("message", e.toString());
//					errorProperty.put("detail", sc.getStackTrace(e));
//					
//					error.add(errorProperty);
//					
//					e.printStackTrace();
//				}
//			}
//		}
//		
//		try {
//			System.out.println("rebuild done.. deleting temporary files");
//			new FileManager().fileWriter(collectionStatus, "status=idle", false);
//			FileUtils.deleteDirectory(new File(collectionHome+"\\"+collectionId+"\\_temprebuild_"));
//			
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
	
	private Viewable checkStatus(String collectionId){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();

		String collectionStatus = collectionHome+"\\"+collectionId+"\\status.collection";
		
		if(!new File(collectionHome+"\\"+collectionId+"\\desc\\aeDescriptor.xml").exists()){
			try {
				if(new File(collectionHome+"\\"+collectionId+"\\pearinstall\\desc\\aeDescriptor.xml").exists()){
					fm.copyFile(collectionHome+"\\"+collectionId+"\\pearinstall\\desc\\aeDescriptor.xml", collectionHome+"\\"+collectionId+"\\desc\\aeDescriptor.xml");
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if(!new File(collectionStatus).exists()){
			try {
				fm.fileWriter(collectionStatus, "status=idle", false);
			} catch (Exception e) {
				Map<String,Object> errorProperty = new HashMap<String,Object>();
				errorProperty.put("code", "500");
				errorProperty.put("message", e.toString());
				errorProperty.put("detail", sc.getStackTrace(e));
				
				error.add(errorProperty);
				
				e.printStackTrace();
			}
		}else{
			try {
				Properties prop = new Properties();
				FileInputStream in = new FileInputStream(collectionStatus);
				prop.load(in);
				
				String status = (prop.getProperty("status"));
				
				Map<String,Object> property = new HashMap<String,Object>();
					
				property.put("message", status);
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
		
		if(error.size() > 0){
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", "idle");
			property.put("value", "0");
			
			apiResponse.put("items", property);
			return new Viewable("/general/ack", apiResponse);
		}
	}
	
	private Viewable getStatusRebuild(String collectionId){
		
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		List<File> filesDest = null;
		List<File> filesSource = new ArrayList<File>();
		int fileDestSize = 0;
		int fileSourceSize = 0;
		int percentage = 0;
		
		ArrayList<String> listFolder = new ArrayList<String>(); 
		listFolder = fm.listFolderFullPath(collectionHome);
		File statusfile = new File(collectionHome+"\\"+collectionId+"\\_temprebuild_\\statusFiles.stat");
		
		String status = "Preparing states..";
		String value = "0";
		
		if(new File(collectionHome+"\\"+collectionId+"\\_temprebuild_\\statusRebuild.stat").exists()){
			
			try {
				String file = FileUtils.readFileToString(statusfile);
				fileSourceSize = Integer.parseInt(file.trim());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); 
			}
			
			fileDestSize = 0;
			try {
				fileDestSize = (int) getNumberDocs(collectionId);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			percentage = (fileDestSize*100)/fileSourceSize;
//			cores = request.process(server);
			status = "Rebuilding.. : "+percentage+"%";
		}
		
		else if(new File(collectionHome+"\\"+collectionId+"\\_temprebuild_").exists()){
			filesDest = (List<File>) FileUtils.listFiles(new File(collectionHome+"\\"+collectionId+"\\_temprebuild_"), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
			fileDestSize = filesDest.size();
			
			if(!statusfile.exists()){
				for(String folder : listFolder){
					if(folder.contains(collectionId)&&folder.contains("WEB_")){
						List<File> filesSourceTemp = (List<File>) FileUtils.listFiles(new File(folder), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
						filesSource.addAll(filesSourceTemp);
					}
				}
				if(filesSource!=null){
					fileSourceSize = filesSource.size();
				}
				
				try {
					FileUtils.write(new File(collectionHome+"\\"+collectionId+"\\_temprebuild_\\statusFiles.stat"), ""+fileSourceSize+"", false);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else{
				try {
					String file = FileUtils.readFileToString(statusfile);
					fileSourceSize = Integer.parseInt(file.trim());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			percentage = (fileDestSize*100)/fileSourceSize;
			status = "Preparing.. : "+percentage+"%";
		}
		
		else if(new File(collectionHome+"\\"+collectionId+"").exists()){
			status = "Rebuild idle..";
		}
		else{
			status = "Error Collection Not Found..";
			value = "1";
		}
		
		if(percentage>99){
			fileDestSize = 0;
			try {
				fileDestSize = (int) getNumberDocs(collectionId);
			} catch (Exception e) {
				// TODO Auto-generated catch block
//				e.printStackTrace();
//				System.out.println("checking too fast, skip checking..");
			}
			percentage = (fileDestSize*100)/fileSourceSize;
//			cores = request.process(server);
			status = "Rebuilding.. : "+percentage+"%";
			
			try {
				FileUtils.write(new File(collectionHome+"\\"+collectionId+"\\_temprebuild_\\statusRebuild.stat"), "copying done!", false);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}		
		
		if(error.size() > 0){
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", status);
			property.put("value", value);
			
			apiResponse.put("items", property);
			return new Viewable("/general/ack", apiResponse);
		}
	}
	
	public long getNumberDocs(String collectionId) throws Exception{
		long numDocs = -1;
		
		SolrServer server = new HttpSolrServer(URL + "/" + collectionId);
		((HttpSolrServer) server).setParser(new XMLResponseParser());
		
		SolrQuery q = new SolrQuery("*:*");
		numDocs = server.query(q).getResults().getNumFound();

		return numDocs;
	}
}