package mata.icalite.api.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PearInstaller {
	
	public static String UIMA_BIN = null;
	public static String ZIP_BIN  = null;
	public static String COLLECTIONID  = null;
	
	public PearInstaller(String collectionHome, String zLoc, String collectionId){
		UIMA_BIN = collectionHome + "\\uima\\bin";
		ZIP_BIN = zLoc;
		COLLECTIONID = collectionId;
	}
	
	public String installPear(String pear_src, String pear_dst) throws Exception {
		System.out.println(UIMA_BIN + "\\runPearInstallerCli.cmd "+ 
				pear_src + " " + pear_dst);
		new FileManager().fileWriter(COLLECTIONID+"\\install.bat", UIMA_BIN + "\\runPearInstallerCli.cmd "+ 
				pear_src + " " + pear_dst+"\r\nexit\r\nexit\r\n", false);
		final Process process = Runtime.getRuntime().exec("cmd /c start /wait "+COLLECTIONID+"\\install.bat");
		process.waitFor();
		return process.toString();
	}
	
	public String extractPear(String pear_src, String pearOutput) throws Exception {
		System.out.println("\"" + ZIP_BIN + "\\7z.exe\" x " + pear_src + " -o" + pearOutput + "");
		new FileManager().fileWriter(pearOutput+"\\tobin.bat", "\"" + ZIP_BIN + "\\7z.exe\" x " + pear_src + " -o" + pearOutput + "\r\nexit\r\n", false);
		final Process process = Runtime.getRuntime().exec("cmd /c start /wait "+pearOutput+"\\tobin.bat");
		process.waitFor();
		return process.toString();
	}
	
	public String getPearId(String pearOutput) throws Exception{
		String pearId = null;
		
		String filepath = pearOutput + "\\metadata\\install.xml";
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(filepath);

//		Node firstChild = doc.getFirstChild();
		
		Node submitted_component = doc.getElementsByTagName("SUBMITTED_COMPONENT").item(0);
		NodeList list = submitted_component.getChildNodes();
		
		for (int i=0;i<list.getLength();i++) {
			Node node = list.item(i);
			
			if (node.getNodeName().equals("ID")) {
				pearId = node.getTextContent();
			}
		}

		System.out.println("Done");
		
		return pearId;
	}
	
	public void modifyPearXML (String pear_path) throws Exception{
		String filepath = pear_path;
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(filepath);
 
		Node primitive = doc.getElementsByTagName("primitive").item(0);
		primitive.setTextContent("false");
		
		Node aeMetadata = doc.getElementsByTagName("analysisEngineMetaData").item(0);
		NodeList list = aeMetadata.getChildNodes();
		
		for (int i=0;i<list.getLength();i++) {
			Node node = list.item(i);
			
			if (node.getNodeName().equals("capabilities")) {
				
			}
		}
 
		// write the content into xml file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(filepath));
		transformer.transform(source, result);
 
		System.out.println("Done");
	}
	
	public void jCasGenRun (String project_path) throws Exception {
		String descLoc = project_path + "\\desc\\aeDescriptor.xml";
		String src_path = project_path + "\\src";
		
		String command = "cmd /c " + UIMA_BIN + "\\jcasgen.bat "+ descLoc + " " + src_path;
//		System.out.println(command);
		Runtime.getRuntime().exec(command);
		
		try {
		    Thread.sleep(2000);                 //1000 milliseconds is one second.
		} catch(InterruptedException ex) {
		    Thread.currentThread().interrupt();
		}
	
//		try {
//			String command = "cmd /c start " + project_path + "\\source.bat";
//			System.out.println(command);
//			Runtime.getRuntime().exec(command);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
	}
	
	public static boolean checkFieldName(String collectionDir, ArrayList<String> fieldNames) 
			throws Exception{
		
		String schemaLoc = collectionDir+"\\conf\\schema.xml";
		BufferedReader br;
		br = null;
		String schemaFinal = "";
	        
		br = new BufferedReader(new FileReader(schemaLoc));
		String sCurrentLine;
		String writeFieldNameTemp = "<!-- THIS IS CUSTOM FIELD -->";
		String writeFieldName = "";
		boolean endCheck = false;
		while((sCurrentLine = br.readLine()) != null) 
		{
			if(!endCheck){
				for(String fieldName : fieldNames){
					writeFieldNameTemp = "	<field name=\""+fieldName+"\" type=\"string\" indexed=\"true\" stored=\"true\" multiValued=\"true\"/>\r\n";
					if(sCurrentLine.contains("<field name=\""+fieldName+"\"")){
	//					  br.close();
	//					  return false;
						  break;
					}
					else if(sCurrentLine.contains("<!-- THIS IS CUSTOM FIELD -->")){
						if(!writeFieldName.contains(writeFieldNameTemp)){
							writeFieldName = writeFieldName + writeFieldNameTemp;
						}
						
					}
					else if(sCurrentLine.contains("<!-- Common metadata fields,")){
						endCheck = true;
						break;
					}
				}
			}
			schemaFinal = schemaFinal + sCurrentLine+"\r\n";
		}
//		System.out.println(writeFieldName);
		
		if(br != null)
	    {
			br.close();
	    }
		
		new FileManager().fileWriter(schemaLoc, schemaFinal.replace("<!-- THIS IS CUSTOM FIELD -->", writeFieldName+"r\n	<!-- THIS IS CUSTOM FIELD -->"), false);
		return true;
	}
}
