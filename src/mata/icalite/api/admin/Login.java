package mata.icalite.api.admin;

import mata.icalite.api.util.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import com.sun.jersey.api.view.Viewable;

@Path("/login")
public class Login {
	private SystemControl sc = null;
	private Json json = null;

	public Login(){
		sc = new SystemControl();
		json = new Json();
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
		
		String groups = null;
		try {
			groups = new Security().getGroupDerby(username);
			collectionId = groups+"-"+collectionId;
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
		
		if(method.equals("checkSession")){
			return checkSession(session);
		}else if(method.equals("logout")){
			return logout(session);
		}else if(method.equals("getGroup")){
			return getGroup(username);
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
			@QueryParam("crawlerId") String crawlerId) {

		Map<String,Object> apiResponse = new HashMap<String,Object>();
		
		if(method.equalsIgnoreCase("loginUser")){
			return loginUser(body);
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
		
	private Viewable loginUser(String body){
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
			
//			e.printStackTrace();
		}
		
		String password = jsonElements.get("password");
		String username = jsonElements.get("username");
		
		Security secureSession = new Security();
		
		try {
			password = secureSession.decryptData(password);
		} catch (Exception e) {
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);
			
//			e.printStackTrace();
		}
		
		try {
			username = secureSession.decryptData(username);
		} catch (Exception e) {
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			errorProperty.put("code", "500");
			errorProperty.put("message", e.toString());
			errorProperty.put("detail", sc.getStackTrace(e));
			
			error.add(errorProperty);
			
//			e.printStackTrace();
		}
		
		String token = null;
		try {
			if (secureSession.ldapConnect(username, password)){
				System.out.println("Username : "+username+" login!");
				secureSession.addGroup(username, password);
				token = secureSession.derbyInsert(username);
				secureSession.checkUserPackage(username);
				secureSession.derbyUpdateCollection(username);
//				secureSession.insertLimitCollection(username);
//				secureSession.derbyInsertCollection(username);
			}
			else{
				Map<String,Object> errorProperty = new HashMap<String,Object>();
				errorProperty.put("code", "520");
				errorProperty.put("message", "Failed to Login");
				errorProperty.put("detail", "Error username and password");
				error.add(errorProperty);
			}
		} catch (Exception e1) {
				Map<String,Object> errorProperty = new HashMap<String,Object>();
				errorProperty.put("code", "500");
				errorProperty.put("message", e1.toString());
				errorProperty.put("detail", sc.getStackTrace(e1));
				
				error.add(errorProperty);
				
//				e1.printStackTrace();
		}
				
		if(error.size() > 0){
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", "successful");
			property.put("token", token);
			property.put("value", "0");
			
			apiResponse.put("items", property);
			return new Viewable("/login/ack", apiResponse);
		}
	}
	
	private Viewable checkSession(String session){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		
		Security secureSession = new Security();
		
		try {
			if(!secureSession.derbyCheck(session)){
				Map<String,Object> property = new HashMap<String,Object>();
				
				property.put("message", "session expired");
				property.put("value", "1");
				
				apiResponse.put("items", property);
				return new Viewable("/general/ack", apiResponse);
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
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", "successful");
			property.put("value", "0");
			
			apiResponse.put("items", property);
			return new Viewable("/login/ack", apiResponse);
		}
	}
	
	private Viewable getGroup(String username){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		
		Security secureSession = new Security();
		String group = "";
		try {
			group = secureSession.getGroupDerby(username);
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
			
			property.put("message", group);
			property.put("value", "0");
			
			apiResponse.put("items", property);
			return new Viewable("/login/ack", apiResponse);
		}
	}
	
	private Viewable logout(String session){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		
		Security secureSession = new Security();
		
		try{
			String username = secureSession.getUser(session);
			System.out.println("Username : "+username+" logout!");
			secureSession.derbyDelete(session);
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
			
			property.put("message", "successful");
			property.put("value", "0");
			
			apiResponse.put("items", property);
			return new Viewable("/login/ack", apiResponse);
		}
	}
}
