package external;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import entity.Item;

public class GitHubClient {
	private static final String URL_TEMPLATE = "https://jobs.github.com/positions.json?description=%s&lat=%s&long=%s";
	private static final String DEFAULT_KEYWORD = "developer";
	
	public List<Item> search(double lat, double lon, String keyword) {
		if (keyword == null) keyword = DEFAULT_KEYWORD;
		try {
			// encode to format of url, since the format of url is not like string
			keyword = URLEncoder.encode(keyword, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String url = String.format(URL_TEMPLATE, keyword, lat, lon);
		
		CloseableHttpClient httpclient = HttpClients.createDefault();
		
		// Create a custom response handler inherited from its super class
		// which helps us to close the client and consume the entity
	    ResponseHandler<List<Item>> responseHandler = new ResponseHandler<List<Item>>() {

	        @Override
	        public List<Item> handleResponse(final HttpResponse response) throws IOException {
	            if (response.getStatusLine().getStatusCode() != 200) {
	            	return new ArrayList<>();
	            }
	            HttpEntity entity = response.getEntity();
	            if (entity == null) {
	            	return new ArrayList<>();
	            }
	            String responseBody = EntityUtils.toString(entity);
	            return getItemList(new JSONArray(responseBody));
	        }
	    };

		try {
			return httpclient.execute(new HttpGet(url), responseHandler);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// return an empty json array if requests failed
		return new ArrayList<>();
	}
	
	private List<Item> getItemList(JSONArray array) {
		List<Item> itemList = new ArrayList<>();
		List<String> descriptionList = new ArrayList<>();
		
		for (int i = 0; i < array.length(); i++) {
			String description = getStringFieldOrEmpty(array.getJSONObject(i), "description");
			// if description is empty, we use title as text to extract
			if (description.equals("") || description.equals("\n")) {
				descriptionList.add(getStringFieldOrEmpty(array.getJSONObject(i), "title"));
			} else {
				descriptionList.add(description);
			}	
		}
		
		// apply MonkeyLearn API to get keywords
		// convert text list to array
		List<List<String>> keywords = MonkeyLearnClient
				.extractKeywords(descriptionList.toArray(new String[descriptionList.size()]));

		for (int i = 0; i < array.length(); i++) {
			JSONObject obj = array.getJSONObject(i);
			Item item = Item.builder()
					.itemId(getStringFieldOrEmpty(obj, "id"))
					.name(getStringFieldOrEmpty(obj, "title"))
					.address(getStringFieldOrEmpty(obj, "location"))
					.url(getStringFieldOrEmpty(obj, "url"))
					.imageUrl(getStringFieldOrEmpty(obj, "company_logo"))
					.keywords(new HashSet<String>(keywords.get(i))) // Item obj expects a Set as keywords argument
					.build();	
			itemList.add(item);
		}
		return itemList;
	}
	
	private String getStringFieldOrEmpty(JSONObject obj, String field) {
		return obj.isNull(field) ? "" : obj.getString(field);
	}

}

