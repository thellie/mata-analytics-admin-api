package mata.icalite.api.util;

import java.math.BigInteger;
import java.security.Key;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

public class Security {
	
	private Connection connect = null;
	private Statement statement = null;
	private ResultSet resultSet = null;
//	private String protocol = "jdbc:derby:";
	private String protocol = "jdbc:derby://127.0.0.1:1527/";
//	private String driver = "org.apache.derby.jdbc.EmbeddedDriver";
	private String driver = "org.apache.derby.jdbc.ClientDriver";
	  
	DirContext ldapContext;
	 	  
	public boolean ldapConnect(String username, String password) throws Exception{
		Hashtable<String, String> ldapEnv = new Hashtable<String, String>(11);
		ldapEnv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		     
		ldapEnv.put(Context.PROVIDER_URL,  "ldap://localhost:389");
		ldapEnv.put(Context.SECURITY_AUTHENTICATION, "simple");
		      
	//		      ldapEnv.put(Context.SECURITY_PRINCIPAL, "cn="+username+",o=bin");
		ldapEnv.put(Context.SECURITY_PRINCIPAL, ""+username+"@cax.mata.org");
		ldapEnv.put(Context.SECURITY_CREDENTIALS, password);
	
		ldapContext = new InitialDirContext(ldapEnv);
		ldapContext.close();
		      
		return true;
	}
	
	public String addGroup(String username, String password) throws Exception{
		Hashtable<String, String> ldapEnv = new Hashtable<String, String>(11);
		ldapEnv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		     
		ldapEnv.put(Context.PROVIDER_URL,  "ldap://localhost:389");
		ldapEnv.put(Context.SECURITY_AUTHENTICATION, "simple");
		      
	//		      ldapEnv.put(Context.SECURITY_PRINCIPAL, "cn="+username+",o=bin");
		ldapEnv.put(Context.SECURITY_PRINCIPAL, ""+username+"@cax.mata.org");
		ldapEnv.put(Context.SECURITY_CREDENTIALS, password);
	
		String groups = "";
		 try {
		        DirContext ctx = new InitialDirContext(ldapEnv);
		        SearchControls ctls = new SearchControls();
		        String[] attrIDs = {"cn"};
		        ctls.setReturningAttributes(attrIDs);
		        ctls.setSearchScope(SearchControls.ONELEVEL_SCOPE);

		        NamingEnumeration<?> answer = ctx.search( "DC=cax,DC=mata,DC=org", "(objectclass=group)",ctls );
		        while(answer.hasMore()) {
		            SearchResult rslt = (SearchResult)answer.next();
		            Attributes attrs = rslt.getAttributes();
		            if(attrs.get("cn").toString().toLowerCase().contains("colgroup")){
		            	groups = attrs.get("cn").toString().replaceAll("^(.*?) ", "").trim();
		            	String[] groupParse = groups.split("-");
//		            	System.out.println(groupParse[0]+":"+groupParse[1]);
		            	if(username.toUpperCase().contains(groupParse[1])){
		            		break;
		            	}
		            }		            
		        }

		        ctx.close();


		    } catch (NamingException e) {
		    	groups = "COLGROUP-DEV";
//		        e.printStackTrace();
		    }
		 
		addGroupDerby(username, groups);
		return groups;
	}
	
	
	private void addGroupDerby(String username, String group) throws Exception{
	  	Class.forName(driver).newInstance();
	  	connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
	    Statement statement = connect.createStatement();
	    ResultSet res = statement.executeQuery("SELECT * FROM usergroup where username='"+username+"'"); 
	    
	    if(res.next()) {
	    	
	    }
	    else {
	    	statement.executeUpdate("insert into usergroup values ('"+username+"', '"+group+"')");
	    }
	    closeDB();		
	}
	
	public String getGroupDerby(String username) throws Exception{
	  	Class.forName(driver).newInstance();
	  	connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
	    Statement statement = connect.createStatement();
	    ResultSet res = statement.executeQuery("SELECT * FROM usergroup where username='"+username+"'"); 
	    String group = "";
	    
	    while(res.next()) {
	    	group = res.getString("groups");
	    	break;
	    }
	    closeDB();
	    return group;
	}
	  
