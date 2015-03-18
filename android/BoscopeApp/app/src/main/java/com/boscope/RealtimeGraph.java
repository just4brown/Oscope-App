/*
package com.boscope;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.widget.LinearLayout;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.widget.LinearLayout;

import com.jjoe64.graphview.BarGraphView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

public class RealtimeGraph extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graphs);

        // init example series data
        GraphViewSeries exampleSeries = new GraphViewSeries(new GraphViewData[] {
                new GraphViewData(1, Math.sin(1))
                , new GraphViewData(2, Math.sin(2))
                , new GraphViewData(2.5, Math.sin(2.5)) // another frequency
                , new GraphViewData(3, Math.sin(3))
                , new GraphViewData(4, Math.sin(4))
                , new GraphViewData(5, Math.sin(5))
        });

        // first init data
        // sin curve
        int num = 150;
        GraphViewData[] data = new GraphViewData[num];
        double v=0;
        for (int i=0; i<num; i++) {
            v += 0.2;
            data[i] = new GraphViewData(i, Math.sin(v));
        }
        GraphViewSeries seriesSin = new GraphViewSeries(data);

        // graph with dynamically genereated horizontal and vertical labels
        GraphView graphView;
        */
/*if (getIntent().getStringExtra("type").equals("bar")) {
            graphView = new BarGraphView(
                    this // context
                    , "GraphViewDemo" // heading
            );
            ((BarGraphView) graphView).setDrawValuesOnTop(true);
        } else {*//*

            graphView = new LineGraphView(
                    this // context
                    , "" // heading
            );
            //((LineGraphView) graphView).setDrawDataPoints(true);
            ((LineGraphView) graphView).setDataPointsRadius(15f);
//        }
        graphView.addSeries(seriesSin); // data

        LinearLayout layout = (LinearLayout) findViewById(R.id.graph1);
        layout.addView(graphView);

        // graph with custom labels and drawBackground
        */
/*if (getIntent().getStringExtra("type").equals("bar")) {
            graphView = new BarGraphView(
                    this
                    , "GraphViewDemo"
            );
            ((BarGraphView) graphView).setDrawValuesOnTop(true);
        } else {*//*


    }
}
*/
