package mata.icalite.api.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;


public class FileManager {
	
	public double fileSize(String filename){
		
//		File file =new File("c:\\java_xml_logo.jpg");
		File file = new File(filename);
		double output = 0;

		if(file.exists()){

			double bytes = file.length();
			double kilobytes = (bytes / 1024);
			output = kilobytes;
			
		}else{
			 System.out.println("File does not exists!");
		}

		return output;
	}

	  public String readData(String filename){
		  BufferedReader br;
		  String outFinal;
		  br = null;
		  outFinal = "";
		  try{
			  br = new BufferedReader(new FileReader(filename));
			  String sCurrentLine;
			  while((sCurrentLine = br.readLine()) != null) 
			  {
				  outFinal = (new StringBuilder(String.valueOf(outFinal))).append(sCurrentLine).append("\r\n").toString();
			  }
		  }
		  catch(Exception e){
			  
		  }
		  finally{
			  try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		  }
		  
		  return outFinal;
		 
	  }
	  
	  public void fileWriter(String outfilename, String values, boolean writeAppend) throws Exception{
		  FileWriter f1;		  
		
		  if(!writeAppend){
			  f1 = new FileWriter(outfilename);
			  f1.write(values);
			  f1.close();
		  }else{			
			  PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outfilename, true)));
			  out.println(values);
			  out.close();
		  }
	  }
	  
	  public String createDir(String fullpath){
		  boolean success = (new File(fullpath)).mkdirs();
		  
		  if (!success) {
		      // Directory creation failed
			  return fullpath;
		  }
		  
		  return fullpath;
	  }
	  
  public boolean deleteFile(String fileFullPath, boolean unlocker){
		  
		  File filePath = new File(fileFullPath);
		  
		  if(filePath.isDirectory()){
			  List<File> files = (List<File>) FileUtils.listFiles(filePath, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
//			  System.out.println(files.size());
			  for(File fileDel : files){
				  if(unlocker){
					  try {
						  int LIMITTRY = 0;
						  fileDel.delete();
//						  System.out.println("deleting "+fileDel.getAbsolutePath()+"");
						  Thread.sleep(150);
						  if(fileDel.exists()){
							  System.out.println("failed to delete "+fileDel.getAbsolutePath()+", using unlocker now!");
							  Runtime.getRuntime().exec("unlocker \""+fileDel.getAbsolutePath()+"\" /S /D");
							  while(LIMITTRY<50){
									if(!fileDel.exists()){
										LIMITTRY = 101;
									}
									else{
										Thread.sleep(1000);
									}
									if(LIMITTRY%5==0){
										Runtime.getRuntime().exec("unlocker \""+fileDel.getAbsolutePath()+"\" /S /D");
									}
									LIMITTRY++;
								}
						  }

					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return false;
					}
				  }
				  else{
					  if(fileDel.delete()){

					  }
				  }
			  }
			  return true;
		  }
		  else{
			  if(unlocker){
				  try {
					  int LIMITTRY = 0;
//					  System.out.println("deleting "+filePath.getAbsolutePath()+"");
					  filePath.delete();
					  Thread.sleep(150);
					  if(filePath.exists()){
						  System.out.println("failed to delete "+filePath.getAbsolutePath()+", using unlocker now!");
						  Runtime.getRuntime().exec("unlocker \""+filePath.getAbsolutePath()+"\" /S /D");
						  while(LIMITTRY<50){
								if(!new File(fileFullPath).exists()){
									LIMITTRY = 101;
								}
								else{
									Thread.sleep(1000);
								}
								if(LIMITTRY%10==0){
									Runtime.getRuntime().exec("unlocker \""+filePath.getAbsolutePath()+"\" /S /D");
								}
								LIMITTRY++;
							}
					  }
				} 
				  catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return false;
				}
			  }
			  else{
				  if(filePath.delete()){
					  return true;
				  }
			  }
			  
		  }

		  return false;
	  }
	  
	  public boolean moveFile(String fileFullPath, String folderFullPath){
		  File afile =new File(fileFullPath);
		   
      	  if(afile.renameTo(new File(folderFullPath + afile.getName()))){
//      		  System.out.println("File is moved successful!");
      	  }else{
//      		  System.out.println("File is failed to move!");
      		  return false;
      	  }
		  
		  return true;
	  }
	  
	  public boolean copyFile(String fileSource, String fileDest) throws Exception{
		  InputStream input = null;
		  OutputStream output = null;
		  
		  input = new FileInputStream(fileSource);
		  output = new FileOutputStream(fileDest);
		  
		  byte[] buf = new byte[1024];
		  int bytesRead;
		 
		  while ((bytesRead = input.read(buf)) > 0) {
			  output.write(buf, 0, bytesRead);
		  }
			
		  input.close();
		  output.close();
		  
		  return true;
	  }
	  
	  public ArrayList<String> listFile(String fullPath){
		  File folder = new File(fullPath);
		  File[] listOfFiles = folder.listFiles();
		  ArrayList<String> pathOutFiles = new ArrayList<String>();
		  
	      for (int i = 0; i < listOfFiles.length; i++) {
	        if (listOfFiles[i].isFile()) {
	        	pathOutFiles.add(listOfFiles[i].getName());
//		        	System.out.println("File " + listOfFiles[i].getName());
	        }
	      }
	      
		  return pathOutFiles;
	  }
	  
	  public ArrayList<String> listFolder(String fullPath){
		  File folder = new File(fullPath);
		  File[] listOfFiles = folder.listFiles();
		  ArrayList<String> pathOutFiles = new ArrayList<String>();

	      for (int i = 0; i < listOfFiles.length; i++) {
	        if (listOfFiles[i].isDirectory()) {
	        	pathOutFiles.add(listOfFiles[i].getName());
//	        	System.out.println("Directory " + listOfFiles[i].getName());
	        }
	      }
	      
		  return pathOutFiles;
	  }
	  
	  public ArrayList<String> listFolderFullPath(String fullPath){
		  File folder = new File(fullPath);
		  File[] listOfFiles = folder.listFiles();
		  ArrayList<String> pathOutFiles = new ArrayList<String>();

	      for (int i = 0; i < listOfFiles.length; i++) {
	        if (listOfFiles[i].isDirectory()) {
	        	pathOutFiles.add(listOfFiles[i].getAbsolutePath());
//	        	System.out.println("Directory " + listOfFiles[i].getName());
	        }
	      }
	      
		  return pathOutFiles;
	  }
	  
	  
	  public double checkFileSize(String filename){
		  File file =new File(filename);
		  
		  double megabytes = 0.0000;
		  double gigabytes = (megabytes / 1024);
		  double terabytes = (gigabytes / 1024);
		  double petabytes = (terabytes / 1024);
		  double exabytes = (petabytes / 1024);
		  double zettabytes = (exabytes / 1024);
		  @SuppressWarnings("unused")
		  double yottabytes = (zettabytes / 1024);
		  
		  if(file.exists()){
			  
				double bytes = file.length();
				double kilobytes = (bytes / 1024);
				megabytes = (kilobytes / 1024);
				gigabytes = (megabytes / 1024);
				terabytes = (gigabytes / 1024);
				petabytes = (terabytes / 1024);
				exabytes = (petabytes / 1024);
				zettabytes = (exabytes / 1024);
				yottabytes = (zettabytes / 1024);
	 
			}else{
				 System.out.println("File does not exists!");
			}
		  
		  return megabytes;
	  }
	  
	  public String getFullPath(String path){
		  File file = new File(path);
		  String dirPath = file.getAbsoluteFile().getParentFile().getAbsolutePath();
		  
		  return dirPath;
	  }
	  
	  public String getExecuteFileLoc(){
		  try {
			return new File(FileManager.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParent();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	  }
}
