package tr.monerostatus.app;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.jjoe64.graphview.LineGraphView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import tr.monerostatus.DataContainer;
import tr.monerostatus.DataContainer.Refreshable;
import tr.monerostatus.R;
import tr.monerostatus.app.GraphHelper.GraphPoint;
import tr.monerostatus.app.MainActivity.CustomSublistFragment;
import tr.monerostatus.jsondata.ExchangeDataParser;

public class GraphFragment extends Fragment implements Refreshable, CustomSublistFragment {
	public static String FRAGMENT_TAG = "graph_fragment";

	public static final String PREFS_NAME = "XMRStatus_depth";

	private static double graphRange = 0.3;
	private static final Object rangeLock = new Object();
    private static final Double REDUCTIONEPSILON = 0.00005;
	private static Set<Class<?>> baseRequirements = new HashSet<Class<?>>();
	static{
		baseRequirements.add(ExchangeDataParser.ExchangeOrderData.class);
		baseRequirements.add(ExchangeDataParser.ExchangeTickerData.class);
	}

	private static List<String> entries = null;
	private static SparseBooleanArray graphExchanges = null;
	private GraphView graphView;
	private GridView listView;
	private View rootView;
	
	private ScaleGestureDetector rangeDetector;

	private Executor graphExecutor = Executors.newSingleThreadExecutor();
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.graph_fragment, container,false);
		
		rangeDetector = new ScaleGestureDetector(getActivity(), new OnScaleGestureListener(){
			@Override
			public boolean onScale(ScaleGestureDetector detector) {
				synchronized (rangeLock){
					graphRange *= 1/detector.getScaleFactor();
					if (graphRange < 0.01)
						graphRange = 0.01;
					else if (graphRange > 1)
						graphRange = 1;
				}
				updateGraph(false);
				return true;
			}

			@Override
			public boolean onScaleBegin(ScaleGestureDetector detector) {
				return true;
			}

			@Override
			public void onScaleEnd(ScaleGestureDetector detector) {
                updateGraph(true);
			}
        });
		
	    graphView = new LineGraphView(getActivity(),"Market Depth"){
	    	@Override
	    	protected void onSizeChanged (int w, int h, int oldw, int oldh){
	    		super.onSizeChanged(w, h, oldw, oldh);
	    		new Handler().postDelayed(
					new Runnable() {
						@Override
						public void run() {
							updateSublist(((MainActivity)getActivity()));
						}
				}, 100);
	    	}
	    	
	    	@Override
	    	public boolean onTouchEvent(MotionEvent event) {
	    		// TODO Auto-generated method stub
	    		return rangeDetector.onTouchEvent(event);
	    	}
	    };
	    synchronized (rangeLock) {
		    graphView.setTitle("Market Depth (Range: +/- "+Double.valueOf(graphRange*100).intValue()+"%)");
		}
	    graphView.getGraphViewStyle().setGridColor(Color.DKGRAY);
	    graphView.getGraphViewStyle().setHorizontalLabelsColor(Color.GRAY);
	    graphView.getGraphViewStyle().setVerticalLabelsColor(Color.GRAY);
	    graphView.getGraphViewStyle().setVerticalLabelsWidth(50);
	    graphView.getGraphViewStyle().setTextSize(getResources().getDimension(R.dimen.graph_textsize));
	    graphView.getGraphViewStyle().setNumHorizontalLabels(5);
	    graphView.getGraphViewStyle().setNumVerticalLabels(5);

		((LinearLayout) rootView.findViewById(R.id.graphbox)).addView(graphView);
		
		if (entries == null){
			entries = new ArrayList<String>(Arrays.asList(ExchangeDataParser.getExchangesWithOrders()));
			entries.add(getString(R.string.graph_total));
		}
		
		return rootView;
	}

	@Override 
	public void onResume(){
		super.onResume();

		DataContainer.registerRefreshable(this);
		
		listView = createDrawerList();
		((MainActivity)getActivity()).setDrawerSublist(FRAGMENT_TAG, listView, true);
		
		updateGraph(true);
	}
	
	@Override 
	public void onPause(){
		super.onPause();
		
		DataContainer.unregisterRefreshable(this);
		
		((MainActivity)getActivity()).setDrawerSublist(FRAGMENT_TAG, listView, false);
	}
	

	public void refresh(Object lastData){
		updateGraph(true);
	}

    @Override
    public Set<Class<?>> continuousRequirements() {
        Set<Class<?>> currentRequirements = new HashSet<Class<?>>(baseRequirements);

        if (entries != null && graphExchanges != null)
            for (int index = 0; index < entries.size()-1; index++)
                if (graphExchanges.get(index))
                    currentRequirements.add(ExchangeDataParser.exchangeClassMap.get(entries.get(index)));

        return currentRequirements;
    }

    private class Updater extends AsyncTask<Void, Void, GraphViewSeries[]>{
		private final SparseBooleanArray graphExchanges;
		private final Resources res;
        private final boolean reCalculateGraphs;
		private Float priceInBtc, volumeInBtc;
		
		public Updater(SparseBooleanArray graphExchanges, Resources res, boolean reCalculateGraphs){
			this.res = res;
			this.graphExchanges = graphExchanges;
            this.reCalculateGraphs = reCalculateGraphs;
		}
		
		@Override
		protected GraphViewSeries[] doInBackground(Void... arg0) {

			priceInBtc = 0f;
            volumeInBtc = 0f;
			int activeExchanges = 0;

			//calculate median
			for (int i=0; i<ExchangeDataParser.getExchangesWithOrders().length; i++){
				String exchange = ExchangeDataParser.getExchangesWithOrders()[i];
				if (DataContainer.getExchangeTickerData(exchange) != null && graphExchanges.get(i)){
                    Float exchangeVolume = DataContainer.getExchangeTickerData(exchange).getVolume()+0.001f;
					priceInBtc += DataContainer.getExchangeTickerData(exchange).getPrice()*exchangeVolume;
                    volumeInBtc += exchangeVolume;
					activeExchanges ++;
				}
			}

			if (activeExchanges > 0)
				priceInBtc /= volumeInBtc;


            if (!reCalculateGraphs)
                return null;

			
			List<GraphViewSeries> ret = new ArrayList<GraphViewSeries>();
			List<GraphPoint> totalBuy = new ArrayList<GraphPoint>();
			List<GraphPoint> totalSell = new ArrayList<GraphPoint>();

            double currentEpsilon = REDUCTIONEPSILON;
            synchronized (rangeLock){
                if (graphRange < 0.03)
                    currentEpsilon = 0;
                else
                    currentEpsilon *= graphRange;
            }

            String total = res.getString(R.string.graph_total);
            int totalIndex = entries.indexOf(total);

			for (String exchange: ExchangeDataParser.getExchangesWithOrders()){
				if (DataContainer.getExchangeOrderData(exchange) == null)
					continue;
				
				int index = Arrays.asList(ExchangeDataParser.getExchangesWithOrders()).indexOf(exchange);
	        	
	        	if (graphExchanges.get(index)){
	        		if (graphExchanges.get(totalIndex)){
		        		totalBuy.addAll(DataContainer.getExchangeOrderData(exchange).getBuyData());
		        		totalSell.addAll(DataContainer.getExchangeOrderData(exchange).getSellData());
	        		}
			        ret.add(GraphHelper.getGraphViewSeries(DataContainer.getExchangeOrderData(exchange),res, currentEpsilon));
	        	}
			}

			if (graphExchanges.get(totalIndex) && !totalBuy.isEmpty() && !totalSell.isEmpty()){
				int color = res.getColor(res.getIdentifier("graph_colour_total", "color", MainActivity.PACKAGE_NAME));
				Collections.sort(totalBuy);
				Collections.sort(totalSell);



                List<GraphPoint> totalList = GraphPoint.createMarketDepthGraphPoints(totalBuy, totalSell);

                if (currentEpsilon > 0)
                    totalList = GraphHelper.reduce(totalList, currentEpsilon);

				GraphViewSeries s = new GraphViewSeries(total, new GraphViewSeriesStyle(color, 1), GraphHelper.GraphPoint.toGraphViewDataArray(totalList));
				ret.add(s);
	    	}
			
			return ret.toArray(new GraphViewSeries[ret.size()]);
		}
		
		protected void onPostExecute(GraphViewSeries[] series) {

            //modify sublist
            try {
                for (int index = 0; index < entries.size(); index++) {
                    int color;
                    if (index == entries.size() - 1)
                        color = res.getColor(res.getIdentifier("graph_colour_total", "color", MainActivity.PACKAGE_NAME));
                    else
                        color = res.getColor(res.getIdentifier("graph_colour_" + ExchangeDataParser.getExchangesWithOrders()[index], "color", MainActivity.PACKAGE_NAME));

                    GridView sublist = (GridView) ((MainActivity) getActivity()).getSublist(FRAGMENT_TAG);

                    if (graphExchanges.get(index) && sublist.getChildAt(index) != null)
                        ((TextView) sublist.getChildAt(index)).setTextColor(color);
                    else if (sublist.getChildAt(index) != null)
                        ((TextView) sublist.getChildAt(index)).setTextColor(res.getColor(R.color.graph_colour_unselected));
                }
            }
            catch (Exception e){

            }

            //modify range
            synchronized (rangeLock){
                graphView.setViewPort(priceInBtc*(1-graphRange), 2*priceInBtc*graphRange);
                graphView.setTitle("Market Depth (Range: +/- "+Double.valueOf(graphRange*100).intValue()+"%)");
            }

            if (reCalculateGraphs) {
                graphView.removeAllSeries();

                for (GraphViewSeries s : series)
                    graphView.addSeries(s);
            }
            graphView.redrawAll();
	     }
	}
	
	private GridView createDrawerList(){
		GridView gridView = new GridView(getActivity()){
		    @Override
		    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
	            int expandSpec = MeasureSpec.makeMeasureSpec(MEASURED_SIZE_MASK,
	                    MeasureSpec.AT_MOST);
	            super.onMeasure(widthMeasureSpec, expandSpec);

	            ViewGroup.LayoutParams params = getLayoutParams();
	            params.height = getMeasuredHeight();
		    }
		};
		
		gridView.setNumColumns(1);
		
		gridView.setAdapter(new ArrayAdapter<String>(getActivity(), R.layout.graph_fragment_entry, entries.toArray(new String[entries.size()])));
		gridView.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {

                boolean value = !graphExchanges.get(position);
                graphExchanges.put(position, value);

                SharedPreferences.Editor prefsEd = getActivity().getSharedPreferences(PREFS_NAME, 0).edit();
                prefsEd.putBoolean(entries.get(position), value);
                prefsEd.commit();

                updateGraph(true);
			}
		});
		
		if (graphExchanges == null){
            SharedPreferences pref = rootView.getContext().getSharedPreferences(PREFS_NAME, 0);

			graphExchanges = new SparseBooleanArray();
			for (int i = 0; i < entries.size(); i++){
                boolean value = pref.getBoolean(entries.get(i), true);
			    gridView.setItemChecked(i, value);
			    graphExchanges.put(i, value);
			}
		}
		else{
			for (int i = 0; i < entries.size(); i++)
			    gridView.setItemChecked(i, graphExchanges.get(i));
		}
			
		
		return gridView;
	}
	

	
	public View getDrawerSubList(){
		return listView;
	}
	
	public void updateGraph(boolean reCalculateGraphs){
		new Updater(graphExchanges.clone(), getResources(), reCalculateGraphs).executeOnExecutor(graphExecutor);
	}

	@Override
	public void updateSublist(MainActivity activity) {
		listView = createDrawerList();
		activity.setDrawerSublist(FRAGMENT_TAG, listView, true);
	}
}
