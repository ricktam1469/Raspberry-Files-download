/*
 * Copyright (c) 2015 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */

package com.ge.predixmachine.data.http.model;

import java.sql.Timestamp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Model object for the data received by the service that will be stored into an RDBMS.
 */
public class HttpData
{
    private static Log                     _logger                         = LogFactory.getLog(HttpData.class);

    // This section defines the column names in the database table.
    /** Column name for transfer ID. */
    public static final String             COLUMN_NAME_TRANSFER_ID         = "TransferId";                     //$NON-NLS-1$
    /** Column name for river name. */
    public static final String             COLUMN_NAME_RIVER_NAME          = "RiverName";                      //$NON-NLS-1$ 
    /** Column name for content type. */
    public static final String             COLUMN_NAME_CONTENT_TYPE        = "ContentType";                    //$NON-NLS-1$
    /** Column name for content disposition. */
    public static final String             COLUMN_NAME_CONTENT_DISPOSITION = "ContentDisposition";             //$NON-NLS-1$
    /** Column name for content description. */
    public static final String             COLUMN_NAME_CONTENT_DESCRIPTION = "ContentDescription";             //$NON-NLS-1$
    /** Column name for transfer time stamp. */
    public static final String             COLUMN_NAME_TIMESTAMP           = "Timestamp";                      //$NON-NLS-1$
    /** Column name for data content. */
    public static final String             COLUMN_NAME_DATA                = "Data";                           //$NON-NLS-1$
    /** Column name for device ID. */
    public static final String             COLUMN_NAME_DEVICE_ID           = "DeviceId";                       //$NON-NLS-1$

    /** Columns in the table. */
    public static final ColumnDescriptor[] COLUMNS                         =
                                                                           {
        //@formatter:off
        //                   Column name                      MySQL/H2 data type Postgres data type             Constraint
        //                   ================================ ================== ============================== ==============
        new ColumnDescriptor(COLUMN_NAME_TRANSFER_ID,         "char(36)",        "char(36)",                    "primary key"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        new ColumnDescriptor(COLUMN_NAME_RIVER_NAME,          "varchar(100)",    "varchar(100)",                "not null"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        new ColumnDescriptor(COLUMN_NAME_CONTENT_TYPE,        "varchar(255)",    "varchar(255)",                "not null"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        new ColumnDescriptor(COLUMN_NAME_CONTENT_DISPOSITION, "varchar(255)",    "varchar(255)",                ""), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        new ColumnDescriptor(COLUMN_NAME_CONTENT_DESCRIPTION, "varchar(255)",    "varchar(255)",                ""), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        new ColumnDescriptor(COLUMN_NAME_TIMESTAMP,           "datetime",        "timestamp without time zone", "not null"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        new ColumnDescriptor(COLUMN_NAME_DATA,                "longblob",        "bytea",                       "not null") , //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ // 4GB
        new ColumnDescriptor(COLUMN_NAME_DEVICE_ID,           "varchar(100)",    "varchar(100)",                "not null")  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ // 4GB

        //@formatter:on
                                                                           };

    private String                         transferId;
    private String                         riverName;
    private String                         contentType;
    private String                         contentDisposition;
    private String                         contentDescription;
    private long                           timestamp;
    private byte[]                         dataContent;
    private String                         deviceId;

    /**
     * @param transferId Identifies the data received from a specific transfer.
     * @param riverName Name of the data sender.
     * @param contentType MIME type of the data content.
     * @param contentDisposition Content disposition of
     * @param contentDescription Description of the data content.
     * @param timestamp The time when the data was sent.
     * @param dataContent Data transferred.
     * @param deviceId The ID of the device which inserted this data
     */
    public HttpData(String transferId, String riverName, String contentType, String contentDisposition,
            String contentDescription, long timestamp, byte[] dataContent, String deviceId)
    {
        super();
        this.transferId = transferId;
        this.riverName = riverName;
        this.contentType = contentType;
        this.contentDisposition = contentDisposition;
        this.contentDescription = contentDescription;
        this.timestamp = timestamp;
        this.dataContent = dataContent;
        this.deviceId = deviceId;

        if ( _logger.isDebugEnabled() )
        {
            _logger.debug(this.toString());
        }
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "HttpData:" + //$NON-NLS-1$
                "\n\ttransferId=" + this.transferId +  //$NON-NLS-1$
                "\n\tdeviceId=" + this.deviceId +  //$NON-NLS-1$
                "\n\triverName=" + this.riverName +  //$NON-NLS-1$
                "\n\tcontentType=" + this.contentType +  //$NON-NLS-1$
                "\n\tcontentDisposition=" + this.contentDisposition +  //$NON-NLS-1$
                "\n\tcontentDescription=" + this.contentDescription +  //$NON-NLS-1$
                "\n\ttimestamp=" + new Timestamp(this.timestamp).toString(); //$NON-NLS-1$
    }

    /**
     * @return the transferId
     */
    public String getTransferId()
    {
        return this.transferId;
    }

    /**
     * @return the riverName
     */
    public String getRiverName()
    {
        return this.riverName;
    }

    /**
     * @return the contentType
     */
    public String getContentType()
    {
        return this.contentType;
    }

    /**
     * @return the contentDisposition
     */
    public String getContentDisposition()
    {
        return this.contentDisposition;
    }

    /**
     * @return the contentDescription
     */
    public String getContentDescription()
    {
        return this.contentDescription;
    }

    /**
     * @return the timestamp
     */
    public long getTimestamp()
    {
        return this.timestamp;
    }

    /**
     * @return the dataContent
     */
    public byte[] getDataContent()
    {
        return this.dataContent;
    }

    /**
     * @return the deviceId
     */
    public String getDeviceId()
    {
        return this.deviceId;
    }

    /**
     * @param deviceId the deviceId to set
     */
    public void setDeviceId(String deviceId)
    {
        this.deviceId = deviceId;
    }

    /**
     * Describes a database column.
     */
    public static class ColumnDescriptor
    {
        private String name;
        private String mysqlType;
        private String postgresType;
        private String constraint;

        /**
         * @param name Name of column.
         * @param mysqlType Data type of column in MySQL/H2.
         * @param postgresType Data type of column in PostgreSQL.
         * @param constraint Database constraint can be "primary key", "foreign key", "not null", etc.
         */
        public ColumnDescriptor(String name, String mysqlType, String postgresType, String constraint)
        {
            super();
            this.name = name;
            this.mysqlType = mysqlType;
            this.postgresType = postgresType;
            this.constraint = constraint;
        }

        /**
         * @return the name
         */
        public String getName()
        {
            return this.name;
        }

        /**
         * @return the mysqlType
         */
        public String getMysqlType()
        {
            return this.mysqlType;
        }

        /**
         * @return the postgresType
         */
        public String getPostgresType()
        {
            return this.postgresType;
        }

        /**
         * @return the constraint
         */
        public String getConstraint()
        {
            return this.constraint;
        }
    }
}