	public void addFirstUserPackage(String username) throws Exception{
	
	  	Class.forName(driver).newInstance();
	  	connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
	    Statement statement = connect.createStatement();
	    if(username.equalsIgnoreCase("administrator")){
	    	statement.executeUpdate("insert into userpackage values ('"+username+"','ULTIMATE')");
	    }
	    else{
	    	statement.executeUpdate("insert into userpackage values ('"+username+"','silver')"); 
	    }
	    closeDB();
	    
	}
	
	public String checkUserPackage(String username) throws Exception{
		
	  	String packageuser = "";
	  	Class.forName(driver).newInstance();
	  	connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
	    Statement statement = connect.createStatement();
	    ResultSet res = statement.executeQuery("SELECT package FROM userpackage where username='"+username+"'"); 
	    
	    if (res.next()) {
	    	packageuser = res.getString("package");
	    }
	    else{
	    	System.out.println("creating first time login user package for :"+username+"");
	    	addFirstUserPackage(username);
	    	packageuser = "silver";
	    }
	    closeDB();
		
		return packageuser;
	}
	
	  private boolean checkUserUnlimitedSession(String sessionID) throws Exception{
		  
		  String username = getUser(sessionID);
		  Class.forName(driver).newInstance();
		  connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
		  Statement statement = connect.createStatement();
		  ResultSet res = statement.executeQuery("SELECT * FROM nosessionuser where username='"+username+"'");
		  
		  if (res.next()) {
			closeDB();
//			System.out.println("unlimited session user found!");
		    return true;
		  }
		  else{
			  closeDB();
			  return false;
		  }
	  }
	  
	  public  boolean derbyCheck(String sessionID) throws Exception{
		  if(!checkUserUnlimitedSession(sessionID)){
			  String sessionid = "";
			  	Class.forName(driver).newInstance();
			  	connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
			    Statement statement = connect.createStatement();
			    ResultSet res = statement.executeQuery("SELECT * FROM sessiontable where sessionid='"+sessionID+"'"); 
			    
			    while (res.next()) {
			    	sessionid = res.getString("sessionid");
			    	if(timeDifferences(res.getString("timestamps"))){
			    		derbyUpdate(sessionid);
			    		closeDB();
			    		return true;
			    	}
			    }
			    closeDB();
//			    derbyDelete(sessionid);
			    return false;
		  }
		  else {
//			  System.out.println("unlimited session user found!");
			  return true;
		  }
	  }
	  
	  public boolean createLimitCrawler(String collectionId, String username) throws Exception{
		  
		  String userpackage = checkUserPackage(username);
		  int limit = 3;
		  
		  if(userpackage.equalsIgnoreCase("custom")){
			  return true;
		  }
		  if(userpackage.equalsIgnoreCase("gold")){
			  limit = 5;
		  }
		  else if(userpackage.equalsIgnoreCase("freeuser")){
			  limit = 1;
		  }
		  else if(userpackage.equalsIgnoreCase("diamond")){
			  limit = 10;
		  }
		  
		  if(username.equalsIgnoreCase("administrator")){
			  limit = 100;
		  }
		  
		  Class.forName(driver).newInstance();
		  connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
		  Statement statement = connect.createStatement();
		  statement.executeUpdate("insert into crawllimit values ('"+username+"', '"+collectionId+"', 0, "+limit+")");
		  closeDB();
		  return true;
	  }
	  
	  public boolean updateLimitCrawler(String collectionId, String username) throws Exception{
		  
		  String userpackage = checkUserPackage(username);
		  int limit = 3;
		  
		  if(userpackage.equalsIgnoreCase("custom")){
			  return true;
		  }
		  if(userpackage.equalsIgnoreCase("gold")){
			  limit = 5;
		  }
		  else if(userpackage.equalsIgnoreCase("freeuser")){
			  limit = 1;
		  }
		  else if(userpackage.equalsIgnoreCase("diamond")){
			  limit = 10;
		  }
		  if(username.equalsIgnoreCase("administrator")){
			  limit = 100;
		  }
		  
		  Class.forName(driver).newInstance();
		  connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
		  Statement statement = connect.createStatement();
		  statement.executeUpdate("update crawllimit set maxcrawl="+limit+" where username='"+username+"'");
		  closeDB();
		  return true;
	  }
	  
	  public boolean deleteLimitCrawler(String collectionId) throws Exception{
		  
		  Class.forName(driver).newInstance();
		  connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
		  Statement statement = connect.createStatement();
		  statement.executeUpdate("delete from crawllimit where collection='"+collectionId+"'");
		  closeDB();
		  return true;
		  
	  }
	  
