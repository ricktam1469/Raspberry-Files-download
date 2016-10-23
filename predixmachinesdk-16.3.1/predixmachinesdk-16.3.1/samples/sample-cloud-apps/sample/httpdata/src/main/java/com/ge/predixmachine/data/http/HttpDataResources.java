/*
 * Copyright (c) 2015 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */
 
package com.ge.predixmachine.data.http;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Give easy access to a resource bundle strings. This class is designed
 * as a singleton to provide basic localization with helper methods.
 */
public class HttpDataResources
{
    private static final String         BUNDLE  = "HttpDataResources"; //$NON-NLS-1$
    private static Logger               _logger = LoggerFactory.getLogger(HttpDataResources.class);
    private static HttpDataResources    _resource;                        // singleton

    /** This is the resource bundle to use for a given package/OSGI bundle */
    private ResourceBundle                 resources;

    /**
     * Empty Constructor.
     */
    protected HttpDataResources()
    {
        try
        {
            this.resources = ResourceBundle.getBundle(BUNDLE);
        }
        catch (MissingResourceException ee)
        {
            _logger.error("Missing Resource Bundle " + BUNDLE, ee); //$NON-NLS-1$
        }
    }

    /**
     * Get an instance of the Resources object.
     * 
     * @return Resources singleton.
     */
    public static HttpDataResources getInstance()
    {
        if ( _resource == null )
        {
            _resource = new HttpDataResources();
        }
        return _resource;
    }

    /**
     * Get the static stored resource bundle.
     * 
     * @return the ResourceBundle used.
     */
    public ResourceBundle getResourceBundle()
    {
        return this.resources;
    }

    /**
     * Get the property defined in the resource bundle for the given key. If
     * none is defined (MissingResourceException is caught), return the key.
     * 
     * @param key the key in the resource bundle.
     * @return the string value for the key in the bundle or key if none was found.
     */
    public String getString(String key)
    {
        if ( this.resources == null )
        {
            _logger.error("Missing Resource Bundle " + BUNDLE); //$NON-NLS-1$
            return key;
        }

        try
        {
            return this.resources.getString(key);
        }
        catch (MissingResourceException ee)
        {
            // catch and ignore and return the defaultValue
            _logger.warn("Missing resource key:" + key); //$NON-NLS-1$
            return key;
        }
    }

    /**
     * Build a formated string from the resource bundle.
     * 
     * @param key the key into the resource bundle that has the formated string.
     * @param args an array of arguments
     * @return the formated string with the arguments in-line.
     */
    public String getString(String key, Object... args)
    {
        MessageFormat formatter = new MessageFormat(getString(key));
        return formatter.format(args);
    }
}
