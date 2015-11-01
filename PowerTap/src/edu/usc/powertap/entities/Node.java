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

abstract class Node extends PowerStat
{
	private int uid;
	private String name;

	public Node(){
		setUid(-1);
		setName("");
	}

	public void reset(){
		setUid(-1);
		setName("");
		super.reset();
	}

	public int getUid(){
		return this.uid;
	}

	public void setUid(int uid){
		this.uid = uid;
	}

	public String getName(){
		return this.name;
	}

	public void setName(String name){
		this.name = name;
	}
}