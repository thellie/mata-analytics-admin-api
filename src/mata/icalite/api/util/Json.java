package mata.icalite.api.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Json {
	public Json(){
		
	}
	
	public Map<String, String> parse(String s) throws Exception{
		Map<String, String> outListConfig = new HashMap<String, String>();
		JSONParser parser = new JSONParser();
		
		Object obj = null;
		
		obj = parser.parse(s);
		
		JSONObject jsonObject = (JSONObject) obj;
		
		if(jsonObject != null){
		    Iterator<?> keys = jsonObject.keySet().iterator();

		    while(keys.hasNext()){
		        String key = (String) keys.next();
		        String value = (String) jsonObject.get(key);
		        
		        outListConfig.put(key, value);
		    }
		}
		
		return outListConfig;  
	}
}
