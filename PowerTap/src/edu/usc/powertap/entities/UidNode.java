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
import java.util.Collections;
import java.util.Comparator;

import edu.usc.powertap.utils.UserProperty;


public class UidNode extends Node
{
	private boolean isShared;
	private Type type;
	private ArrayList<Package> packageList;
	private ArrayList<PidNode> pidList;

	private static enum Type{
		LINUX,  ANDROID,  USER;
	}

	public UidNode()
	{
		setShared(false);
		setType(Type.USER);
		setPackageList(new ArrayList<Package>());
		setPidList(new ArrayList<PidNode>());
	}

	UidNode(int uid)
	{
		setShared(false);
		setUid(uid);
		setPackageList(new ArrayList<Package>());
		setPidList(new ArrayList<PidNode>());
		if (uid < 1000)
		{
			setType(Type.LINUX);
			setName("Linux");
		}
		else if (uid < 10000)
		{
			setType(Type.ANDROID);
			setName("Android");
		}
		else
		{
			setType(Type.USER);
		}
	}

	public UidNode(Package item)
	{
		setShared(false);
		setType(Type.USER);
		setPackageList(new ArrayList<Package>());
		setPidList(new ArrayList<PidNode>());
		addPackage(item);
	}

	public boolean isShared()
	{
		return this.isShared;
	}

	public void setShared(boolean isShared)
	{
		this.isShared = isShared;
	}

	public Type getType()
	{
		return this.type;
	}

	public void setType(Type type)
	{
		this.type = type;
	}

	public ArrayList<Package> getPackageList()
	{
		return packageList;
	}

	public void setPackageList(ArrayList<Package> packageList)
	{
		this.packageList = packageList;
	}

	public ArrayList<PidNode> getPidList()
	{
		return pidList;
	}

	public void setPidList(ArrayList<PidNode> pidList)
	{
		this.pidList = pidList;
	}

	public void addPackage(Package item)
	{
		if (getUid() == -1) {
			if ((item.getSharedUserID() != "") && (item.getSharedUserID() != null))
			{
				setShared(true);
				setName("Shard ID");
				setUid(Integer.parseInt(item.getSharedUserID()));
				if (item.getSharedUserID().length() < 4) {
					setType(Type.LINUX);
				} else if (item.getSharedUserID().length() < 5) {
					setType(Type.ANDROID);
				} else {
					setType(Type.USER);
				}
			}
			else if ((item.getUserID() != "") && (item.getUserID() != null))
			{
				setShared(false);
				setName(item.getAppName());
				setUid(Integer.parseInt(item.getUserID()));
				if (item.getUserID().length() < 4) {
					setType(Type.LINUX);
				} else if (item.getUserID().length() < 5) {
					setType(Type.ANDROID);
				} else {
					setType(Type.USER);
				}
			}
		}
		this.packageList.add(item);
		if (isShared()) {
			setName("Shared for " + this.packageList.size() + " packages");
		}
	}

	void updatePower()
	{
		PidNode pidNode;

		ArrayList<UsageNode> usageNodeList = new ArrayList<UsageNode>();

		for (int i = 0; i < pidList.size(); i++){
			pidNode = pidList.get(i);
			usageNodeList.addAll(pidNode.getUsageList());
			
			for (int j = 0; j < UserProperty.numOfComponent; j++) {
				addIntervalEnergy(j, pidNode.getComponentIntervalEnergy(j));
			}
		}

		Collections.sort(usageNodeList, new Comparator<UsageNode>() {
			@Override
			public int compare(UsageNode arg0, UsageNode arg1) {
				double diff = arg0.getTimeStamp() - arg1.getTimeStamp();

				if (diff>0)
					return 1;
				else if (diff==0)
					return 0;
				else
					return -1;			}
		});

		double power;
		for(int k=0 ; k<UserProperty.numOfComponent ; k++){
			for (int i = 0; i < usageNodeList.size(); i++) {
				power= usageNodeList.get(i).getActivePower()[k];
				
				setMinPower(k,power);
				
				for (int j = i+1; j < usageNodeList.size(); j++) {
					if (usageNodeList.get(j).getTimeStamp() > usageNodeList.get(i).getTimeStamp() + usageNodeList.get(i).getDuration()){	//no more overlap
						break;
					}else{
						power= usageNodeList.get(j).getActivePower()[k];
					}				
				}
				setMaxPower(k,power);
			}	
		}

	}
	
	void addPowerEnergy(double[] _power, double duration){
		for (int i = 0; i < numOfComponent; i++){
			addIntervalEnergy(i, _power[i] * duration);
		}
	}

	public int getNumOfPackages()
	{
		return packageList.size();
	}
}
