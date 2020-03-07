package tr.monerostatus.jsondata.blockexplorers;

import android.content.res.Resources;
import android.util.Log;

import org.json.JSONObject;
import org.json.JSONTokener;

import tr.monerostatus.jsondata.BlockExplorerDataParser;
import tr.monerostatus.jsondata.JSONRetriever;
import tr.monerostatus.jsondata.JSONRetriever.JSONParser;
import tr.monerostatus.jsondata.JSONRetriever.JSONUpdatable;

public class MoneroBlocksParser implements JSONParser{
	public final static String explorer_code = "moneroblocks";

    private final static String api_request_url = "https://moneroblocks.info/api/get_stats";
    private final static String parse_difficulty = "difficulty";
    private final static String parse_total = "total_emission";
    private final static String parse_height = "height";
    private final static String parse_reward = "last_reward";
    private final static String parse_hashrate = "hashrate";

    private MoneroBlocksParser(){
    }
    
    public static void get(JSONUpdatable updatable, Resources resources){
    	
    	new JSONRetriever(api_request_url, new MoneroBlocksParser(), updatable, resources).execute();
    }

	@Override
	public Object parseJSONData(JSONRetriever retriever, JSONTokener data) {
		
		try {
			JSONObject obj = new JSONObject(data);

            Long difficulty = obj.getLong(parse_difficulty);
            Double totalCoins = obj.getDouble(parse_total);
            Integer height = obj.getInt(parse_height);
            Double reward = obj.getDouble(parse_reward);
            Double hashrate = obj.getDouble(parse_hashrate);

            return new MoneroBlocks(difficulty, totalCoins, height, reward, hashrate);
		} 
		catch (Exception e) {
	        Log.e(this.getClass().getSimpleName(),"Json parse failed: "+e.getLocalizedMessage());
		}
		return null;
	}

	
	public static class MoneroBlocks implements BlockExplorerDataParser.BlockExplorerData {
		private final Long difficulty;
		private final Double totalCoins;
		private final Integer height;
        private final Double reward;
        private final Double hashrate;
		
		public MoneroBlocks(Long difficulty, Double totalCoins, Integer height, Double reward, Double hashrate){
			this.difficulty = difficulty;
			this.totalCoins = totalCoins/1000000000000.0;
			this.height = height;
            this.reward = reward/1000000000000.0;
            this.hashrate = hashrate/1000000.0;
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