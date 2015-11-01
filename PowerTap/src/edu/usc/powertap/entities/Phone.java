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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.usc.powertap.powermodel.CPUPower;
import edu.usc.powertap.powermodel.DRAMPower;
import edu.usc.powertap.powermodel.DiskPower;
import edu.usc.powertap.powermodel.DisplayPower;
import edu.usc.powertap.powermodel.GPUPower;
import edu.usc.powertap.powermodel.NetworkPower;
import edu.usc.powertap.utils.UserProperty;
import edu.usc.powertap.utils.UserProperty.componentNames;
import edu.usc.thermtap.PowerTap;

public class Phone extends Node{
	public int coreCount = 4;
	private HashMap<Integer, UidNode> uidMap;
	
	static double [][] phoneIdleBetas = {{300000e-6, 0.27394157},
								{422400e-6, 0.2943105865},
								{652800e-6, 0.3119558172},
								{729600e-6, 0.3343751909},
								{883200e-6, 0.3399991025},
								{960000e-6, 0.4944666876},
								{1036800e-6, 0.5050272224},
								{1190400e-6, 0.5054845187},
								{1267200e-6, 0.5877738631},
								{1497600e-6, 0.6037092463},
								{1574400e-6, 0.6423621954},
								{1728000e-6, 0.648497545},
								{1958400e-6, 0.6562274214},
								{2265600e-6, 0.6704929921}};
	
//	private double energy;
	

//	private double phoneIdlePower =  0.3; //=2.345; 2.065 is the total idle power; 0.37 is due to usb adb serial port ;
																	// 0.09 is due to usb wireless mouse
	
	
	public Phone(){
		setName("Total");
		uidMap =new HashMap<Integer, UidNode>();
//		energy = 0;
	}

	
	public void updatePowerEnergy(componentNames component,double duration, double p, int uid, int pid){
		double [] power = new double[numOfComponent];
//		if(duration < 0){
//			System.out.println("duration is: "+duration+" "+component.toString());
//		}
//		if(component == componentNames.Display)
//			System.out.println("DISPLAY: " + duration +"  " + p);
		
		
		
		power[componentNames.CPU.ordinal()] = CPUPower.getTotalPower();
		power[componentNames.Disk.ordinal()] = DiskPower.getPower();
		power[componentNames.GPU.ordinal()] = GPUPower.getTotalPower();//TODO:
		power[componentNames.LTE.ordinal()] = NetworkPower.getPower(false);
		power[componentNames.WIFI.ordinal()] = NetworkPower.getPower(true);
		power[componentNames.DRAM.ordinal()] = DRAMPower.getPower();
		power[componentNames.Display.ordinal()] = DisplayPower.getPower();
		power[componentNames.idle.ordinal()] = getPhoneIdlePower();
		
		
		setMaxPower(power);
		setMinPower(power);
		
		
		addIntervalEnergy(component.ordinal(), p * duration);
		
		if(pid == -1)
			return;
		
		power = new double[numOfComponent];
		power[componentNames.CPU.ordinal()] = 0;
		power[componentNames.Disk.ordinal()] = 0;
		power[componentNames.GPU.ordinal()] = GPUPower.getIdlePower();//TODO:
		power[componentNames.LTE.ordinal()] = 0;
		power[componentNames.WIFI.ordinal()] = 0;
		power[componentNames.DRAM.ordinal()] = 0;
		power[componentNames.Display.ordinal()] = 0;
		power[componentNames.idle.ordinal()] = 0;
		
		//also add this energy to corresponding pid & uid
		
		
		if(uid == -1){
			for (UidNode unode : uidMap.values()) {
				for (PidNode pnode : unode.getPidList()) {
					if(pnode.getPid() == pid)
						pnode.addPowerEnergy(power, duration);
				}
			}
		}else{
			UidNode uidNode = uidMap.get(uid);
			for (PidNode pidNode : uidNode.getPidList()) {
				if(pidNode.getPid() == pid)
					pidNode.addPowerEnergy(power, duration);
			}
		}
	}
	
