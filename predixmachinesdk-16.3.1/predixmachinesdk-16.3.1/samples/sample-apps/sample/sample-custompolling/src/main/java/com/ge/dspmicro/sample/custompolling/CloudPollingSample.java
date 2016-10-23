/*
 * Copyright (c) 2016 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */

package com.ge.dspmicro.sample.custompolling;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.proximetry.osgiagent.api.DevicesFacade;
import com.proximetry.osgiagent.api.Result;
import com.proximetry.osgiagent.api.Result.Status;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

/**
 * This is an example of how to poll the cloud for updates and commands on demand (on a programmatic schedule
 * instead of periodic schedule defined in the configuration file).
 * 
 * This example write the polling state to "on demand" when this sample is activated and back to polling when done. 
 * This normally would not be done and instead just modify the property to the correct state instead of using code.
 * 
 * @author Predix Machine Cloud Polling Sample
 */

// @Component is use to register this component as Service in the container.

@Component(name = CloudPollingSample.SERVICE_PID)
public class CloudPollingSample

{
    /** service id */
    protected final static String   SERVICE_PID = "com.ge.dspmicro.sample.cloudpolling";            //$NON-NLS-1$
    private final static String     PROXIMETRY_PID = "com.proximetry.osgiagent.impl.DevicesService";   //$NON-NLS-1$ 
    // Next polling interval/
    private final static int        POLLTIME = 5;        

    /** Create logger to report errors, warning massages, and info messages (runtime Statistics) */
    protected static Logger          _logger     = LoggerFactory.getLogger(CloudPollingSample.class);
    
    private ConfigurationAdmin      configAdmin;
    private DevicesFacade           devicesFacade;

    /**
     * The activate method is called when bundle is started.
     * 
     * @param ctx Component Context.
     */
    @SuppressWarnings("nls")
    @Activate
    public void activate(ComponentContext ctx)
    {
        // use the logger service for debugging purpose
        if ( _logger.isDebugEnabled() )
        {
            _logger.debug("Starting sample " + ctx.getBundleContext().getBundle().getSymbolicName());
        }
        
        setPollingState(false);
        
        // Create a thread to do the on demand polling.
        Runnable runner = new Runnable()
        {
            @Override
            public void run()
            {
                DevicesFacade facade = null;
                while ((facade = getDevicesFacade()) != null)
                {
                    try
                    {
                        // Report to cloud (poll for updates/commands) now and we tell the DevicesFacade the next time we poll again (in seconds).
                        // This is important for the cloud to know when to expect the next connection from this device so that 
                        // the EdgeManager can correctly show if the device is "unreachable" or "reachable". 
                        Future<Result<Void>> future = facade.reportToCloud(POLLTIME);
                        Result<Void> result = future.get();
                        if (result.status != Status.OK)
                        {
                            _logger.info("Proximetry polling failed: " + result.error + "(" + result.errorCode + ")");
                        }
                        else
                        {
                            _logger.info("Proximetry polling completed. Next poll in " + POLLTIME + " seconds.");
                        }
                    }
                    catch (Exception ee)
                    {
                        // reportToCloud throws an exception when not configured. This will try again.
                        _logger.info("Proximetry polling failed.", ee);
                    }
                    
                    // sleep for the poll time to make the request again.
                    try
                    {
                        TimeUnit.SECONDS.sleep(POLLTIME);
                    }
                    catch (InterruptedException e)
                    {
                        // no error
                    } 
                }
            }
        };
            
        new Thread(runner).start();

    }

    /**
     * This method is called when the bundle is stopped.
     * 
     * @param ctx Component Context
     */
    @Deactivate
    public void deactivate(ComponentContext ctx)
    {
        // Put your clean up code here when container is shutting down

        if ( _logger.isDebugEnabled() )
        {
            _logger.debug("Stopped sample for " + ctx.getBundleContext().getBundle().getSymbolicName()); //$NON-NLS-1$
        }
        setPollingState(true);
    }

    /**
     * @return the proximetry API devicesFacade
     */
    public DevicesFacade getDevicesFacade()
    {
        return this.devicesFacade;
    }

    /**
     * Use Dependency injection of the devicesFacade service
     * 
     * @param devicesFacade the proximetry API to set
     */
    @Reference
    public void setDevicesFacade(DevicesFacade devicesFacade)
    {
        this.devicesFacade = devicesFacade;
    }

    /**
     * clears the dependency injection of the devicesFacade service
     * 
     * @param devicesFacade The proximetry API to unset
     */
    public void unsetDevicesFacade(@SuppressWarnings("hiding") DevicesFacade devicesFacade)
    {
        this.devicesFacade = null;
    }
    
    /**
     * Dependency injection of ConfigurationAdmin OSGi registered service.
     * 
     * @param configAdmin
     *            OSGi registered implementation of ConfigurationAdmin interface.
     */
    @Reference
    public void setConfigAdmin(ConfigurationAdmin configAdmin)
    {
        this.configAdmin = configAdmin;
    }

    /**
     * clear the Dependency injection of ConfigurationAdmin
     * 
     * @param configAdmin
     *            the ConfigurationAdmin to clear
     */
    public void unsetConfigAdmin(@SuppressWarnings("hiding") ConfigurationAdmin configAdmin)
    {
        if ( this.configAdmin == configAdmin )
        {
            this.configAdmin = null;
        }
    }
    
    // Set the polling type. This allows for a container to easily push between on-demand and periodic.
    private void setPollingState(boolean bPeriodic)
    {
        String type = bPeriodic ? "PERIODIC" : "ON_DEMAND";  //$NON-NLS-1$//$NON-NLS-2$
        _logger.info("Setting polling state to " + type); //$NON-NLS-1$
        
        try
        {
            Configuration configuration = this.configAdmin.getConfiguration(PROXIMETRY_PID);
            Dictionary<String, Object> props = cloneConfigurationsProperties(configuration.getProperties());
            
            if (!type.equals(props.get("cloud.reporting.mode"))) //$NON-NLS-1$
            {
                props.put("cloud.reporting.mode", type);    //$NON-NLS-1$
                configuration.update(props);
            }
        }
        catch (Exception e)
        {
            _logger.error("Failed setting polling state to " + type, e); //$NON-NLS-1$
            return;
        }
    }

    /**
     * When calling configuration manager to update a property we should never use the original properties object 
     * as this is designated as "read-only" by OSGi. In addition, this may write properties from OSGi into the configuration 
     * file that causes modify on the service to be called over and over.
     * 
     * @param origProps  the properties return from ConfigurationAdmin-instance.getConfiguration(pid).getProperties();
     * @return clone of the properties ready to have values set.
     */
    private Dictionary<String, Object> cloneConfigurationsProperties(Dictionary<String, Object> origProps)
    {
        // The origDictionary is 'readonly' and this is enforced by prosyst.
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        Enumeration<String> en = origProps.keys();
        while (en.hasMoreElements())
        {
            String key = en.nextElement();
            // Don't copy items that are added by OSGi, since we don't want them written into the properties file.
            // writing "service.bundleLocation" into the property file will cause a service modify method to be called over and over.
            if (Constants.SERVICE_PID.equals(key) || "service.bundleLocation".equals(key) )   //$NON-NLS-1$
            {
                continue;
            }
            props.put(key, origProps.get(key));
        }
        return props;
    }

}
