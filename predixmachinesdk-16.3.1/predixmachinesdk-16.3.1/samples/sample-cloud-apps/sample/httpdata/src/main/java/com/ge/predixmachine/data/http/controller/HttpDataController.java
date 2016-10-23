/*
 * Copyright (c) 2015 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */

package com.ge.predixmachine.data.http.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ge.predixmachine.data.http.HttpDataResources;
import com.ge.predixmachine.data.http.model.HttpData;

/**
 * This class handles all HTTP requests for Predix Machine HTTP Data service.
 */
@RestController
public class HttpDataController
{
    private static Log                     _logger                   = LogFactory.getLog(HttpDataController.class);
    private static final HttpDataResources _resources                = HttpDataResources.getInstance();

    // //////// URI Paths //////////
    /** Version path for the supported REST APIs */
    public static final String             URI_PATH_VERSION          = "/v1";                                      //$NON-NLS-1$
    /** REST request path for the welcome message */
    public static final String             URI_PATH_WELCOME          = URI_PATH_VERSION + "/welcome";              //$NON-NLS-1$    
    /** REST request path for saving data to RDBMS or messaging queues. */
    public static final String             URI_PATH_SAVE             = URI_PATH_VERSION + "/save";                 //$NON-NLS-1$
    /** REST request path for retrieving data from RDBMS. */
    public static final String             URI_PATH_RETRIEVE         = URI_PATH_VERSION + "/retrieve";             //$NON-NLS-1$

    // //////// Request Parameter Names //////////
    // These parameter names need to be in sync with HTTP River.
    private static final String            PARAM_TRANSFER_ID         = "transferId";                               //$NON-NLS-1$
    private static final String            PARAM_RIVER_NAME          = "riverName";                                //$NON-NLS-1$
    private static final String            PARAM_CONTENT_TYPE        = "contentType";                              //$NON-NLS-1$
    private static final String            PARAM_CONTENT_DISPOSITION = "contentDisposition";                       //$NON-NLS-1$
    private static final String            PARAM_CONTENT_DESCRIPTION = "contentDescription";                       //$NON-NLS-1$
    private static final String            PARAM_TIMESTAMP           = "timestamp";                                //$NON-NLS-1$
    private static final String            PARAM_DATA                = "data";                                     //$NON-NLS-1$

    /** Handles JDBC operations */
    @Autowired
    HttpDataDao                            httpDataDao;

    /**
     * Prints welcome message
     * 
     * @return welcome message
     */
    @ResponseBody
    @RequestMapping(value = URI_PATH_WELCOME, method =
    {
            RequestMethod.POST, RequestMethod.GET
    })
    public String welcome()
    {
        // HttpDataController.welcome.message=Welcome to Predix Machine HTTP Data Service!\n
        return _resources.getString("HttpDataController.welcome.message"); //$NON-NLS-1$
    }