	  public boolean derbyCheckCollection(String username) throws Exception{
//		  	ArrayList<Integer> limitCol = new ArrayList<Integer>();
		  	Class.forName(driver).newInstance();
		  	connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
		    Statement statement = connect.createStatement();
		    ResultSet res = statement.executeQuery("SELECT * FROM collimit where username='"+username+"'"); 
		    int curcol = 0;
		    int maxcol = 3;
		    
		    if (res.next()) {
		    	curcol = res.getInt("curcol");
		    	maxcol = res.getInt("maxcol");
		    	if(curcol==maxcol){
		    		closeDB();
		    		return false;
		    	}
		    }
		    closeDB();
		    return true;
	  }
	  
	  public boolean insertLimitCollection(String username) throws Exception {
		  
		 	Class.forName(driver).newInstance();
		  	connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
		    Statement statement = connect.createStatement();
		    ResultSet res = statement.executeQuery("SELECT * FROM collimit where username='"+username+"'"); 
		    int curcol = 0;
		    
		    if (res.next()) {
		    	curcol = res.getInt("curcol");
		    	curcol = curcol + 1;
		    }
		    
		    else{
		    	System.out.println("first time login: creating limit collection");
		    	derbyInsertCollection(username);
		    	curcol = 1;
		    }
//		    
		    closeDB();
		    try{
		    	updateCollimit(curcol, username);
		    }
		    catch(Exception e){

		    }
		    
		  return true;
	  } 
	  
	  public String getUserFromCollection(String collectionId) throws Exception{
		  String user = "";
		  
		 	Class.forName(driver).newInstance();
		  	connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
		    Statement statement = connect.createStatement();
		    ResultSet res = statement.executeQuery("SELECT username FROM crawllimit where collection='"+collectionId+"'"); 
		    
		    if (res.next()) {
		    	user = res.getString("username");
		    }
		  
		    closeDB();
		  return user;
	  }
	  
	  public boolean derbyInsertCollection(String username) throws Exception{
		  
		  String userpackage = checkUserPackage(username);
		  int limit = 3;
		  
		  if(userpackage.equalsIgnoreCase("custom")){
			  return true;
		  }
		  if(userpackage.equalsIgnoreCase("gold")){
			  limit = 5;
		  }
		  else if(userpackage.equalsIgnoreCase("diamond")){
			  limit = 5;
		  }else if(userpackage.equalsIgnoreCase("freeuser")){
			  limit = 1;
		  }
		  if(username.equalsIgnoreCase("administrator")){
			  limit = 100;
		  }
		  
		  Class.forName(driver).newInstance();
		  connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
		  Statement statement = connect.createStatement();
		  statement.executeUpdate("insert into collimit values ('"+username+"', 0, "+limit+")");
		  closeDB();
		  return true;
	  }
	  
	  public boolean derbyUpdateCollection(String username) throws Exception{
		  
		  String userpackage = checkUserPackage(username);
		  int limit = 3;
		  
		  if(userpackage.equalsIgnoreCase("custom")){
			  return true;
		  }
		  if(userpackage.equalsIgnoreCase("gold")){
			  limit = 5;
		  }
		  else if(userpackage.equalsIgnoreCase("diamond")){
			  limit = 5;
		  }
		  else if(userpackage.equalsIgnoreCase("freeuser")){
			  limit = 1;
		  }
		  
		  if(username.equalsIgnoreCase("administrator")){
			  limit = 100;
		  }
		  
		  Class.forName(driver).newInstance();
		  connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
		  Statement statement = connect.createStatement();
		  ResultSet res = statement.executeQuery("SELECT * FROM collimit where username='"+username+"'");
		  
		  if(res.next()){
			  statement.executeUpdate("update collimit set maxcol="+limit+" where username='"+username+"'");
		  }
		  else{
			  closeDB();
			  return false;
		  }
		  
		  closeDB();
		  return true;
	  }
	  
