/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.snapshot.rest;

/**
 * @author Daniel Bernstein
 *         Date: Jul 14, 2014
 */
public class CreateSnapshotParams {
    private String host;
    private String port;
    private String storeId;
    private String spaceId;

    /**
     * @param host
     * @param port
     * @param storeId
     * @param spaceId
     */
    public CreateSnapshotParams(
        String host, String port, String storeId, String spaceId) {
        this.host = host;
        this.port = port;
        this.storeId = storeId;
        this.spaceId = spaceId;
    }
    
    /**
     * 
     */
    public CreateSnapshotParams() {}

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }
    /**
     * @param host the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }
    /**
     * @return the port
     */
    public String getPort() {
        return port;
    }
    /**
     * @param port the port to set
     */
    public void setPort(String port) {
        this.port = port;
    }
    /**
     * @return the storeId
     */
    public String getStoreId() {
        return storeId;
    }
    /**
     * @param storeId the storeId to set
     */
    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }
    /**
     * @return the spaceId
     */
    public String getSpaceId() {
        return spaceId;
    }
    /**
     * @param spaceId the spaceId to set
     */
    public void setSpaceId(String spaceId) {
        this.spaceId = spaceId;
    }
}
