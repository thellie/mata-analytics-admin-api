package mata.icalite.api.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import com.sun.jersey.api.view.Viewable;

import mata.icalite.api.util.RegistrationControl;
import mata.icalite.api.util.SystemControl;

@Path("/registration")
public class RegisterPage {

	private SystemControl sc = null;
	private String caxHome = null;
	private String emailTemplateDir = null;
	private String userEmailTemplate = null;
	private String userEmailNoPassTemplate = null;
	private String recipientAdmin = null;
	private String adminEmailTemplate = null;
	
	public RegisterPage(){
		sc = new SystemControl();
		caxHome = System.getenv("SOLR_HOME");
		emailTemplateDir = caxHome + "\\example\\resources\\config_template\\template_email";
		userEmailTemplate = emailTemplateDir+"\\user_template.txt";
		userEmailNoPassTemplate = emailTemplateDir+"\\user_nopass_template.txt";
		adminEmailTemplate = emailTemplateDir+"\\admin_template.txt";
		recipientAdmin = emailTemplateDir+"\\admin_recipient_email_list.txt";
	}
	
	@POST
	@Produces("application/xml")
	public Viewable postParam(@QueryParam("method") String method, 
			@QueryParam("sentEmailtoUser") String sentEmailtoUser,	String body) 
	{
		Map<String,Object> apiResponse = new HashMap<String,Object>();		
		if(method.equals("register")){
			return register(body,sentEmailtoUser);
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
	
	private Viewable register(String body, String sentEmailtoUser){
		Map<String,Object> apiResponse = new HashMap<String,Object>();
		List<Object> error = new ArrayList<Object>();
		Boolean sentEmailtoUserBool = Boolean.valueOf(sentEmailtoUser); 
		
		try {
			RegistrationControl register = new RegistrationControl(body);
			String message = register.parsingRegister();
			if(message.equalsIgnoreCase("successful")){
				register.addNewUser();
				register.registerNewUser();
				register.setPackageFreeUser();
				register.sendMailToAdmin(body,adminEmailTemplate,recipientAdmin);
//				register.sendMailToRegisteredUser(body,userEmailTemplate);
				if(sentEmailtoUserBool){
					System.out.println("sending notification to user");
//					register.sendMailToRegisteredUser(body,userEmailTemplate);
				}
				else{
//					register.sendMailToRegisteredUserNoPassword(body,userEmailNoPassTemplate);
				}
				System.out.println("New user registered!");
			}
			else{
				Map<String,Object> property = new HashMap<String,Object>();
				
				property.put("message", message);
				property.put("value", "1");
				
				apiResponse.put("items", property);
				return new Viewable("/general/ack", apiResponse);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
