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
public class RestoreStatus {
    public static enum RestoreStatusType {
        NO_RESTORATION_FOUND,
        REQUEST_ISSUED,
        COMPLETED;
        
    }
    
    private RestoreStatusType statusType;
    
    private String message;

    
    /**
     * 
     */
    public RestoreStatus() {
        // TODO Auto-generated constructor stub
    }
    
    /**
     * @param statusType
     * @param message
     */
    public RestoreStatus(RestoreStatusType statusType, String message) {
        super();
        this.statusType = statusType;
        this.message = message;
    }

    /**
     * @return the statusType
     */
    public RestoreStatusType getStatusType() {
        return statusType;
    }

    /**
     * @param statusType the statusType to set
     */
    public void setStatusType(RestoreStatusType statusType) {
        this.statusType = statusType;
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
}
