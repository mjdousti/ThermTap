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

public class PidNode extends Node
{
	private int pid;
	private int tgid;
	private Status status;

	private static enum Status{
		ALIVE,  DEAD,  UNKNOWN;
	}

	public PidNode(){
		setPid(0);
		setTgid(0);
		setName("Unknown");
		setStatus(Status.UNKNOWN);
		this.reset();//setMaxPower(0);
	}

	PidNode(UsageNode node)
	{
		setPid(node.getPid());
		setTgid(node.getTgid());
		setUid(node.getUid());
		setName(node.getProcessName());
		setStatus(Status.UNKNOWN);
	}

	public int getPid()
	{
		return this.pid;
	}

	public void setPid(int pid)
	{
		this.pid = pid;
	}

	public int getTgid()
	{
		return this.tgid;
	}

	public void setTgid(int tgid)
	{
		this.tgid = tgid;
	}

	public Status getStatus()
	{
		return this.status;
	}

	public void setStatus(Status status)
	{
		this.status = status;
	}

	void addPowerEnergy(double[] _power, double duration){
		for (int i = 0; i < numOfComponent; i++){
			addIntervalEnergy(i, _power[i] * duration);
		}
		
		setMinPower(_power);
		setMaxPower(_power);
	}
	
}