    // See http://stackoverflow.com/questions/2770877/spring-jdbctemplate-insert-blob-and-return-generated-key
    /**
     * Saves the received data into an RDBMS.
     * Maximum size of data is restricted by database column size (MySQL.LongBlob 4GB).
     * Data of zero size OK.
     * 
     * @param transferId UUID of the transfer.
     * @param riverName Name of the sending river.
     * @param contentType MIME type of the data content.
     * @param contentDisposition Disposition of data content.
     * @param contentDescription Description of data content.
     * @param timestamp Date and time when data is sent.
     * @param data Data received.
     * @return response message
     * @throws Exception if data cannot be accessed correctly.
     */
    @ResponseBody
    @RequestMapping(value = URI_PATH_SAVE, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String save(@RequestParam(value = PARAM_TRANSFER_ID) String transferId,
            @RequestParam(value = PARAM_RIVER_NAME) String riverName,
            @RequestParam(value = PARAM_CONTENT_TYPE) String contentType,
            @RequestParam(value = PARAM_CONTENT_DISPOSITION, required = false) String contentDisposition,
            @RequestParam(value = PARAM_CONTENT_DESCRIPTION, required = false) String contentDescription,
            @RequestParam(value = PARAM_TIMESTAMP) String timestamp,
            @RequestParam(value = PARAM_DATA) MultipartFile data)
            throws Exception
    {
        if ( _logger.isDebugEnabled() )
        {
            _logger.debug("Request parameters:" +  //$NON-NLS-1$
                    "\n\t" + PARAM_TRANSFER_ID + ": " + transferId +  //$NON-NLS-1$ //$NON-NLS-2$
                    "\n\t" + PARAM_RIVER_NAME + ": " + riverName +  //$NON-NLS-1$ //$NON-NLS-2$                
                    "\n\t" + PARAM_CONTENT_TYPE + ": " + contentType +  //$NON-NLS-1$ //$NON-NLS-2$
                    "\n\t" + PARAM_CONTENT_DISPOSITION + ": " + contentDisposition +  //$NON-NLS-1$ //$NON-NLS-2$
                    "\n\t" + PARAM_CONTENT_DESCRIPTION + ": " + contentDescription +  //$NON-NLS-1$ //$NON-NLS-2$
                    "\n\t" + PARAM_TIMESTAMP + ": " + timestamp //$NON-NLS-1$ //$NON-NLS-2$
            );
        }

        // Calculate and log performance info.
        long threadId = Thread.currentThread().getId();
        long receiveTime = System.currentTimeMillis();
        long sendTime = (Timestamp.valueOf(timestamp)).getTime();
        double elapsedTimeInSeconds = new BigDecimal(((double) (receiveTime - sendTime)) / 1000).setScale(2,
                RoundingMode.HALF_UP).doubleValue(); // Round value to 2 decimal places
        long dataSize = data.getSize();
        double bytePerSecond = dataSize / elapsedTimeInSeconds;

        // HttpDataController.performance.data.transfer=\nPerformance from client to server:
        // \n\tThread ID: {0}
        // \n\tSend time: {1}
        // \n\tReceive time: {2}
        // \n\tTotal time: {3} seconds
        // \n\tBytes received: {4}
        // \n\tAverage speed: {5} per second
        _logger.info(_resources.getString("HttpDataController.performance.data.transfer",  //$NON-NLS-1$
                threadId,                                   // Thread ID
                timestamp,                                  // Send time
                (new Timestamp(receiveTime)).toString(),    // Receive time
                elapsedTimeInSeconds,                       // Total time in seconds
                getHumanReadableByteCount(dataSize),        // Bytes received
                getHumanReadableByteCount(bytePerSecond))); // Average byte per second

        byte[] dataContent;
        try
        {
            dataContent = data.getBytes();
        }
        catch (IOException ioe)
        {
            String message = "File access failed."; //$NON-NLS-1$
            _logger.error(message);
            throw new Exception(message, ioe);
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String deviceId = "unknown"; //$NON-NLS-1$
        if ( auth != null )
        {
            deviceId = auth.getName();
        }

        // Saves to database
        HttpData receivedData = new HttpData(transferId, riverName, contentType, contentDisposition,
                contentDescription, Timestamp.valueOf(timestamp).getTime(), dataContent, deviceId);
        this.httpDataDao.insertHttpData(receivedData);

        String resultStr = "transferId: " + transferId + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
                "riverName: " + riverName //$NON-NLS-1$
                + "\n" + //$NON-NLS-1$ 
                "contentType: " + contentType //$NON-NLS-1$
                + "\n" + //$NON-NLS-1$ 
                "contentDisposition: " + contentDisposition //$NON-NLS-1$
                + "\n" + //$NON-NLS-1$ 
                "contentDescription: " + contentDescription //$NON-NLS-1$
                + "\n" + //$NON-NLS-1$ 
                "timestamp: " + Timestamp.valueOf(timestamp).getTime() + "\n" //$NON-NLS-1$ //$NON-NLS-2$
                + "deviceId: " + deviceId; //$NON-NLS-1$ 

        // HttpDataController.performance.data.persist=\nPerformance for data persistence:
        // \n\tThread ID: {0}
        // \n\tReceive time: {1}
        // \n\tData save completion time: {2}
        // \n\tTotal time: {3} seconds
        // \n\tBytes received: {4}
        // \n\tAverage speed: {5} per second
        long saveCompletionTime = System.currentTimeMillis();
        elapsedTimeInSeconds = new BigDecimal(((double) (saveCompletionTime - receiveTime)) / 1000).setScale(2,
                RoundingMode.HALF_UP).doubleValue(); // Round value to 2 decimal places
        bytePerSecond = dataSize / elapsedTimeInSeconds;
        _logger.info(_resources.getString("HttpDataController.performance.data.persist",  //$NON-NLS-1$
                threadId,                                       // Thread ID
                (new Timestamp(receiveTime)).toString(),        // Receive time
                (new Timestamp(saveCompletionTime)).toString(), // Save completion time
                elapsedTimeInSeconds,                           // Total time in seconds
                getHumanReadableByteCount(dataSize),            // Bytes received
                getHumanReadableByteCount(bytePerSecond)));     // Average byte per second

        return resultStr;
    }

    /**
     * Retrieve data with the specified transfer ID.
     * 
     * @param transferId ID of the piece of data to be retrieved
     * @return {@code ResponseEntity<byte[]>} containing the data.
     */
    @ResponseBody
    @RequestMapping(value = URI_PATH_RETRIEVE, method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> retrieve(@RequestParam(value = PARAM_TRANSFER_ID) String transferId)
    {
        List<HttpData> results = this.httpDataDao.selectByTransferId(transferId);

        if ( results.size() == 0 )
        {
            return new ResponseEntity<byte[]>(HttpStatus.NOT_FOUND);
        }

        HttpData result = results.get(0);
        byte[] data = result.getDataContent();

        if ( data.length == 0 )
        {
            return new ResponseEntity<byte[]>(HttpStatus.NO_CONTENT);
        }

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(result.getContentType()));
        return new ResponseEntity<byte[]>(data, headers, HttpStatus.OK);
    }

    /**
     * Handles uncaught exceptions thrown from request-mapped methods.
     * This method constructs a JSON response by serializing a Map containing error message and exception stacktrace.
     * 
     * @param ex Exception being handled
     * @param response Response to be sent.
     * @return A Map containing the error message and stack trace.
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Map<String, String> exceptionHandler(Exception ex, HttpServletResponse response)
    {
        Map<String, String> errorMap = new HashMap<String, String>();
        errorMap.put("errorMessage", ex.getMessage()); //$NON-NLS-1$ 

        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

        _logger.error("Internal server error: ", ex); //$NON-NLS-1$

        return errorMap;
    }

    /**
     * Create a human readable string for the number of bytes.
     * 
     * @param numBytes The number of bytes for which the string is created.
     * @return Return a string representation of the specified number of bytes.
     * @see <a href="original code">http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java</a>
     */
    private String getHumanReadableByteCount(double numBytes)
    {
        final int kilo = 1024;
        final DecimalFormat df = new DecimalFormat("#.00"); // Show just 2 decimal places //$NON-NLS-1$

        if ( numBytes < kilo )
        {
            return df.format(numBytes) + " B"; //$NON-NLS-1$
        }

        // Byte units: Kilobyte, Megabyte, Gigabyte, Terabyte, Petabyte, Exabyte, Zettabyte, Yottabyte
        final String[] units =
        {
                "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
        }; 
        // Don't let exp go beyond the biggest unit we can represent
        int exp = Math.min((int) (Math.log(numBytes) / Math.log(kilo)), units.length);
        String suffix = units[exp - 1];

        return df.format(numBytes / Math.pow(kilo, exp)) + " " + suffix; //$NON-NLS-1$
    }
}