	  public boolean insertLimitCrawler(String username, String collectionId) throws Exception {
		  
		 	Class.forName(driver).newInstance();
		  	connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
		    Statement statement = connect.createStatement();
		    ResultSet res = statement.executeQuery("SELECT * FROM crawllimit where collection='"+collectionId+"'"); 
		    int curcrawl = 0;
		    
		    if (res.next()) {
		    	curcrawl = res.getInt("curcrawl");
		    	curcrawl = curcrawl + 1;
		    }
//		    System.out.println(curcrawl);
		    closeDB();
		    try{
		    	updateCrawllimit(curcrawl, username, collectionId);
		    }
		    catch(Exception e){

		    }
		  return true;
	  } 
	  
	  public boolean updateCollimit(int limit, String username) throws Exception{
		 	Class.forName(driver).newInstance();
		  	connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
		    Statement statement = connect.createStatement();
		    statement.executeUpdate("update collimit set curcol="+limit+" where username='"+username+"'");
		    closeDB();
		    return true;
		    
	  }	  
	  
	  public boolean updateCrawllimit(int limit, String username, String collectionId) throws Exception{
		 	Class.forName(driver).newInstance();
		  	connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
		    Statement statement = connect.createStatement();
		    statement.executeUpdate("update crawllimit set curcrawl="+limit+" where collection='"+collectionId+"'");
		    closeDB();
		    return true;
	  }
	  
	  public boolean derbyCheckCrawler(String collectionId, String username) throws Exception{
		  
		  	Class.forName(driver).newInstance();
		  	connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
		    Statement statement = connect.createStatement();
		    ResultSet res = statement.executeQuery("SELECT * FROM crawllimit where username='"+username+"' AND collection='"+collectionId+"'"); 
		    int curcol = 0;
		    int maxcol = 3;
		    
		    if (res.next()) {
		    	curcol = res.getInt("curcrawl");
		    	maxcol = res.getInt("maxcrawl");
		    	if(curcol==maxcol){
		    		closeDB();
		    		return false;
		    	}
		    }
		    closeDB();
		    return true;
	  }
	  
	  public boolean removeLimitCollection(String collectionId) throws Exception {
		  
		  String username = getUserFromCollection(collectionId);
//		  System.out.println("username : "+username);
		 	Class.forName(driver).newInstance();
		  	connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
		    Statement statement = connect.createStatement();
		    ResultSet res = statement.executeQuery("SELECT * FROM collimit where username='"+username+"'"); 
		    int curcol = 0;
		    
		    if (res.next()) {
		    	curcol = res.getInt("curcol");
		    	if(curcol!=0){
		    		curcol = curcol - 1;
		    	}
		    }
		    closeDB();
		    try{
		    	updateCollimit(curcol, username);
		    }
		    catch(Exception e){

		    }
		    
		  return true;
	  }
	  
	  public boolean removeLimitCrawler(String username, String collectionId) throws Exception {
		  
		  username = getUserFromCollection(collectionId);
		  
		 	Class.forName(driver).newInstance();
		  	connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
		    Statement statement = connect.createStatement();
		    ResultSet res = statement.executeQuery("SELECT * FROM crawllimit where username='"+username+"' AND collection='"+collectionId+"'"); 
		    int curcrawl = 0;
		    
		    if (res.next()) {
		    	curcrawl = res.getInt("curcrawl");
		    	if(curcrawl!=0){
		    		curcrawl = curcrawl - 1;
		    	}
		    }
		    System.out.println(curcrawl);
		    closeDB();
		    try{
		    	updateCrawllimit(curcrawl, username, collectionId);
		    }
		    catch(Exception e){

		    }
		    
		  return true;
	  } 
	  
	  public String derbyInsert(String username) throws Exception{
		  	String sessionid = "";
		  	username = username.toLowerCase();
		  	Class.forName(driver).newInstance();
		  	connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
//		    PreparedStatement statement = connect.prepareStatement("CREATE TABLE sessiontable (id int, text varchar(50)");
		    Statement statement = connect.createStatement();
		    sessionid = nextSessionId();
		    ResultSet res = statement.executeQuery("SELECT * FROM sessiontable where username='"+username+"'");
		    
		    if(res.next()) {
//		    	System.out.println("sessionid found, using old session");
		    	sessionid = res.getString("sessionid");
		    	closeDB();
		    	derbyUpdate(sessionid);
		    }
		    else{
		    	statement.executeUpdate("insert into sessiontable values ('"+sessionid+"', current_timestamp, '"+username+"')");
		    	closeDB();
		    }
		    
//		    PreparedStatement statement = connect.prepareStatement("select * from sessiontable");
//		   System.out.println(sessionid);
		    return sessionid;
	  }
	  
	  
	  
