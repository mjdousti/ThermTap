/*
 *
 * Copyright (C) 2015 Mohammad Javad Dousti, Majid Ghasemi-Gol, Mahdi Nazemi, 
 *    and Massoud Pedram, SPORT lab, University of Southern California. All 
 *    rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 *
 */
package edu.usc.powertap.powermodel;

import java.util.HashMap;




public class GPUPower {
	@SuppressWarnings("serial")
	static final HashMap<Integer, Double> gpuActiveBetas  = new HashMap<Integer, Double>(){{
		put(0,1.180961039);
		put(1,1.246973684);
		put(2,1.065633803);
		put(3,0.99472973);
		put(4,0.887101449);
		put(5,0.914603175);
		put(6,0.338043478);
	}}; 

	@SuppressWarnings("serial")
	static final HashMap<Integer, Double> gpuIdleBetas  = new HashMap<Integer, Double>(){{
		put(0,0.432178961);
		put(1,0.343226316);
		put(2,0.364466197);
		put(3,0.50857027);
		put(4,0.502798551);
		put(5,0.393996825);
		put(6,0.440356522);
	}}; 
		
	private static int pid = -1;
	private static double util = 0;
	private static boolean clockGated = true;
	private static int pwrlevel;
	
	public static void setPwrlevel(int pwrlevel) {
		GPUPower.pwrlevel = pwrlevel;
	}
	
	
	public static void setClockGated(boolean b){
		clockGated = b;
	}
	
	public static void setCurrentPid(int pidNew){
		pid = pidNew;
	}
	
	
	public static void setUtilization(double utilNew){
		util = utilNew;
	}
	
	public static double getActivePower(){
		double power = 0;
		if(!clockGated)
			if(gpuActiveBetas.containsKey(pwrlevel)){
//				System.out.println("gpu power requested, freq: "+freq+ " power: "+gpuActiveBetas.get(freq));
				power = gpuActiveBetas.get(pwrlevel)*util;
			}else{
				System.err.println("GPU power level "+pwrlevel+" not found!");
				System.exit(-1);
			}
		if(power < 0)
			System.out.println("gpu active power got negative!! "+ power);
		if(Math.abs(power) > 100)
			System.out.println("gpu active power got so big!! "+ power);
//		System.out.println("gpu power requested, but gpu is power gated!");
		return power;
	}
	
	public static double getIdlePower(){
		double power = 0 ;
		if(!clockGated)
			if(gpuIdleBetas.containsKey(pwrlevel))
				power = gpuIdleBetas.get(pwrlevel);
			else{
				System.err.println("GPU power level "+pwrlevel+" not found!");
				System.exit(-1);
			}
		if(power < 0)
			System.out.println("gpu idle  power got negative!! "+ power);
		if(Math.abs(power) > 100)
			System.out.println("gpu idle power got so big!! "+ power);
		return power;
	}
	
	public static double getTotalPower(){
		double power = getActivePower() + getIdlePower();
		return power;
	}
	
	public static int getPid() {
		return pid;
	}
}
