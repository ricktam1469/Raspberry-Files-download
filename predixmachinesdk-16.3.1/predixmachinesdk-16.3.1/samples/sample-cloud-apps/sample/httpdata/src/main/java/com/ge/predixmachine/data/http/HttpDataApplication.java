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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.ServletContextInitializer;
import org.springframework.context.annotation.Bean;

import com.ge.predix.uaa.token.lib.FastTokenServices;

/**
 * This allows the service to be run as a standalone application for testing.
 */
@SpringBootApplication
public class HttpDataApplication
        implements ServletContextInitializer
{
    /**
     * Entry point of the cloud application.
     * 
     * @param args Not used
     */
    public static void main(String[] args)
    {
        SpringApplication.run(HttpDataApplication.class, args);
    }

    /**
     * Bean for TokenServices to use to authenticate requests
     * 
     * @return A new instance of FastTokenServices
     */
    @Bean
    public FastTokenServices tokenServices()
    {
        FastTokenServices services = new FastTokenServices();
        services.setTrustedIssuerIdsRegex(".*"); // This is insecure and should instead be set to a regular expression matching only the issuers which your //$NON-NLS-1$
                                                 // application should trust
        return services;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.boot.context.embedded.ServletContextInitializer#onStartup(javax.servlet.ServletContext)
     */
    @Override
    public void onStartup(ServletContext context)
            throws ServletException
    {
        context.getSessionCookieConfig().setSecure(true);
        context.getSessionCookieConfig().setHttpOnly(true);
    }
}
