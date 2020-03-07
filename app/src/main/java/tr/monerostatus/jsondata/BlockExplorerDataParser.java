package tr.monerostatus.jsondata;

import android.content.res.Resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tr.monerostatus.jsondata.JSONRetriever.JSONUpdatable;
import tr.monerostatus.jsondata.blockexplorers.ChainRadarParser;
import tr.monerostatus.jsondata.blockexplorers.MoneroBlocksParser;

public class BlockExplorerDataParser {

	private static final Class<?> blockExplorerClasses[] = {ChainRadarParser.class, MoneroBlocksParser.class/*, MoneroChainParser.class*/};
    private static final Map<String, Class<?>> explorerParsers = new HashMap<String, Class<?>>();
    private static final List<String> explorerCodesTmp = new ArrayList<String>();
	
	static{
		loadExplorers();
	}

    private static void loadExplorers(){
        for (Class<?> c: blockExplorerClasses){
            String explorer = null;
            try {
                explorer = (String)c.getField("explorer_code").get(null);
            }
            catch (Exception e) {
                continue;
            }
        explorerParsers.put(explorer, c);
        explorerCodesTmp.add(explorer);
        }
    }

    public static final String explorerCodes[] = explorerCodesTmp.toArray(new String[explorerCodesTmp.size()]);
    
    public static void getData(String explorer, JSONUpdatable updatable, Resources resources){
    	try {
            if (explorerParsers.containsKey(explorer))
                explorerParsers.get(explorer).getMethod("get", JSONUpdatable.class, Resources.class).invoke(null, updatable, resources);
		}
    	catch (Exception e) {
    	}
    }
    
    public interface BlockExplorerData{

        public String getExplorer();
        public Long getDifficulty();
        public Double getTotalCoins();
        public Double getHashrate();
        public Integer getHeight();
        public Double getReward();
    }
}
