/*
 * Copyright (c) 2015 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */

package com.ge.predixmachine.data.http.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;

import com.ge.predix.uaa.token.lib.FastTokenServices;

/**
 * Sample security configuration for HTTP River receive service.
 * This sample configuration delegates security to the configured OAuth2 Authorization
 * Server. Only requests which include a valid token from a trusted issuer will
 * be allowed to post data to the /save endpoint
 */
@Configuration
@EnableResourceServer
public class HttpDataSecurityConfig extends ResourceServerConfigurerAdapter
{

    private static final String RESOURCE_ID = "pm"; //$NON-NLS-1$

    @Autowired
    private FastTokenServices   tokenServices;

    @Override
    public void configure(ResourceServerSecurityConfigurer resources)
    {
        resources.resourceId(RESOURCE_ID);
        resources.tokenServices(this.tokenServices);
    }

    @Override
    public void configure(HttpSecurity http)
            throws Exception
    {
        //@formatter:off
            http
                .csrf().disable() //TODO assess any risk here
                .authorizeRequests()
                    .antMatchers("/v1/save", "/").authenticated(); //$NON-NLS-1$ //$NON-NLS-2$
        //@formatter:on
    }

}
