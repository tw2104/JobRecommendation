package external;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.monkeylearn.MonkeyLearn;
import com.monkeylearn.MonkeyLearnException;
import com.monkeylearn.MonkeyLearnResponse;
import com.monkeylearn.ExtraParam;

public class MonkeyLearnClient {
	
	private static final String API_KEY = "ea19671a0f0081f213381dae662457f00b1f7e73";
	private static final String MODEL = "ex_YCya9nrn";
	
	public static List<List<String>> extractKeywords(String[] text) {
		if (text == null || text.length == 0) return new ArrayList<>();
		
		MonkeyLearn ml = new MonkeyLearn(API_KEY);
		ExtraParam[] extraParams = {new ExtraParam("max_keywords", "3")}; 
		MonkeyLearnResponse response;	
		
		try {
			response = ml.extractors.extract(MODEL, text, extraParams);
			return getKeywords(response.arrayResult);
		} catch (MonkeyLearnException e) {
			e.printStackTrace();
		}
		
		return new ArrayList<>();
	}
	
	private static List<List<String>> getKeywords(JSONArray mlResponse) {
		List<List<String>> topKeywords = new ArrayList<>();
		for (int i = 0; i < mlResponse.size(); i++) {
			List<String> keywords = new ArrayList<>();
			// we cast here since JSONObejct can be any format
			JSONArray keywordsArray = (JSONArray) mlResponse.get(i);
			for (int j = 0; j < keywordsArray.size(); j++) {
				JSONObject keywordObj = (JSONObject) keywordsArray.get(j);
				String keyword = (String) keywordObj.get("keyword");
				keywords.add(keyword);
			}
			topKeywords.add(keywords);
		}
		
		return topKeywords;
	}

}