	  public  void derbyDelete(String sessionID) throws Exception{
		  if(!checkUserUnlimitedSession(sessionID)){
			  System.out.println("user loging out, deleting record");
			  	Class.forName(driver).newInstance();
			  	connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
//			    PreparedStatement statement = connect.prepareStatement("CREATE TABLE sessiontable (id int, text varchar(50)");
			    Statement statement = connect.createStatement();
			    statement.executeUpdate("delete from sessiontable where sessionid='"+sessionID+"'");
//			    PreparedStatement statement = connect.prepareStatement("select * from sessiontable");
				closeDB();
		  }
		  else{
			  System.out.println("unlimited session user logout, not deleting record");
		  }
	  }
	  
	  public  void derbyUpdate(String sessionID) throws Exception{
		  	Class.forName(driver).newInstance();
		  	connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
	//		    PreparedStatement statement = connect.prepareStatement("CREATE TABLE sessiontable (id int, text varchar(50)");
		    Statement statement = connect.createStatement();
		    statement.executeUpdate("update sessiontable set timestamps = current_timestamp where sessionid='"+sessionID+"'");
	//		    PreparedStatement statement = connect.prepareStatement("select * from sessiontable");
		    closeDB();
	  }  
	  
	public  String currentDate(){
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date();
			return dateFormat.format(date);
	}
	  
	  public  boolean timeDifferences(String derbyTime) throws Exception{
		  	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		  
			Date d1 = null;
			Date d2 = null;
	 
			d1 = format.parse(derbyTime);
			d2 = format.parse(currentDate());
			
			//in milliseconds
				long diff = d2.getTime() - d1.getTime();
	 
				long diffSeconds = diff / 1000;
	//			long diffMinutes = diff / (60 * 1000) % 60;
	//			long diffHours = diff / (60 * 60 * 1000) % 24;
	//			long diffDays = diff / (24 * 60 * 60 * 1000);
			if(diffSeconds>(60*30*2)){
				return false;
			}
			
			return true;
	  }
	  
	  public  String nextSessionId() {
		  SecureRandom random = new SecureRandom();
		  return new BigInteger(130, random).toString(35);
	  }
	  
	  public  void closeDB() throws Exception{
		  	if (resultSet != null) {
	          resultSet.close();
	        }
		  	
	        if (statement != null) {
	          statement.close();
	        }
	        
	        if (connect != null) {
	          connect.close();
	        }
	  }
	  
	  public String decryptData(String password) throws Exception{
		  String text = password;
		  String key = "-CAXliteCAXlite-"; // 128 bit key

		  // Create key and cipher
		  Key aesKey = new SecretKeySpec(key.getBytes(), "AES");
		  Cipher cipher = Cipher.getInstance("AES");

		  text = remLastChar(text);
		  String[] decrypt = text.split("A");
		  byte[] decCipher = new byte[16];
		  
		  int x = 0;
		  for (String dec : decrypt){
			decCipher[x] = (byte) Integer.parseInt(dec);
			x++;
		  }
		  // decrypt the text
		  cipher.init(Cipher.DECRYPT_MODE, aesKey);
		  String decrypted = new String(cipher.doFinal(decCipher));
//		  System.err.println(decrypted);
		  
		  return decrypted;
	  }
	  
	  public String encryptData(String password) throws Exception{
		  String text = password;
		  String key = "-CAXliteCAXlite-"; // 128 bit key

		  // Create key and cipher
		  Key aesKey = new SecretKeySpec(key.getBytes(), "AES");
		  Cipher cipher = Cipher.getInstance("AES");

		  // encrypt the text
		  cipher.init(Cipher.ENCRYPT_MODE, aesKey);
		  byte[] encrypted = cipher.doFinal(text.getBytes());
		  System.err.println(new String(encrypted));
		  
		  String outputEncrypted = "";
		  for (int i=0; i<encrypted.length; i++){
			  outputEncrypted = outputEncrypted + Integer.toString(encrypted[i]) + "A";
		  }

		  return outputEncrypted;
	  }
	  
	  public String remLastChar(String str) {
		    if (str.length() > 0 && str.charAt(str.length()-1)=='A') {
		      str = str.substring(0, str.length()-1);
		    }
		    return str;
	  }
	  
