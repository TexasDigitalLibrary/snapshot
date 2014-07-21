/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.snapshot.restoration;

/**
 * @author Daniel Bernstein
 *         Date: Jul 15, 2014
 */
public class RestorationRequest {
    public static enum RestoreStatus {
        INITIALIZED,
        WAITING_FOR_DPN,
        DPN_TRANSFER_COMPLETE,
        TRANSFERRING_TO_DURACLOUD,
        VERIFYING_TRANSFERRED_CONTENT,
        RESTORATION_COMPLETE;
    }
    
    private RestoreStatus status;
    private String id;
    private RestoreRequestConfig config;
    private String message;

    
    
    /**
     * @param status
     * @param message
     */
    public RestorationRequest(
        String id, RestoreRequestConfig config, RestoreStatus status) {
        super();
        this.status = status;
        this.config = config;
        this.id = id;
    }

    public RestorationRequest() {}

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the status
     */
    public RestoreStatus getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(RestoreStatus status) {
        this.status = status;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @param config the config to set
     */
    public void setConfig(RestoreRequestConfig config) {
        this.config = config;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * @return the config
     */
    public RestoreRequestConfig getConfig() {
        return config;
    }
   
}
