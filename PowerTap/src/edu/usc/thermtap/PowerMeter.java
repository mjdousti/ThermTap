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

import java.util.ArrayList;

import edu.usc.powertap.entities.Phone;
import edu.usc.powertap.entities.UsageNode;
import edu.usc.powertap.entities.UsageNode.Types;
import edu.usc.powertap.powermodel.CPUPower;
import edu.usc.powertap.powermodel.DiskPower;
import edu.usc.powertap.powermodel.DisplayPower;
import edu.usc.powertap.powermodel.GPUPower;
import edu.usc.powertap.powermodel.NetworkPower;
import edu.usc.powertap.utils.Tool;
import edu.usc.powertap.utils.UserProperty;
import edu.usc.powertap.utils.UserProperty.componentNames;


public class PowerMeter{
	private final ArrayList<UsageNode> usageDataList = new ArrayList<UsageNode>();
	double startTime = -1;
	double endTime = -1;
	double prevTime = -1;

	/*
	 * Calculates the duration that UsageNode node consumes power 
	 */
	private double calcDuration(UsageNode node, int index){
		double duration=0;
		for (int j = index+1; j < usageDataList.size(); j++){
			UsageNode nextNode = usageDataList.get(j);

			if ((nextNode.getUsageNodeType() == Types.disk ||
					nextNode.getUsageNodeType() == Types.LTE ||
					nextNode.getUsageNodeType() == Types.wifi) &&
					nextNode.getUsageNodeType()==node.getUsageNodeType() && 
					nextNode.isEnd() && 
					nextNode.getPid() == node.getPid()){

				duration = nextNode.getTimeStamp() - node.getTimeStamp();
				break;
			}else if (nextNode.getUsageNodeType() == Types.cntx_switch &&
					nextNode.getUsageNodeType()==node.getUsageNodeType() &&
					nextNode.getCoreID() == node.getCoreID()){

				duration = nextNode.getTimeStamp() - node.getTimeStamp();
				break;
			}else if ((nextNode.getUsageNodeType() == Types.display) &&
					nextNode.getUsageNodeType()==node.getUsageNodeType()){

				duration = nextNode.getTimeStamp() - node.getTimeStamp();
				break;
			}else if ((node.getUsageNodeType() == Types.cpu_freq ||
					node.getUsageNodeType() == Types.cpu_disable) &&
					(nextNode.getUsageNodeType() == Types.cpu_freq ||
					nextNode.getUsageNodeType() == Types.cpu_disable)&&
					nextNode.getCoreID() == node.getCoreID()){

				duration = nextNode.getTimeStamp() - node.getTimeStamp();
				break;
			}else if((node.getUsageNodeType() == Types.gpuenable ||
					node.getUsageNodeType() == Types.gpulevel ||
					node.getUsageNodeType() == Types.gpu_cntx ||
					node.getUsageNodeType() == Types.gpu_util) && 
					(nextNode.getUsageNodeType() == Types.gpuenable ||
					nextNode.getUsageNodeType() == Types.gpulevel ||
					nextNode.getUsageNodeType() == Types.gpu_cntx ||
					nextNode.getUsageNodeType() == Types.gpu_util)){
				duration = nextNode.getTimeStamp() - node.getTimeStamp();


				break;
			}

		}
		if (duration==0)	//if it is added to the end, pick the very last item
			duration = usageDataList.get(usageDataList.size()-1).getTimeStamp() - node.getTimeStamp();

		return duration;
	}