	  public ArrayList<String> checkPrivilage(String sessionID) throws Exception{
		  	ArrayList<String> collectionLists = new ArrayList<String>();
		  	String userName = "";
		  	Class.forName(driver).newInstance();
		  	connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
//		    PreparedStatement statement = connect.prepareStatement("CREATE TABLE sessiontable (id int, text varchar(50)");
		    Statement statement = connect.createStatement();
		    ResultSet res = statement.executeQuery("SELECT * FROM sessiontable where sessionid='"+sessionID+"'"); 
		    
		    while (res.next()) {
//		    	System.out.println(res.getString("sessionid"));
//		    	System.out.println(res.getString("timestamps"));
//		    	System.out.println(res.getString("username"));
		    	userName = res.getString("username");
//		    	System.out.println("Welcome Back "+userName+"");
		    }
		  
		    Statement statement2 = connect.createStatement();
		    ResultSet res2 = statement2.executeQuery("SELECT * FROM PRIVILAGEUSER WHERE USERNAME='"+userName+"'");
		    
		    while(res2.next()){
		    	collectionLists.add(res2.getString("COLLECTION"));
		    }
		    closeDB();
		    return collectionLists;
	  }
	  
	  public String getUser(String sessionID) throws Exception{
		  	String userName = "";
		  	Class.forName(driver).newInstance();
		  	connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
//		    PreparedStatement statement = connect.prepareStatement("CREATE TABLE sessiontable (id int, text varchar(50)");
		    Statement statement = connect.createStatement();
		    ResultSet res = statement.executeQuery("SELECT * FROM sessiontable where sessionid='"+sessionID+"'"); 
		    
		    while (res.next()) {
//		    	System.out.println(res.getString("sessionid"));
//		    	System.out.println(res.getString("timestamps"));
//		    	System.out.println(res.getString("username"));
		    	userName = res.getString("username");
//		    	System.out.println("Welcome Back "+userName+"");
		    }
		    closeDB();
		    return userName;
	  }
	  
	  public String getSession(String username) throws Exception{
		  	String session = "";
		  	Class.forName(driver).newInstance();
		  	connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
//		    PreparedStatement statement = connect.prepareStatement("CREATE TABLE sessiontable (id int, text varchar(50)");
		    Statement statement = connect.createStatement();
		    ResultSet res = statement.executeQuery("SELECT * FROM sessiontable where username='"+username+"'"); 
		    
		    while (res.next()) {
//		    	System.out.println(res.getString("sessionid"));
//		    	System.out.println(res.getString("timestamps"));
//		    	System.out.println(res.getString("username"));
		    	session = res.getString("sessionid");
//		    	System.out.println("Welcome Back "+userName+"");
		    }
		    closeDB();
		    return session;
	  }
	  
	  public boolean addPrivilage(String sessionID, String collectionId) throws Exception{
		  
		  try{
			  removePrivilage(sessionID, collectionId);
		  }
		  catch(Exception e){
			  System.out.println("failed to remove first, new privilage given");
		  }
		  	String userName = "";
		  	Class.forName(driver).newInstance();
		  	connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
//		    PreparedStatement statement = connect.prepareStatement("CREATE TABLE sessiontable (id int, text varchar(50)");
		    Statement statement = connect.createStatement();
		    ResultSet res = statement.executeQuery("SELECT * FROM sessiontable where sessionid='"+sessionID+"'");
		    
		    while(res.next()){
		    	userName = res.getString("username");
		    	userName = userName.toLowerCase();
//		    	System.out.println("username found: "+userName);
		    }
		    
		    try{
		    	statement.executeUpdate("insert into privilageuser values ('"+userName+"','"+collectionId+"')");
		    }
		    catch(Exception e){
		    	e.printStackTrace();
		    	closeDB();
		    	return false;
		    }
		    finally{
		    	closeDB();
		    }
		    return true;
	  }
	  
