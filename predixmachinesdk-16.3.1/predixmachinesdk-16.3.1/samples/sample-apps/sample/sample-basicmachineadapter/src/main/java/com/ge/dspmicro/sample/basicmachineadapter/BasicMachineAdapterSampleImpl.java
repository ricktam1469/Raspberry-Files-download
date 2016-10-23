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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Modified;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;

import com.ge.dspmicro.machinegateway.api.adapter.IMachineAdapter;
import com.ge.dspmicro.machinegateway.api.adapter.MachineAdapterException;
import com.ge.dspmicro.machinegateway.api.adapter.MachineAdapterInfo;
import com.ge.dspmicro.machinegateway.api.adapter.MachineAdapterState;
import com.ge.dspmicro.machinegateway.types.PDataNode;
import com.ge.dspmicro.machinegateway.types.PDataValue;
import com.ge.dspmicro.machinegateway.types.PEnvelope;

/**
 * 
 * @author Predix Machine Sample
 */
@SuppressWarnings("javadoc")
@Component(name = BasicMachineAdapterSampleImpl.SERVICE_PID, provide = IMachineAdapter.class, designate = BasicMachineAdapterSampleImpl.Config.class, configurationPolicy = ConfigurationPolicy.require)
public class BasicMachineAdapterSampleImpl
        implements IMachineAdapter
{
    // Meta mapping for configuration properties
    @Meta.OCD(name = "%component.name", localization = "OSGI-INF/l10n/bundle")
    interface Config
    {
        @Meta.AD(name = "%updateInterval.name", description = "%updateInterval.description", id = UPDATE_INTERVAL, required = false, deflt = "")
        String updateInterval();

        @Meta.AD(name = "%numberOfNodes.name", description = "%numberOfNodes.description", id = NUMBER_OF_NODES, required = false, deflt = "")
        String numberOfNodes();

        @Meta.AD(name = "%adapterName.name", description = "%adapterName.description", id = ADAPTER_NAME, required = false, deflt = "")
        String adapterName();

        @Meta.AD(name = "%adapterDescription.name", description = "%adapterDescription.description", id = ADAPTER_DESCRIPTION, required = false, deflt = "")
        String adapterDescription();
    }
    
    /** Service PID for Sample Machine Adapter */
    public static final String         SERVICE_PID         = "com.ge.dspmicro.sample.basicmachineadapter";                //$NON-NLS-1$
    /** Key for Update Interval */
    public static final String         UPDATE_INTERVAL     = SERVICE_PID + ".UpdateInterval";                         //$NON-NLS-1$
    /** Key for number of nodes */
    public static final String         NUMBER_OF_NODES     = SERVICE_PID + ".NumberOfNodes";                          //$NON-NLS-1$
    /** key for machine adapter name */
    public static final String         ADAPTER_NAME        = SERVICE_PID + ".Name";                                   //$NON-NLS-1$
    /** Key for machine adapter description */
    public static final String         ADAPTER_DESCRIPTION = SERVICE_PID + ".Description";                            //$NON-NLS-1$

    // Create logger to report errors, warning massages, and info messages (runtime Statistics)
    private static final Logger        _logger             = LoggerFactory.getLogger(BasicMachineAdapterSampleImpl.class);
    
    private UUID                       uuid = UUID.randomUUID();
    @SuppressWarnings("unused")
    private Dictionary<String, Object> props;
    private MachineAdapterInfo         adapterInfo;
    private MachineAdapterState        adapterState;
    private Map<UUID, BasicSampleDataNode>  dataNodes           = new HashMap<UUID, BasicSampleDataNode>();
    private BasicRandomDataGenerator        randomDataGenerator;
    private int                        updateInterval;

    private Config                     config;

    /**
     * Data cache for holding latest data updates
     */
    protected Map<UUID, PDataValue>    dataValueCache      = new ConcurrentHashMap<UUID, PDataValue>();

    private ICallable                  dataUpdateHandler   = new ICallable()
        {
            @Override
            public void onPushData(Map<UUID, Float> data)
            {
                for (UUID key : data.keySet())
                {
                    // Convert data to PDataValue
                    PEnvelope envelope = new PEnvelope(data.get(key));
                    PDataValue value = new PDataValue(key, envelope);
     
                    BasicMachineAdapterSampleImpl.this.dataValueCache
                            .put(key, value);
                }
            }
        };

    /*
     * ###############################################
     * # OSGi service lifecycle management #
     * ###############################################
     */

    /**
     * OSGi component lifecycle activation method
     * 
     * @param ctx component context
     * @throws IOException on fail to load/set configuration properties
     */
    @Activate
    public void activate(ComponentContext ctx) throws IOException
    {
        // use the logger service for debugging purpose
        if ( _logger.isDebugEnabled() )
        {
            _logger.debug("Starting sample " + ctx.getBundleContext().getBundle().getSymbolicName()); //$NON-NLS-1$
        }

        // Get all properties and create nodes.
        this.props = ctx.getProperties();
        
        this.config = Configurable.createConfigurable(Config.class, ctx.getProperties());
        
        this.updateInterval = Integer.parseInt(this.config.updateInterval());
        int count = Integer.parseInt(this.config.numberOfNodes());
        createNodes(count);

        this.adapterInfo = new MachineAdapterInfo(this.config.adapterName(), BasicMachineAdapterSampleImpl.SERVICE_PID,
                this.config.adapterDescription(), ctx.getBundleContext().getBundle().getVersion().toString());

        // Start data generator and sign up for data updates.
        this.randomDataGenerator = new BasicRandomDataGenerator(this.updateInterval);
        this.randomDataGenerator.setCallback(new ArrayList<BasicSampleDataNode>(this.dataNodes.values()),
                this.dataUpdateHandler);
        new Thread(this.randomDataGenerator).start();
    }

    /**
     * OSGi component lifecycle deactivation method
     * 
     * @param ctx component context
     */
    @Deactivate
    public void deactivate(ComponentContext ctx)
    {
        // Put your clean up code here when container is shutting down
        
        if ( _logger.isDebugEnabled() )
        {
            _logger.debug("Stopped sample for " + ctx.getBundleContext().getBundle().getSymbolicName()); //$NON-NLS-1$
        }

        // Stop random data generator.
        this.randomDataGenerator.stop();
        this.adapterState = MachineAdapterState.Stopped;
    }

    /**
     * OSGi component lifecycle modified method. Called when
     * the component properties are changed.
     * 
     * @param ctx component context
     */
    @Modified
    public synchronized void modified(ComponentContext ctx)
    {
        // Handle run-time changes to properties.

        this.props = ctx.getProperties();
    }

    /*
     * #######################################
     * # IMachineAdapter interface methods #
     * #######################################
     */

    @Override
    public UUID getId()
    {
        return this.uuid;
    }

    @Override
    public MachineAdapterInfo getInfo()
    {
        return this.adapterInfo;
    }

    @Override
    public MachineAdapterState getState()
    {
        return this.adapterState;
    }

    /*
     * Returns all data nodes. Data nodes are auto-generated at startup.
     */
    @Override
    public List<PDataNode> getNodes()
    {
        return new ArrayList<PDataNode>(this.dataNodes.values());
    }

    /*
     * Reads data from data cache. Data cache always contains latest values.
     */
    @Override
    public PDataValue readData(UUID nodeId)
            throws MachineAdapterException
    {
        if ( this.dataValueCache.containsKey(nodeId) )
        {
            return this.dataValueCache.get(nodeId);
        }

        // Do not return null.
        return new PDataValue(nodeId);
    }

    /*
     * Writes data value into data cache.
     */
    @Override
    public void writeData(UUID nodeId, PDataValue value)
            throws MachineAdapterException
    {
        if ( this.dataValueCache.containsKey(nodeId) )
        {
            // Put data into cache. The value typically should be written to a device node.
            this.dataValueCache.put(nodeId, value);
        }
    }

    /*
     * #####################################
     * # Private methods #
     * #####################################
     */

    /**
     * Generates random nodes
     * 
     * @param count of nodes
     */
    private void createNodes(int count)
    {
        for (int index = 1; index <= count; index++)
        {
            // Generate random IDs.
            UUID id = UUID.randomUUID();

            // Create data generator ID.
            String anotherId = "Node" + Integer.toString(index); //$NON-NLS-1$

            // Create a new node and put it in the cache.
            this.dataNodes.put(id, new BasicSampleDataNode(id, anotherId));
        }
    }
}
