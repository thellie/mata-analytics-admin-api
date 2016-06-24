package mata.icalite.api.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.Context;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.joda.time.DateTime;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class RegistrationControl {

	private Connection connect = null;
	private Statement statement = null;
	private ResultSet resultSet = null;
	private String protocol = "jdbc:derby://127.0.0.1:1527/";
	private String driver = "org.apache.derby.jdbc.ClientDriver";

	private String email = "";
	private String username = "";
	private String firstname = "";
	private String lastname = "";
	private String organization = "";
	private String phonenum;
	private String password = "";
	private final String organisationUnit = "Users";
	
	private static final String DOMAIN_NAME = "cax.mata.org";
	private static final String DOMAIN_ROOT = "DC=cax,DC=mata,DC=org";
	private static final String DOMAIN_URL = "ldap://127.0.0.1:389";
	private static final String ADMIN_NAME = "CN=Administrator,CN=Users,DC=cax,DC=mata,DC=org";
	private static final String ADMIN_PASS = "passw0rd!";
	
	final String usernameLogin = "support@mataprima.com";
	final String passwordLogin = "m@tapr1maunivers4l!";

	private LdapContext context;
	
    int UF_ACCOUNTDISABLE = 0x0002;
    int UF_PASSWD_NOTREQD = 0x0020;
    int UF_PASSWD_CANT_CHANGE = 0x0040;
    int UF_NORMAL_ACCOUNT = 0x0200;
    int UF_DONT_EXPIRE_PASSWD = 0x10000;
    int UF_PASSWORD_EXPIRED = 0x800000;

	public RegistrationControl(String body) throws Exception{
		JSONParser parser = new JSONParser();
		JSONObject jsonBody = (JSONObject) parser.parse(body);

		email = (String) jsonBody.get("email");
		username = (String) jsonBody.get("username");
		firstname = (String) jsonBody.get("firstname");
		lastname = (String) jsonBody.get("lastname");
		phonenum = (String) jsonBody.get("phone");
		organization = (String) jsonBody.get("organization");
		password = getSaltString();
	}

	private static String getSaltString() {
		
		String SALT0CHARS = "abcdefghijklmnopqrstuvwxyz";
        String SALT1CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String SALT2CHARS = "1234567890";
        String SALT3CHARS = "!@";
        
        StringBuilder salt0 = new StringBuilder();
        StringBuilder salt1 = new StringBuilder();
        StringBuilder salt2 = new StringBuilder();
        StringBuilder salt3 = new StringBuilder();
        
        Random rnd = new Random();
        while (salt0.length() < 4) {
            int index = (int) (rnd.nextFloat() * SALT0CHARS.length());
            salt0.append(SALT0CHARS.charAt(index));
        }
        String saltStr = salt0.toString();
        
        while (salt1.length() < 4) {
            int index = (int) (rnd.nextFloat() * SALT1CHARS.length());
            salt1.append(SALT1CHARS.charAt(index));
        }
        saltStr = saltStr + salt1.toString();
        
        while (salt2.length() < 3) {
            int index = (int) (rnd.nextFloat() * SALT2CHARS.length());
            salt2.append(SALT2CHARS.charAt(index));
        }
        saltStr = saltStr + salt2.toString();
        
        while (salt3.length() < 1) {
            int index = (int) (rnd.nextFloat() * SALT3CHARS.length());
            salt3.append(SALT3CHARS.charAt(index));
        }
        saltStr = saltStr + salt3.toString();
        
        return saltStr;

    }
	
	public boolean registerNewUser() throws Exception{		
		Class.forName(driver).newInstance();
		connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
		Statement statement = connect.createStatement();

		statement.executeUpdate("insert into registrationtable values ('"+email+"','"+username+"',"
				+ "'"+firstname+"','"+lastname+"','"+phonenum+"','"+organization+"','"+password+"')");

		closeDB();
		return true;
	}

	public String parsingRegister() throws Exception{		
		String errorMessage = "successful";

		if(!checkEmailValidation(email)){
			errorMessage = "email is not valid or already registered";
		}
		else if(!checkUsernameValidation(username)){
			errorMessage = "username is not valid or already registered";
		}
		else if(!checkFirstnameValidation(firstname)){
			errorMessage = "firstname is not valid, please fill your firstname";
		}
		else if(!checkLastnameValidation(lastname)){
			errorMessage = "lastname is not valid, please fill your lastname";
		}
		else if(!checkPhoneValidation(phonenum)){
			errorMessage = "phone number is not valid, please fill your phone number";
		}
		else if(!checkOrganizationValidation(organization)){
			errorMessage = "organization is not valid, please fill your organization";
		}	
		else if(!checkPasswordValidation(password)){
			errorMessage = "password is not valid, please follow password rule";
		}

		return errorMessage;
	}

	private boolean checkEmailValidation(String email) throws Exception{
		boolean isValid = true;
		String EMAIL_PATTERN = 
				"^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
						+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
		Pattern pattern = Pattern.compile(EMAIL_PATTERN);
		Matcher m = pattern.matcher(email);
		if(!m.find()){
			isValid = false;
		}

		Class.forName(driver).newInstance();
		connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
		Statement statement = connect.createStatement();

		ResultSet res = statement.executeQuery("SELECT * FROM registrationtable where email='"+email+"'"); 

		if(res.next()) {
			isValid = false;
		}
		closeDB();
		return isValid;
	}

	private boolean checkPasswordValidation(String password) throws Exception{
		boolean isValid = true;

		if(password==null){
			isValid = false;
		}
		if(password.length()<1){
			isValid = false;
		}
		
		return isValid;
	}
	
	private boolean checkUsernameValidation(String username) throws Exception{
		boolean isValid = true;

		if(username==null){
			isValid = false;
		}
		if(username.length()<1){
			isValid = false;
		}
		Pattern p = Pattern.compile("[^A-Za-z0-9]");
		Matcher m = p.matcher(username);
		if(m.find()){
			isValid = false;
		}
		
		Class.forName(driver).newInstance();
		connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
		Statement statement = connect.createStatement();

		ResultSet res = statement.executeQuery("SELECT * FROM registrationtable where username='"+username+"'"); 

		if(res.next()) {
			isValid = false;
		}
		closeDB();
		return isValid;
	}

	private boolean checkFirstnameValidation(String firstname){
		boolean isValid = true;

		if(firstname==null){
			isValid = false;
		}
		if(firstname.length()<1){
			isValid = false;
		}
		Pattern p = Pattern.compile("[^A-Za-z]");
		Matcher m = p.matcher(firstname);
		if(m.find()){
			isValid = false;
		}

		return isValid;
	}

	private boolean checkLastnameValidation(String lastname){
		boolean isValid = true;

		if(lastname==null){
			isValid = false;
		}
		if(lastname.length()<1){
			isValid = false;
		}
		Pattern p = Pattern.compile("[^A-Za-z]");
		Matcher m = p.matcher(lastname);
		if(m.find()){
			isValid = false;
		}
		return isValid;
	}

	private boolean checkPhoneValidation(String phone){
		boolean isValid = true;

		if(phone==null){
			isValid = false;
		}
		if(phone.length()<1){
			isValid = false;
		}

		return isValid;
	}

	private boolean checkOrganizationValidation(String organization){
		boolean isValid = true;

		if(organization==null){
			isValid = false;
		}
		if(organization.length()<1){
			isValid = false;
		}

		return isValid;
	}

	private void closeDB() throws Exception{
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

	public void addNewUser() throws Exception {

		Hashtable<String, String> env = new Hashtable<String, String>();

		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");

		// set security credentials, note using simple cleartext authentication
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, ADMIN_NAME);
		env.put(Context.SECURITY_CREDENTIALS, ADMIN_PASS);

		// connect to my domain controller
		env.put(Context.PROVIDER_URL, DOMAIN_URL);

		this.context = new InitialLdapContext(env, null);
		
		addUser();
	}

	private boolean addUser() throws Exception {

		// Create a container set of attributes
		Attributes container = new BasicAttributes();

		// Create the objectclass to add
		Attribute objClasses = new BasicAttribute("objectClass");
		objClasses.add("top");
		objClasses.add("person");
		objClasses.add("organizationalPerson");
		objClasses.add("user");

		// Assign the username, first name, and last name
		Attribute cn = new BasicAttribute("cn", username);
		Attribute sAMAccountName = new BasicAttribute("sAMAccountName", username);
		Attribute principalName = new BasicAttribute("userPrincipalName", username
				+ "@" + DOMAIN_NAME);
		Attribute givenName = new BasicAttribute("givenName", firstname);
		Attribute sn = new BasicAttribute("sn", lastname);
		Attribute uid = new BasicAttribute("uid", username);
		Attribute mail = new BasicAttribute("mail", email);
		Attribute company = new BasicAttribute("company", organization);
		Attribute phone = new BasicAttribute("mobile", phonenum);

		// Add these to the container
		container.put(objClasses);
		container.put(sAMAccountName);
		container.put(principalName);
		container.put(cn);
		container.put(sn);
		container.put(givenName);
		container.put(uid);
		container.put(mail);
		container.put(company);
		container.put(phone);

		container.put("userAccountControl",Integer.toString(UF_NORMAL_ACCOUNT + UF_PASSWD_NOTREQD + UF_PASSWORD_EXPIRED+ UF_ACCOUNTDISABLE));

		// Create the entry
		System.out.println("creating user");
		context.createSubcontext(getUserDN(username, organisationUnit), container);
		Thread.sleep(3000);
		setPassword();
		
		return true;
	}
	
	private String getUserDN(String aUsername, String aOU) {
		System.out.println("cn=" + aUsername + ",cn=" + aOU + "," + DOMAIN_ROOT);
		return "cn=" + aUsername + ",cn=" + aOU + "," + DOMAIN_ROOT;
	}
	
	public  void sendMailToRegisteredUserNoPassword(String body, String userEmailTemplate) throws Exception{
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.mataprima.com");
		props.put("mail.smtp.port", "587");

		Session session = Session.getInstance(props,
		  new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(usernameLogin, passwordLogin);
			}
		  });
		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress("support@mataprima.com"));
		message.setRecipients(Message.RecipientType.TO,
				InternetAddress.parse(email));
		message.setSubject("Mata Analytics Free User Registration");
		String contentEmail = new FileManager().readData(userEmailTemplate);
		message.setText(contentEmail);

		Transport.send(message);
	}
	
	public  void sendMailToRegisteredUser(String body, String userEmailTemplate) throws Exception{
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.mataprima.com");
		props.put("mail.smtp.port", "587");

		Session session = Session.getInstance(props,
		  new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(usernameLogin, passwordLogin);
			}
		  });
		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress("support@mataprima.com"));
		message.setRecipients(Message.RecipientType.TO,
				InternetAddress.parse(email));
		message.setSubject("Mata Analytics Free User Registration");
		String contentEmail = new FileManager().readData(userEmailTemplate);
		contentEmail = contentEmail.replace("[usernametowrite]", username);
		contentEmail = contentEmail.replace("[passwordtowrite]", password);
		message.setText(contentEmail);

		Transport.send(message);
	}
	
	public  void sendMailToAdmin(String body, String adminEmailTemplate, String recipientTemplate) throws Exception{	
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.mataprima.com");
		props.put("mail.smtp.port", "587");

		Session session = Session.getInstance(props,
		  new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(usernameLogin, passwordLogin);
			}
		  });

		String contentEmail = new FileManager().readData(adminEmailTemplate);
		String recipientAdmin = new FileManager().readData(recipientTemplate);
		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress("support@mataprima.com"));
		message.setRecipients(Message.RecipientType.TO,
				InternetAddress.parse(recipientAdmin));
		
		message.setSubject("New Registration User");
		contentEmail = contentEmail.replace("[emailtowrite]", email);
		contentEmail = contentEmail.replace("[usernametowrite]", username);
		contentEmail = contentEmail.replace("[fistnametowrite]", firstname);
		contentEmail = contentEmail.replace("[lastnametowrite]", lastname);
		contentEmail = contentEmail.replace("[phonetowrite]", phonenum);
		contentEmail = contentEmail.replace("[organizationtowrite]", organization);
		contentEmail = contentEmail.replace("[passwordtowrite]", password);
		message.setText(contentEmail);

		Transport.send(message);
	}
	
	public boolean setPackageFreeUser() throws Exception{
		
		Class.forName(driver).newInstance();
		connect = DriverManager.getConnection(protocol + "c:\\caxDB\\");
		Statement statement = connect.createStatement();

		statement.executeUpdate("insert into userpackage values ('"+username+"','freeuser')");
		closeDB();
		return true;
	}
	
	private String getExpiredDate(){
		DateFormat dateFormat = new SimpleDateFormat("MMMM,dd,yyyy");
		Date date = new Date();
		Date daysExpired = new DateTime(date).plusDays(14).toDate();
		
		return dateFormat.format(daysExpired);
	}
	
	private boolean setPassword() throws Exception{
		String command = "net user "
				+ ""+username+" "
				+ ""+password+" "
				+ "/active:yes "
				+ "/passwordchg:no "
				+ "/passwordreq:yes "
				+ "/expires:"+getExpiredDate()+"";
		new SystemControl().runExec(command);
		System.out.println("running command : "+command);
		return true;	
	}
}