	  public boolean removePrivilage(String sessionID, String collectionId) throws Exception{
		  	String userName = "";
		  	Class.forName(driver).newInstance();
		  	connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
//		    PreparedStatement statement = connect.prepareStatement("CREATE TABLE sessiontable (id int, text varchar(50)");
		    Statement statement = connect.createStatement();
		    ResultSet res = statement.executeQuery("SELECT * FROM sessiontable where sessionid='"+sessionID+"'");
		    
		    while(res.next()){
		    	userName = res.getString("username");
		    	userName = userName.toLowerCase();
//		    	System.out.println("username found: "+userName);
		    }
		    
		    try{
		    	userName = getUserFromCollection(collectionId);
		    	statement.executeUpdate("delete from privilageuser where username='"+userName+"' AND collection='"+collectionId+"'");
		    }
		    catch(Exception e){
		    	e.printStackTrace();
		    	closeDB();
		    	return false;
		    }
		    finally{
		    	closeDB();
		    }
		    return true;
	  }
	  
	  public boolean remAdminPrivilege(String collectionId) throws Exception{
		  	Class.forName(driver).newInstance();
		  	connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
//		    PreparedStatement statement = connect.prepareStatement("CREATE TABLE sessiontable (id int, text varchar(50)");
		    Statement statement = connect.createStatement();
		    
		    try{
		    	statement.executeUpdate("delete from privilageuser where username='administrator' AND collection='"+collectionId+"'");
		    }
		    catch(Exception e){
		    	e.printStackTrace();
		    	closeDB();
		    	return false;
		    }
		    finally{
		    	closeDB();
		    }
		    return true;
	  }
	  
	  public boolean addAdminPrivilage(String collectionId) throws Exception{
		  
		  try{
			  remAdminPrivilege(collectionId);
		  }
		  catch(Exception e){
			  System.out.println("failed to remove first, new privilage given");
		  }
		  	Class.forName(driver).newInstance();
		  	connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
//		    PreparedStatement statement = connect.prepareStatement("CREATE TABLE sessiontable (id int, text varchar(50)");
		    Statement statement = connect.createStatement();
		    try{
			    statement.executeUpdate("insert into privilageuser values ('administrator','"+collectionId+"')");
		    }
		    catch(Exception e){
		    	e.printStackTrace();
		    	closeDB();
		    	return false;
		    }
		    finally{
		    	closeDB();
		    }
		    return true;
	  }
//	  
//	  private ArrayList<String> getCollectionLists() throws Exception{
//		  ArrayList<String> colLists = new ArrayList<String>();
//		  
//		  Class.forName(driver).newInstance();
//		  connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
//		  Statement statement = connect.createStatement();
//		  ResultSet res = statement.executeQuery("SELECT * FROM privilageuser");
//		  
//		  
//		  return colLists;
//	  }
	  
	  public ArrayList<ArrayList<String>> getListSocMedToken(String username) throws Exception{
		  	Class.forName(driver).newInstance();
		  	connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
		    Statement statement = connect.createStatement();
		    
		    String query = "SELECT * FROM sosmedtoken where "
		    		+ "username='" + username + "' OR username='freeusertrial'";
		    
		    if(username.equalsIgnoreCase("administrator")){
		    	query = "SELECT * FROM sosmedtoken";
		    }
		    
		    ResultSet res = statement.executeQuery(query);
		    ArrayList<ArrayList<String>> tokens = new ArrayList<ArrayList<String>>();
		    
		    while(res.next()) {
		    	ArrayList<String> tokenParts = new ArrayList<String>();
		    	tokenParts.add(res.getString("sosmed"));
		    	tokenParts.add(res.getString("token"));
		    	tokenParts.add(res.getString("alias"));
		    	tokenParts.add(res.getString("keyid"));
		    	tokenParts.add(res.getString("secret"));
		    	tokens.add(tokenParts);
		    }
		    
		    closeDB();
		    return tokens;
	  }
	  
	  public boolean setSocMedToken(String username, String token, 
			  String type, String alias, String keyid, String secret) throws Exception{
		  	Class.forName(driver).newInstance();
		  	connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
		    Statement statement = connect.createStatement();
		    
		    type = type.toLowerCase();
		    
		    String query = "SELECT * FROM sosmedtoken";
		    ResultSet res = statement.executeQuery(query);
		    
		    while(res.next()){
		    	String aliasCheck = res.getString("alias");
		    	if(aliasCheck.equalsIgnoreCase(alias)){
		    		System.out.println("alias found");
		    		return false;
		    	}
		    }
		    
		    if(alias != null && !alias.isEmpty()){
		    	 statement.executeUpdate("INSERT INTO sosmedtoken "
		    	 		+ "(username, sosmed, token, alias, keyid, secret) VALUES ('"
				    		+ username + "','"
				    		+ type + "','"
				    		+ token + "','"
				    		+ alias + "','"
				    		+ keyid + "','"
				    		+ secret
				    		+ "')"
				    );
		    }else{
			    statement.executeUpdate("INSERT INTO sosmedtoken (username, sosmed, token1) VALUES ('"
			    		+ username + "','"
			    		+ type + "','"
			    		+ token
			    		+ "')"
			    );
		    }
		    
		    closeDB();
		    return true;
	  }
	  
