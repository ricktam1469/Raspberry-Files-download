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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import com.ge.predixmachine.data.http.model.HttpData;
import com.ge.predixmachine.data.http.model.HttpData.ColumnDescriptor;

/**
 * Data Access Object of HttpData.
 */
@Component
public class HttpDataDao
{
    private static Log          _logger      = LogFactory.getLog(HttpDataDao.class);

    /** Name of database table that holds received data. */
    private static final String TABLE_NAME   = "HttpData";                          //$NON-NLS-1$

    // SQL statements
    private static String       sqlCreateTableMysql;
    private static String       sqlCreateTablePostgresql;
    private static String       sqlInsertHttpData;
    private static String       sqlSelectAll;
    private static String       sqlSelectByTransferId;

    // SQL statements are different depending on the backend RDBMS being used.
    private boolean             isPostgresql = false;

    // Initialize SQL statements
    static
    {
        // Initialize sqlCreateTableMysql
        sqlCreateTableMysql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("; //$NON-NLS-1$ //$NON-NLS-2$
        for (ColumnDescriptor column : HttpData.COLUMNS)
        {
            sqlCreateTableMysql += column.getName() + " " + column.getMysqlType() + " " + column.getConstraint() + ", "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        sqlCreateTableMysql = sqlCreateTableMysql.substring(0, sqlCreateTableMysql.length() - 2); // Remove trailing comma
        sqlCreateTableMysql += ")"; //$NON-NLS-1$

        // Initialize sqlCreateTablePostgresql
        sqlCreateTablePostgresql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("; //$NON-NLS-1$ //$NON-NLS-2$
        for (ColumnDescriptor column : HttpData.COLUMNS)
        {
            sqlCreateTablePostgresql += column.getName()
                    + " " + column.getPostgresType() + " " + column.getConstraint() + ", "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        sqlCreateTablePostgresql = sqlCreateTablePostgresql.substring(0, sqlCreateTablePostgresql.length() - 2); // Remove trailing comma
        sqlCreateTablePostgresql += ")"; //$NON-NLS-1$

        // Initialize sqlInsertHttpData
        sqlInsertHttpData = "INSERT INTO " + TABLE_NAME + " ("; //$NON-NLS-1$ //$NON-NLS-2$
        String valueClause = "values ("; //$NON-NLS-1$
        for (ColumnDescriptor column : HttpData.COLUMNS)
        {
            sqlInsertHttpData += column.getName() + ", "; //$NON-NLS-1$
            valueClause += "?, "; //$NON-NLS-1$
        }
        sqlInsertHttpData = sqlInsertHttpData.substring(0, sqlInsertHttpData.length() - 2); // Remove trailing comma
        valueClause = valueClause.substring(0, valueClause.length() - 2); // Remove trailing comma
        sqlInsertHttpData += ") " + valueClause + ")"; //$NON-NLS-1$ //$NON-NLS-2$

        // Initialize sqlSelectAll
        sqlSelectAll = "select * from " + TABLE_NAME; //$NON-NLS-1$

        // Initialize sqlSelectByTransferId
        sqlSelectByTransferId = "select * from " + TABLE_NAME + " where " + HttpData.COLUMN_NAME_TRANSFER_ID + " = ?"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        // Debug print
        if ( _logger.isDebugEnabled() )
        {
            _logger.debug("Insert statement: " + sqlInsertHttpData); //$NON-NLS-1$
            _logger.debug("Select all statement: " + sqlSelectAll); //$NON-NLS-1$
            _logger.debug("Select by transfer ID statement: " + sqlSelectByTransferId); //$NON-NLS-1$
        }
    }

    /** Dependency injection of JdbcTemplate to takes care of all JDBC operations. */
    @Autowired
    private JdbcTemplate        jdbcTemplate;

    /**
     * Creates database table to persist received data if it doesn't exist.
     * 
     * Note: This cannot be done in constructor because JdbcTemplate is injected after instance is created.
     * Therefore @PostConstruct.
     * 
     * @throws SQLException
     */
    @PostConstruct
    private void init()
            throws SQLException
    {
        if ( _logger.isDebugEnabled() ) _logger.debug("Initializing data access object ..."); //$NON-NLS-1$ 

        String url = getJdbcTemplate().getDataSource().getConnection().getMetaData().getURL();
        if ( _logger.isDebugEnabled() ) _logger.debug("Database URL = " + url); //$NON-NLS-1$

        this.isPostgresql = url.startsWith("jdbc:postgresql:"); //$NON-NLS-1$

        // Create database table if it doesn't exist.
        // Note: Need to move out to a manual admin task to handle upgrades etc.
        String ddl = this.isPostgresql ? sqlCreateTablePostgresql : sqlCreateTableMysql;
        if ( _logger.isDebugEnabled() ) _logger.debug("DDL = " + ddl); //$NON-NLS-1$
        getJdbcTemplate().execute(ddl);
    }

    /**
     * Extracted into a method so unit test can return mocked object.
     * 
     * @return the injected JdbcTemplate
     */
    private JdbcTemplate getJdbcTemplate()
    {
        return this.jdbcTemplate;
    }

    /**
     * Insert data into database table.
     * 
     * @param data Data to be inserted.
     */
    public void insertHttpData(HttpData data)
    {
        if ( _logger.isDebugEnabled() ) _logger.debug("Inserting HTTP data into database ..."); //$NON-NLS-1$

        getJdbcTemplate().update(sqlInsertHttpData, data.getTransferId(), data.getRiverName(),
                data.getContentType(), data.getContentDisposition(), data.getContentDescription(),
                new Timestamp(data.getTimestamp()), data.getDataContent(), data.getDeviceId());
    }

    /**
     * Retrieve data with the specified transfer ID.
     * 
     * @param transferId Identifier of data.
     * @return A list of {@code HttpData}.
     */
    public List<HttpData> selectByTransferId(String transferId)
    {
        if ( _logger.isDebugEnabled() ) _logger.debug("Retrieving data by transferId ..."); //$NON-NLS-1$

        return this.jdbcTemplate.query(sqlSelectByTransferId, new Object[]
        {
            transferId
        }, new RowMapper<HttpData>()
        {
            @Override
            public HttpData mapRow(ResultSet rs, int rowNum)
                    throws SQLException
            {
                return new HttpData(rs.getString(HttpData.COLUMN_NAME_TRANSFER_ID), rs
                        .getString(HttpData.COLUMN_NAME_RIVER_NAME), rs
                        .getString(HttpData.COLUMN_NAME_CONTENT_TYPE), rs
                        .getString(HttpData.COLUMN_NAME_CONTENT_DISPOSITION), rs
                        .getString(HttpData.COLUMN_NAME_CONTENT_DESCRIPTION), rs.getTimestamp(
                        HttpData.COLUMN_NAME_TIMESTAMP).getTime(), rs.getBytes(HttpData.COLUMN_NAME_DATA), rs
                        .getString(HttpData.COLUMN_NAME_DEVICE_ID));
            }
        });
    }

    /**
     * Retrieve all {@code HttpData} from HttpData table.
     * 
     * @return a list of {@code HttpData}
     */
    public List<HttpData> selectAll()
    {
        if ( _logger.isDebugEnabled() ) _logger.debug("Retrieving all records ..."); //$NON-NLS-1$

        return this.jdbcTemplate.query(sqlSelectAll, new RowMapper<HttpData>()
        {
            @Override
            public HttpData mapRow(ResultSet rs, int rowNum)
                    throws SQLException
            {
                return new HttpData(rs.getString(HttpData.COLUMN_NAME_TRANSFER_ID), rs
                        .getString(HttpData.COLUMN_NAME_RIVER_NAME), rs
                        .getString(HttpData.COLUMN_NAME_CONTENT_TYPE), rs
                        .getString(HttpData.COLUMN_NAME_CONTENT_DISPOSITION), rs
                        .getString(HttpData.COLUMN_NAME_CONTENT_DESCRIPTION), rs.getTimestamp(
                        HttpData.COLUMN_NAME_TIMESTAMP).getTime(), rs.getBytes(HttpData.COLUMN_NAME_DATA), rs
                        .getString(HttpData.COLUMN_NAME_DEVICE_ID));
            }
        });
    }

    /**
     * Count the number of row with the specified transfer ID.
     * 
     * @param transferId ID of the record(s) to be retrieved.
     * @return The number of rows with the specified transfer ID.
     */
    public int selectCountByTransferId(String transferId)
    {
        if ( _logger.isDebugEnabled() ) _logger.debug("Counting number of records ..."); //$NON-NLS-1$

        String sql = "select count(*) from " + TABLE_NAME + " where " + HttpData.COLUMN_NAME_TRANSFER_ID + " = ?"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        return getJdbcTemplate().queryForObject(sql, new Object[]
        {
            transferId
        }, Integer.class);
    }
}
