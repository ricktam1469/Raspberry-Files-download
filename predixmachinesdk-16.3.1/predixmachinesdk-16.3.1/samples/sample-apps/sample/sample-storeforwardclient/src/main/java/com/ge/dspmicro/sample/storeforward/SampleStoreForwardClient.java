/*
 * Copyright (c) 2015 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */

package com.ge.dspmicro.sample.storeforward;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Meta;

import com.ge.dspmicro.storeforward.api.ForwardStatus;
import com.ge.dspmicro.storeforward.api.IForwardCallback;
import com.ge.dspmicro.storeforward.api.IPersistable;
import com.ge.dspmicro.storeforward.api.IStoreForward;
import com.ge.dspmicro.storeforward.api.PersistenceObject;
import com.ge.dspmicro.storeforward.api.StoreForwardException;
import com.ge.dspmicro.validateroute.api.IPingMessage;
import com.ge.dspmicro.validateroute.api.PongMessage.PongStatus;

/**
 * Do nothing interface for component registration of SampleStoreForward
 */
interface ISampleStoreForwardClient
{
    /**
     * Service PID for Spillway service
     */
    public final static String SERVICE_PID  = "com.ge.dspmicro.sample.storeforwardclient"; //$NON-NLS-1$

    /** Required Property key for storeforward name */
    public final static String STOREFORWARD = SERVICE_PID + ".storeforward";              //$NON-NLS-1$

}

/**
 * Sample client code to use StoreForward service
 * 
 * @author Predix Machine Sample
 */
