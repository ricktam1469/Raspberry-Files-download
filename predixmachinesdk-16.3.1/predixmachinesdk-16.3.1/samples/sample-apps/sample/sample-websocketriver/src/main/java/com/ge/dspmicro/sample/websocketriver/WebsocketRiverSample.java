/*
 * Copyright (c) 2015 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */

package com.ge.dspmicro.sample.websocketriver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ge.dspmicro.machinegateway.types.PDataValue;
import com.ge.dspmicro.machinegateway.types.PEnvelope;
import com.ge.dspmicro.machinegateway.types.PQuality.QualityEnum;
import com.ge.dspmicro.machinegateway.types.PTimestamp;
import com.ge.dspmicro.websocketriver.send.api.IWebsocketSend;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

/**
 * Sample bundle for Predix Machine WebSocket River.
 * 
 * WebSocketRiver Send is injected into this bundle and passed as parameter to the actual tests.
 * 
 */
@SuppressWarnings("all")
@Component(name = WebsocketRiverSample.SERVICE_PID, provide = {}, configurationPolicy = ConfigurationPolicy.ignore)
public class WebsocketRiverSample
        implements Runnable
{
    protected final static String SERVICE_PID     = "com.ge.dspmicro.websocketriver.sample.send";       //$NON-NLS-1$

    private static final Logger   _logger         = LoggerFactory.getLogger(WebsocketRiverSample.class);

    private IWebsocketSend        wsRiverSend;

    private static final int      RECEIVE_TIMEOUT = 5000;

    /** Lock object to sync the async call and callback. */
    private static Object         _syncLock       = new Object();

    /**
     * Called when the bundle is started ... depending on
     * declarative services settings the bundle may not start
     * if all service dependencies are not met.
     * 
     * @param ctx component context
     */
    @Activate
    protected void activate(ComponentContext ctx)
    {
        if ( _logger.isDebugEnabled() )
        {
            _logger.debug("Starting sample " + ctx.getBundleContext().getBundle().getSymbolicName()); //$NON-NLS-1$
        }
        new Thread(this).start();
    }

    /**
     * Called when the bundle is stopped.
     * 
     * @param ctx component context
     */
    @Deactivate
    protected void deactivate(ComponentContext ctx)
    {
        if ( _logger.isDebugEnabled() )
        {
            _logger.debug("Stopping sample " + ctx.getBundleContext().getBundle().getSymbolicName());      //$NON-NLS-1$
        }

    }

    /**
     * Dependency injection of WebSocket River Send service.
     * 
     * @param sender OSGi registered implementation of IWebsocketSend interface.
     */
    @Reference
    protected void setWsRiverSend(IWebsocketSend sender)
    {
        this.wsRiverSend = sender;
    }

    /**
     * Remove this WebSocketRiver Send service from dependency injection
     * 
     * @param sender OSGi registered implementation of IWebsocketSend interface.
     */
    protected void unsetWsRiverSend(IWebsocketSend sender)
    {
        this.wsRiverSend = null;
    }

    private void sendFormattedData()
            throws InterruptedException, IOException
    {
        _logger.info("Running sendFormatted data with test data"); //$NON-NLS-1$

        // Construct a PDataValue inputstream
        try ( InputStream srcFile = getClass().getResourceAsStream("/sampleTimeSeriesData.txt") ) //$NON-NLS-1$
        {
            _logger.info("Sending sampleTimeSeriesData.txt to Predix TimeSeries Service."); //$NON-NLS-1$

            // Send file
            UUID transferId;
            synchronized (_syncLock)
            {
                transferId = this.wsRiverSend.transfer(srcFile, null);

                _syncLock.wait(RECEIVE_TIMEOUT); // Callback will notify when transfer completes
            }
        }
    }

    private void sendPDataValue()
            throws IOException, URISyntaxException, InterruptedException
    {
        _logger.info("Running sendPDataValue with test data"); //$NON-NLS-1$

        // Construct a PDataValue inputStream
        try ( ByteArrayInputStream pDataInputStream = constructTestPData() )
        {
            _logger.info("Sending test data to cloud."); //$NON-NLS-1$
            // Send the data
            UUID transferId;
            synchronized (_syncLock)
            {
                transferId = this.wsRiverSend.transfer(pDataInputStream, null);

                _syncLock.wait(RECEIVE_TIMEOUT); // Callback will notify when transfer completes
            }
        }
    }

    /**
     * Helper method to create an input stream with a test PDataValue
     * 
     * @return the PDataValue input stream
     * @throws URISyntaxException
     * @throws IOException
     */
    private ByteArrayInputStream constructTestPData()
            throws URISyntaxException, IOException
    {
        UUID id = UUID.randomUUID();
        URI nodeAddress = new URI("adapter://address/id/unit"); //$NON-NLS-1$
        PDataValue pDataValue = new PDataValue("sensor_tag_1234", id, new PEnvelope(4L), QualityEnum.GOOD); //$NON-NLS-1$
        pDataValue.setAddress(nodeAddress);
        pDataValue.setTimestamp(new PTimestamp(1435854353541L));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write('[');
        out.write(pDataValue.toBytes());
        out.write(']');

        return new ByteArrayInputStream(out.toByteArray());
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
        try
        {
            for (int i = 0; i < 10; i++)
            {
                sendPDataValue();
                sendFormattedData();
            }
        }
        catch (Exception e)
        {
            _logger.error("Error encountered while attempting to send the data", e); //$NON-NLS-1$
        }
    }

}
