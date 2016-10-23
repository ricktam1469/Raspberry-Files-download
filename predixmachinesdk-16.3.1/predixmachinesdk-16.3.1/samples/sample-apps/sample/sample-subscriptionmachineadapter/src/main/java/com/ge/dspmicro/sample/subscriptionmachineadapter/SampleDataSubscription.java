/*
 * Copyright (c) 2014 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */

package com.ge.dspmicro.sample.subscriptionmachineadapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ge.dspmicro.machinegateway.api.adapter.IDataSubscription;
import com.ge.dspmicro.machinegateway.api.adapter.IDataSubscriptionListener;
import com.ge.dspmicro.machinegateway.api.adapter.ISubscriptionMachineAdapter;
import com.ge.dspmicro.machinegateway.types.PDataNode;
import com.ge.dspmicro.machinegateway.types.PDataValue;
import com.ge.dspmicro.machinegateway.types.PEnvelope;

/**
 * 
 * @author Predix Machine Sample
 */
public class SampleDataSubscription
        implements Runnable, IDataSubscription
{
    private UUID                            uuid;
    private String                          name;
    private int                             updateInterval;
    private ISubscriptionMachineAdapter     adapter;
    private Map<UUID, SampleDataNode>       nodes         = new HashMap<UUID, SampleDataNode>();
    private List<IDataSubscriptionListener> listeners     = new ArrayList<IDataSubscriptionListener>();
    private Random                          dataGenerator = new Random();
    private final AtomicBoolean             threadRunning = new AtomicBoolean();

    /**
     * Constructor
     * 
     * @param adapter machine adapter
     * @param subName Name of this subscription
     * @param updateInterval in milliseconds
     * @param nodes list of nodes for this subscription
     */
    public SampleDataSubscription(ISubscriptionMachineAdapter adapter, String subName, int updateInterval,
            List<SampleDataNode> nodes)
    {
        if ( updateInterval > 0 )
        {
            this.updateInterval = updateInterval;
        }
        else
        {
            throw new IllegalArgumentException("updataInterval must be greater than zero."); //$NON-NLS-1$
        }

        if ( nodes != null && nodes.size() > 0 )
        {
            for (SampleDataNode node : nodes)
            {
                this.nodes.put(node.getNodeId(), node);
            }
        }
        else
        {
            throw new IllegalArgumentException("nodes must have values."); //$NON-NLS-1$
        }

        this.adapter = adapter;

        // Generate unique id.
        this.uuid = UUID.randomUUID();
        this.name = subName;

        // Initialize random data generator.
        this.dataGenerator.setSeed(Calendar.getInstance().getTimeInMillis());
        this.threadRunning.set(false);
    }

    @Override
    public UUID getId()
    {
        return this.uuid;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public int getUpdateInterval()
    {
        return this.updateInterval;
    }

    @Override
    public List<PDataNode> getSubscriptionNodes()
    {
        return new ArrayList<PDataNode>(this.nodes.values());
    }

    /**
     * @param listener callback listener
     */
    @Override
    public synchronized void addDataSubscriptionListener(IDataSubscriptionListener listener)
    {
        if ( !this.listeners.contains(listener) )
        {
            this.listeners.add(listener);
        }
    }

    /**
     * @param listener callback listener
     */
    @Override
    public synchronized void removeDataSubscriptionListener(IDataSubscriptionListener listener)
    {
        if ( !this.listeners.contains(listener) )
        {
            this.listeners.remove(listener);
        }
    }

    /**
     * get all listeners
     * 
     * @return a list of listeners.
     */
    @Override
    public synchronized List<IDataSubscriptionListener> getDataSubscriptionListeners()
    {
        return this.listeners;
    }

    /**
     * Thread to generate random data for the nodes in this subscription.
     */
    @Override
    public void run()
    {
        if ( !this.threadRunning.get() && this.nodes.size() > 0 )
        {
            this.threadRunning.set(true);

            while (this.threadRunning.get())
            {
                // Generate random data for each node and push data update.
                List<PDataValue> data = new ArrayList<PDataValue>();

                for (SampleDataNode node : this.nodes.values())
                {
                    // Simulating the data.
                    PEnvelope envelope = new PEnvelope(this.dataGenerator.nextFloat());
                    PDataValue value = new PDataValue(node.getNodeId(), envelope);
                    value.setNodeName(node.getName());
                    value.setAddress(node.getAddress());
                    
                    data.add(value);
                }

                // Writing the simulated data into cache
                ((SampleSubscriptionMachineAdapterImpl) this.adapter).putData(data);
                
                //Provide the subscription name as a property of the data. If the data is being
                //published on the databus river, the subscription name will be used as the publish topic
                
            	HashMap<String,String> properties = new HashMap<String,String>();
            	properties.put(IDataSubscription.PROPKEY_SUBSCRIPTION, this.name);

                for (IDataSubscriptionListener listener : this.listeners)
                {
                    listener.onDataUpdate(this.adapter, properties, data);
                }

                try
                {
                    // Wait for an updateInterval period before pushing next data update.
                    Thread.sleep(this.updateInterval);
                }
                catch (InterruptedException e)
                {
                    // ignore
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

            // Send notification to all listeners.
            for (IDataSubscriptionListener listener : this.listeners)
            {
                listener.onSubscriptionDelete(this.adapter, this.uuid);
            }

            // Do other clean up if needed...
        }
    }

}
