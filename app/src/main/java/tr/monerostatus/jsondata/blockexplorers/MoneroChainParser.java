package tr.monerostatus.jsondata.blockexplorers;

import android.content.res.Resources;
import android.util.Log;

import org.json.JSONObject;
import org.json.JSONTokener;

import tr.monerostatus.jsondata.BlockExplorerDataParser;
import tr.monerostatus.jsondata.JSONRetriever;
import tr.monerostatus.jsondata.JSONRetriever.JSONParser;
import tr.monerostatus.jsondata.JSONRetriever.JSONUpdatable;

public class MoneroChainParser implements JSONParser{
	public final static String explorer_code = "monerochain";

    private final static String api_request_url = "http://monerochain.info/api/stats";
    private final static String api_block_request_url = "http://monerochain.info/api/block/";
    private final static String parse_difficulty = "difficulty";
    private final static String parse_total = "total_coins";
    private final static String parse_height = "height";
    private final static String parse_reward = "reward";

    private final boolean withReward;
    private final JSONUpdatable updatable;

    private Integer block = 0;
    private MoneroChain data = null;
    
    private MoneroChainParser(boolean withReward, JSONUpdatable updatable){
    	this.withReward = withReward;
    	this.updatable = updatable;
    }
    
    public static void get(boolean withReward, JSONUpdatable updatable, Resources resources){
    	
    	new JSONRetriever(api_request_url, new MoneroChainParser(withReward, updatable), updatable, resources).execute();
    }

	@Override
	public Object parseJSONData(JSONRetriever retriever, JSONTokener data) {
		
		try {
			JSONObject obj = new JSONObject(data);
			
			if (block == 0){
                Long difficulty = obj.getLong(parse_difficulty);
				Double totalCoins = obj.getDouble(parse_total);
				Integer height = obj.getInt(parse_height);
				if (withReward){
					block = height-1;
					new JSONRetriever(api_block_request_url+block, this, updatable, retriever.resources).execute();
				}
				
				this.data = new MoneroChain(difficulty, totalCoins, height);
				return data;
			}
			else{
				Double reward = obj.getDouble(parse_reward);
				return new MoneroChain(this.data, reward);
			}			
		} 
		catch (Exception e) {
	        Log.e(this.getClass().getSimpleName(),"Json parse failed: "+e.getLocalizedMessage());
		}
		return null;
	}

	
	public static class MoneroChain implements BlockExplorerDataParser.BlockExplorerData {
		private final Long difficulty;
		private final Double totalCoins;
		private final Integer height;
		private final Double reward;
		private final Double hashrate;
		
		public MoneroChain(Long difficulty, Double totalCoins, Integer height){
			this.difficulty = difficulty;
			this.totalCoins = totalCoins;
			this.height = height;
			this.reward = null;
			this.hashrate = difficulty/120.0/1000000.0;
		}
		
		public MoneroChain(MoneroChain data, Double reward){
			this.difficulty = data.difficulty;
			this.totalCoins = data.totalCoins;
			this.height = data.height;
			this.reward = reward;
            this.hashrate = difficulty/120.0/1000000.0;
		}

		public String getExplorer(){
			return explorer_code;
		}
		
		public Long getDifficulty(){
			return difficulty;
		}
		
		public Double getTotalCoins(){
			return totalCoins;
		}

		@Override
		public Double getHashrate() {
			return hashrate;
		}

		public Integer getHeight(){
			return height;
		}
		
		public Double getReward(){
			return reward;
		}
	}
}