	  public void editSocMedToken(String username, String token, 
			  String type, String alias, String keyid, String secret) throws Exception{
		  	Class.forName(driver).newInstance();
		  	connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
		    Statement statement = connect.createStatement();
		    
		    type = type.toLowerCase();
	    	String command = "update sosmedtoken set "
	    			+ "token='"+token+"', "
	    			+ "keyid='"+keyid+"', "
	    			+ "secret='"+secret+"' "
	    			+ "where username='"+username+"' and "
	    			+ "alias='"+alias+"'";
	    	if(username.equalsIgnoreCase("administrator")){
	    		command = "update sosmedtoken set "
		    			+ "token='"+token+"', "
		    			+ "keyid='"+keyid+"', "
		    			+ "secret='"+secret+"' "
		    			+ "where "
		    			+ "alias='"+alias+"'"; 
	    	}
	    	statement.executeUpdate(command);
		    closeDB();
	  }
	  
	  public void deleteSocMedToken(String username, String alias) throws Exception{
		  	Class.forName(driver).newInstance();
		  	connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
		    Statement statement = connect.createStatement();
		    
		    String command = "DELETE FROM sosmedtoken WHERE alias='"
		    		+ alias + "' and username='"+username+"'";
		    
		    if(username.equalsIgnoreCase("administrator")){
		    	command = "DELETE FROM sosmedtoken WHERE alias='"
			    		+ alias + "'";
		    }
		    
		    statement.executeUpdate(command);
		    
		    closeDB();
	  }
	  
	  public ArrayList<String> getUserPackages(String username) throws Exception{
		  if(!username.equalsIgnoreCase("administrator")){
			return null;  
		  }
		  ArrayList<String> userPackages = new ArrayList<String>();
		  Class.forName(driver).newInstance();
		  connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
		  //		    PreparedStatement statement = connect.prepareStatement("CREATE TABLE sessiontable (id int, text varchar(50)");
		  Statement statement = connect.createStatement();
		  ResultSet res = statement.executeQuery("SELECT * FROM userpackage where username<>'"+username+"'"); 

		  while (res.next()) {
			  String users = res.getString("username");
			  String packages = res.getString("package");
			  userPackages.add(users+","+packages);
		  }
		  closeDB();
		  return userPackages;
	  }
	  
	  public boolean setUserPackages(String username, String userToChange, String packages, String maxCrawler, String maxCollection) throws Exception{
		  if(!username.equalsIgnoreCase("administrator")){
			return false;  
		  }
		  if(packages.equalsIgnoreCase("silver")||packages.equalsIgnoreCase("gold")||
				  packages.equalsIgnoreCase("diamond")||packages.equalsIgnoreCase("custom")||
				  packages.equalsIgnoreCase("freeuser")||packages.equalsIgnoreCase("administrator")||
				  packages.equalsIgnoreCase("ultimate")){
			  
			  Class.forName(driver).newInstance();
			  connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
			  //		    PreparedStatement statement = connect.prepareStatement("CREATE TABLE sessiontable (id int, text varchar(50)");
			  Statement statement = connect.createStatement();
			  String command = "update userpackage set "
		    			+ "package='"+packages+"' "
		    			+ "where username='"+userToChange+"'";
			  statement.executeUpdate(command); 
			  
			  if(packages.equalsIgnoreCase("custom")){
				  String command2 = "update collimit set "
			    			+ "maxcol="+maxCollection+" "
			    			+ "where username='"+userToChange+"'";
				  statement.executeUpdate(command2); 
				  String command3 = "update crawllimit set "
			    			+ "maxcrawl="+maxCrawler+" "
			    			+ "where username='"+userToChange+"'";
				  statement.executeUpdate(command3); 
			  }
			  
			  closeDB();
			  return true;
		  }
		  return false;
	  }
}
