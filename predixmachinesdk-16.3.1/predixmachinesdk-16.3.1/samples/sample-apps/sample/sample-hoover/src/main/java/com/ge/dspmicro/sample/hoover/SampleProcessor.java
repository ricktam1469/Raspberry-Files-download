/*
 * Copyright (c) 2015 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */

package com.ge.dspmicro.sample.hoover;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import com.ge.dspmicro.hoover.api.processor.IProcessor;
import com.ge.dspmicro.hoover.api.processor.ProcessorException;
import com.ge.dspmicro.hoover.api.spillway.ITransferData;
import com.ge.dspmicro.machinegateway.types.ITransferable;
import com.ge.dspmicro.machinegateway.types.PDataValue;
import com.ge.dspmicro.security.admin.api.ISecurityUtils;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;

/**
 * This class provides the sample for a Processor implementation which will process the data as per configuration on the spillway.
 * @author Predix Machine Sample
 */
@Component(name = SampleProcessor.SERVICE_PID, configurationPolicy = ConfigurationPolicy.optional, provide =
{
    IProcessor.class
})
public class SampleProcessor
        implements IProcessor
{
    /**
     * Service PID
     */
    public static final String                      SERVICE_PID      = "com.ge.dspmicro.sample.hoover.processor";                  //$NON-NLS-1$

    /**
     * process type which converts data value to upper case
     */
    public static final String                      pTypeToUpperCase = "toUpperCase";                                              //$NON-NLS-1$
    /**
     * process type which converts data value to lower case
     */
    public static final String                      pTypeToLowerCase = "toLowerCase";                                              //$NON-NLS-1$
    /**
     * process type which converts data value to upper case
     */
    public static final String                      pTypeToXOR       = "toXORwithPredix";                                          //$NON-NLS-1$
    /**
     * process type which print
     */
    public static final String                      pPrint           = "print";                                                    //$NON-NLS-1$
    /**
     * process type which print
     */
    public static final String                      pEncrypt         = "encrypt";                                                  //$NON-NLS-1$

    /** Create logger to report errors, warning massages, and info messages (runtime Statistics) */
    protected static Logger                         _logger          = LoggerFactory.getLogger(SampleProcessor.class);

    /** The map is used to buffer data */
    private Map<ITransferData, List<ITransferable>> buffer           = new ConcurrentHashMap<ITransferData, List<ITransferable>>();

    /** The map is used to store lock objects for buffer data */
    private Map<ITransferData, Object>              bufferLock       = new ConcurrentHashMap<ITransferData, Object>();

    /** Create logger to report errors, warning massages, and info messages (runtime Statistics) */
    private int                                     buffer_threshold = 10;

    /** If client choose to copy the message to a Rabbit MQ for analysis/ archival/ alerting etc, set this flag to true otherwise false */
    private boolean                                 msgCopy          = false;
    /**
     * Connection to the RMQ
     */
    private Connection                              conn;
    /**
     * Channel to the RMQ
     */
    private Channel                                 channel;

    private ISecurityUtils                          securityUtils;

    /**
     * @param ctx context of the bundle.
     */
    @Activate
    public void activate(ComponentContext ctx)
    {
        // Rabbit MQ connection. By Default it will connect to local RMQ server
        // If remote RMQ server, specify connection details
        ConnectionFactory factory = new ConnectionFactory();
        // factory.setUsername(userName);
        // factory.setPassword(password);
        // factory.setVirtualHost(virtualHost);
        // factory.setHost(hostName);
        // factory.setPort(portNumber);
        try
        {
            this.conn = factory.newConnection();
            this.channel = this.conn.createChannel();
        }
        catch (IOException e)
        {
            // If not using RMQ, or RMQ server down, the msgCopy flag is turned off and processing is proceeded without Messaging logic.
            _logger.info(" Exception in activate " + e.getMessage()); //$NON-NLS-1$
            this.msgCopy = false;
        }
    }

    /**
     * @param ctx context of the bundle.
     */
    @Deactivate
    public void deactivate(ComponentContext ctx)
    {
        try
        {
            // Empty the buffer before leaving
            Set<ITransferData> keys = this.bufferLock.keySet();
            for (ITransferData key : keys)
            {
                // Synchronized block to send/append/empty the buffer.
                synchronized (this.bufferLock.get(key))
                {
                    // Buffer data and send when over threshold
                    List<ITransferable> data = this.buffer.get(key);

                    this.buffer.put(key, data);
                    if ( data != null && data.size() > 0 )
                    {
                        if ( this.msgCopy ) sendToMQ(key.toString(), data);
                        key.transferData(data, null);
                        // Empty buffer after transfer
                        this.buffer.put(key, new ArrayList<ITransferable>());
                    }
                }
            }

            // Remove listener from subscriptions.
            if(this.channel != null)
            {
                this.channel.close();
            }
            if(this.conn != null)
            {
                this.conn.close();
            }
        }
        catch (IOException e)
        {
            _logger.error("Exception in deactivate.", e); //$NON-NLS-1$
        }

        if ( _logger.isDebugEnabled() )
        {
            _logger.debug("Spillway service deactivated."); //$NON-NLS-1$
        }
    }

    /**
     * @param securityUtils the securityUtils to set
     */
    @Reference
    public void setSecurityUtils(ISecurityUtils securityUtils)
    {
        this.securityUtils = securityUtils;
    }
    /*
     * (non-Javadoc)
     * @see com.ge.dspmicro.hoover.api.processor.IProcessor#processValues(java.lang.String, java.util.List, com.ge.dspmicro.hoover.api.spillway.ITransferData)
     */
    /**
     * @deprecated The processValues method with the new properties parameter should be used
     */
    @SuppressWarnings("deprecation")
	@Deprecated
    @Override
    public void processValues(String processType, List<ITransferable> values, ITransferData transferData)
            throws ProcessorException
    {
    	this.processValues(processType, null, values, transferData);
    }
    /*
     * (non-Javadoc)
     * @see com.ge.dspmicro.hoover.api.processor.IProcessor#processValues(java.lang.String, java.util.List, com.ge.dspmicro.hoover.api.spillway.ITransferData)
     */
    /**
     * @since 16.3.0
     */
    @Override
    public void processValues(String processType, Map<String,String> properties, List<ITransferable> values, ITransferData transferData)
            throws ProcessorException
    {
       
    	//If the adapter provided the name of the subcription for which the data was received,
    	//it is available as a property here
    	
    	String subscriptionName=null;
    	if (properties != null)
    	{
    		subscriptionName=properties.get(ITransferData.PROPKEY_SUBSCRIPTION);
    	}
    	
        synchronized (transferData)
        {
            Object lock = this.bufferLock.get(transferData);

            if ( lock == null )
            {
                lock = new Object();
                this.bufferLock.put(transferData, lock);
            }
        }

        // Switch cases for appropriate processing
        switch (processType)
        {
            case pPrint:
            	_logger.info("Received Data for Subscription: " + subscriptionName); //$NON-NLS-1$
                for (ITransferable value : values)
                {
                    _logger.info(" value : " + value.toString());    //$NON-NLS-1$
                }
                break;
            case pTypeToUpperCase:
                for (ITransferable value : values)
                {
                    if ( value instanceof PDataValue )
                    {
                        ((PDataValue) value).getEnvelope().setValue(
                                ((PDataValue) value).getEnvelope().getValue().toString().toUpperCase());
                    }
                }
                break;
            case pTypeToLowerCase:
                for (ITransferable value : values)
                {
                    if ( value instanceof PDataValue )
                    {
                        ((PDataValue) value).getEnvelope().setValue(
                                ((PDataValue) value).getEnvelope().getValue().toString().toLowerCase());
                    }
                }
                break;
            case pTypeToXOR:
                for (ITransferable value : values)
                {
                    if ( value instanceof PDataValue )
                    {
                        ((PDataValue) value).getEnvelope().setValue(
                                xorWithPredix(((PDataValue) value).getEnvelope().getValue().toString()));
                    }
                }
                break;
            case pEncrypt:
                for (ITransferable value : values)
                {
                    if ( value instanceof PDataValue )
                    {
                        ((PDataValue) value).getEnvelope().setValue(
                                this.securityUtils.encrypt(((PDataValue) value).getEnvelope().getValue().toString()
                                        .toCharArray()));
                    }
                }
                break;
            default:
                break;
        }

        // Synchronized block to send/append/empty the buffer.
        synchronized (this.bufferLock.get(transferData))
        {
            // Buffer data and send when over threshold
            List<ITransferable> data = this.buffer.get(transferData);
            if ( data == null )
            {
                data = values;
            }
            else
            {
                data.addAll(values);
            }
            //
            this.buffer.put(transferData, data);
            if ( data.size() >= this.buffer_threshold )
            {
                // The name of the object Instance is unique, maintain uniqueness in toString of Spillway. Used as key
                if ( this.msgCopy ) sendToMQ(transferData.toString(), data);
                transferData.transferData(data, null);
                // Empty buffer after transfer
                this.buffer.put(transferData, new ArrayList<ITransferable>());
            }
        }
    }

    /**
     * @param value String to be XORed
     * @return XORed string
     */
    public String xorWithPredix(String value)
    {
        String key = "predix"; //$NON-NLS-1$
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < value.length(); k++)
            sb.append((value.charAt(k) ^ key.charAt(k % key.length())));
        String result;
        result = sb.toString();
        return result;
    }

    private void sendToMQ(String river, List<ITransferable> values)
    {
        _logger.info("Sending to MQ to Queue :" + river); //$NON-NLS-1$
        try
        {
            this.channel.queueDeclare(river, false, false, false, null);
            for (ITransferable value : values)
            {
                if ( value instanceof PDataValue )
                    this.channel.basicPublish("", river, null, value.toString().getBytes()); //$NON-NLS-1$
            }
        }
        catch (Exception e)
        {
            _logger.error("sendToMQ failed. ", e); //$NON-NLS-1$
        }
    }
}