@Component(name = ISampleStoreForwardClient.SERVICE_PID, provide = ISampleStoreForwardClient.class, configurationPolicy = ConfigurationPolicy.require)
public class SampleStoreForwardClient
        implements ISampleStoreForwardClient, IForwardCallback
{
    /**
     * 
     * @author Predix Machine Sample
     */
    @Meta.OCD(name = "%component.name", factory = true, localization = "OSGI-INF/l10n/bundle")
    interface Configuration
    {
        /**
         * @return String with storeforward name
         */
        @Meta.AD(id = ISampleStoreForwardClient.STOREFORWARD, name = "%storeforward.name", description = "%storeforward.description", required = false, deflt = "")
        String storeforward();
    }

    /**
     * Logger for SampleStoreForward
     */
    static Logger                      _logger          = LoggerFactory.getLogger(SampleStoreForwardClient.class);

    @SuppressWarnings("javadoc")
    IStoreForward                      storeforward;

    private Map<String, IStoreForward> mapStoreforward  = new HashMap<String, IStoreForward>();
    private String                     storeForwardName = null;
    private int                        UPDATE_INTERVAL  = 60;                                                     // In secs
    private DataProducer               producerRunner   = new DataProducer(this.UPDATE_INTERVAL);
    private Thread                     producer         = new Thread(this.producerRunner);

    /**
     * @param ctx context of the bundle.
     */
    @Activate
    public void activate(ComponentContext ctx)
    {

        if ( _logger.isDebugEnabled() )
        {
            _logger.debug("SampleStoreForwardClient service started."); //$NON-NLS-1$
        }

        // setup callback for storeforward.
        Dictionary<?, ?> config = ctx.getProperties();

        this.storeForwardName = (String) config.get(STOREFORWARD);

        synchronized (this)
        {
            this.storeforward = this.mapStoreforward.get(this.storeForwardName);
            if ( this.storeforward != null )
            {
                _logger.info("################## The StoreForward name is :" + this.storeForwardName + " ######################"); //$NON-NLS-1$ //$NON-NLS-2$
                this.storeforward.setForwardCallback(this);
                if ( !this.producer.isAlive() ) this.producer.start();
            }
        }
    }

    /**
     * @param ctx context of the bundle.
     */
    @Deactivate
    public void deactivate(ComponentContext ctx)
    {
        this.storeforward = null;
        this.mapStoreforward.clear();
        this.producerRunner.stop();

        if ( _logger.isDebugEnabled() )
        {
            _logger.debug("Spillway service deactivated."); //$NON-NLS-1$
        }
    }

    /**
     * @param storefwd StoreForward to add.
     * @throws StoreForwardException exception thrown
     */
    @Reference(type = '*')
    public synchronized void addStoreForward(IStoreForward storefwd)
    {
        // add storeforward instance
        this.mapStoreforward.put(storefwd.getName(), storefwd);
        if ( this.storeForwardName != null )
        {
            this.storeforward = this.mapStoreforward.get(this.storeForwardName);
            if ( this.storeforward != null )
            {
                _logger.info("@@@@@@@@@@@@@@@@@ The StoreForward name is :" + this.storeForwardName + " ######################"); //$NON-NLS-1$ //$NON-NLS-2$
                this.storeforward.setForwardCallback(this);
                if ( !this.producer.isAlive() ) this.producer.start();
            }
        }
    }

    /**
     * @param storefwd StoreForward to add.
     */
    public synchronized void removeStoreForward(IStoreForward storefwd)
    {
        if ( this.storeForwardName != null )
        {
            // check the name of storeforward & unset
            if ( storefwd.getName().equals(this.storeForwardName) )
            {
                this.storeforward = null;
            }
        }
        this.mapStoreforward.remove(storefwd);
    }

    /*
     * Log the data to the log file as the forward functionality. Always return ForwardStatus.SUCCESSFUL
     * (non-Javadoc)
     * @see com.ge.dspmicro.storeforward.api.IForwardCallback#forward(com.ge.dspmicro.storeforward.api.IPersistable)
     */
    @Override
    public ForwardStatus forward(IPersistable data)
            throws StoreForwardException
    {
        // Received data from callback.
        if ( data.getData() == null ) return null;

        StringBuilder sb = new StringBuilder(2048); // Define a size if you have an idea of it.
        char[] read = new char[128]; // Your buffer size.

        try (InputStreamReader ir = new InputStreamReader(data.getData(), StandardCharsets.UTF_8))
        {
            for (int i; -1 != (i = ir.read(read)); sb.append(read, 0, i))
            {
                // do nothing
            }
        }
        catch (Throwable t) // NOSONAR
        {
            // ignore
        }

        _logger.info("Received to forward data with data: " + sb.toString()); //$NON-NLS-1$

        // When signal recieved, send the ForwardStatus
        return ForwardStatus.SUCCESSFUL;
        // return ForwardStatus.FAILED;

    }

    /**
     * A thread to produce data for persistence.
     * 
     * @author Predix Machine Sample
     */
    public class DataProducer
            implements Runnable
    {
        private int                 updateInterval;
        private final AtomicBoolean threadRunning = new AtomicBoolean();

        // private IStoreForward storeforward;
        /**
         * Constructor
         * 
         * @param adapter machine adapter
         * @param subName Name of this subscription
         * @param updateInterval in milliseconds
         * @param nodes list of nodes for this subscription
         */
        public DataProducer(int updateInterval)
        {
            if ( updateInterval > 0 )
            {
                this.updateInterval = updateInterval;
            }
            else
            {
                throw new IllegalArgumentException("updataInterval must be greater than zero."); //$NON-NLS-1$
            }
            this.threadRunning.set(false);
        }

        @Override
        public void run()
        {
            if ( !this.threadRunning.get() )
            {
                this.threadRunning.set(true);

                while (this.threadRunning.get())
                {
                    Map<String, String> dataProps;

                    dataProps = new HashMap<String, String>();
                    dataProps.put("name", "SampleStoreForwardClient"); //$NON-NLS-1$ //$NON-NLS-2$

                    // Generate data for each node and push data update.
                    for (int i = 0; i < 10; i++)
                    {
                        // Simulating the data.
                        PersistenceObject value = new PersistenceObject();
                        value.setTimestamp(System.currentTimeMillis());
                        value.setProperties(dataProps);
                        value.setData(new ByteArrayInputStream(("The Sample Data " + i).getBytes())); //$NON-NLS-1$
                        _logger.info("Sending data to queue: The Sample Data " + i); //$NON-NLS-1$ 

                        // Writing the simulated data into cache
                        SampleStoreForwardClient.this.storeforward.add(value);
                    }
                    try
                    {
                        // Wait for an updateInterval period before pushing next data update.
                        Thread.sleep(this.updateInterval * 1000); // Convert sec to ms.
                    }
                    catch (InterruptedException e)
                    {
                        // ignore interrupted exception
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

    /*
     * (non-Javadoc)
     * @see com.ge.dspmicro.storeforward.api.IForwardCallback#forward(com.ge.dspmicro.validateroute.api.IPingMessage)
     */
    @Override
    public void forward(IPingMessage pingMessage)
            throws StoreForwardException
    {
        pingMessage.pong("STOREFORWARD_FORWARD", PongStatus.SUCCESSFUL, "Ping message forwarded successfully."); //$NON-NLS-1$ //$NON-NLS-2$
        // You may forward the ping message to other components here.
    }
}
