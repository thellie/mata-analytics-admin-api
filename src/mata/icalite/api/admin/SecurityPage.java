package mata.icalite.api.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.json.JSONObject;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.OAuth2Token;
import twitter4j.conf.ConfigurationBuilder;
import mata.icalite.api.util.Security;
import mata.icalite.api.util.SystemControl;

import com.sun.jersey.api.view.Viewable;

@Path("/securitypage")
public class SecurityPage {
	private SystemControl sc = null;
	
	public SecurityPage(){
		sc = new SystemControl();
	}
	
	@GET
	@Produces("application/xml")
	public Viewable getParam(@QueryParam("method") String method,
				@QueryParam("sessionId") String session,
				@QueryParam("apiKey") String apiKey,
				@QueryParam("alias") String alias,
				@QueryParam("secretKey") String secretKey
			) {
		
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		
		Security secure = new Security();
		String username = null;
		
		try {
			if(!new Security().derbyCheck(session)){
				Map<String,Object> property = new HashMap<String,Object>();
				
				property.put("message", "session expired");
				property.put("value", "1");
				
				apiResponse.put("items", property);
				return new Viewable("/general/ack", apiResponse);
			}else {
				try{
					username = secure.getUser(session);
				}
				catch(Exception e){
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
		
		if(method.equals("getListToken")){
			return getListToken(username);
		}else if(method.equals("getTwitterToken")){
			return getTwitterToken(apiKey,secretKey);
		}else if(method.equals("deleteToken")){
			return deleteToken(username,alias);
		}else if(method.equals("getUserPackages")){
			return getUserPackages(username);
		}else{
			Map<String,Object> errorProperty = new HashMap<String,Object>();
			
			errorProperty.put("code", "405");
			errorProperty.put("message", "Method Not Allowed");
			errorProperty.put("detail", 
					"The REST service does not support the operation implied by the HTTP "
					+ "method for the resource that is addressed by the URI that is "
					+ "passed in");
			
			error.add(errorProperty);
			
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}
	}
	
	@POST
	@Produces("application/xml")
	public Viewable postParam(@QueryParam("method") String method, 
			String body,
			@QueryParam("sessionId") String session
		) {
		
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		Security secure = new Security();
		JSONObject jsonobj = null;
		String username = null;
		
		try {
			if(!new Security().derbyCheck(session)){
				Map<String,Object> property = new HashMap<String,Object>();
				
				property.put("message", "session expired");
				property.put("value", "1");
				
				apiResponse.put("items", property);
				return new Viewable("/general/ack", apiResponse);
			}else {
				try {
					jsonobj = new JSONObject(body);
				} catch (Exception e) {
					Map<String,Object> errorProperty = new HashMap<String,Object>();
					errorProperty.put("code", "500");
					errorProperty.put("message", e.toString());
					errorProperty.put("detail", sc.getStackTrace(e));
					
					error.add(errorProperty);
					
					e.printStackTrace();
				}
				
				try{
					username = secure.getUser(session);
				}catch(Exception e){
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
		
		if(method.equals("postToken")){
			String token = jsonobj.getString("token");
			String type = jsonobj.getString("type");
			String keyid = jsonobj.getString("keyid");
			String secret = jsonobj.getString("secret");
			
			String alias = null;
			try{
				alias = jsonobj.getString("alias");
			}catch(Exception e){}
			
			return postToken(username, token, type, alias, keyid, secret);
		}else if(method.equals("editToken")){
			String token = jsonobj.getString("token");
			String type = jsonobj.getString("type");
			String keyid = jsonobj.getString("keyid");
			String secret = jsonobj.getString("secret");
			
			String alias = null;
			try{
				alias = jsonobj.getString("alias");
			}catch(Exception e){}
			
			return editToken(username, token, type, alias, keyid, secret);
		}else if(method.equals("setUserPackages")){
			String userToChange = jsonobj.getString("usertochange");
			String packages = jsonobj.getString("packages");
			String maxCrawler = jsonobj.getString("maxcrawler");
			String maxCollection = jsonobj.getString("maxcollection");
			
			return setUserPackages(username, userToChange, packages, maxCrawler, maxCollection);
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
	
	private Viewable getListToken(String username){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		List<Object> tokenList = new ArrayList<Object>();
		
		try {
			ArrayList<ArrayList<String>> tokens = new Security().getListSocMedToken(username);
			
			for(ArrayList<String> tokenParts : tokens){
				Map<String,Object> tokenProperty = new HashMap<String,Object>();
				tokenProperty.put("type", tokenParts.get(0));
				tokenProperty.put("token", tokenParts.get(1));
				tokenProperty.put("alias", tokenParts.get(2));
				tokenProperty.put("keyid", tokenParts.get(3));
				tokenProperty.put("secret", tokenParts.get(4));
				
				tokenList.add(tokenProperty);
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
			apiResponse.put("items", tokenList);
			return new Viewable("/securitypage/getList", apiResponse);
		}
	}
	
	private Viewable postToken(String username, String token, 
			String type, String alias, String keyid, String secret){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		
		Security secure = new Security();
		
		try {
			boolean isAvailable = secure.setSocMedToken(username, token, type, alias, keyid, secret);
			if(!isAvailable){
				Map<String,Object> property = new HashMap<String,Object>();
				
				property.put("message", "failed, alias already exists");
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
			return new Viewable("/general/ack", apiResponse);
		}
	}
	
	private Viewable editToken(String username, String token, 
			String type, String alias, String keyid, String secret){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		
		Security secure = new Security();
		
		try {
			secure.editSocMedToken(username, token, type, alias, keyid, secret);
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
	
	public Viewable deleteToken(String username, String alias){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		
		Security secure = new Security();
		
		try {
			secure.deleteSocMedToken(username, alias);
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
	
	private Viewable getTwitterToken(String apikey, String apisecret){
		
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		
		ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setApplicationOnlyAuthEnabled(true);
        
        Twitter twitter = new TwitterFactory(builder.build()).getInstance();
        twitter.setOAuthConsumer(apikey, apisecret);
        
        String twitterToken = "";
        try {
			OAuth2Token token = twitter.getOAuth2Token();
			
//			System.out.println(token.getAccessToken());
			twitterToken = token.getAccessToken();
			
		} catch (TwitterException e) {
			e.printStackTrace();
		}
        
		if(error.size() > 0){
			apiResponse.put("items", error);
			return new Viewable("/exception/error", apiResponse);
		}else{
			Map<String,Object> property = new HashMap<String,Object>();
			
			property.put("message", twitterToken);
			property.put("value", "0");
			
			apiResponse.put("items", property);
			return new Viewable("/general/ack", apiResponse);
		}
	}
	
	private Viewable getUserPackages(String username){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		
		Security secure = new Security();
		ArrayList<String> userPackages = new ArrayList<String>();
		try {
			userPackages = secure.getUserPackages(username);
			if(userPackages==null){
				Map<String,Object> property = new HashMap<String,Object>();
				
				property.put("message", "failed, you dont have privilage to use this API");
				property.put("value", "1");
				
				apiResponse.put("items", property);
				return new Viewable("/general/ack", apiResponse);
			}
			else{
				
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
			apiResponse.put("items", userPackages);
			return new Viewable("/securitypage/getListUserPackage", apiResponse);
		}
	}
	
	private Viewable setUserPackages(String username, String userToChange, String packages, String maxCrawler, String maxCollection){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		
		Security secure = new Security();
		try {
			if(!secure.setUserPackages(username, userToChange, packages, maxCrawler, maxCollection)){
				Map<String,Object> property = new HashMap<String,Object>();
				
				property.put("message", "failed, you dont have privilage to use this API");
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
			return new Viewable("/general/ack", apiResponse);
		}
	}
}
