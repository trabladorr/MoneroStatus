package tr.monerostatus.app;

import android.content.res.Resources;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import tr.monerostatus.R;
import tr.monerostatus.jsondata.ExchangeDataParser;

public class GraphHelper {

    public static class GraphPoint implements Comparable<GraphPoint>{
        private final Double x;
        private final Double y;

        public GraphPoint(Double x, Double y){
            this.x = x;
            this.y = y;
        }

        public Double getX(){
            return x;
        }

        public Double getY(){
            return y;
        }

        public int compareTo(GraphPoint that) {
            return this.x.compareTo(that.x);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof GraphPoint))
                return false;
            GraphPoint that = (GraphPoint)o;
            return this.x.equals(that.x);
        }

        public GraphView.GraphViewData toGraphViewData(){
            return new GraphView.GraphViewData(x, y);
        }

        public static GraphView.GraphViewData[] toGraphViewDataArray(List<GraphPoint> list){
            List<GraphView.GraphViewData> ret = new ArrayList<GraphView.GraphViewData>();
            for (GraphPoint point : list)
                ret.add(new GraphView.GraphViewData(point.getX(),point.getY()));
            return ret.toArray(new GraphView.GraphViewData[ret.size()]);
        }

        public static GraphView.GraphViewData[] createMarketDepthGraphViewDatas(List<GraphPoint> buy, List<GraphPoint> sell){
            return toGraphViewDataArray(createMarketDepthGraphPoints(buy, sell));
        }

        public static List<GraphPoint> createMarketDepthGraphPoints(List<GraphPoint> buy, List<GraphPoint> sell){
            List<GraphPoint> buyList = new ArrayList<GraphPoint>();
            List<GraphPoint> sellList =  new ArrayList<GraphPoint>();

            Double prevY = Double.valueOf(0);

            for (int i = buy.size()-1; i>=0; i--){
                prevY += buy.get(i).y;
                buyList.add(new GraphPoint(buy.get(i).x, prevY));
            }
            prevY = Double.valueOf(0);

            for (int i = 0; i<sell.size(); i++){
                prevY += sell.get(i).y;
                sellList.add(new GraphPoint(sell.get(i).x, prevY));
            }
            Collections.reverse(buyList);
            if (!buyList.isEmpty() && !sellList.isEmpty()){
                if (buyList.get(buyList.size()-1).getX() < sellList.get(0).getX()){
                    buyList.add(new GraphPoint(buyList.get(buyList.size()-1).getX()+0.000001, 0d));
                    sellList.add(0, new GraphPoint(sellList.get(0).getX()-0.000001, 0d));
                }
                else
                    while(buyList.get(buyList.size()-1).getX() > sellList.get(0).getX()){
                        if (buyList.get(buyList.size()-1).getY() > sellList.get(0).getY())
                            sellList.remove(0);
                        else
                            buyList.remove(buyList.size()-1);
                    }

            }
            buyList.addAll(sellList);

            return buyList;
        }



        public static GraphPoint[] add(GraphPoint[] ... pairs){
            List<GraphPoint> ret = new ArrayList<GraphPoint>();
            for (GraphPoint[] data:pairs)
                ret.addAll(Arrays.asList(data));
            return (GraphPoint[])ret.toArray(new GraphPoint[ret.size()]);
        }

        @Override
        public String toString(){
            //return "("+x+","+y+")";
            return ""+x;
        }

    }

    public static class Line {

        private GraphPoint start;
        private GraphPoint end;

        private Double dx;
        private Double dy;
        private Double sxey;
        private Double exsy;
        private Double length;

        public Line(GraphPoint start, GraphPoint end) {
            this.start = start;
            this.end = end;
            dx = start.getX() - end.getX();
            dy = start.getY() - end.getY();
            sxey = start.getX() * end.getY();
            exsy = end.getX() * start.getY();
            length = Math.sqrt(dx*dx + dy*dy);
        }

        public List<GraphPoint> asList() {
            return Arrays.asList(start, end);
        }

        Double distance(GraphPoint p) {
            return Math.abs(dy * p.getX() - dx * p.getY() + sxey - exsy) / length;
        }
    }

    public static List<GraphPoint> reduce(List<GraphPoint> points, double epsilon) {
        if (epsilon < 0) {
            throw new IllegalArgumentException("Epsilon cannot be less then 0.");
        }
        Double furthestPointDistance = 0.0;
        int furthestPointIndex = 0;
        Line line = new Line(points.get(0), points.get(points.size() - 1));
        for (int i = 1; i < points.size() - 1; i++) {
            double distance = line.distance(points.get(i));
            if (distance > furthestPointDistance ) {
                furthestPointDistance = distance;
                furthestPointIndex = i;
            }
        }
        if (furthestPointDistance > epsilon) {
            List<GraphPoint> reduced1 = reduce(points.subList(0, furthestPointIndex+1), epsilon);
            List<GraphPoint> reduced2 = reduce(points.subList(furthestPointIndex, points.size()), epsilon);
            List<GraphPoint> result = new ArrayList<GraphPoint>(reduced1);
            result.addAll(reduced2.subList(1, reduced2.size()));
            return result;
        } else {
            return line.asList();
        }
    }

    public static GraphViewSeries getGraphViewSeries(ExchangeDataParser.ExchangeOrderData data, Resources res, Double reductionEpsilon) {
        return createGraphViewSeries(res, data.getExchange(), data.getBuyData(), data.getSellData(), reductionEpsilon);
    }

    protected static GraphViewSeries createGraphViewSeries(Resources res, String exchange, List<GraphPoint> buy, List<GraphPoint> sell, Double reductionEpsilon){
        String exchanges[] = res.getStringArray(R.array.exchanges);
        int index = Arrays.asList(ExchangeDataParser.exchangeCodes).indexOf(exchange);
        int color = res.getColor(res.getIdentifier("graph_colour_"+exchange, "color", MainActivity.PACKAGE_NAME));

        List<GraphPoint> graph = GraphPoint.createMarketDepthGraphPoints(buy, sell);

        if (reductionEpsilon > 0)
            graph = GraphHelper.reduce(graph, reductionEpsilon);

        return new GraphViewSeries(exchanges[index], new GraphViewSeries.GraphViewSeriesStyle(color, 1), GraphHelper.GraphPoint.toGraphViewDataArray(graph));
    };

}
