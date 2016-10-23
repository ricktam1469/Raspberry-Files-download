/*
 * Copyright (c) 2014 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */

package com.ge.dspmicro.sample.gitrepository;

import java.util.concurrent.TimeUnit;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import com.ge.dspmicro.gitrepository.api.IGitRepository;

/**
 * 
 * @author Predix Machine Git Repository Sample
 */

// @Component is use to register this component as Service in the container.

@Component(name = GitRepositorySample.SERVICE_PID)
public class GitRepositorySample

{
    /** service id */
    protected final static String SERVICE_PID = "com.ge.dspmicro.sample.gitrepository";            //$NON-NLS-1$

    /** Create logger to report errors, warning massages, and info messages (runtime Statistics) */
    protected static Logger       _logger     = LoggerFactory.getLogger(GitRepositorySample.class);

    private IGitRepository        gitRepository;

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

        Runnable runner = new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
    
                    for(int ii = 0; ii < 20; ii++)
                    {
                        // The git thread has not completed yet. Best practice is to do these git methods in a thread since you don't know how long it will take to 
                        // download the git repository.
                        if (getGitRepository().getDefaultGit() == null)
                        {
                            TimeUnit.SECONDS.sleep(1);
                        }
                        else
                        {
                            break;
                        }
                    }
                    if (getGitRepository().getDefaultGit() == null)
                    {
                        _logger.error("Git Repository not available after 20 seconds.");
                        return;
                    }
                    // Removing files that are not under version control
                    getGitRepository().clean();
                    
                    // Removes and changes to tracked files
//                  getGitRepository().reset();
                
                    // Incorporates changes from a remote repository into the current 
//                  getGitRepository().pull();
                 
                    // Updates remote repository with local references.
//                  getGitRepository().push();
                }
                catch (Exception ee)
                {
                    _logger.error("Git Repository failed.", ee);
                }
            }
        };
            
        new Thread(runner).start();

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
     * @return the gitRepository
     */
    public IGitRepository getGitRepository()
    {
        return this.gitRepository;
    }

    /**
     * Use Dependency injection of the gitRepository service
     * 
     * @param gitRepository the gitRepository to set
     */
    @Reference
    public void setGitRepository(IGitRepository gitRepository)
    {
        this.gitRepository = gitRepository;
    }

    /**
     * clears the dependency injection of the gitRepository service
     * 
     * @param gitRepository The Git Repository to unset
     */
    public void unsetGitRepository(@SuppressWarnings("hiding") IGitRepository gitRepository)
    {
        this.gitRepository = null;
    }
}
