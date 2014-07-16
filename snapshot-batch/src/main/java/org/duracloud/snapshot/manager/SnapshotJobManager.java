/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.snapshot.manager;

import java.util.List;
import java.util.concurrent.Future;

import org.duracloud.snapshot.manager.config.SnapshotConfig;
import org.duracloud.snapshot.manager.config.SnapshotJobManagerConfig;

/**
 * An interface defining the interaction between the REST
 * api and job engine.  Primary function is to hide the details 
 * of the underlying job processing subsystem.
 * @author Daniel Bernstein
 *         Date: Feb 11, 2014
 */
public interface SnapshotJobManager  {

    
    public final static String JOB_REPOSITORY_KEY = "jobRepository";
    public final static String JOB_EXPLORER_KEY = "jobExplorer";
    public final static String JOB_LAUNCHER_KEY = "jobLauncher";
    
    /**
     * Lazily initializes the component.  The data source must be fully configured
     * when this method is called; otherwise it will fail.
     * @param duracloudCredential
     */
    void init(SnapshotJobManagerConfig duracloudCredential);

    /**
     * This method of executing a snapshot is guaranteed to be an asynchronous version of executeSnapshot().
     * That is, the method will return after creating the job and queuing it up, rather than waiting until
     * it has been executed.
     * @param config
     * @return
     */
    Future<SnapshotStatus>  executeSnapshotAsync(SnapshotConfig config)
        throws SnapshotException;

    /**
     * This method creates an underlying job and executes it. 
     * @param config
     * @return
     * @throws SnapshotException
     */
    SnapshotStatus executeSnapshot(SnapshotConfig config) throws SnapshotException;

    /**
     * 
     * @param snapshotId
     * @return
     * @throws SnapshotNotFoundException
     * @throws SnapshotException
     */
    SnapshotStatus getStatus(String snapshotId)
        throws SnapshotNotFoundException,
            SnapshotException;

    /**
     * @return
     * @throws SnapshotException 
     */
    List<SnapshotSummary> getSnapshotList() throws SnapshotException;

}
