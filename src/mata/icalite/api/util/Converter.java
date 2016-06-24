package mata.icalite.api.util;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class Converter {
	
	DocumentBuilder dBuilder = null;
	
	public Converter() throws Exception{
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dBuilder = dbFactory.newDocumentBuilder();
	}
	
	public String convertForMax(String data) throws Exception{
		InputSource is = new InputSource(new StringReader(data));
		Document doc = dBuilder.parse(is);
		doc.getDocumentElement().normalize();
		
		Document docFinal = dBuilder.newDocument();
		Element addElement = docFinal.createElement("add");
		docFinal.appendChild(addElement);
		Element docElement = docFinal.createElement("doc");
		addElement.appendChild(docElement);
		
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		
		NodeList listFields = doc.getDocumentElement().getChildNodes();
		int count = 0;
		while(count<listFields.getLength()){
			Node nNode = listFields.item(count);
			if (nNode.getNodeType() == Node.ELEMENT_NODE){
				String field = listFields.item(count).getNodeName();
				String content = listFields.item(count).getTextContent(); 
				
				if(content!=null && !content.isEmpty()){
					Element childElement = docFinal.createElement("field");
					field = normalizedField(field);
					childElement.setAttribute("name", field);
					childElement.appendChild(docFinal.createTextNode(content));
					docElement.appendChild(childElement);
				}
			}			
			count++;
		}
		
//		data = getStringFromDoc(docFinal);
		DOMSource source = new DOMSource(docFinal);
		StreamResult result = new StreamResult(new StringWriter());
		transformer.transform(source, result);
		data = result.getWriter().toString();
		
		return data;
	}
	
//	private String getStringFromDoc(Document doc)    {
//	    DOMImplementationLS domImplementation = (DOMImplementationLS) doc.getImplementation();
//	    LSSerializer lsSerializer = domImplementation.createLSSerializer();
//	    return lsSerializer.writeToString(doc);   
//	}
	
	private String normalizedField(String field){
		if(field.equalsIgnoreCase("postingdate")){
			field = "date";
		}
		return field;
	}
}
