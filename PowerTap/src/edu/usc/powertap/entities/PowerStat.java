/*
 *
 * Copyright (C) 2015 Mohammad Javad Dousti, Majid Ghasemi-Gol, Mahdi Nazemi, 
 *    and Massoud Pedram, SPORT lab, University of Southern California. All 
 *    rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 *
 */
package edu.usc.powertap.entities;

import java.util.ArrayList;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import edu.usc.powertap.utils.Tool;
import edu.usc.powertap.utils.UserProperty;


abstract class PowerStat{
	final int numOfComponent = UserProperty.numOfComponent;
	final int windowSize = 8;
	
	CircularFifoQueue<ArrayList<Double>> movingEnergy;
	CircularFifoQueue<Double> movingStartTimes;
	CircularFifoQueue<Double> movingEndTimes;
	
	private double [] maxPower = new double[numOfComponent];
	private double [] minPower = new double[numOfComponent];
	
	public ArrayList<Double> intervalEnergy = new ArrayList<Double>(numOfComponent);

	private ArrayList<UsageNode> usageList = new ArrayList<UsageNode>();

	public PowerStat(){
		setUsageList(new ArrayList<UsageNode>());
		for (int i = 0; i < numOfComponent; i++) {
			intervalEnergy.add(0.0);
		}
		
		movingEnergy = new CircularFifoQueue<ArrayList<Double>>(windowSize);
		movingStartTimes = new CircularFifoQueue<Double>(windowSize);
		movingEndTimes = new CircularFifoQueue<Double>(windowSize);
	}
	
	public void updateMovingEnergy(double startTime, double endTime){
		movingEnergy.add(intervalEnergy);
		movingStartTimes.add(startTime);
		movingEndTimes.add(endTime);
	}
	
	public synchronized void resetIntervalEnergy(){
		intervalEnergy = new ArrayList<Double>(numOfComponent);
		for (int i = 0; i < numOfComponent; i++) {
			intervalEnergy.add(0.0);
		}
	}
	
	public synchronized double getComponentIntervalEnergy(int i)
	{
		return (double)intervalEnergy.get(i);
	}
	
	public synchronized double getTotalIntervalEnergy(){
		return Tool.getSumOfArray(intervalEnergy);
	}
	
	
	public synchronized void addIntervalEnergy(int index, double e){
		intervalEnergy.set(index, intervalEnergy.get(index) + e );
	}



	void addUsage(UsageNode _usage){
		usageList.add(_usage);
	}

	void reset(){
		intervalEnergy.clear();
		for(int i=0 ; i<minPower.length ; i++){
			intervalEnergy.add(0.0);
			maxPower[i] = minPower[i]= 0;
		}
		movingEnergy.clear();
		movingStartTimes.clear();
		movingEndTimes.clear();
		setUsageList(new ArrayList<UsageNode>());
	}
	
	public ArrayList<UsageNode> getUsageList(){
		return usageList;
	}

	public void setUsageList(ArrayList<UsageNode> usageList){
		this.usageList = usageList;
	}

	public double [] getMaxPower(){
		return maxPower;
	}
	
	public double getTotalMaxPower(){
		return Tool.getSumOfArray(maxPower);
	}

	public void setMaxPower(double[]  power){
		for(int i=0 ; i<minPower.length ; i++){
			maxPower[i] = Math.max(maxPower[i], power[i]);
		}
	}
	
	public void setMaxPower(int index, double power){
		maxPower[index] = Math.max(maxPower[index], power);
	}

	public double [] getMinPower(){
		return minPower;
	}

	public void setMinPower(double [] power){
		for(int i=0 ; i<minPower.length ; i++){
			if (minPower[i]==0)
				minPower[i] = power[i];
			else
				minPower[i] = Math.min(minPower[i] , power[i]);
		}
	}
	
	public void setMinPower(int index, double power){
		if (minPower[index]==0)
			minPower[index] = power;
		else
			minPower[index] = Math.min(minPower[index] , power);
	}

	public double [] getAvgPower(){
		double[] avgPower = new double[UserProperty.numOfComponent];
		
		if(movingEnergy.size() == 0)
			return avgPower;
		
		double start = movingStartTimes.get(0);
		double end = movingEndTimes.get(movingEndTimes.size()-1);
		
		for (int i = 0; i < avgPower.length; i++) {
			for (int j = 0; j < movingEnergy.size() ; j++) {
				avgPower[i] += movingEnergy.get(j).get(i);
			}
			avgPower[i] /= (end - start);
		}
		return avgPower;
	}
	
	public double [] getInstPower(){
		double[] instPower = new double[UserProperty.numOfComponent];
		
		if(movingEnergy.size() == 0)
			return instPower;
		
		double start = movingStartTimes.get(movingEndTimes.size()-1);
		double end = movingEndTimes.get(movingEndTimes.size()-1);
		
		for (int i = 0; i < instPower.length; i++) {
			instPower[i] += movingEnergy.get(movingEndTimes.size()-1).get(i);
			instPower[i] /= (end - start);
		}
		return instPower;
	}
	
	public double getTotalEnergy(){
		double temp = 0;
		for (int i = 0; i < movingEnergy.size() ; i++) {
			for (int j = 0; j < UserProperty.numOfComponent; j++) {
				temp += movingEnergy.get(i).get(j);
			}
		}
		
		return temp;
	}
	
	public double getComponentEnergy(int c){
		double temp = 0;
		for (int i = 0; i < movingEnergy.size() ; i++) {
			temp += movingEnergy.get(i).get(c);
		}
		
		return temp;
	}
	
	public CircularFifoQueue<ArrayList<Double>> getMovingEnergy() {
		return movingEnergy;
	}
	
	public CircularFifoQueue<Double> getMovingStartTimes() {
		return movingStartTimes;
	}
	
	public CircularFifoQueue<Double> getMovingEndTimes() {
		return movingEndTimes;
	}
}
