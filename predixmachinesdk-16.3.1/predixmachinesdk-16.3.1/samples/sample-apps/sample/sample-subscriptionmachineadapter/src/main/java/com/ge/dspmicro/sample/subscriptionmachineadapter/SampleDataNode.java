/*
 * Copyright (c) 2014 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */

package com.ge.dspmicro.sample.subscriptionmachineadapter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import com.ge.dspmicro.machinegateway.types.PDataNode;

/**
 * 
 * 
 * @author Predix Machine Sample
 */
public class SampleDataNode extends PDataNode
{
    // Add node specific attribute here as needed.
    private String nodeSpecificAttribute;

    /**
     * Constructor
     * 
     * @param machineAdapterId a unique id
     * @param name string value
     */
    public SampleDataNode(UUID machineAdapterId, String name)
    {
        super(machineAdapterId, name);

        // Do other initialization if needed.
    }

    /**
     * @return the nodeSpecificAttribute
     */
    public String getNodeSpecificAttribute()
    {
        return this.nodeSpecificAttribute;
    }

    /**
     * @param nodeSpecificAttribute the nodeSpecificAttribute to set
     */
    public void setNodeSpecificAttribute(String nodeSpecificAttribute)
    {
        this.nodeSpecificAttribute = nodeSpecificAttribute;
    }

    /**
     * Node address to uniquely identify the node.
     */
    @Override
    public URI getAddress()
    {
        try
        {
            URI address = new URI("sample.subscription.adapter", null, "localhost", -1, "/" + getName(), null, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            return address;
        }
        catch (URISyntaxException e)
        {
            return null;
        }
    }
}
