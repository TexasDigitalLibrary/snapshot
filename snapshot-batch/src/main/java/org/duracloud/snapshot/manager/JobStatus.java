/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.snapshot.manager;

/**
 * @author Daniel Bernstein
 *         Date: Feb 5, 2014
 */
public class JobStatus {
    private String id;
    public static enum SnapshotStatusType {
        ABANDONNED,
        COMPLETED,
        FAILED,
        STARTING,
        STARTED,
        STOPPING,
        STOPPED,
        UNKNOWN;
    }

    private SnapshotStatusType status;

    /**
     * 
     */
    public JobStatus() {
    }

    
    /**
     * @param id
     * @param status
     */
    public JobStatus(String id, SnapshotStatusType status) {
        super();
        this.id = id;
        this.status = status;
    }


    /**
     * @return the id
     */
    public String getId() {
        return id;
    }
    
    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * @return the status
     */
    public SnapshotStatusType getStatus() {
        return status;
    }
    
    /**
     * @param status the status to set
     */
    public void setStatus(SnapshotStatusType status) {
        this.status = status;
    }
}
