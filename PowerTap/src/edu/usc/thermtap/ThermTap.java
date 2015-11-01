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

 
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.logging.Level;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import edu.usc.powertap.entities.Phone;
import edu.usc.powertap.entities.PidNode;
import edu.usc.powertap.entities.UidNode;
import edu.usc.powertap.utils.Tool;
import edu.usc.powertap.utils.UserProperty;
import edu.usc.powertap.utils.UserProperty.componentNames;

public class ThermTap
{
	public static final int samplingInterval = 1000; //powertap sampling interval length in milliseconds 
	public static final int communicationInterval = 2000; //the interval powertap prepare data for therminator
	
	private static Level loggingLevel = Level.SEVERE;
	private static String outputFileAddr = "output.txt";
	private static double runTime = 10;	//10 seconds by default
	private static PowerTap powerTap;
	public static CommunicatorInterface communicator;
	private static HashMap<String,Integer> therminatorComponents= new HashMap<String,Integer>();
	public static String therminatorOutput = null;
	static UserInterface ui;
	private static String packageSpecFile = "package_N5.xml";
	

	public enum Actions{CONNECT, START, STOP, DISCONNECT};

	public static void main(String[] args){
		powerTap = new PowerTap();
		ui = new UserInterface(powerTap);
	}

	public static Phone getPhone(){
		return powerTap.getPhone();
	}
	
	public static String getPackageSpecFile() {
		return packageSpecFile;
	}

	public static Level getLoggingLevel(){
		return loggingLevel;
	}

	public static String getOutputFileAddr(){
		return new String(outputFileAddr);
	}

	public static double getRunTime(){
		return runTime;
	}
	
	

	
	public static void initTherminatorComponents(String fileName){
		packageSpecFile = fileName;
		try {
			SAXReader reader = new SAXReader();
	        Document document = reader.read(new File(fileName));
	        
	        Element root = document.getRootElement();
	        for (Iterator i = root.element("components").elementIterator("component") ; i.hasNext(); ) {
	        	Element comp = (Element) i.next();
	        	therminatorComponents.put(comp.attributeValue("name"),
	        			Integer.parseInt(comp.element("resolution").elementText("height")));
	        }
	        
		} catch (DocumentException e) {
			System.err.println("Tried to read therminator output but no output is there!");
			e.printStackTrace();
		}
	}
	
	public static HashMap<String , Integer> getTherminatorComponents(){
		return therminatorComponents;
	}
	
	synchronized public static void loadTherminatorOutput(String name){
		Scanner sc;
		therminatorOutput = "";
		try {
			sc = new Scanner(new File(name));
			while(sc.hasNext()){
				therminatorOutput = therminatorOutput.concat(sc.nextLine());
				therminatorOutput = therminatorOutput.concat("\n");
			}
		} catch (FileNotFoundException e) {
			System.err.println("Tried to read therminator output but no output is there!");
			e.printStackTrace();
		}
	}
	
	synchronized public static double[][][] parseTherminatorOutput(String name, int layer){
		ArrayList<ArrayList<Double>> data = new ArrayList<ArrayList<Double>>();
		ArrayList<Double> temp;
//		Vector<double[][]> tempValues = new Vector<double[][]>();
		Scanner sc = null;
		String line;
		String[] tempdata;
		boolean flag = false;
		
		sc = new Scanner(therminatorOutput);
		
		while(sc.hasNext()){
			line = sc.nextLine();
			if(line.contains(name)){ // component name
				flag = true;
			}if(line.contains("z=")){
				if(flag){
					if(layer == Integer.parseInt(line.substring(line.indexOf('=')+1, line.length()))){
						line = sc.nextLine();
						while(sc.hasNext() && !line.contains("=") && !line.contains(":")){
							tempdata = line.split("\t");
							temp = new ArrayList<Double>();
							for(int i = 0 ; i < tempdata.length ; i++){
								temp.add(Double.parseDouble(tempdata[i]));
							}
							data.add(temp);
							line = sc.nextLine();
						}
						break;
					}
					
				}
			}else{	// data
				continue;
			}
		}
		
		double [][][] dataArray = new double[1][data.size()][data.get(0).size()];
		for (int i = 0; i < data.size(); i++) {
			for (int j = 0; j < data.get(0).size(); j++) {
				dataArray[0][i][j] = data.get(i).get(j);
			}
		}
		sc.close();
		return dataArray;
	}
	
