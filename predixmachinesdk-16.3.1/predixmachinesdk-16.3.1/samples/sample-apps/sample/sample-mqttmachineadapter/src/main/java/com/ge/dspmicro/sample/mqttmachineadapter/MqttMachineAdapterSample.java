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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import com.ge.dspmicro.machineadapter.mqtt.api.MqttDataNode;
import com.ge.dspmicro.machinegateway.api.MachineGatewayException;
import com.ge.dspmicro.machinegateway.api.adapter.IDataSubscription;
import com.ge.dspmicro.machinegateway.api.adapter.IDataSubscriptionListener;
import com.ge.dspmicro.machinegateway.api.adapter.IMachineAdapter;
import com.ge.dspmicro.machinegateway.api.adapter.ISubscriptionMachineAdapter;
import com.ge.dspmicro.machinegateway.types.PDataNode;
import com.ge.dspmicro.machinegateway.types.PDataValue;
import com.ge.dspmicro.security.admin.api.ISecurityUtils;

/**
 * MQTT machine adapter sample to receive MQTT messages
 * 
 * @author Predix Machine Sample
 */
@Component(name = MqttMachineAdapterSample.SERVICE_PID, configurationPolicy = ConfigurationPolicy.optional, provide = {})
public class MqttMachineAdapterSample
        implements Runnable
{
    /** service PID */
    public static final String          SERVICE_PID  = "com.ge.dspmicro.sample.mqttmachineadapter";            //$NON-NLS-1$
    private static final String         ADAPTER_TYPE = "com.ge.dspmicro.machineadapter.mqtt";                  //$NON-NLS-1$
    private static final String         ADAPTER_NAME = "Mqtt Machine Adapter";                                 //$NON-NLS-1$

    /**
     * logger
     */
    static final Logger                 _logger      = LoggerFactory.getLogger(MqttMachineAdapterSample.class);

    private ISecurityUtils              securityUtils;
    private ISubscriptionMachineAdapter mqttAdapter;

    private List<SampleSubscriber>      subscribers  = new ArrayList<>();
    private MqttClient                  mqttClient;

    /**
     * @param ctx component context
     * @throws MqttException exception
     */
    @Activate
    public void activate(ComponentContext ctx)
            throws MqttException
    {

        addSubscribers();
        this.mqttClient = SimulatedMqttPublisher
                .getMqttClient(this.mqttAdapter.getInfo().getName(), this.securityUtils);
        if (null == this.mqttClient)
        {
            return; // can't start
        }

        new Thread(this).start();

        if ( _logger.isDebugEnabled() )
        {
            _logger.debug("Starting sample bundle for " + ctx.getBundleContext().getBundle().getSymbolicName()); //$NON-NLS-1$
        }
    }

    /**
     * @param ctx component context
     */
    @Deactivate
    public void deactivate(ComponentContext ctx)
    {
        clearSubscribers();
        if ( _logger.isDebugEnabled() )
        {
            _logger.debug("Stopping sample bundle for " + ctx.getBundleContext().getBundle().getSymbolicName()); //$NON-NLS-1$
        }
    }

    /**
     * @param securityUtils security utils
     */
    @Reference
    public void setSecurityUtils(ISecurityUtils securityUtils)
    {
        this.securityUtils = securityUtils;
    }

    /**
     * @param securityUtils security utils
     */
    @SuppressWarnings("hiding")
    public void unsetSecurityUtils(ISecurityUtils securityUtils)
    {
        this.securityUtils = null;
    }

    /**
     * @param machineAdapter machine adapter to add.
     * @throws MachineGatewayException exception thrown
     */
    @Reference(type = '*')
    public synchronized void addMachineAdapter(ISubscriptionMachineAdapter machineAdapter)
            throws MachineGatewayException
    {
        if ( machineAdapter.getInfo() != null && ADAPTER_TYPE.equals(machineAdapter.getInfo().getAdapterType())
                && ADAPTER_NAME.equals(machineAdapter.getInfo().getName()) )
        {
            this.mqttAdapter = machineAdapter;
        }
    }

    /**
     * @param machineAdapter machine adapter to remove.
     */
    public synchronized void removeMachineAdapter(IMachineAdapter machineAdapter)
    {
        if ( machineAdapter.getInfo() != null && ADAPTER_TYPE.equals(machineAdapter.getInfo().getAdapterType())
                && ADAPTER_NAME.equals(machineAdapter.getInfo().getName()) )
        {
            this.mqttAdapter = null;
        }
    }

    private void clearSubscribers()
    {
        if ( this.mqttAdapter != null )
        {
            List<IDataSubscription> subscriptions = this.mqttAdapter.getSubscriptions();

            for (IDataSubscription subscription : subscriptions)
            {
                for (SampleSubscriber sampleSubscriber : this.subscribers)
                {
                    subscription.removeDataSubscriptionListener(sampleSubscriber);
                }

            }
        }
    }

    private void addSubscribers()
    {
        if ( this.mqttAdapter == null )
        {
            _logger.warn("\n\n*****************\nNo suitable adapter found registered.\n*****************\n"); //$NON-NLS-1$
            return;
        }
        for (IDataSubscription sub : this.mqttAdapter.getSubscriptions())
        {
            SampleSubscriber subscriber = new SampleSubscriber(sub.getId(), sub.getName());

            sub.addDataSubscriptionListener(subscriber);
            this.subscribers.add(subscriber);
        }
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
        for (IDataSubscription sub : this.mqttAdapter.getSubscriptions())
        {
            for (PDataNode node : sub.getSubscriptionNodes())
            {
                MqttDataNode mqttNode = (MqttDataNode) node;
                MqttMessage message = new MqttMessage(("Sample test message for topic " + mqttNode.getTopic()).getBytes()); //$NON-NLS-1$
                message.setQos(mqttNode.getQos());
                try
                {
                    this.mqttClient.publish(mqttNode.getTopic(), message);
                }
                catch (MqttException e)
                {
                    _logger.error("Failed to publish message for topic " + mqttNode.getTopic(), e); //$NON-NLS-1$
                }
            }
        }
    }

    /**
     * Sample data subscriber.
     *
     */

    private static class SampleSubscriber
            implements IDataSubscriptionListener
    {
        private UUID   id = UUID.randomUUID();
        @SuppressWarnings("unused")
        private UUID   subscriptionId;
        @SuppressWarnings("unused")
		private String subscriptionName;

        /**
         * @param subscriptionId subscription id
         * @param subscriptionName subscription name
         *
         */
        public SampleSubscriber(UUID subscriptionId, String subscriptionName)
        {
            this.subscriptionId = subscriptionId;
            this.subscriptionName = subscriptionName;
        }

        /*
         * (non-Javadoc)
         * @see com.ge.dspmicro.machinegateway.api.adapter.IDataSubscriptionListener#getId()
         */
        @Override
        public UUID getId()
        {
            return this.id;
        }

        /*
         * (non-Javadoc)
         * @see
         * com.ge.dspmicro.machinegateway.api.adapter.IDataSubscriptionListener#onDataUpdate(com.ge.dspmicro.machinegateway.api.adapter.ISubscriptionMachineAdapter
         * , java.util.List)
         */
        @SuppressWarnings("deprecation")
		@Override
        public synchronized void onDataUpdate(ISubscriptionMachineAdapter sender, List<PDataValue> values)
        {
        	this.onDataUpdate(sender, null, values);
        }
        /*
         * (non-Javadoc)
         * @see
         * com.ge.dspmicro.machinegateway.api.adapter.IDataSubscriptionListener#onDataUpdate(com.ge.dspmicro.machinegateway.api.adapter.ISubscriptionMachineAdapter
         * , java.util.List)
         */
        @Override
        public synchronized void onDataUpdate(ISubscriptionMachineAdapter sender, Map<String,String> properties, List<PDataValue> values)
        {
        	String subscription = null;
            if (properties != null)
            {
            	subscription = properties.get(IDataSubscription.PROPKEY_SUBSCRIPTION);
            }
        	for (PDataValue dataValue : values)
            {
                _logger.info("Data received in the sample from mqtt adapter: node=" + dataValue.getNodeName() + ", type=" //$NON-NLS-1$ //$NON-NLS-2$
                        + subscription + ", value=" + dataValue.getEnvelope().getValue().toString()); //$NON-NLS-1$
            }
        }
        /*
         * (non-Javadoc)
         * @see
         * com.ge.dspmicro.machinegateway.api.adapter.IDataSubscriptionListener#onDataError(com.ge.dspmicro.machinegateway.api.adapter.ISubscriptionMachineAdapter
         * )
         */
        @Override
        public synchronized void onDataError(ISubscriptionMachineAdapter sender)
        {
            // nothing
        }

        /*
         * (non-Javadoc)
         * @see com.ge.dspmicro.machinegateway.api.adapter.IDataSubscriptionListener#onSubscriptionDelete(com.ge.dspmicro.machinegateway.api.adapter.
         * ISubscriptionMachineAdapter, java.util.UUID)
         */
        @Override
        public synchronized void onSubscriptionDelete(ISubscriptionMachineAdapter sender,
                @SuppressWarnings("hiding") UUID subscriptionId)
        {
            if ( _logger.isDebugEnabled() )
            {
                _logger.debug("Subscription with ID " + subscriptionId.toString() + " removed."); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

    }

}
