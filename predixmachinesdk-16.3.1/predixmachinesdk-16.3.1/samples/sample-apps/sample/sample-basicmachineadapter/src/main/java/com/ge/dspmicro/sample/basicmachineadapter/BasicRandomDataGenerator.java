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

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 
 * @author Predix Machine Sample
 */
public class BasicRandomDataGenerator implements Runnable
{
	private int 							updateInterval;
	private Map<String, BasicSampleDataNode> 	nodes 			= new HashMap<String, BasicSampleDataNode>();
	private Random 							dataGenerator		= new Random();
	private ICallable 						callback;
	private final AtomicBoolean 			threadRunning 		= new AtomicBoolean();
	
	/**
	 * Constructor
	 * 
	 * @param updateInterval in milliseconds
	 */
	public BasicRandomDataGenerator(int updateInterval)
	{
		if ( updateInterval > 0 )
		{
			// Initialize random data generator.
			this.dataGenerator.setSeed(Calendar.getInstance().getTimeInMillis());
			this.updateInterval = updateInterval;
			this.threadRunning.set(false);
		}
		else
		{
			throw new IllegalArgumentException("updataInterval must be greater than zero."); //$NON-NLS-1$
		}
	}
	
	/**
	 * @param nodes list of SampleDataNode
	 * @param callback reference to a callback object
	 */
	public void setCallback(List<BasicSampleDataNode> nodes, ICallable callback)
	{
		if ( nodes != null && nodes.size() > 0)
		{
			for ( BasicSampleDataNode node : nodes )
			{
				this.nodes.put(node.getDataGeneratorId(), node);
			}
			this.callback = callback;
		}
		else
		{
			throw new IllegalArgumentException("nodes must have values."); //$NON-NLS-1$
		}
	}

	@Override
	public void run()
	{
		if ( !this.threadRunning.get() && this.nodes.size() > 0 && this.callback != null )
		{
			this.threadRunning.set(true);
			
			while(this.threadRunning.get())
			{
				// Generate random data for each node and push data update.
				Map<UUID, Float> data = new HashMap<UUID, Float>();
				for (BasicSampleDataNode node : this.nodes.values())
				{
					data.put(node.getNodeId(), this.dataGenerator.nextFloat());		
				}
				this.callback.onPushData(data);

				try 
				{
					// Wait for an updateInterval period before pushing next data update.
					Thread.sleep(this.updateInterval);
				} 
				catch (InterruptedException e) 
				{
					// Handle exception;
				}
			}
		}		
	}
	
	/**
	 * Stops generating random data.
	 */
	public void stop()
	{
		if ( this.threadRunning.get() )
		{
			this.threadRunning.set(false);
		}
	}
	
}
