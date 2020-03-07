package tr.monerostatus.jsondata.blockexplorers;

import android.content.res.Resources;
import android.util.Log;

import org.json.JSONObject;
import org.json.JSONTokener;

import tr.monerostatus.jsondata.BlockExplorerDataParser;
import tr.monerostatus.jsondata.JSONRetriever;
import tr.monerostatus.jsondata.JSONRetriever.JSONParser;
import tr.monerostatus.jsondata.JSONRetriever.JSONUpdatable;

public class ChainRadarParser implements JSONParser{
	public final static String explorer_code = "chainradar";

    private final static String api_request_url = "https://chainradar.com/api/v1/mro/status";
    private final static String parse_difficulty = "difficulty";
    private final static String parse_total = "alreadyGeneratedCoins";
    private final static String parse_height = "height";
    private final static String parse_reward = "reward";
    private final static String parse_hashrate = "instantHashrate";

	// 864 seconds = 86400 seconds(day) / 100 api calls per day
	// z = y*(100-x)/3600, z + x/2 = 24, z=8     : z = hours of only widget usage, x = calls for hours of only widget usage, y = time limit
	private static final long apiTimeLimit = 424000; //dropping that down, expecting 8 hours of rigorous usage

	private static long lastCall = 0;

    private ChainRadarParser(){
    }
    
    public static void get(JSONUpdatable updatable, Resources resources){
    	if (System.currentTimeMillis() >= lastCall + apiTimeLimit) {
			lastCall = System.currentTimeMillis();
			new JSONRetriever(api_request_url, new ChainRadarParser(), updatable, resources).execute();
		}
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

            return new ChainRadar(difficulty, totalCoins, height, reward, hashrate);
		} 
		catch (Exception e) {
	        Log.e(this.getClass().getSimpleName(),"Json parse failed: "+e.getLocalizedMessage());
		}
		return null;
	}

	
	public static class ChainRadar implements BlockExplorerDataParser.BlockExplorerData {
		private final Long difficulty;
		private final Double totalCoins;
		private final Integer height;
		private final Double reward;
        private final Double hashrate;
		
		public ChainRadar(Long difficulty, Double totalCoins, Integer height, Double reward, Double hashrate){
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