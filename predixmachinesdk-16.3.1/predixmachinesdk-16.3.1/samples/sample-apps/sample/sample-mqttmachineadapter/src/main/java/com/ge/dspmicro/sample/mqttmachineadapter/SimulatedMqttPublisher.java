/*
 * Copyright (c) 2015 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */

package com.ge.dspmicro.sample.mqttmachineadapter;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ge.dspmicro.machinegateway.api.adapter.ISubscriptionMachineAdapter;
import com.ge.dspmicro.security.admin.api.ISecurityUtils;

/**
 * Simulated MQTT client to publish message to MQTT Adapter broker
 * 
 * @author Predix Machine Sample
 */
public class SimulatedMqttPublisher
{
    private static final Logger _logger                = LoggerFactory.getLogger(SimulatedMqttPublisher.class);

    private static final String SERVICE_PID            = "com.ge.dspmicro.machineadapter.mqtt";                //$NON-NLS-1$
    private final static String PROPKEY_MQTT_BROKER    = SERVICE_PID + ".broker";                              //$NON-NLS-1$
    private final static String PROPKEY_MQTT_USER_NAME = SERVICE_PID + ".userName";                            //$NON-NLS-1$
    private final static String PROPKEY_MQTT_PASSWORD  = SERVICE_PID + ".password";                            //$NON-NLS-1$

    /**
     * @param adapterName MQTT adapter name
     * @param securityUtils security util
     * @return MQTT client
     * @throws MqttException exception
     */
    public static MqttClient getMqttClient(String adapterName, ISecurityUtils securityUtils)
            throws MqttException
    {
        ServiceReference<ISubscriptionMachineAdapter> mqttAdapterRef = getMqttAdapter(adapterName);

        String broker = (String) mqttAdapterRef.getProperty(PROPKEY_MQTT_BROKER);
        if (null == broker || broker.isEmpty())
        {
            _logger.warn("\n\n\n****************\n\nNo broker provided. Client cannot be created.\n\n****************\n"); //$NON-NLS-1$
            return null;
        }
        String clientId = MqttClient.generateClientId();
        String userName = (String) mqttAdapterRef.getProperty(PROPKEY_MQTT_USER_NAME);
        // if the password is encrypted, decrypt it.
        char[] decryptedPassword = securityUtils.getDecryptedConfigProperty(
                (String) mqttAdapterRef.getProperty(Constants.SERVICE_PID), PROPKEY_MQTT_PASSWORD);
        String password = new String(decryptedPassword);

        MemoryPersistence persistence = new MemoryPersistence();
        MqttClient testClient = new MqttClient(broker, clientId, persistence);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        connOpts.setUserName(userName);
        connOpts.setPassword(password.toCharArray());

        _logger.info("Connecting to broker: " + broker); //$NON-NLS-1$
        testClient.connect(connOpts);
        _logger.info("Connected"); //$NON-NLS-1$
        return testClient;
    }

    private static ServiceReference<ISubscriptionMachineAdapter> getMqttAdapter(String adapterName)
    {
        try
        {
            BundleContext ctx = FrameworkUtil.getBundle(SimulatedMqttPublisher.class).getBundleContext();
            @SuppressWarnings("unchecked")
            ServiceReference<ISubscriptionMachineAdapter>[] adapters = (ServiceReference<ISubscriptionMachineAdapter>[]) ctx
                    .getAllServiceReferences(ISubscriptionMachineAdapter.class.getName(), null);
            if ( adapters != null )
            {
                for (ServiceReference<ISubscriptionMachineAdapter> adapter : adapters)
                {
                    ISubscriptionMachineAdapter service = ctx.getService(adapter);
                    if ( (service.getInfo() != null) && (service.getInfo().getName() != null)
                            && service.getInfo().getName().equals(adapterName) )
                    {
                        return adapter;
                    }
                }
            }
        }
        catch (InvalidSyntaxException e)
        {
            _logger.error("Failed to find adapter instance with name " + adapterName, e); //$NON-NLS-1$
        }
        return null;
    }
}
