/*
 * Copyright (c) 2015 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */

package com.ge.dspmicro.sample.healthmachineadapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import com.ge.dspmicro.machinegateway.api.IMachineGateway;
import com.ge.dspmicro.machinegateway.api.adapter.IDataSubscription;
import com.ge.dspmicro.machinegateway.api.adapter.IDataSubscriptionListener;
import com.ge.dspmicro.machinegateway.api.adapter.IMachineAdapter;
import com.ge.dspmicro.machinegateway.api.adapter.ISubscriptionMachineAdapter;
import com.ge.dspmicro.machinegateway.types.PDataValue;

@SuppressWarnings(
{
        "nls", "javadoc", "unused"
})
@Component(name = HealthMachineAdapterSample.SERVICE_PID, immediate = true)
public class HealthMachineAdapterSample
{
    /** service PID */
    public static final String            SERVICE_PID         = "com.ge.dspmicro.sample.healthmachineadapter";
    public static final String            ADAPTER_TYPE        = "com.ge.dspmicro.machineadapter.healthmonitor";
    public static final String            ADAPTER_NAME        = "healthmonitor";
    private static final String[]         VALID_SUBSCRIPTIONS = // NOSONAR
                                                              {
            "SYSTEM_CPU_USAGE", "SYSTEM_MEMORY_USAGE", "SYSTEM_DISK_USAGE", "JVM_MEMORY_USAGE"
                                                              };

    public static final ArrayList<String> SUBSCRIPTION_LIST   = new ArrayList<String>(
                                                                      Arrays.asList(VALID_SUBSCRIPTIONS));

    protected static final Logger         _logger             = LoggerFactory
                                                                      .getLogger(HealthMachineAdapterSample.class);

    private IMachineGateway               gateway;

    private ISubscriptionMachineAdapter   healthAdapter;

    private List<SampleSubscriber>        subscribers         = new ArrayList<>();

    @Activate
    public void activate(ComponentContext ctx)
    {

        init();
        if ( _logger.isDebugEnabled() )
        {
            _logger.debug("Starting sample bundle for " + ctx.getBundleContext().getBundle().getSymbolicName()); //$NON-NLS-1$
        }
    }

    @Deactivate
    public void deactivate(ComponentContext ctx)
    {
        if ( _logger.isDebugEnabled() )
        {
            _logger.debug("Stopping sample bundle for " + ctx.getBundleContext().getBundle().getSymbolicName()); //$NON-NLS-1$
        }
    }

    @Reference
    public void setGateway(IMachineGateway gateway)
    {
        this.gateway = gateway;
    }

    public void unsetGateway(IMachineGateway aGateway)
    {
        clearSubscribers();
        this.gateway = null;
        this.healthAdapter = null;

    }

    private void clearSubscribers()
    {
        if ( this.healthAdapter != null )
        {
            List<IDataSubscription> subscriptions = this.healthAdapter.getSubscriptions();

            for (IDataSubscription subscription : subscriptions)
            {
                for (SampleSubscriber sampleSubscriber : this.subscribers)
                {
                    subscription.removeDataSubscriptionListener(sampleSubscriber);
                }

            }
        }
    }

    private void init()
    {
        List<IMachineAdapter> adapters = this.gateway.getMachineAdapters();
        for (IMachineAdapter adapter : adapters)
        {
            if ( adapter.getInfo() == null )
            {
                continue;
            }
            if ( _logger.isDebugEnabled() )
            {
                _logger.debug("Found adapter " + adapter.getInfo().getName() + " of type " + adapter.getInfo().getAdapterType()); //$NON-NLS-1$
            }

            if ( ADAPTER_TYPE.equals(adapter.getInfo().getAdapterType())
                    && ADAPTER_NAME.equals(adapter.getInfo().getName()) )
            {
                this.healthAdapter = (ISubscriptionMachineAdapter) adapter;
                initSubscriptions();
            }
        }
    }

    private void initSubscriptions()
    {
        List<IDataSubscription> subscriptions = this.healthAdapter.getSubscriptions();

        for (IDataSubscription sub : subscriptions)
        {
            if ( SUBSCRIPTION_LIST.contains(sub.getName()) )
            {
                SampleSubscriber subscriber = new SampleSubscriber(sub.getId(), sub.getName());

                sub.addDataSubscriptionListener(subscriber);
                this.subscribers.add(subscriber);
            }
            else
            {
                _logger.error("Invalid subscription dected: " + sub.getName());
            }
        }
    }

    /**
     * Sample data subscriber.
     *
     * @throws InterruptedException
     */

    private static class SampleSubscriber
            implements IDataSubscriptionListener
    {
        private UUID   id = UUID.randomUUID();
        private UUID   subscriptionId;
        private String subscriptionName;

        /**
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
            for (PDataValue dataValue : values)
            {
                _logger.info("Data received from healthmonitor adapter: node=" + dataValue.getNodeName() + ", type="
                        + this.subscriptionName + ", value=" + dataValue.getEnvelope().getValue().toString());
            }
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
            for (PDataValue dataValue : values)
            {
                _logger.info("Data received from healthmonitor adapter: node=" + dataValue.getNodeName() + ", type="
                        + this.subscriptionName + ", value=" + dataValue.getEnvelope().getValue().toString());
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
                _logger.debug("Subscription with ID " + subscriptionId.toString() + " removed.");
            }
        }

    }

}