	public void getPowers(){
		Phone phone = ThermTap.getPhone();
		UsageNode usageNode;

		if (usageDataList.isEmpty()){	//no data is recorded
			System.out.println("no usage node has been added!!!");
			return;
		}


		double duration=0;

		int coreID, packetSize, chunkSize;
		boolean lteWifi, sendRecv, readWrite;


		/*
		 * Adding CPU Idle power that are not included in the events
		 */

		startTime = usageDataList.get(0).getTimeStamp();
		endTime = usageDataList.get(usageDataList.size()-1).getTimeStamp();

		if(PowerTap.startTime == -1)
			PowerTap.startTime = startTime;

		/// all always power consuming components should be covered each time

		// cpu:
		boolean found;
		for (coreID = 0; coreID < phone.coreCount; coreID++) {
			found = false;
			for (int j = 0; j < usageDataList.size(); j++) {
				usageNode =usageDataList.get(j); 
				if (usageNode.getUsageNodeType()==Types.cpu_freq){
					found = true;
					duration = usageNode.getTimeStamp() - startTime;
					phone.updatePowerEnergy(componentNames.CPU,duration, CPUPower.getIdlePower(coreID), -1, -1);
					break;
				}
			}
			if (!found){
				phone.updatePowerEnergy(componentNames.CPU, usageDataList.get(usageDataList.size()-1).getTimeStamp() 
						- usageDataList.get(0).getTimeStamp(), CPUPower.getIdlePower(coreID), -1, -1);
			}
		}
		// display
		found = false;
		for (int j = 0; j < usageDataList.size(); j++) {
			usageNode =usageDataList.get(j); 
			if (usageNode.getUsageNodeType()==Types.display){
				found = true;
				duration = usageNode.getTimeStamp() - startTime;
				phone.updatePowerEnergy(componentNames.Display, duration, DisplayPower.getPower(), -1, -1);
				break;
			}
		}
		if (!found){
			phone.updatePowerEnergy(componentNames.Display, usageDataList.get(usageDataList.size()-1).getTimeStamp() 
					- usageDataList.get(0).getTimeStamp(), DisplayPower.getPower(), -1, -1);
		}
		// gpu
		found = false;
		for (int j = 0; j < usageDataList.size(); j++) {
			usageNode =usageDataList.get(j); 
			if (usageNode.getUsageNodeType()==Types.gpuenable ||
					usageNode.getUsageNodeType()==Types.gpulevel ||
					usageNode.getUsageNodeType()==Types.gpu_cntx ||
					usageNode.getUsageNodeType()==Types.gpu_util){
				found = true;
				duration = usageNode.getTimeStamp() - startTime;
				phone.updatePowerEnergy(componentNames.GPU, duration, GPUPower.getTotalPower(), -1, GPUPower.getPid());
				break;
			}
		}
		if (!found){
			phone.updatePowerEnergy(componentNames.GPU, usageDataList.get(usageDataList.size()-1).getTimeStamp() 
					- usageDataList.get(0).getTimeStamp(), GPUPower.getTotalPower(), -1, GPUPower.getPid());
		}


		/*
		 * TODO: Add Wifi idle power here
		 */

		double[] tempPower = new double[UserProperty.numOfComponent];

		for (int i = 0; i < usageDataList.size(); i++){



			usageNode = (UsageNode)usageDataList.get(i);


			if (usageNode.isEnd()){	//This node marks the end of one power usage profile
				if (usageNode.getUsageNodeType()==Types.wifi){
					NetworkPower.makeWifiDisable();
				}else if(usageNode.getUsageNodeType()==Types.LTE){
					NetworkPower.makeLTEDisable();
				}else if (usageNode.getUsageNodeType()==Types.disk){
					DiskPower.makeDisable();
				}
				phone.updatePowerEnergy(componentNames.CPU, 0, 0, -1, -1);//just to update max and min power
				continue;
			}			

			duration = calcDuration(usageNode, i);
			usageNode.setDuration(duration);
			if(duration < 0) //TODO:why this happens??
				continue;

			if(duration > 100.0){
				duration = 0;
			}

			if(usageNode == null || usageNode.getUsageNodeType() == null){
				System.err.println("usagenode is null!!! " + usageNode);
				System.exit(-1);

			}

			switch (usageNode.getUsageNodeType()) {
			case cpu_freq:
				coreID = usageNode.getCoreID();				
				CPUPower.setFreq(coreID, usageNode.getCoreFrequency());
				phone.updatePowerEnergy(componentNames.CPU,duration, CPUPower.getIdlePower(coreID), -1, -1);
				continue;
			case cntx_switch:
				coreID = usageNode.getCoreID();
				CPUPower.setUtilization(coreID, usageNode.getCoreUtilization());
				tempPower[componentNames.CPU.ordinal()] = CPUPower.getActivePower(coreID);
				break;
			case cpu_disable:
				coreID = usageNode.getCoreID();				
				CPUPower.turnOffCore(coreID);
				CPUPower.setFreq(coreID, 0.0);
				phone.updatePowerEnergy(componentNames.CPU,duration, CPUPower.getIdlePower(coreID), -1, -1);
				continue;
			case LTE:
				if(usageNode.isEnd())
					continue;
				packetSize = usageNode.getPacketSize();
				sendRecv = usageNode.isRecv();
				lteWifi = false;
				tempPower[componentNames.LTE.ordinal()] = NetworkPower.getPower(lteWifi, sendRecv, packetSize,duration);
				break;
			case wifi:
				if(usageNode.isEnd())
					continue;
				packetSize = usageNode.getPacketSize();
				sendRecv = usageNode.isRecv();
				lteWifi = true;
				tempPower[componentNames.WIFI.ordinal()] = NetworkPower.getPower(lteWifi, sendRecv, packetSize,duration);
				break;
			case disk:
				if(usageNode.isEnd())
					continue;
				chunkSize = usageNode.getChunkSize();
				readWrite = usageNode.isWrite();
				tempPower[componentNames.Disk.ordinal()] = DiskPower.getPower(readWrite, usageNode.getChunkSize(),duration);
				//TODO: disk power depends on read/write speed! our power measurements are for full speed!
				duration = duration < DiskPower.getDuration(readWrite, chunkSize) ? duration : DiskPower.getDuration(readWrite, chunkSize);
				//				System.out.println("disk: "+duration+" "+tempPower[componentNames.Disk.ordinal()]);
				break;
			case gpu_util:
				GPUPower.setUtilization(usageNode.getGpuUtil());
				usageNode.setPid(GPUPower.getPid());
				tempPower[componentNames.GPU.ordinal()] = GPUPower.getActivePower();
				phone.updatePowerEnergy(componentNames.GPU,duration, GPUPower.getIdlePower(), -1, GPUPower.getPid()); //for idle part
				break;
			case gpu_cntx:
				//GPUPower.setUtilization(usageNode.getGpuUtil());
				GPUPower.setCurrentPid(usageNode.getPid());
				tempPower[componentNames.GPU.ordinal()] = GPUPower.getActivePower();
				phone.updatePowerEnergy(componentNames.GPU,duration, GPUPower.getIdlePower(), -1, GPUPower.getPid()); //for idle part
				break;
			case gpulevel:
				GPUPower.setPwrlevel(usageNode.getGpuLevel());
				//phone.updatePowerEnergy(componentNames.GPU,duration, GPUPower.getIdlePower());
				phone.updatePowerEnergy(componentNames.GPU,duration, GPUPower.getIdlePower(), -1, GPUPower.getPid());
				tempPower[componentNames.GPU.ordinal()] = GPUPower.getActivePower();
				break;
			case gpuenable:
				GPUPower.setClockGated(usageNode.isClockGated());
				phone.updatePowerEnergy(componentNames.GPU,duration, GPUPower.getIdlePower(), -1, GPUPower.getPid());
				tempPower[componentNames.GPU.ordinal()] = GPUPower.getActivePower();//TODO:
				break;
			case display:
				DisplayPower.setBrightness(usageNode.getDisplayBrightness());
				phone.updatePowerEnergy(componentNames.Display,duration, DisplayPower.getPower(), -1, -1);
				continue;
			case dram://TOD:
			case gps://TODO:
			default:
				System.err.println("Node type "+usageNode.getUsageNodeType()+" not found!");
				System.exit(-1);
				break;
			}

			phone.addNode(usageNode); //if this is the first time seeing this uid/pid add them
			usageNode.setActivePower(tempPower); //save power info for min/max computation for pid/uid

			if(duration < 0)
				System.out.println("duration is negative " + duration);

			phone.updatePowerEnergy(duration, tempPower,usageNode.getUid() , usageNode.getPid());

			Tool.resetArray(tempPower);
		}


		phone.finalizeIntervalEnergy(startTime, endTime); //TODO: remove usage nodes at the end!!????
		ThermTap.refreshPowerCharts();
	}

	public void addDataFromInlineData(UsageNode n){
		UsageNode node = n;
		usageDataList.add(node);
	}


	public ArrayList<UsageNode> getUsageDataList()
	{
		return usageDataList;
	}
}
