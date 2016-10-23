/*
 * Copyright (c) 2014 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */

package com.ge.dspmicro.sample.security;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import com.ge.dspmicro.security.admin.api.ISecurityUtils;

/**
 * 
 * @author Predix Machine Sample
 * 
 *         NOTE: The Security sample requires configuration of Predix Machine keystores environment.
 *         Please refer to Predix Machine documentation on how to setup environment to run security and this Sample.
 * 
*/

// @Component is use to register this component as Service in the container.

@Component(name = SecuritySample.SERVICE_PID)
public class SecuritySample

{
    /** service id */
    protected final static String SERVICE_PID = "com.ge.dspmicro.sample.security";         //$NON-NLS-1$

    // Create logger to report errors, warning massages, and info messages (runtime Statistics)
    private static Logger         _logger     = LoggerFactory.getLogger(SecuritySample.class);

    private ISecurityUtils        securityUtil;

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

        try
        {
            char[] plainText = "This string contains senstitive information".toCharArray();
            String encryptedData = this.securityUtil.encrypt(plainText);
            char[] decryptedData = this.securityUtil.decrypt(encryptedData);
           
            _logger.info("Encrypted data:" + encryptedData);
            _logger.info("Decrypted data:" + String.valueOf(decryptedData));
            
            if (! String.valueOf(decryptedData).equals( String.valueOf(plainText)))
            {
                _logger.error("SecuritySample failed.  Decrypted data does not match original data");
            }
            
        }
        catch (Exception ee)
        {
            _logger.error("SecuritySample failed.", ee);
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

        if ( _logger.isDebugEnabled() )
        {
            _logger.debug("Stopped sample for " + ctx.getBundleContext().getBundle().getSymbolicName()); //$NON-NLS-1$
        }
    }

    /**
     * @return the securityUtil
     */
    public ISecurityUtils getSecurityUtil()
    {
        return this.securityUtil;
    }

    /**
     * Use Dependency injection of ISecurityUtils
     * 
     * @param securityUtil the securityUtil to set
     */
    @Reference
    public void setSecurityUtil(ISecurityUtils securityUtil)
    {
        this.securityUtil = securityUtil;
    }

    /**
     * clear the Dependency injection of ISecurityUtils
     * 
     * @param securityUtil the securityUtil to unset
     */
    public void unsetSecurityUtil(@SuppressWarnings("hiding") ISecurityUtils securityUtil)
    {
        this.securityUtil = null;
    }

}
