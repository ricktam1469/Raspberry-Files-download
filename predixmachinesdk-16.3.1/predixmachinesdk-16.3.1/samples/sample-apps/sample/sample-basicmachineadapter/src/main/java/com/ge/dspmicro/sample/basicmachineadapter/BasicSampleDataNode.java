/*
 * Copyright (c) 2014 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */
 
package com.ge.dspmicro.sample.basicmachineadapter;

import java.util.UUID;

import com.ge.dspmicro.machinegateway.types.PDataNode;

/**
 * 
 * 
 * @author Predix Machine Sample
 */
public class BasicSampleDataNode extends PDataNode 
{
	private String dataGeneratorId;
	
	/**
	 * Constructor
	 * 
	 * @param machineAdapterId a unique id
	 * @param name string value
	 */
	public BasicSampleDataNode(UUID machineAdapterId, String name) 
	{
		super(machineAdapterId, name);
		
		// Do other initialization if needed.
	}
	
	/**
	 * @return the dataGeneratorId
	 */
	public String getDataGeneratorId() 
	{
		return this.dataGeneratorId;
	}
	/**
	 * @param dataGeneratorId the dataGeneratorId to set
	 */
	public void setDataGeneratorId(String dataGeneratorId) 
	{
		this.dataGeneratorId = dataGeneratorId;
	}

	
}
