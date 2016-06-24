package mata.icalite.api.admin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import mata.icalite.api.util.FileManager;
import mata.icalite.api.util.Security;
import mata.icalite.api.util.SystemControl;

import com.sun.jersey.api.view.Viewable;

@Path("/field")
public class Field {
	private String caxHome = null;
	private String collectionHome = null;
	private SystemControl sc = null;

	public Field(){
		caxHome = System.getenv("SOLR_HOME");
		collectionHome = caxHome + "\\example\\solr";
		
		sc = new SystemControl();
	}
	
	@GET
	@Produces("application/xml")
	public Viewable getParam(@QueryParam("method") String method,
				@QueryParam("collectionId") String collectionId,
				@QueryParam("crawlerId") String crawlerId,
				@QueryParam("name") String name,
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
		
		if(method.equals("getFieldSchema")){
			return getFieldSchema(collectionId);
		}else if(method.equals("deleteFieldSchema")){
			return deleteFieldSchema(collectionId, name);
		}else if(method.equals("getAnnotator")){
			return getAnnotator(collectionId);
		}else if(method.equals("getFieldFeature")){
			return getFieldFeature(collectionId);
		}else if(method.equals("getListCustomDictionary")){
			return getListCustomDictionary(collectionId);
		}else if(method.equals("getSpecificCustomDictionary")){
			return getSpecificCustomDictionary(collectionId,name);
		}else if(method.equals("deleteCustomDictionary")){
			return deleteCustomDictionary(collectionId,name);
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
			@QueryParam("collectionId") String collectionId,
			@QueryParam("crawlerId") String crawlerId,
			@QueryParam("sessionId") String session,
			@QueryParam("name") String name,
			String body) {
		
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		Security secure = new Security();
//		JSONObject jsonobj = null;
		
		try {
			if(!new Security().derbyCheck(session)){
				Map<String,Object> property = new HashMap<String,Object>();
				
				property.put("message", "session expired");
				property.put("value", "1");
				
				apiResponse.put("items", property);
				return new Viewable("/general/ack", apiResponse);
			}
			else {
				try {
//					jsonobj = new JSONObject(body);
				} catch (Exception e) {
					Map<String,Object> errorProperty = new HashMap<String,Object>();
					errorProperty.put("code", "500");
					errorProperty.put("message", e.toString());
					errorProperty.put("detail", sc.getStackTrace(e));
					
					error.add(errorProperty);
					
					e.printStackTrace();
				}
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
				try{
					if(collectionId.length()>1){
//						jsonobj.remove("collectionId");
//						jsonobj.put("collectionId", collectionId);
//						body = jsonobj.toString();
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
		
		if(method.equalsIgnoreCase("addFieldSchema")){
			return addFieldSchema(collectionId, body);
		}else if(method.equalsIgnoreCase("editFieldSchema")){
			return editFieldSchema(collectionId, body, name);
		}else if(method.equalsIgnoreCase("addFieldMapping")){
			return addFieldMapping(collectionId, body);
		}else if(method.equalsIgnoreCase("deleteFieldFeature")){
			return deleteFieldFeature(collectionId, body);
		}else if(method.equalsIgnoreCase("addCustomDictionary")){
			return addCustomDictionary(collectionId, body, name);
		}else if(method.equalsIgnoreCase("editCustomDictionary")){
			return editCustomDictionary(collectionId, body, name);
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
	
	private String checkingErrorField(String schema){
		
		Pattern p = Pattern.compile("<!-- THIS IS CUSTOM FIELD -->");
		Matcher m = p.matcher(schema);
		int count = 0;
		while(m.find()){
			if(count>0){
				schema = schema.replace("<!-- THIS IS CUSTOM FIELD -->", "");
				schema = schema.replace("<!-- CUSTOM ANNOTATOR FIELD -->", "<!-- CUSTOM ANNOTATOR FIELD -->\r\n		<!-- THIS IS CUSTOM FIELD -->");
				break;
			}
			count++;
		}
		
		return schema;
	}
		
	private Viewable getFieldSchema(String collectionId){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
	
		List<Object> error = new ArrayList<Object>();
		List<Object> fieldList = new ArrayList<Object>();
		
		String schemaLoc = collectionHome+"\\"+collectionId+"\\conf\\schema.xml";
		
		
		try{
			FileManager fman = new FileManager();
			String schema = fman.readData(schemaLoc);		
			schema = checkingErrorField(schema);
			
			Pattern p = Pattern.compile("<field (.*?)/>");
			Pattern p1 = Pattern.compile("name=\"(.*?)\"");
			Pattern p2 = Pattern.compile("type=\"(.*?)\"");
			Pattern p3 = Pattern.compile("multiValued=\"(.*?)\"");
			Matcher m = p.matcher(schema);
			while(m.find()){
				Map<String,Object> fields = new HashMap<String,Object>();
				Matcher m1 = p1.matcher(m.group());
				if(m1.find()){
					String nameField = m1.group().replaceAll("name=\"|\"", "").trim();
					fields.put("name", nameField);
				}
				Matcher m2 = p2.matcher(m.group());
				if(m2.find()){
					String typeField = m2.group().replaceAll("type=\"|\"", "").trim();
					fields.put("type", typeField);
				}
				Matcher m3 = p3.matcher(m.group());
				if(m3.find()){
					String multiValue = m3.group().replaceAll("multiValued=\"|\"", "").trim();
					fields.put("multi", multiValue);
				}
				fieldList.add(fields);
			}
		}catch(Exception e){
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);
			
			e.printStackTrace();			
		}
		
		if(error.size() > 0){
			System.out.println("error");
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}else{				
			apiResponse.put("items", fieldList);
			return new Viewable("/field/getField", apiResponse);
		}
	}
	
	private Viewable getFieldFeature(String collectionId){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
	
		List<Object> error = new ArrayList<Object>();
		
		String solrconfigLocation = collectionHome+"\\"+collectionId+"\\conf\\solrconfig.xml";
		List<Object> listAnnotator = new ArrayList<Object>();
		
		try{
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(new File(solrconfigLocation));
			
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
			
			Node processorNode = doc.getElementsByTagName("processor").item(0);		
			Node lstNode = processorNode.getChildNodes().item(0);
			Node mappingNode = lstNode.getChildNodes().item(4);
			
			NodeList listMapping = mappingNode.getChildNodes();
			
			for (int i = 0; i < listMapping.getLength(); i++){
				Node mapping = listMapping.item(i);
				Node name = mapping.getChildNodes().item(0);
//				System.out.println(name.getTextContent());
				List<Object> mapFeatures = new ArrayList<Object>();
				Map<String, Object> mapName = new HashMap<String, Object>();
				for( int j = 1; j < mapping.getChildNodes().getLength(); j++){
					Node featureMap = mapping.getChildNodes().item(j);
					Map<String, String> mapFeature = new HashMap<String, String>();
					if (featureMap.getNodeName().equals("lst")){
						String featureName = featureMap.getFirstChild().getTextContent();
						String fieldName = featureMap.getLastChild().getTextContent();
						mapFeature.put("featurename", featureName);
						mapFeature.put("fieldname", fieldName);
						mapFeatures.add(mapFeature);
					}
				}
				mapName.put("name", name.getTextContent());
				mapName.put("mapping", mapFeatures);
				listAnnotator.add(mapName);
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
			
		if(error.size() > 0){
			System.out.println("error");
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}else{				
			apiResponse.put("items", listAnnotator);
			return new Viewable("/field/getFieldFeature", apiResponse);
		}
	}
	
	private Viewable getAnnotator(String collectionId){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
	
		List<Object> error = new ArrayList<Object>();
		String descLocation = collectionHome+"\\"+collectionId+"\\peartemp\\desc";
		ArrayList<String> listFolder = new ArrayList<String>();
		List<File> files = null;
		ArrayList<File> allFiles = new ArrayList<File>();
		ArrayList<String> cleanUpList = new ArrayList<String>();
		FileManager fman = new FileManager();

		List<Object> listMap = new ArrayList<Object>();
		  
		listFolder = fman.listFolderFullPath(descLocation);
//		System.out.println(listFolder);
		for(String folder:listFolder){
			files = (List<File>) FileUtils.listFiles(new File(folder), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
	//			System.out.println(files.toString());
			for(File file : files){
				if(file.toString().contains("-ts.xml")){
					allFiles.add(file);
				}
			}
		}
		
		try{
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(new File(descLocation+"\\TypeCleanup5.xml"));
			
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
			
			Node nList = doc.getElementsByTagName("array").item(0);
//			System.out.println(nList.getNodeName());
			
			NodeList typeProperties = nList.getChildNodes();
			for (int j=0;j<typeProperties.getLength();j++) {
				if(typeProperties.item(j).getNodeName().equals("string")){
//					System.out.println();
					cleanUpList.add(typeProperties.item(j).getTextContent());
				}	  
			}
			
//			System.out.println("Cleanup List : "+cleanUpList);
		}
		catch(Exception e){
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);
			
			e.printStackTrace();
		}
		try{
			  for(File fileXML : allFiles){
					DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
					Document doc = docBuilder.parse(fileXML);
						
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
				//	System.out.println(nList.item(0).getNodeName());
					Map<String, Object> map = new HashMap<String, Object>();
						
					for (int i=0;i<nList.getLength();i++) {
						NodeList typeProperties = nList.item(i).getChildNodes();
							for (int j=0;j<typeProperties.getLength();j++) {
								map = new HashMap<String, Object>();
								if(typeProperties.item(j).getNodeName().equals("name")){
//									System.out.println("Checking : "+typeProperties.item(j).getTextContent());
									if(!cleanUpList.contains(typeProperties.item(j).getTextContent())){
//										System.out.println("name: "+typeProperties.item(j).getTextContent());
										map.put("name", typeProperties.item(j).getTextContent().trim());
									}else{
										break;
									}
								}
								try{
									if(typeProperties.getLength()>3){
										if (typeProperties.item(j+3).getNodeName().equals("features")) {
											NodeList featureDescription = typeProperties.item(3).getChildNodes();
											ArrayList<String> listFeatures = new ArrayList<String>();
											for (int k=0;k<featureDescription.getLength();k++) {
												Node features = featureDescription.item(k).getFirstChild();
//												System.out.println("feature: "+features.getTextContent());
												listFeatures.add(features.getTextContent().trim());
											}
											listFeatures.add("begin");
											listFeatures.add("end");
											map.put("features", listFeatures);
										}
//										System.out.println("map: "+map);
										break;
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
							if(map!=null && map.size()>0){
//								System.out.println("adding map: "+map);
								listMap.add(map);
							}
//							System.out.println(map.toString());
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
		
		if(error.size() > 0){
			System.out.println("error");
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}else{				
			apiResponse.put("items", listMap);
			return new Viewable("/field/getAnnotator", apiResponse);
		}
	}
	
	private Viewable deleteFieldSchema(String collectionId, String name){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		
		String schemaLoc = collectionHome+"\\"+collectionId+"\\conf\\schema.xml";
		
		try {
			FileManager fman = new FileManager();
			
			String schema = fman.readData(schemaLoc);
			
			Pattern p = Pattern.compile("<field name=\""+name+"\"(.*?)/>");
			Matcher m = p.matcher(schema);
			if(m.find()){
				schema = schema.replaceAll("<field name=\""+name+"\"(.*?)/>", "");
				fman.fileWriter(schemaLoc, schema, false);
				System.out.println("delete field schema : "+name+" for collection : "+collectionId+"");
			}
			
		} catch (Exception e) {
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);
			
			e.printStackTrace();
		}
		
		try {
			overwriteTemplate(collectionId);
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
	
	private Viewable deleteFieldFeature(String collectionId, String body){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
	
		List<Object> error = new ArrayList<Object>();
		
		String solrconfigLocation = collectionHome+"\\"+collectionId+"\\conf\\solrconfig.xml";

		try{
			JSONObject jsonobj = new JSONObject(body);
			String featureDelete = (String) jsonobj.get("featurename");
			String fieldDelete = (String) jsonobj.get("fieldname");
			
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(new File(solrconfigLocation));
			
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
			
			Node processorNode = doc.getElementsByTagName("processor").item(0);		
			Node lstNode = processorNode.getChildNodes().item(0);
			Node mappingNode = lstNode.getChildNodes().item(4);
			
			NodeList listMapping = mappingNode.getChildNodes();
			
			for (int i = 0; i < listMapping.getLength(); i++){
				Node mapping = listMapping.item(i);

				for( int j = 1; j < mapping.getChildNodes().getLength(); j++){
					Node featureMap = mapping.getChildNodes().item(j);
					if (featureMap.getNodeName().equals("lst")){
						String featureName = featureMap.getFirstChild().getTextContent();
						String fieldName = featureMap.getLastChild().getTextContent();
						if(featureName.equals(featureDelete) && fieldName.equals(fieldDelete)){
							listMapping.item(i).removeChild(featureMap);
						}
					}
				}
			}
			
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(solrconfigLocation));
			transformer.transform(source, result);
			
			System.out.println("delete mapped feature : "+featureDelete+" for collection : "+collectionId+"");
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
			System.out.println("error");
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
	
	private Viewable addFieldSchema(String collectionId, String body){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		
//		String schemaTempLoc = collectionHome+"\\"+collectionId+"\\conf\\schema_template.xml";
		String schemaLoc = collectionHome+"\\"+collectionId+"\\conf\\schema.xml";
		
//		JSONParser parser = new JSONParser();
//		System.out.println(body);
		JSONObject jsonobj = new JSONObject(body);
		try {
//			Object obj = parser.parse(body);
//			JSONObject jsonObject = (JSONObject) obj;
			String name = (String) jsonobj.get("name");
			String type = (String) jsonobj.get("type");
			if(type.equalsIgnoreCase("int")){
				type = "integer";
			}
			String multivalue = (String) jsonobj.get("multivalue");
			String finalObject = "<field name=\""+name+"\" type=\""+type+"\" indexed=\"true\" stored=\"true\"  multiValued=\""+multivalue+"\"/>";
			
			
			try{
				FileManager fman = new FileManager();
				String schema = fman.readData(schemaLoc);
				boolean check = checkFieldName(name, schema);
				String findStr = "<!-- THIS IS CUSTOM FIELD -->";
				
				if(!check){
					Map<String,Object> property = new HashMap<String,Object>();
					
					property.put("message", "field exist");
					property.put("value", "1");
					
					apiResponse.put("items", property);
					return new Viewable("/general/ack", apiResponse);
				}
				
				String[] dataBuild = schema.split(findStr);
				int count = 0;
				String finalData = "";
				for(String buildstr : dataBuild){
					if(count==0){
						finalData = finalData + buildstr + findStr;
					}
					else{
						finalData = finalData + buildstr;
					}
					count++;
				}
				
				schema = finalData;
				schema = schema.replace(findStr, findStr +"\r\n"+ finalObject);
				
				fman.fileWriter(schemaLoc, schema, false);
				System.out.println("adding field schema : "+name+" for collection : "+collectionId+"");
			}
			catch(Exception e){
				Map<String,Object> errorProperty = new HashMap<String,Object>();
				errorProperty.put("code", "500");
				errorProperty.put("message", e.toString());
				errorProperty.put("detail", sc.getStackTrace(e));
				
				error.add(errorProperty);
				
				e.printStackTrace();
			}
		} catch (Exception e) {
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);
			
			e.printStackTrace();
		}
		
		try {
			overwriteTemplate(collectionId);
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
	
	private Viewable addFieldMapping(String collectionId, String body){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		
		String solrConfigLoc = collectionHome+"\\"+collectionId+"\\conf\\solrconfig.xml";
		
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		Document doc; 
		
		JSONObject jsonobj = new JSONObject(body);
//		System.out.println(body);
		try {
			String name = (String) jsonobj.get("name");
			String feature = (String) jsonobj.get("feature");
			String fieldName = (String) jsonobj.get("fieldname");
			
			if(name.length()<1 || feature.length()<1 || fieldName.length()<1){
				Map<String,Object> property = new HashMap<String,Object>();
				
				property.put("message", "failed");
				property.put("details", "error inputing data");
				property.put("value", "1");
				
				apiResponse.put("items", property);
				return new Viewable("/field/ack", apiResponse);
			}

			docBuilder = docFactory.newDocumentBuilder();
			doc = docBuilder.parse(new File(solrConfigLoc));
			
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
			
			boolean boolSucceed = false;
			
			if(checkNames(doc, name))
			{
				NodeList nList = doc.getElementsByTagName("lst");
				int x = 0;
				while(x<nList.getLength()){
					NamedNodeMap attrib = nList.item(x).getAttributes();
					Node nodeAttrib = attrib.getNamedItem("name");
					if(nodeAttrib!=null){
						if(nodeAttrib.getTextContent().equalsIgnoreCase("fieldMappings")){
//							System.out.println(nodeAttrib.getTextContent());
							Element type = doc.createElement("lst");
							type.setAttribute("name", "type");
							
							Element elName = doc.createElement("str");
							elName.setAttribute("name", "name");
							elName.setTextContent(name);
//							System.out.println("add name: "+name);
							type.appendChild(elName);
							
							Element elMapping = doc.createElement("lst");
							elMapping.setAttribute("name", "mapping");
							
							Element elFeature = doc.createElement("str");
							elFeature.setAttribute("name", "feature");
							elFeature.setTextContent(feature);
//							System.out.println("add feature: "+feature);
							
							Element elFieldName = doc.createElement("str");
							elFieldName.setAttribute("name", "field");
							elFieldName.setTextContent(fieldName);
//							System.out.println("add fieldName: "+fieldName);
							
							elMapping.appendChild(elFeature);
							elMapping.appendChild(elFieldName);
							
							type.appendChild(elMapping);
	
							nList.item(x).appendChild(type);
							boolSucceed = true;
							break;
						}
					}
					x++;
				}
				if(!boolSucceed){
					Map<String,Object> property = new HashMap<String,Object>();
					
					property.put("message", "failed");
					property.put("details", "error inputing data");
					property.put("value", "1");
					
					apiResponse.put("items", property);
					return new Viewable("/field/ack", apiResponse);
				}
			}else{
				NodeList nList = doc.getElementsByTagName("lst");
				int x = 0;	
				while(x<nList.getLength()){
					NamedNodeMap attrib = nList.item(x).getAttributes();
					Node nodeAttrib = attrib.getNamedItem("name");
					boolean noFeature = true;
					if(nodeAttrib!=null){
						if(nodeAttrib.getTextContent().equalsIgnoreCase("type")){
							Node nodeName = nList.item(x).getFirstChild();
							if(name.equalsIgnoreCase(nodeName.getTextContent())){
								Node checkExistNode = nList.item(x);
								NodeList listExists = checkExistNode.getChildNodes();
								int y = 0;
								while(y<listExists.getLength()){
									Node checkNode = listExists.item(y).getFirstChild();
									if(checkNode.getTextContent().equalsIgnoreCase(feature)){
//										System.out.println(checkNode.getTextContent());
										noFeature = false;
										
										Map<String,Object> property = new HashMap<String,Object>();
										
										property.put("message", "failed");
										property.put("details", "feature name already exist");
										property.put("value", "1");
										
										apiResponse.put("items", property);
										return new Viewable("/field/ack", apiResponse);
										
									}
									y++;
								}
								if(noFeature){
									
									Element elMapping = doc.createElement("lst");
									elMapping.setAttribute("name", "mapping");
									
									Element elFeature = doc.createElement("str");
									elFeature.setAttribute("name", "feature");
									elFeature.setTextContent(feature);
									
									Element elFieldName = doc.createElement("str");
									elFieldName.setAttribute("name", "field");
									elFieldName.setTextContent(fieldName);
									
									elMapping.appendChild(elFeature);
									elMapping.appendChild(elFieldName);
																	
									nList.item(x).appendChild(elMapping);
									
									break;
								}
							}
						}
					}
					x++;
				}
			}
			
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(solrConfigLoc));
			transformer.transform(source, result);
			
			System.out.println("mapping feature : "+feature+" to field : "+fieldName+" for collection : "+collectionId+"");
			
		} catch (Exception e) {
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);
			
			e.printStackTrace();
		}
		
		//overwriteTemplate(collectionId);
		
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
	
	private Viewable getListCustomDictionary(String collectionId){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
	
		List<Object> error = new ArrayList<Object>();
		
		String customDictionaryLocation = collectionHome+"\\"+collectionId+"\\conf\\";
		
		FileManager fm = new FileManager();
		ArrayList<String> listFile = new ArrayList<String>();
		ArrayList<String> listFileFinal = new ArrayList<String>();
		listFile = fm.listFile(customDictionaryLocation);
		for(String file : listFile){
			if(file.contains("customdict_")){
				listFileFinal.add(file);
			}
		}
		
		if(error.size() > 0){
			System.out.println("error");
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}else{				
			apiResponse.put("items", listFileFinal);
			return new Viewable("/field/getListUserDictionary", apiResponse);
		}
	}
	
	private Viewable editCustomDictionary(String collectionId, String body, String name){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
	
		String syn_filename = "";
		String customdict_filename = "";
		
		if(!name.contains("customdict_")){
			customdict_filename = "customdict_"+name;
			syn_filename = "syn_"+name;
		}
		else{
			customdict_filename = name;
			syn_filename = name.replace("customdict_", "");
			syn_filename = "syn_"+name;
		}
		
		List<Object> error = new ArrayList<Object>();
		
		JSONObject jsonobj = new JSONObject(body);
		JSONArray jsonArray = jsonobj.getJSONArray("custom");
		ArrayList<String> listDictionary = new ArrayList<String>();
		for(int n = 0; n < jsonArray.length(); n++)
		{
		    JSONObject object = jsonArray.getJSONObject(n);
		    String customdictionary = (String) object.get("dictionary");
		    listDictionary.add(customdictionary);
		}
		
		boolean successcreatefile = createSynonymDictionaryFile(collectionId, listDictionary, syn_filename);
		successcreatefile = createCustomDictionaryFile(collectionId, listDictionary, customdict_filename);
		
		if(!successcreatefile){
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", "error");
			errorProperty.put("detail", "error creating file custom dictionary");
			
			error.add(errorProperty);
			System.out.println("error");
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}
		
		if(error.size() > 0){
			System.out.println("error");
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
	
	private Viewable getSpecificCustomDictionary(String collectionId, String filename){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
	
		List<Object> error = new ArrayList<Object>();
		
		filename = filename.replace("customdict_", "").replace("syn_", "");
		filename = "syn_"+filename;
		
		String customDictionary = collectionHome+"\\"+collectionId+"\\conf\\"+filename;
		
		FileManager fm = new FileManager();
		String[] splitString = null;
		try {
			splitString = fm.readData(customDictionary).split("\r\n");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);
			
			e.printStackTrace();
		}
		
		ArrayList<String> listDictionary = new ArrayList<String>(Arrays.asList(splitString));
		
		if(error.size() > 0){
			System.out.println("error");
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}else{				
			apiResponse.put("items", listDictionary);
			return new Viewable("/field/getSpecifyUserDictionary", apiResponse);
		}
	}
	
	private boolean createSynonymDictionaryFile(String collectionId, ArrayList<String> listDictionary, String file){	
		String customDictionaryLocation = collectionHome+"\\"+collectionId+"\\conf\\"+file;
		FileManager fm = new FileManager();
		
		StringBuilder customDictionary = new StringBuilder();
		for(String dictionary : listDictionary){
			customDictionary.append(dictionary);
			customDictionary.append("\r\n");
		}
		
		try {
			fm.fileWriter(customDictionaryLocation, customDictionary.toString(), false);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	private boolean createCustomDictionaryFile(String collectionId, ArrayList<String> listDictionary, String file){	
		String customDictionaryLocation = collectionHome+"\\"+collectionId+"\\conf\\"+file;
		FileManager fm = new FileManager();
		
		StringBuilder customDictionary = new StringBuilder();
		for(String dictionary : listDictionary){
			String[] firstWord = dictionary.split(",");
			customDictionary.append(firstWord[0]);
			customDictionary.append("\r\n");
		}
		
		try {
			fm.fileWriter(customDictionaryLocation, customDictionary.toString(), false);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	private boolean editSchemaXML(String collectionId, String name, String filename) throws Exception{
		String customType = name+"_customtype";
		String customField = name+"_customfield";
		
		String synonym_file = "syn_"+name;
		
		File fXmlFile = new File(collectionHome+"\\"+collectionId+"\\conf\\schema.xml");
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fXmlFile);
		doc.getDocumentElement().normalize();
		Node fields = doc.getElementsByTagName("fields").item(0);
		Node types = doc.getElementsByTagName("types").item(0);
		
		//checking existing fieldtype
		NodeList nList = doc.getElementsByTagName("types");
		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
			NodeList nList2 = nNode.getChildNodes();
			for (int temp2 = 0; temp2 < nList2.getLength(); temp2++) {
				Node nNode2 = nList2.item(temp2);
				if(nNode2.getNodeName().equalsIgnoreCase("fieldType")){
					Element el = (Element) nNode2;
//					System.out.println(el.getAttribute("name"));
					if(el.getAttribute("name").equalsIgnoreCase(customType)){
						System.out.println("custom type exist");
						return false;
					}
				}
			}
		}
		
		//checking existing field
		NodeList nListFields = doc.getElementsByTagName("fields");
		for (int temp = 0; temp < nListFields.getLength(); temp++) {
			Node nNode = nListFields.item(temp);
			NodeList nList2 = nNode.getChildNodes();
			for (int temp2 = 0; temp2 < nList2.getLength(); temp2++) {
				Node nNode2 = nList2.item(temp2);
				if(nNode2.getNodeName().equalsIgnoreCase("field")){
					Element el = (Element) nNode2;
//					System.out.println(el.getAttribute("name"));
					if(el.getAttribute("name").equalsIgnoreCase(customField)){
						System.out.println("custom field exist");
						return false;
					}
				}
			}
		}
		
		//create new element fieldtype
		//create new element fieldtype
		Element fieldType = doc.createElement("fieldType");
		Element filter1 = doc.createElement("filter");
		Element filter2 = doc.createElement("filter");
		Element filter3 = doc.createElement("filter");
		Element tokenizer = doc.createElement("tokenizer");
		Element analyzer = doc.createElement("analyzer");
		
		filter1.setAttribute("class", "solr.LowerCaseFilterFactory");
		filter2.setAttribute("class", "solr.SynonymFilterFactory");
		filter2.setAttribute("synonyms", synonym_file);
		filter2.setAttribute("ignoreCase", "true");
		filter2.setAttribute("expand", "false");
		filter3.setAttribute("class", "solr.KeepWordFilterFactory");
		filter3.setAttribute("words", filename);
		tokenizer.setAttribute("class", "solr.StandardTokenizerFactory");
		
		analyzer.appendChild(tokenizer);
		analyzer.appendChild(filter1);
		analyzer.appendChild(filter2);
		analyzer.appendChild(filter3);
		
		fieldType.setAttribute("class", "solr.TextField");
		fieldType.setAttribute("indexed", "true");
		fieldType.setAttribute("name", customType);
		fieldType.appendChild(analyzer);
		types.appendChild(fieldType);
		
		//Create new element fieldname
		Element fieldname = doc.createElement("field");
		fieldname.setAttribute("name", customField);
		fieldname.setAttribute("type", customType);
		fieldname.setAttribute("indexed", "true");
		fieldname.setAttribute("multiValued", "true");
		fieldname.setAttribute("stored", "false");
		fields.appendChild(fieldname);
		
		//create new element copyfield
		Element copyField = doc.createElement("copyField");
		copyField.setAttribute("source", "body");
		copyField.setAttribute("dest", customField);
		fields.appendChild(copyField);
		
		//write new file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(fXmlFile);
		transformer.transform(source, result);
		
		return true;
	}

	private Viewable addCustomDictionary(String collectionId, String body, String name){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		
		String syn_filename = "";
		String customdict_filename = "";
		
		if(!name.contains("customdict_")){
			customdict_filename = "customdict_"+name;
			syn_filename = "syn_"+name;
		}
		else{
			customdict_filename = name;
			syn_filename = name.replace("customdict_", "");
			syn_filename = "syn_"+name;
		}
		
		String fileLoc = collectionHome+"\\"+collectionId+"\\conf\\";
		
		if(new File(fileLoc+"\\"+customdict_filename).exists()){
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", "failed, existing custom dictionary exists");
			property.put("value", "1");
			
			apiResponse.put("items", property);
			return new Viewable("/general/ack", apiResponse);
		}
		
		JSONObject jsonobj = new JSONObject(body);
		JSONArray jsonArray = jsonobj.getJSONArray("custom");
		ArrayList<String> listDictionary = new ArrayList<String>();
		for(int n = 0; n < jsonArray.length(); n++)
		{
		    JSONObject object = jsonArray.getJSONObject(n);
		    String customdictionary = (String) object.get("dictionary");
		    listDictionary.add(customdictionary);
		}
		
		System.out.println("creating custom dictionary "+name+" : "+listDictionary);
		boolean successcreatefile = createSynonymDictionaryFile(collectionId, listDictionary, syn_filename);
		successcreatefile = createCustomDictionaryFile(collectionId, listDictionary, customdict_filename);
		
		if(!successcreatefile){
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", "error");
			errorProperty.put("detail", "error creating file custom dictionary");
			
			error.add(errorProperty);
			System.out.println("error");
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}
		
		try {
			editSchemaXML(collectionId, name, customdict_filename);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);
			
			e.printStackTrace();
		}
		
		
		if(error.size() > 0){
			System.out.println("error");
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
	
	private boolean deleteSchemaXML(String fileSchema, String name) throws Exception{
		String customType = name+"_customtype";
		String customField = name+"_customfield";
		
		File fXmlFile = new File(fileSchema);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fXmlFile);
		doc.getDocumentElement().normalize();
		boolean typesuccess = false;
		boolean fieldsuccess = false;
		boolean copyfieldsuccess = false;
		
		NodeList nList = doc.getElementsByTagName("types");
		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
			NodeList nList2 = nNode.getChildNodes();
			for (int temp2 = 0; temp2 < nList2.getLength(); temp2++) {
				Node nNode2 = nList2.item(temp2);
				if(nNode2.getNodeName().equalsIgnoreCase("fieldType")){
					Element el = (Element) nNode2;
//					System.out.println(el.getAttribute("name"));
					if(el.getAttribute("name").equalsIgnoreCase(customType)){
						System.out.println("custom type found");
						nNode.removeChild(el);
						typesuccess = true;
					}
				}
			}
		}
		
		NodeList nListFields2 = doc.getElementsByTagName("fields");
		for (int temp = 0; temp < nListFields2.getLength(); temp++) {
//			System.out.println("fields lenght "+nListFields2.getLength());
			Node nNode = nListFields2.item(temp);
			NodeList nList2 = nNode.getChildNodes();
			for (int temp2 = 0; temp2 < nList2.getLength(); temp2++) {
				Node nodeDalem = nList2.item(temp2);
				if(nodeDalem.getNodeName().equalsIgnoreCase("field")){
					Element elementDalem2 = (Element) nodeDalem;
//					System.out.println(elementDalem2.getAttribute("name"));
					if(elementDalem2.getAttribute("name").equalsIgnoreCase(customField)){
						System.out.println("custom field found");
						nNode.removeChild(elementDalem2);
						fieldsuccess = true;
					}
				}
			}
		}
		
		NodeList nListFields = doc.getElementsByTagName("fields");
		for (int temp = 0; temp < nListFields.getLength(); temp++) {
//			System.out.println("fields lenght "+nListFields.getLength());
			Node nNode = nListFields.item(temp);
			NodeList nList2 = nNode.getChildNodes();
			for (int temp2 = 0; temp2 < nList2.getLength(); temp2++) {
				Node nodeDalem = nList2.item(temp2);
				if(nodeDalem.getNodeName().equalsIgnoreCase("copyField")){
					Element elementDalem = (Element) nodeDalem;
//					System.out.println(elementDalem.getAttribute("dest"));
					if(elementDalem.getAttribute("dest").equalsIgnoreCase(customField)){
						System.out.println("copy field found");
						nNode.removeChild(elementDalem);
						copyfieldsuccess = true;

					}
				}
			}
		}
		
		System.out.println("fieldtype : "+typesuccess+", field : "+fieldsuccess+", copyfield : "+copyfieldsuccess);
		if(typesuccess && fieldsuccess && copyfieldsuccess){
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(fXmlFile);
			transformer.transform(source, result);
			return true;
		}
//		System.out.println("custom type not found");
		return false;
	}
	
	
	private Viewable deleteCustomDictionary(String collectionId, String name){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		
		String syn_filename = "";
		String customdict_filename = "";
		
		if(!name.contains("customdict_")){
			customdict_filename = "customdict_"+name;
			syn_filename = "syn_"+name;
		}
		else{
			customdict_filename = name;
			name = name.replace("customdict_", "");
			syn_filename = "syn_"+name;
		}
		
		String fileSchema = collectionHome+"\\"+collectionId+"\\conf\\schema.xml";
		String customDictionaryLocation = collectionHome+"\\"+collectionId+"\\conf\\"+customdict_filename;
		String synonymDictionaryLocation = collectionHome+"\\"+collectionId+"\\conf\\"+syn_filename;
		
		try {
			boolean successDeleteXML = deleteSchemaXML(fileSchema, name);
			if(successDeleteXML){
				new FileManager().deleteFile(customDictionaryLocation, false);
				new FileManager().deleteFile(synonymDictionaryLocation, false);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);
			
			e.printStackTrace();
		}
		
		if(error.size() > 0){
			System.out.println("error");
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
	
	private Viewable editFieldSchema(String collectionId, String body, String name){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		
		String schemaLoc = collectionHome+"\\"+collectionId+"\\conf\\schema.xml";
		
//		JSONParser parser = new JSONParser();
		JSONObject jsonobj = new JSONObject(body);
		try {
			FileManager fman = new FileManager();
//			Object obj = parser.parse(body);
//			JSONObject jsonObject = (JSONObject) obj;
			String nameEdit = (String) jsonobj.get("name");
			String typeEdit = (String) jsonobj.get("type");
			String multivalueEdit = (String) jsonobj.get("multivalue");
			String finalObject = "<field name=\""+nameEdit+"\" type=\""+typeEdit+"\" indexed=\"true\" stored=\"true\"  multiValued=\""+multivalueEdit+"\"/>\r\n	<!-- THIS IS CUSTOM FIELD -->";
						
			String schema = fman.readData(schemaLoc);
			Pattern p = Pattern.compile("<field name=\""+name+"\"(.*?)/>");
			Matcher m = p.matcher(schema);
			if(m.find()){
				schema = schema.replaceAll("<field name=\""+name+"\"(.*?)/>", finalObject);
				fman.fileWriter(schemaLoc, schema, false);
				System.out.println("editing field : "+name+" for collection : "+collectionId+"");
			}
			
		} catch (Exception e) {
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);
			
			e.printStackTrace();
		}
		
		try {
			overwriteTemplate(collectionId);
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
	
	private void overwriteTemplate(String collectionId) throws Exception{
		String schemaTempLoc = collectionHome+"\\"+collectionId+"\\conf\\schema_template.xml";
		String schemaLoc = collectionHome+"\\"+collectionId+"\\conf\\schema.xml";
		
		new FileManager().copyFile(schemaLoc, schemaTempLoc);
	}
	
	private boolean checkNames(Document doc,String name){
		NodeList nameList = doc.getElementsByTagName("str");
		int y = 0;
		while(y<nameList.getLength()){
			NamedNodeMap attrib = nameList.item(y).getAttributes();
			Node nodeAttrib = attrib.getNamedItem("name");
			if(nodeAttrib!=null){
				if(nodeAttrib.getTextContent().equalsIgnoreCase("name")){
					if(nameList.item(y).getTextContent().equalsIgnoreCase(name)){
						return false;
					}
				}
				
			}
			y++;
		}
		return true;
	}
	
	private boolean checkFieldName(String name, String schema){
		
		Pattern p = Pattern.compile("<field(.*?)name=\""+name+"\"(.*?)/>");
		Matcher m = p.matcher(schema);
		if(m.find()){
			return false;
		}
		else{
			return true;
		} 
	}
	
}