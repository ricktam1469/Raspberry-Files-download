/*
 * Copyright (c) 2015 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */

package com.ge.dspmicro.sample.httpriver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ge.dspmicro.httpriver.send.api.IHttpRiverSend;
import com.ge.dspmicro.river.api.IRiverStatusCallback;
import com.ge.dspmicro.river.api.TransferStatus;
import com.ge.dspmicro.river.api.TransferStatus.State;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

/**
 * This sample class demonstrates how to send files to the cloud using HTTP River.
 * It works hand-in-hand with the HTTP Data sample, which is the receiving end in the cloud.
 * 
 * Prerequisites:
 * - HTTP Data application is installed and running in Predix Cloud (see HTTP Data sample in sample-cloud-apps.zip).
 * - HttpRiverSend service is installed in Predix Machine and configured to talk to HTTP Data in the cloud.
 * 
 * 
 * @author Predix Machine Sample
 */
@SuppressWarnings("all")
@Component(name = HttpRiverSample.SERVICE_PID, configurationPolicy = ConfigurationPolicy.optional, provide =
{
    IRiverStatusCallback.class
})
public class HttpRiverSample
        implements IRiverStatusCallback, Runnable
{
    protected static final String         SERVICE_PID    = "com.ge.dspmicro.httpriver.sample.send";

    private static final Logger           _logger        = LoggerFactory.getLogger(HttpRiverSample.class);

    /** HttpRiverSend service that is installed and running in Predix Machine. */
    private IHttpRiverSend                httpRiverSend;

    /** Location of test and retrieved files. */
    //@formatter:off
    private static final String           HTTPRIVER_DIR  = ( (System.getProperty("predix.data.dir") == null) 
                                                         ? System.getProperty("user.dir") 
                                                         : System.getProperty("predix.data.dir"))
                                                             + File.separator + "appdata" + File.separator + "httpriver";

    /** Sample files to be sent to the cloud. */
    private static final FileDescriptor[] TEST_FILES     = new FileDescriptor[]
                                                         {
        //                 Filename        Content type                Content disposition              Content description
        //                 =============== =========================== ================================ ================================
        new FileDescriptor("nonAscii.txt", ContentType.TEXT_PLAIN,     "inline; filename=nonAscii.txt", "A piece of literature."),
        new FileDescriptor("random.dat",   ContentType.DEFAULT_BINARY, "inline; filename=random.dat",   "A bunch of random binary data."),        
                                                         };
    //@formatter:on

    private TransferStatus                transferStatus = null;

    /** Lock object to sync the async call and callback. */
    private static Object                 _syncLock      = new Object();

    /**
     * Called when the bundle is started ... depending on
     * declarative services settings the bundle may not start
     * if all service dependencies are not met.
     * 
     * @param ctx component context
     */
    @Activate
    protected void activate(ComponentContext ctx)
    {
        if ( _logger.isDebugEnabled() )
        {
            _logger.debug("Starting sample " + ctx.getBundleContext().getBundle().getSymbolicName());
        }
        new Thread(this).start();
    }

    /**
     * Called when the bundle is stopped.
     * 
     * @param ctx component context
     */
    @Deactivate
    protected void deactivate(ComponentContext ctx)
    {
        if ( _logger.isDebugEnabled() )
        {
            _logger.debug("Stopping sample " + ctx.getBundleContext().getBundle().getSymbolicName());      //$NON-NLS-1$
        }

    }

    /**
     * Dependency injection of HTTP River Send service.
     * 
     * @param sender OSGi registered implementation of IHttpRiverSend interface.
     */
    @Reference
    protected void setHttpRiverSend(IHttpRiverSend sender)
    {
        this.httpRiverSend = sender;
    }

    /**
     * Remove this HTTP River Send service from dependency injection.
     * 
     * @param sender OSGi registered implementation of IHttpRiverSend interface.
     */
    protected void unsetHttpRiverSend(IHttpRiverSend sender)
    {
        this.httpRiverSend = null;
    }

    /**
     * Sends the specified file to cloud.
     * 
     * @param fileDesc Descriptor of file to be sent.
     */
    private void sendFile(FileDescriptor fileDesc)
            throws Exception
    {
        _logger.info("Running sendFile() with " + fileDesc.name);

        File srcFile = new File(HTTPRIVER_DIR + File.separator + fileDesc.name);
        _logger.info("Sending " + srcFile.getAbsolutePath() + " to cloud.");

        // Create file transfer request
        Map<String, String> requestProps = new HashMap<>();
        requestProps.put(IHttpRiverSend.PROPKEY_CONTENT_TYPE, fileDesc.contentType.toString());
        requestProps.put(IHttpRiverSend.PROPKEY_CONTENT_DESCRIPTION, fileDesc.contentDescription);
        requestProps.put(IHttpRiverSend.PROPKEY_CONTENT_DISPOSITION, fileDesc.contentDisposition);
        requestProps.put("Test-Header", "Test-header-value");

        UUID transferId = null;
        try (FileInputStream data = new FileInputStream(srcFile))
        {
            // Send file
            synchronized (_syncLock)
            {
                transferId = this.httpRiverSend.transfer(data, requestProps);

                _syncLock.wait(); // Callback will notify when transfer completes
            }
        }
        catch (Exception e)
        {
            _logger.error("Error occurred", e);
        }

        // Callback has set _transferStatus. Report status successful.
        if ( transferId != null && this.transferStatus != null && this.transferStatus.getTransferUuid() == transferId
                && this.transferStatus.getState() == State.SUCCESSFUL )
        {
            _logger.info("SEND: successful with transfer ID: " + transferId.toString());
        }
        else
        {
            _logger.error("SEND: failed with transfer ID: " + transferId.toString());
        }

        // Retrieve file from cloud and write to tmp location.
        File outFile = new File(HTTPRIVER_DIR + File.separator + "out-" + System.currentTimeMillis() + fileDesc.name);
        retrieveData(transferId, outFile);

        // Verified the retrieved file is the same as the sent file.
        if ( outFile.exists() && digestEquals(srcFile, outFile) )
        {
            _logger.info("RETRIEVE: successful comparing retrieved file " + outFile.getName() + " with sent file "
                    + fileDesc.name);
        }
        else
        {
            _logger.error("RETRIEVE: failed comparing retrieved file " + outFile.getName() + " with sent file "
                    + fileDesc.name);
        }
    }

    /**
     * This method retrieves a file from the cloud and writes it to the local disk for verification purposes.
     * It uses Apache's HTTP libraries, not HTTP River.
     * 
     * @param transferId Identifier of data uploaded.
     * @param filename Output file.
     * @throws Exception
     */
    private void retrieveData(UUID transferId, File outFile)
            throws Exception
    {
        _logger.info("Retrieving saved data from cloud ...");

        CloseableHttpClient httpClient = HttpClients.createDefault();

        String host = this.httpRiverSend.getRiverConfig().getDestinationHost();
        String uriString = "http://" + host + "/v1/retrieve";
        _logger.info("Retrieve data URL: " + uriString);

        URIBuilder uriBuilder = new URIBuilder(uriString);
        uriBuilder.setParameter("transferId", transferId.toString());

        HttpGet retrieveDataRequest = new HttpGet(uriBuilder.build());
        CloseableHttpResponse response = httpClient.execute(retrieveDataRequest);
        _logger.info("Retrieve data response status: " + response.getStatusLine().getStatusCode());

        HttpEntity responseEntity = response.getEntity();
        InputStream inputStream = responseEntity.getContent();

        _logger.info("Writing output file to " + outFile.getAbsolutePath());
        FileOutputStream outputStream = new FileOutputStream(outFile);
        int read = 0;
        byte[] bytes = new byte[1024];

        while ((read = inputStream.read(bytes)) != -1)
        {
            outputStream.write(bytes, 0, read);
        }

        outputStream.close();
        inputStream.close();
        response.close();
        httpClient.close();
    }

    /**
     * Compares two files.
     * 
     * @param file1 1st file to be compared
     * @param file2 2nd file to be compared
     * @return Returns true if the digest of both files are equal
     * @throws NoSuchAlgorithmException
     */
    private boolean digestEquals(File file1, File file2)
            throws NoSuchAlgorithmException
    {
        MessageDigest md1 = MessageDigest.getInstance("MD5");
        MessageDigest md2 = MessageDigest.getInstance("MD5");

        try (InputStream stream1 = Files.newInputStream(file1.toPath(), StandardOpenOption.READ);
                InputStream stream2 = Files.newInputStream(file2.toPath(), StandardOpenOption.READ);)
        {
            DigestInputStream digestStream1 = new DigestInputStream(stream1, md1);
            DigestInputStream digestStream2 = new DigestInputStream(stream2, md2);
        }
        catch (IOException e)
        {
            _logger.error("File comparison failed.", e);
            return false;
        }

        byte[] digest1 = md1.digest();
        byte[] digest2 = md2.digest();

        return Arrays.equals(digest1, digest2);
    }

    /*
     * (non-Javadoc)
     * @see com.ge.dspmicro.river.api.IRiverStatusCallback#getContentCategory()
     */
    @Override
    public ContentCategory getContentCategory()
    {
        return ContentCategory.TEST;
    }

    /*
     * (non-Javadoc)
     * @see com.ge.dspmicro.river.api.IRiverStatusCallback#transferStatusChange(com.ge.dspmicro.river.api.TransferStatus)
     */
    @Override
    public void transferStatusChange(TransferStatus transferStatus)
    {
        if ( this.httpRiverSend.getRiverConfig().getName().equals(transferStatus.getRiverName()) == false )
        {
            // Ignore if this is not the river of interest.
            return;
        }

        _logger.info("IRiverStatusCallback invoked with this transfer status:\n" + transferStatus.toString());
        this.transferStatus = transferStatus;

        synchronized (_syncLock)
        {
            _syncLock.notify(); // sendFile() continues
        }
    }

    @SuppressWarnings("javadoc")
    public static class FileDescriptor
    {
        public String      name;
        public ContentType contentType;
        public String      contentDisposition;
        public String      contentDescription;

        public FileDescriptor(String name, ContentType contentType, String contentDisposition, String contentDescription)
        {
            super();
            this.name = name;
            this.contentType = contentType;
            this.contentDisposition = contentDisposition;
            this.contentDescription = contentDescription;
        }
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
        for (FileDescriptor fileDesc : TEST_FILES)
        {
            try
            {
                sendFile(fileDesc);
            }
            catch (Exception e)
            {
                _logger.error(e.getLocalizedMessage());
            }
        }
    }
}