	public static String[] getComponentNames(){
		String[] names = new String[UserProperty.numOfComponent-3];
		int i = 0;
		for (componentNames comp:componentNames.values()) {
			if(comp.name().contains("GPS") || comp.name().contains("LTE") || comp.name().contains("DRAM"))
				continue;
			if(comp.name().contains("idle"))
				names[i] = new String("Other");
			else
				names[i] = comp.name();
			i++;
		}
		
		return names;
	}
	
	public static double[][] getPowerTrace(int uid, int pid){
		Phone phone = powerTap.getPhone();
		
		CircularFifoQueue<ArrayList<Double>> movingEnergy ;
		CircularFifoQueue<Double> movingStartTimes ;
		CircularFifoQueue<Double> movingEndTimes ;
		
		UidNode uidNode = phone.getUidMap().get(uid);
		PidNode pidNode = null;
		if(uidNode != null){
			for (PidNode node : uidNode.getPidList()) {
				if(node.getPid() == pid)
					pidNode = node;
			}
		}
		
		if(pid != -1 && pidNode != null){
			movingEnergy = pidNode.getMovingEnergy();
			movingStartTimes = pidNode.getMovingStartTimes();
			movingEndTimes = pidNode.getMovingEndTimes();
		}else if(uid != -1 && uidNode != null){
			movingEnergy = uidNode.getMovingEnergy();
			movingStartTimes = uidNode.getMovingStartTimes();
			movingEndTimes = uidNode.getMovingEndTimes();
		}else{
			movingEnergy = phone.getMovingEnergy();
			movingStartTimes = phone.getMovingStartTimes();
			movingEndTimes = phone.getMovingEndTimes();
		}
		
		
		double[][] trace = new double[2][movingEndTimes.size()*2];
		
		for (int i = 0; i < trace[0].length; i+=2) {
			trace[0][i] = movingStartTimes.get(i/2);
			trace[1][i] = trace[1][i+1] = Tool.getSumOfArray(movingEnergy.get(i/2))/(movingEndTimes.get(i/2)-movingStartTimes.get(i/2)) * 1.08;
			trace[0][i+1] = movingEndTimes.get(i/2);
		}
		return trace;
	}
	
	public static void refreshPowerCharts(){
		ui.refreshPowerCharts();
	}
	