	public void updatePowerEnergy(double duration, double [] p, int uid, int pid){
		double [] power = new double[numOfComponent];
		
		power[componentNames.CPU.ordinal()] = CPUPower.getTotalPower();
		power[componentNames.Disk.ordinal()] = DiskPower.getPower();
		power[componentNames.GPU.ordinal()] = GPUPower.getTotalPower();//TODO:
		power[componentNames.LTE.ordinal()] = NetworkPower.getPower(false);
		power[componentNames.WIFI.ordinal()] = NetworkPower.getPower(true);
		power[componentNames.DRAM.ordinal()] = DRAMPower.getPower();
		power[componentNames.Display.ordinal()] = DisplayPower.getPower();
		power[componentNames.idle.ordinal()] = getPhoneIdlePower();
		
		setMaxPower(power);
		setMinPower(power);
		
		for (componentNames comp : componentNames.values()) {
			addIntervalEnergy(comp.ordinal(), p[comp.ordinal()] * duration);
		}
		
		//also add this energy to corresponding pid & uid
		UidNode uidNode = uidMap.get(uid);
		for (PidNode pidNode : uidNode.getPidList()) {
			if(pidNode.getPid() == pid)
				pidNode.addPowerEnergy(p, duration);
		}
		
		if(uid == -1 && pid != -1){
//			System.out.println("here we are!!!"+pid+" "+uid);
			for (UidNode unode : uidMap.values()) {
				for (PidNode pnode : unode.getPidList()) {
					if(pnode.getPid() == pid)
						pnode.addPowerEnergy(p, duration);
				}
			}
		}
		
	}
	


	
	public HashMap<Integer, UidNode> getUidMap()
	{
		return uidMap;
	}


//	public UidNode getUidNode(int keyValue)
//	{
//		return (UidNode)uidMap.get(Integer.valueOf(keyValue));
//	}

	public void appDataDump(ArrayList<UidNode> UidList){
		for (int i = 0; i < UidList.size(); i++) {
			uidMap.put(Integer.valueOf(((UidNode)UidList.get(i)).getUid()), (UidNode)UidList.get(i));
		}
	}

	public int getNumOfUids()
	{
		return uidMap.size();
	}


	public Iterator<Map.Entry<Integer, UidNode>> getIterator()
	{
		return uidMap.entrySet().iterator();
	}

	public void addNode(UsageNode node){
		PidNode pidNode;
		boolean isDuplicated = false;
		
//		if(node.getUid() < 0)
//			return;

		UidNode uidNode = uidMap.get(node.getUid());
		if (uidNode == null)
		{
			uidNode = new UidNode(node.getUid());
			uidMap.put(Integer.valueOf(node.getUid()), uidNode);
		}
		try
		{
			for (int i = 0; i < uidNode.getPidList().size(); i++) {
				pidNode = uidNode.getPidList().get(i);

				if (pidNode.getPid() == node.getPid()){
					pidNode.addUsage(node);
					isDuplicated = true;
					break;
				}
			}
			if (!isDuplicated)
			{
				pidNode = new PidNode(node);
				uidNode.getPidList().add(pidNode);
				pidNode.addUsage(node);
			}
			
		}
		
		
		catch (Exception e)
		{
			e.toString();
			e.printStackTrace();
		}
	}

	public void finalizeIntervalEnergy(double startTime, double endTime)
	{
		if(startTime == -1 || endTime == -1){
			System.err.println("start/end times are not set!!!");
			System.exit(-1);
		}
		//add idle energy
		addIntervalEnergy(componentNames.idle.ordinal(), getPhoneIdlePower() * (endTime - startTime));
			
		//update moving energy, update first and last
		for (int uid : uidMap.keySet()) {
			UidNode uidNode = uidMap.get(uid);
			uidNode.updatePower(); // update max and min power
			for (PidNode pidNode : uidNode.getPidList()) {
				pidNode.updateMovingEnergy(startTime-PowerTap.startTime, endTime-PowerTap.startTime);
			}
			uidNode.updateMovingEnergy(startTime-PowerTap.startTime, endTime-PowerTap.startTime);
		}
		double tempe = intervalEnergy.get(UserProperty.componentNames.CPU.ordinal());
		
		if((startTime - PowerTap.startTime) < 0)
			System.out.println("start time: " + startTime + " global start time: " + PowerTap.startTime);
		updateMovingEnergy(startTime-PowerTap.startTime, endTime-PowerTap.startTime);
		
		
		
		// reset interval energy
		for (int uid : uidMap.keySet()) {
			UidNode uidNode = uidMap.get(uid);
			for (PidNode pidNode : uidNode.getPidList()) {
				pidNode.resetIntervalEnergy();
			}
			uidNode.resetIntervalEnergy();
		}
		resetIntervalEnergy();
		
		
	}
	
	public static double getPhoneIdlePower(){
		double freq = CPUPower.getFreq(0);
		
		for (int i = 0; i < phoneIdleBetas.length; i++) {
			if(Double.compare(freq, phoneIdleBetas[i][0]) == 0)
				return phoneIdleBetas[i][1];
		}
		System.err.println("Bad Freq for cpu 0 while computing phone idle power!!");
		System.exit(-1);
		return 0;
	}
	
}
