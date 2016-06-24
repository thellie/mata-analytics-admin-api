package mata.icalite.api.util;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.request.DirectXmlRequest;

public class Post {
	private String pendingFilepath = null;
	private String currentDir = null;
	private static final String URL = "http://127.0.0.1:8983/solr";
	private boolean convert = true;
	
	public Post(String currentDir){
		this.currentDir = currentDir;
		pendingFilepath = currentDir + "\\pending_to_indexer.txt";
	}
	
	/**
	 * @param fileToPost = new crawled file
	 * @throws IOException
	 */
	public boolean run(String fileToPost, boolean convert) throws Exception{
		this.convert = convert;
		String[] currentDirParts = currentDir.split("\\\\");
		String collectionId = currentDirParts[currentDirParts.length - 1];
		String[] collParse = collectionId.split("\\.");
		collectionId = collParse[0];
		boolean goRun = false;
		
		String newCrawledFile = fileToPost;
//		System.out.println(newCrawledFile);
//		System.out.println(currentDir);
		File pendingFile = new File(pendingFilepath);
		
		if(!pendingFile.isFile()){
			FileUtils.write(pendingFile, "", false);
		}
		
		try {
			goRun = postFile(newCrawledFile, collectionId);
			if(goRun){
				deletePendingFile(newCrawledFile);
			}
			else{
				addPendingFile(newCrawledFile);
			}
		} catch (IOException | SolrServerException e) {
			e.printStackTrace();
			
			try {
				addPendingFile(newCrawledFile);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		
		return goRun;
	}
	
	public void runPendingFile() throws Exception{
		
		File pendingFile = new File(pendingFilepath);
		String[] currentDirParts = currentDir.split("\\\\");
		String collectionId = currentDirParts[currentDirParts.length - 1];
		String[] collParse = collectionId.split("\\.");
		collectionId = collParse[0];
		
		if(!pendingFile.isFile()){
			FileUtils.write(pendingFile, "", false);
		}
		
		List<String> pendingFilenames = (List<String>) FileUtils.readLines(pendingFile);
		System.out.println("size pending file : " +pendingFilenames.size());
		boolean goRun = false;
		
		for(String pendingFilename : pendingFilenames){
			try {
				goRun = postFile(pendingFilename, collectionId);
//				System.out.println(goRun);
				if(goRun){
					deletePendingFile(pendingFilename);
				}
				else{
					addPendingFile(pendingFilename);
				}
			} catch (Exception e) {
				e.printStackTrace();
				
				try {
					addPendingFile(pendingFilename);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
	
	public void addPendingFile(String newFilename) throws IOException{
		List<String> filenames = FileUtils.readLines(new File(pendingFilepath));
//		System.out.println(pendingFilepath);
//		System.out.println(filenames);
		
		if(!filenames.contains(newFilename)){
			FileUtils.write(new File(pendingFilepath), newFilename+"\r\n", true);
		}
	}
	
	public void deletePendingFile(String filenameToDelete) throws IOException{
		List<String> filenames = FileUtils.readLines(new File(pendingFilepath));
//		System.out.println(pendingFilepath);
//		System.out.println(filenames);
		
		if(filenames.contains(filenameToDelete)){
			filenames.remove(filenameToDelete);
		}
		
		FileUtils.writeLines(new File(pendingFilepath), filenames, false);
	}
	
	public boolean postFile(String filename, String collectionId) throws Exception, SolrServerException{
//		collectionId = "collection2";
		File homeDirectory = new File(currentDir).getParentFile();
		String statusCollection = homeDirectory.getAbsolutePath()+"\\"+collectionId+"\\status.collection";
//		System.out.println("status location loc : "+statusCollection);
//		String state2 = new FileManager().readData(statusCollection);
//		System.out.println(state2);
		if(new File(statusCollection).exists()){
//			System.out.println("status collection file exist");
			String state = new FileManager().readData(statusCollection);
//			System.out.println(state);
			if(state.contains("idle")){
				System.out.println("Post: " + filename + ", to server");
				SolrServer server = new HttpSolrServer(URL);
				((HttpSolrServer) server).setParser(new XMLResponseParser());
				 
				String body = FileUtils.readFileToString(new File(filename), "UTF-8");
				if(convert){
					try {
						body = new Converter().convertForMax(body);
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				DirectXmlRequest xmlreq = new DirectXmlRequest( "/" + collectionId + "/update", 
						body); 
				
				try{
//					SolrPingResponse ping = server.ping();
						int x = 0;
						if(x == 0){
				//			System.out.println("server up");

							try{
				//				server.commit(true,true,true);
				//				System.out.println(ur);
								server.request(xmlreq);
								System.out.println("request sent");
							}
							catch(Exception e){
								new FileManager().fileWriter(currentDir+"\\log.txt", SystemControl.getCurrentTime()+" - FAILED TO PUSH TO SOLR, SERVER DEAD. PUT IN LIST - FILE : "+filename+"", true);
								e.printStackTrace();
								return false;
							}
				//			System.out.println("request commited");
							return true;
						}
					}
					catch(Exception e){
						new FileManager().fileWriter(currentDir+"\\log.txt", SystemControl.getCurrentTime()+" - FAILED TO PUSH TO SOLR, SERVER DEAD. PUT IN LIST - FILE : "+filename+"", true);
						return false;
					}

			}
		}
		else{
			new FileManager().fileWriter(currentDir+"\\log.txt", SystemControl.getCurrentTime()+" - FAILED TO PUSH TO SOLR, SERVER DEAD. PUT IN LIST  - FILE : "+filename+"", true);
			System.out.println("collection busy, pending file : "+filename);
		}
		return false;
	}
}
