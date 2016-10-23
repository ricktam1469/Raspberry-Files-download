/*
 * Copyright (c) 2014 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */

package com.ge.dspmicro.sample.websocketserver;

import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

/**
 *
 * @author Predix Machine Sample
 */
@Component(name = WebSocketServerConnectorSample.SERVICE_PID)
public class WebSocketServerConnectorSample
{
    /**
     * SERVICE_PID String used as the component service name
     */
    protected final static String SERVICE_PID = "com.ge.dspmicro.wsserver.sample";                            //$NON-NLS-1$
    private static Logger         _logger     = LoggerFactory.getLogger(WebSocketServerConnectorSample.class);
    private ServerContainer       serverContainer;

    /**
     * Adds a server endpoint to the server container
     * 
     * @throws DeploymentException Depoloyment exception thrown if server deployment is unsuccessful
     */
    @Activate
    protected void addServer()
            throws DeploymentException
    {
        try
        {
            this.serverContainer.addEndpoint(WebSocketServerMsgHandlerSample.class);
            _logger.info("Server: Server endpoint deployed");    //endpoint is immediately available //$NON-NLS-1$
        }
        catch (DeploymentException e)
        {
            _logger.error("Server: Error occurred while deploying the server endpoint", e); //$NON-NLS-1$
        }
    }

    /**
     * Dependency injection of server container
     * 
     * @param serverContainer The server container
     */
    @Reference
    protected void setServerContainerContainer(ServerContainer serverContainer)
    {
        this.serverContainer = serverContainer;
    }

    /**
     * Unsets the server container
     * 
     * @param serverContainer The server container
     */
    protected void unsetServerContainerContainer(@SuppressWarnings("hiding") ServerContainer serverContainer)
    {
        this.serverContainer = null;
    }
}
