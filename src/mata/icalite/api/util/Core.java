package mata.icalite.api.util;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.common.params.CoreAdminParams.CoreAdminAction;

public class Core {
	private static final String URL = "http://127.0.0.1:8983/solr";
	
	public Core(){
		
	}
	
	public boolean isCoreLoaded(String collectionId) throws Exception{
		SolrServer server = new HttpSolrServer(URL);
		((HttpSolrServer) server).setParser(new XMLResponseParser());
		
		CoreAdminRequest request = new CoreAdminRequest();
		CoreAdminResponse cores = null;
		
		request.setAction(CoreAdminAction.STATUS);
		cores = request.process(server);
		
		if(cores != null){
			for(int i = 0; i < cores.getCoreStatus().size(); i++){
				if(collectionId.equals(cores.getCoreStatus().getName(i))){
					return true;
				}
			}
		}
		
		return false;
	}
}