	public static void printStats(int samplinguid, int samplingpid){
		Phone phone = powerTap.getPhone();
		PrintStream ps;
		try {
			ps = new PrintStream(new File("trace"));
	
			ps.println("Snapdragon_800 DRAM	Display	Battery	Transceiver	eMMC	WiFi AudioCODEC Main_Board PMIC");
			
			UidNode uidNode = ThermTap.getPhone().getUidMap().get(samplinguid);
			PidNode pidNode = null;
			if(uidNode != null){
				for (PidNode node : uidNode.getPidList()) {
					if(node.getPid() == samplingpid)
						pidNode = node;
				}
			}
			
			if(samplingpid != -1 && pidNode != null){
				ps.print((pidNode.getInstPower()[componentNames.CPU.ordinal()]+pidNode.getInstPower()[componentNames.GPU.ordinal()]) + "\t");
				ps.print(0.0 + "\t");
				ps.print(pidNode.getInstPower()[componentNames.Display.ordinal()] + "\t");
				ps.print(0.0 + "\t");
				ps.print(pidNode.getInstPower()[componentNames.LTE.ordinal()] + "\t");
				ps.print(pidNode.getInstPower()[componentNames.Disk.ordinal()] + "\t");
				ps.print(pidNode.getInstPower()[componentNames.WIFI.ordinal()] + "\t");
				ps.print(0.0 + "\t");
				ps.print(pidNode.getInstPower()[componentNames.idle.ordinal()] + "\t");
				ps.print(0.0 + "\t");
			}else if(samplinguid != -1 && uidNode != null){
				ps.print((uidNode.getInstPower()[componentNames.CPU.ordinal()]+uidNode.getInstPower()[componentNames.GPU.ordinal()]) + "\t");
				ps.print(0.0 + "\t");
				ps.print(uidNode.getInstPower()[componentNames.Display.ordinal()] + "\t");
				ps.print(0.0 + "\t");
				ps.print(uidNode.getInstPower()[componentNames.LTE.ordinal()] + "\t");
				ps.print(uidNode.getInstPower()[componentNames.Disk.ordinal()] + "\t");
				ps.print(uidNode.getInstPower()[componentNames.WIFI.ordinal()] + "\t");
				ps.print(0.0 + "\t");
				ps.print(uidNode.getInstPower()[componentNames.idle.ordinal()] + "\t");
				ps.print(0.0 + "\t");
			}else{
				ps.print((phone.getInstPower()[componentNames.CPU.ordinal()]+phone.getInstPower()[componentNames.GPU.ordinal()]+phone.getInstPower()[componentNames.idle.ordinal()]/2.0) + "\t");
				ps.print(0.0 + "\t");
				ps.print(phone.getInstPower()[componentNames.Display.ordinal()] + "\t");
				ps.print(0.0 + "\t");
				ps.print(phone.getInstPower()[componentNames.LTE.ordinal()] + "\t");
				ps.print(phone.getInstPower()[componentNames.Disk.ordinal()] + "\t");
				ps.print(phone.getInstPower()[componentNames.WIFI.ordinal()] + "\t");
				ps.print(0.0 + "\t");
				ps.print(phone.getInstPower()[componentNames.idle.ordinal()]/2.0 + "\t");
				ps.print(0.0 + "\t");
			}
			
			
			ps.flush();
			ps.close();
			
			ps = new PrintStream(new File("next"));
			ps.print(32423);
			ps.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public static void printStatsInitialization(int samplinguid, int samplingpid){
		Phone phone = powerTap.getPhone();
		PrintStream ps;
		try {
			ps = new PrintStream(new File("trace"));
	
			ps.println("Snapdragon_800 DRAM	Display	Battery	Transceiver	eMMC	WiFi AudioCODEC Main_Board PMIC");
			
			UidNode uidNode = ThermTap.getPhone().getUidMap().get(samplinguid);
			PidNode pidNode = null;
			if(uidNode != null){
				for (PidNode node : uidNode.getPidList()) {
					if(node.getPid() == samplingpid)
						pidNode = node;
				}
			}
			
			if(samplingpid != -1 && pidNode != null){
				ps.print((pidNode.getAvgPower()[componentNames.CPU.ordinal()]+pidNode.getAvgPower()[componentNames.GPU.ordinal()]) + "\t");
				ps.print(0 + "\t");
				ps.print(pidNode.getAvgPower()[componentNames.Display.ordinal()] + "\t");
				ps.print(0 + "\t");
				ps.print(pidNode.getAvgPower()[componentNames.LTE.ordinal()] + "\t");
				ps.print(pidNode.getAvgPower()[componentNames.Disk.ordinal()] + "\t");
				ps.print(pidNode.getAvgPower()[componentNames.WIFI.ordinal()] + "\t");
				ps.print(0 + "\t");
				ps.print(pidNode.getAvgPower()[componentNames.idle.ordinal()] + "\t");
				ps.print(0 + "\t");
			}else if(samplinguid != -1 && uidNode != null){
				ps.print((uidNode.getAvgPower()[componentNames.CPU.ordinal()]+uidNode.getAvgPower()[componentNames.GPU.ordinal()]) + "\t");
				ps.print(0 + "\t");
				ps.print(phone.getAvgPower()[componentNames.Display.ordinal()] + "\t");
				ps.print(0 + "\t");
				ps.print(uidNode.getAvgPower()[componentNames.LTE.ordinal()] + "\t");
				ps.print(uidNode.getAvgPower()[componentNames.Disk.ordinal()] + "\t");
				ps.print(uidNode.getAvgPower()[componentNames.WIFI.ordinal()] + "\t");
				ps.print(0 + "\t");
				ps.print(uidNode.getAvgPower()[componentNames.idle.ordinal()] + "\t");
				ps.print(0 + "\t");
			}else{
				ps.print((phone.getAvgPower()[componentNames.CPU.ordinal()]+phone.getAvgPower()[componentNames.GPU.ordinal()]+phone.getAvgPower()[componentNames.idle.ordinal()]/2.0) + "\t");
				ps.print(0 + "\t");
				ps.print(phone.getAvgPower()[componentNames.Display.ordinal()] + "\t");
				ps.print(0 + "\t");
				ps.print(phone.getAvgPower()[componentNames.LTE.ordinal()] + "\t");
				ps.print(phone.getAvgPower()[componentNames.Disk.ordinal()] + "\t");
				ps.print(phone.getAvgPower()[componentNames.WIFI.ordinal()] + "\t");
				ps.print(0 + "\t");
				ps.print(phone.getAvgPower()[componentNames.idle.ordinal()]/2.0 + "\t");
				ps.print(0 + "\t");
			}
			
			ps.flush();
			ps.close();
			
			ps = new PrintStream(new File("init_power"));
			ps.print(32423);
			ps.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
