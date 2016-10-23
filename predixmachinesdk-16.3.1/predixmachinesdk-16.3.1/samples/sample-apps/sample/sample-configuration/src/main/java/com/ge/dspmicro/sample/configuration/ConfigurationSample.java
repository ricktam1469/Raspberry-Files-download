/*
 * Copyright (c) 2014 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */

package com.ge.dspmicro.sample.configuration;

import java.util.Dictionary;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Modified;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;

import com.ge.dspmicro.security.admin.api.ISecurityUtils;

@SuppressWarnings("javadoc")
interface IConfigurationSample
{
    // does nothing
}

/**
 * 
 * @author Predix Machine Sample
 * 
 */

// @Component is use to register this component as Service in the container.
// Since ConfigurationPolicy=ConfigurationPolicy.require,
// You need to copy the com.ge.dspmicro.sample.configuration.cfg file
// in the sample-apps/configuration/machine/ folder to the <PredixMachineInstallation>/configuration/machine/ folder
// Unless the configurationPolicy=ConfigurationPolicy.optional
//

@Component(name = ConfigurationSample.SERVICE_PID, provide = IConfigurationSample.class, designate = ConfigurationSample.Configuration.class, configurationPolicy = ConfigurationPolicy.require)
public class ConfigurationSample
        implements IConfigurationSample

{
    // Meta mapping for configuration properties
    @SuppressWarnings("javadoc")
    @Meta.OCD(name = "%component.name", localization = "OSGI-INF/l10n/bundle")
    interface Configuration
    {
        @Meta.AD(name = "%username.name", description = "%username.description", id = SERVICE_PID
                + "credentials.username", required = false, deflt = "")
        String username();

        @Meta.AD(name = "%password.name", description = "%password.description", id = KEY_CREDENTIALS_PASSWORD, required = false, deflt = "")
        String password();

        // Don't want to make a property for user to set the password that is encrypted.
        // @Meta.AD(id = SERVICE_PID + "credentials.password.encrypted", required = false, deflt = "")
        // String passwordEncrypted();

        @Meta.AD(name = "%server.uri", description = "%server.uri.description", id = SERVICE_PID + "server.uri", required = false, deflt = "")
        String serverURI();
    }

    /** service id */
    protected final static String SERVICE_PID              = "com.ge.dspmicro.sample.configuration";            //$NON-NLS-1$

    /** password field */

    public static final String    KEY_CREDENTIALS_PASSWORD = SERVICE_PID + ".credentials.password";             //$NON-NLS-1$

    // Create logger to report errors, warning massages, and info messages (runtime Statistics)
    private static Logger         _logger                  = LoggerFactory.getLogger(ConfigurationSample.class);

    private ISecurityUtils        securityUtils;

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
            // Use the SecurityUtils service to encrypt the password field
            this.securityUtils.encryptConfigProperty(SERVICE_PID, KEY_CREDENTIALS_PASSWORD);

            // read the configuration property values and use it.
            Dictionary<?, ?> props = ctx.getProperties();
            Configuration config = Configurable.createConfigurable(Configuration.class, props);
            _logger.info("Service URI: " + config.serverURI());
            _logger.info("User Name: " + config.username());
            _logger.info("Password: " + config.password());
            
            // Use the SecurityUtils service to bind the configuration properties to this bundle
            this.securityUtils.bindConfigurationToBundle(SERVICE_PID, ctx.getBundleContext().getBundle().getLocation());
        }
        catch (Exception ee)
        {
            _logger.error("SecuritySample failed.", ee);
        }
    }
    
    /**
     * OSGi component lifecycle modified method. Called when
     * the component properties are changed.
     * 
     * @param ctx component context
     * 
     * When configuration changes by other application, this will show the actual value changes
     */
    @SuppressWarnings("nls")
    @Modified
    public void modified(ComponentContext ctx)
    {
        this.securityUtils.encryptConfigProperty(SERVICE_PID, KEY_CREDENTIALS_PASSWORD);
        Dictionary<?, ?> props = ctx.getProperties();
        Configuration config = Configurable.createConfigurable(Configuration.class, props);

        _logger.info("Service URI: " + config.serverURI());
        _logger.info("User Name: " + config.username());
        _logger.info("Password: " + config.password());

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
     * @param securityUtils the securityUtils to set
     */
    @Reference
    public void setSecurityUtils(ISecurityUtils securityUtils)
    {
        this.securityUtils = securityUtils;
    }

    /**
     * clear the Dependency injection of ISecurityUtils
     * 
     * @param securityUtils the securityUtils to unset
     */

    public void unsetSecurityUtils(@SuppressWarnings("hiding") ISecurityUtils securityUtils)
    {
        this.securityUtils = null;
    }

}
