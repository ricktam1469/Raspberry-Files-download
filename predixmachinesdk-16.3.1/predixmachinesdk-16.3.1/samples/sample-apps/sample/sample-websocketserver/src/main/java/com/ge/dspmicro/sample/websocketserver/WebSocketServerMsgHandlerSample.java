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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Predix Machine Sample
 */

@ServerEndpoint(value = "/testwebsocketendpoint")
public class WebSocketServerMsgHandlerSample
{
    private static Logger _logger = LoggerFactory.getLogger(WebSocketServerMsgHandlerSample.class);

    /**
     * 
     * @param session The web socket session
     */
    @OnOpen
    public void onOpen(Session session)
    {
        _logger.info("Server: opened... " + session.getId()); //$NON-NLS-1$
    }

    /**
     * Defines the behavior of the server when a string message is received.
     * Receives the message from the client, prints to the logger, and sends back.
     * 
     * @param message The string message that was received
     * @param session The web socket session
     * @return An echo string message that is sent to the client
     */
    @OnMessage
    public String onStringMessage(String message, Session session)
    {
        _logger.info("Server: received... " + message + ". Sending back"); //$NON-NLS-1$ //$NON-NLS-2$
        return message;
    }

    /**
     * Defines the behavior of the server when a ByteBuffer message is received.
     * Receives the message from the client, prints to the logger, and sends back.
     * 
     * @param message The ByteBuffer message that was received
     * @param session The web socket session
     * @return An echo ByteBuffer message that is sent to the client
     */
    @OnMessage
    public ByteBuffer onByteMessage(ByteBuffer message, Session session)
    {
        String result = new String(message.array(), Charset.forName("UTF-8")); //$NON-NLS-1$
        _logger.info("Server: received... " + result + ". Sending back"); //$NON-NLS-1$ //$NON-NLS-2$
        return message;
    }

    /**
     * Defines the behavior of the server when the session is closed.
     * Prints to the logger.
     * 
     * @param session The web socket session
     * @param closeReason Provides information on the session close including
     *            close reason phrase, code, etc...
     */
    @OnClose
    public void onClose(Session session, CloseReason closeReason)
    {
        _logger.info("Server: Session " + session.getId() + " closed because of " + closeReason.toString()); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
