/*
 * Copyright (c) 2015 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */

package com.ge.dspmicro.sample.mqttclient;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;

/**
 * 
 * @author Predix Machine Sample
 */

// @Component is use to register this component as Service in the container.
@SuppressWarnings("nls")
@Component(name = MqttClientSample.SERVICE_PID, provide = {}, designate = MqttClientSample.Configuration.class, configurationPolicy = ConfigurationPolicy.require)
public class MqttClientSample
        implements MqttCallback
{
    // Meta mapping for configuration properties
    @SuppressWarnings("javadoc")
    @Meta.OCD(name = "%component.name", localization = "OSGI-INF/l10n/bundle")
    interface Configuration
    {
        /** [Required] A topic is a UTF-8 string, which is used by the broker to filter messages for each connected client. */
        @Meta.AD(name = "%topic.name", description = "%topic.description", id = MQTT_TOPIC, required = true, deflt = "")
        String topic();

        /**
         * [Required] The broker is primarily responsible for receiving all messages, filtering them, decide who is interested in it and then sending the
         * message to all
         * subscribed clients. Example, tcp://hostname:port
         */
        @Meta.AD(name = "%broker.name", description = "%broker.description", id = MQTT_BROKER, required = true, deflt = "")
        String broker();

        /**
         * [Required] An agreement between sender and receiver of a message regarding the guarantees of delivering a message. Valid value: 0, 1, 2.
         */
        @Meta.AD(name = "%qos.name", description = "%qos.description", id = MQTT_QOS, required = true, deflt = "2")
        int qos();

        /** [Required] The client identifier (short ClientId) is an identifier of each MQTT client connecting to a MQTT broker. */
        @Meta.AD(name = "%clientId.name", description = "%clientId.description", id = MQTT_CLIENT_ID, required = true, deflt = "")
        String clientId();

        /** [Optional] User name for authentication if the broker requires. */
        @Meta.AD(name = "%userName.name", description = "%userName.description", id = MQTT_USER_NAME, required = false, deflt = "")
        String userName();

        /** [Optional] Password for authentication if the broker requires. */
        @Meta.AD(name = "%password.name", description = "%password.description", id = MQTT_PASSWORD, required = false, deflt = "")
        String password();
    }

    /** service id */
    protected final static String SERVICE_PID    = "com.ge.dspmicro.sample.mqttclient";

    private final static String   MQTT_TOPIC     = SERVICE_PID + ".topic";
    private final static String   MQTT_BROKER    = SERVICE_PID + ".broker";
    private final static String   MQTT_QOS       = SERVICE_PID + ".qos";
    private final static String   MQTT_CLIENT_ID = SERVICE_PID + ".clientId";
    private final static String   MQTT_USER_NAME = SERVICE_PID + ".userName";
    private final static String   MQTT_PASSWORD  = SERVICE_PID + ".password";

    /** Create logger to report errors, warning massages, and info messages (runtime Statistics) */
    protected static Logger       _logger        = LoggerFactory.getLogger(MqttClientSample.class);

    /** Sample client */
    private MqttClient            sampleClient;

    /**
     * The activate method is called when bundle is started.
     * 
     * @param ctx Component Context.
     */
    @Activate
    public void activate(ComponentContext ctx)
    {

        // use the logger service for debugging purpose
        if ( _logger.isDebugEnabled() )
        {
            _logger.debug("Starting sample " + ctx.getBundleContext().getBundle().getSymbolicName());
        }

        Configuration config = Configurable.createConfigurable(Configuration.class, ctx.getProperties());

        String contentPublished = "Message from MqttPublishSample"; //$NON-NLS-1$
        MemoryPersistence persistence = new MemoryPersistence();

        try
        {
            // create a synchronous MQTT client
            this.sampleClient = new MqttClient(config.broker(), config.clientId(), persistence);

            // change the connection options
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            if ( config.userName() != null && !config.userName().trim().isEmpty() )
            {
                connOpts.setUserName(config.userName());
                if ( config.password() != null )
                {
                    connOpts.setPassword(config.password().toCharArray());
                }
            }

            // set the call back for publish and subscribe
            this.sampleClient.setCallback(this);

            // connect to the broker
            if ( _logger.isInfoEnabled() )
            {
                _logger.info("Connecting to broker: " + config.broker());
            }
            this.sampleClient.connect(connOpts);
            if ( _logger.isInfoEnabled() )
            {
                _logger.info("Connected");
            }

            // subscribe to the topic
            this.sampleClient.subscribe(config.topic(), config.qos());
            if ( _logger.isInfoEnabled() )
            {
                _logger.info("Subscribed to topic:  " + config.topic());
            }

            // publish the message
            MqttMessage message = new MqttMessage(contentPublished.getBytes());
            message.setQos(config.qos());
            this.sampleClient.publish(config.topic(), message);
            if ( _logger.isInfoEnabled() )
            {
                _logger.info("Message published");
            }

        }
        catch (MqttException me)
        {
            _logger.error("Failed to publish message.", me);
        }
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
        if ( this.sampleClient != null )
        {
            try
            {
                // disconnect the broker
                this.sampleClient.disconnect();
                if ( _logger.isInfoEnabled() )
                {
                    _logger.info("Disconnected");
                }
            }
            catch (MqttException e)
            {
                _logger.error("Failed to disconnect.", e);
            }
        }

        if ( _logger.isDebugEnabled() )
        {
            _logger.debug("Stopped sample for " + ctx.getBundleContext().getBundle().getSymbolicName()); //$NON-NLS-1$
        }
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.paho.client.mqttv3.MqttCallback#connectionLost(java.lang.Throwable)
     */
    @Override
    public void connectionLost(Throwable cause)
    {
        if ( _logger.isInfoEnabled() )
        {
            _logger.info("Connection is lost.");
        }
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.paho.client.mqttv3.MqttCallback#messageArrived(java.lang.String, org.eclipse.paho.client.mqttv3.MqttMessage)
     */
    @Override
    public void messageArrived(String contentTopic, MqttMessage message)
            throws Exception
    {
        if ( _logger.isInfoEnabled() )
        {
            _logger.info(String.format("Message received. Topic: %s Message: %s", contentTopic,
                    new String(message.getPayload())));
        }
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.paho.client.mqttv3.MqttCallback#deliveryComplete(org.eclipse.paho.client.mqttv3.IMqttDeliveryToken)
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token)
    {
        if ( _logger.isInfoEnabled() )
        {
            _logger.info("Message delivered.");
        }
    }

}
