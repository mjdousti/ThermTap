/*
 *
 * Copyright (C) 2015 Mohammad Javad Dousti, Majid Ghasemi-Gol, Mahdi Nazemi, 
 *    and Massoud Pedram, SPORT lab, University of Southern California. All 
 *    rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 *
 */
package edu.usc.thermtap;


import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Paint;
import java.text.DecimalFormat;
import java.util.Vector;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.util.Rotation;
import org.jzy3d.chart.Chart;
import org.jzy3d.chart.controllers.mouse.camera.CameraMouseController;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.colors.colormaps.ColorMapRainbow;
import org.jzy3d.maths.Range;
import org.jzy3d.plot3d.builder.Builder;
import org.jzy3d.plot3d.builder.Mapper;
import org.jzy3d.plot3d.builder.concrete.OrthonormalGrid;
import org.jzy3d.plot3d.primitives.Shape;
import org.jzy3d.plot3d.rendering.legends.colorbars.ColorbarLegend;


public class ChartManager {
	
	public static Component getPieChart(double [] data, String[] names) {
		PieDataset dataset = createDataset(data, names);
		JFreeChart chart = createChart(dataset, "Chart");
		chart.setBackgroundPaint(new java.awt.Color(0, 0, 0, 0));
		
		chart.setTextAntiAlias(true);
		Font font = new Font("TimesRoman", Font.BOLD, 15);
		chart.getLegend().setItemFont(font);
		ChartPanel chartPanel = new ChartPanel(chart);
		
		chartPanel.setPreferredSize(new Dimension(400, 400));
		return chartPanel;
	}
	
	public static Component getXYChart(double [] x, double [] y){
		
		XYSeries series = new XYSeries("");
		for (int i = 0; i < y.length; i++) {
			series.add(x[i], y[i]);
		}

        XYSeriesCollection data = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(
            "",
            "Time (s)", 
            "Power (W)", 
            data,
            PlotOrientation.VERTICAL,
            false,
            false,
            false
        );
        
        chart.setBackgroundPaint(new java.awt.Color(0, 0, 0, 0));
       
        final XYPlot plot1 = (XYPlot) chart.getPlot();
        NumberAxis rangeAxis = (NumberAxis) plot1.getRangeAxis();
        NumberAxis domainAxis = (NumberAxis) plot1.getDomainAxis();
        
		Font font = new Font("TimesRoman", Font.BOLD, 16);

        rangeAxis.setLabelFont(font);
        domainAxis.setLabelFont(font);
        
        
        domainAxis.setVerticalTickLabels(true); 
        
        DecimalFormat df = new DecimalFormat("0.00");
        rangeAxis.setNumberFormatOverride(df);
        rangeAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits ());
        
        domainAxis.setNumberFormatOverride(df);
        domainAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits ());
        
        
        
        rangeAxis.setTickUnit (new NumberTickUnit(rangeAxis.getRange().getLength()/10, df, 0));
        domainAxis.setTickUnit (new NumberTickUnit(domainAxis.getRange().getLength()/15, df, 0));
        
        
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.getRenderer().setSeriesPaint(0, (Paint)java.awt.Color.blue);
        plot.setBackgroundAlpha(0.2f);
        plot.setBackgroundPaint((Paint)java.awt.Color.black);
        plot.setOutlinePaint((Paint)java.awt.Color.black);
        
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(500, 270));
        return chartPanel;
	}


    public static Chart getThermalChart(double [][][] data, double w, double l) {
        // Define a function to plot

        // Define range and precision for the function to plot
    	double width = w * 100.0;
    	double length = l * 100.0;
    	
    	
    	
    	if(data == null)
    		return new Chart() {
		};
		
		
       
        int xSteps = 2*data[0][0].length;
        int ySteps = 2*data[0].length;
        Range xRange = new Range(0, width - 1e-5);
        Range yRange = new Range(0, length - 1e-5);
        float maxZ = 0 , minZ = -1;
        Chart chart = new Chart("awt");

        // Create the object to represent the function over the given range.
        Vector<Shape> surfaces = new Vector<Shape>();
        for(int i = 0 ; i<data.length ; i++)
        	surfaces.add((Shape) Builder.buildOrthonormal(new OrthonormalGrid(xRange, xSteps, yRange, ySteps), new MyMapper(data[i],width,length)));

        for (int i = 0; i < data.length; i++) {
			if(minZ < 0 || minZ > surfaces.get(i).getBounds().getZmin())
				minZ = surfaces.get(i).getBounds().getZmin();
			if(maxZ < surfaces.get(i).getBounds().getZmax())
				maxZ = surfaces.get(i).getBounds().getZmax();
		}

        chart.addController(new CameraMouseController());
        
        
        for(int i = 0 ; i<surfaces.size() ; i++){
        	surfaces.get(i).setColorMapper(new ColorMapper(new ColorMapRainbow(), minZ
        							, maxZ, new Color(1, 1, 1, .5f)));
        	
        	surfaces.get(i).setWireframeDisplayed(false);
      	surfaces.get(i).setWireframeColor(Color.BLACK);
        	surfaces.get(i).setFaceDisplayed(true);
        	
        	chart.getScene().getGraph().add(surfaces.get(i));
        }
        
        ColorbarLegend cbar = new ColorbarLegend(surfaces.get(0), chart.getView().getAxe().getLayout());
        surfaces.get(0).setLegend(cbar);

        return chart;
    }
    
    static void disposeThermalChart(Chart chart){
    	if(chart != null)
    		chart.dispose();
    }
    
    private  static PieDataset createDataset(double [] data, String[] names) {
        DefaultPieDataset result = new DefaultPieDataset();
        if(data.length != names.length)
        	System.err.println("pie chart, data and names should have the same length");
        for (int i = 0; i < names.length; i++) {
        	result.setValue(names[i], data[i]);
		}
        
        return result;
        
    }
    
    private static JFreeChart createChart(PieDataset dataset, String title) {
        
        JFreeChart chart = ChartFactory.createPieChart("",          // chart title
            dataset,                // data
            true,                   // include legend
            true,
            true);

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setDirection(Rotation.CLOCKWISE);
        plot.setLabelGenerator(null);
        plot.setForegroundAlpha(1);
        return chart;
        
    }
}

class MyMapper extends Mapper{
	double [][] data;
	double w, l;
	
	MyMapper(double [][] d, double w, double l){
		data = d;
		this.w  = w;
		this.l = l;
	}

	@Override
	public double f(double x, double y) {
    	int intX = (int)Math.floor((x*data[0].length)/w);
    	int intY = (int)Math.floor((y*data.length)/l);
    	if(intY >=0 && intY < data.length)
    		if(intX >= 0 && intX < data[intY].length)
    			return data[intY][intX];
    	return 0;
	}
	
}