/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.snapshot.restoration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.duracloud.common.json.JaxbJsonSerializer;
import org.duracloud.common.notification.NotificationManager;
import org.duracloud.common.notification.NotificationType;
import org.duracloud.common.util.IOUtil;
import org.duracloud.snapshot.manager.SnapshotException;
import org.duracloud.snapshot.manager.SnapshotInProcessException;
import org.duracloud.snapshot.manager.SnapshotJobManager;
import org.duracloud.snapshot.manager.SnapshotNotFoundException;
import org.duracloud.snapshot.manager.SnapshotStatus;
import org.duracloud.snapshot.manager.SnapshotStatus.SnapshotStatusType;
import org.duracloud.snapshot.restoration.RestoreStatus.RestoreStatusType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Daniel Bernstein
 *         Date: Jul 15, 2014
 */
@Component
public class SnapshotRestorationManagerImpl  implements SnapshotRestorationManager{
    private static Logger log =
        LoggerFactory.getLogger(SnapshotRestorationManagerImpl.class);
    private RestorationConfig config;
    private SnapshotJobManager jobManager;
    private NotificationManager notificationManager;
    
    @Autowired
    public SnapshotRestorationManagerImpl(
        SnapshotJobManager jobManager, NotificationManager notificationManager) {
        this.jobManager = jobManager;
        this.notificationManager = notificationManager;
    }    

    
    /* (non-Javadoc)
     * @see org.duracloud.snapshot.restoration.SnapshotRestorationManager#restoreSnapshot(java.lang.String)
     */
    @Override
    public RestoreStatus restoreSnapshot(String snapshotId)
        throws SnapshotNotFoundException,
            SnapshotInProcessException,
            SnapshotException {
        checkInitialized();
        
        //check that snapshot exists
        SnapshotStatus status = this.jobManager.getStatus(snapshotId);
        
        if(!SnapshotStatusType.COMPLETED.equals(status.getStatus())){
            throw new SnapshotInProcessException("Snapshot is not complete. " +
                                                 "Restoration can only occur on a " +
                                                 "completed snapshot.");
        }
        
        RestoreStatus restoreStatus = getRestoreStatus(snapshotId);
        RestoreStatusType statusType = restoreStatus.getStatusType();
        //verify that restore has not already started.
        if(statusType.equals(RestoreStatusType.COMPLETED) || statusType.equals(RestoreStatusType.REQUEST_ISSUED)){
            return restoreStatus;
        }else if(statusType.equals(RestoreStatusType.NO_RESTORATION_FOUND)){
            //create directory
            File restoreDir = getRestoreDir(snapshotId);
            restoreDir.mkdirs();
            
            
            //send email to DPN 
            notificationManager.sendNotification(NotificationType.EMAIL,
                                     "Snapshot Restoration Request for Snapshot ID = " + snapshotId,
                                     "Please restore the following snapshot to " + restoreDir.getAbsolutePath(),
                                     getAllEMailAddresses(this.config));

            //serialize status
            restoreStatus = new RestoreStatus(RestoreStatusType.REQUEST_ISSUED,
                                              "request issued at "
                                                  + new Date());
            
            writeStatusToDisk(snapshotId, restoreStatus);
            
            return restoreStatus;
        }else{
            throw new SnapshotException("Unrecognized restore state: " + statusType, null);
        }
        
    }



    /**
     * @param snapshotId
     * @param restoreStatus
     * @throws SnapshotException
     */
    private void writeStatusToDisk(String snapshotId,
                                   RestoreStatus restoreStatus)
        throws SnapshotException {
        File restoreStatusFile = getRestorationStatusFile(snapshotId);
        try (FileOutputStream os = new FileOutputStream(restoreStatusFile)){
            JaxbJsonSerializer<RestoreStatus> serializer = new JaxbJsonSerializer<>(RestoreStatus.class);
            String jsonString =
                serializer.serialize(restoreStatus);
            os.write(jsonString.getBytes());
        }catch(IOException ex){
            throw new SnapshotException("Failed to save status: " + ex.getMessage(), ex);
        }
    }
    
    /**
     * @param config2
     * @return
     */
    private String[] getAllEMailAddresses(RestorationConfig config) {
        List<String> allAddresses = new ArrayList<String>();
        allAddresses.addAll(Arrays.asList(config.getDuracloudEmailAddresses()));
        allAddresses.addAll(Arrays.asList(config.getDpnEmailAddresses()));
        return allAddresses.toArray(new String[allAddresses.size()]);
    }



    /**
     * @param snapshotId
     * @return
     */
    public RestoreStatus getRestoreStatus(String snapshotId) throws SnapshotException{
        checkInitialized();
        File statusFile = getRestorationStatusFile(snapshotId);
        
        if(!statusFile.exists()){
            return new RestoreStatus(RestoreStatusType.NO_RESTORATION_FOUND, "No restoration has been requested");
        }
        
        try (FileInputStream fis = new FileInputStream(statusFile)){
            String json = IOUtil.readStringFromStream(fis);
            JaxbJsonSerializer<RestoreStatus> serializer = new JaxbJsonSerializer<>(RestoreStatus.class);
            RestoreStatus status = serializer.deserialize(json);
            return status;
        }catch(IOException ex){
            throw new SnapshotException("failed to deserialize status file for snapshot " + snapshotId, ex);
        }
    }

    /**
     * @param snapshotId
     * @return
     */
    private File getRestorationStatusFile(String snapshotId) {
        String restoreStatusFilePath =
            this.config.getRestorationRootDir()
                + File.separator + "state-" + snapshotId + ".json";
        return new File(restoreStatusFilePath);
    }



    /**
     * @param snapshotId
     * @return
     */
    private File getRestoreDir(String snapshotId) {
        File restoreDir = new File(getSnapshotRestorationDir(snapshotId));
        return restoreDir;
    }



    /* (non-Javadoc)
     * @see org.duracloud.snapshot.restoration.SnapshotRestorationManager#snapshotRestorationCompleted(java.lang.String)
     */
    @Override
    public RestoreStatus snapshotRestorationCompleted(String snapshotId)
        throws SnapshotNotFoundException,
            SnapshotInProcessException,
            NoRestorationInProcessException,
            SnapshotException {
        
        RestoreStatus status = getRestoreStatus(snapshotId);
        
        RestoreStatusType statusType = status.getStatusType();
        if(statusType.equals(RestoreStatusType.NO_RESTORATION_FOUND)){
            log.warn("unable to process restoration completed call:  snapshot id does not exist: "
                + snapshotId);
            throw new SnapshotNotFoundException("snapshot "
                + snapshotId + " not found. Unable to issue completed call.");
        } else if(statusType.equals(RestoreStatusType.COMPLETED)){
            log.warn("snapshot " + snapshotId + " already completed. Ignoring...");
            return status;
        } else if(statusType.equals(RestoreStatusType.REQUEST_ISSUED)){
            log.info("caller has indicated that snapshot " + snapshotId + " is complete.");
            status = new RestoreStatus(RestoreStatusType.COMPLETED, "completed on " + new Date());
            writeStatusToDisk(snapshotId, status);
            return status;
        } else{
            String message =
                "restore status type "
                    + statusType + " not recognized. (snapshotId = "
                    + snapshotId + ")";
            log.error(message);
            throw new SnapshotException(message,null);
        }
        
    }
    
    
    /**
     * 
     */
    private void checkInitialized() throws SnapshotException {   
        if(this.config == null){
            throw new SnapshotException("The snapshot restoration manager has not been initialized.", null);
        }
    }

    /* (non-Javadoc)
     * @see org.duracloud.snapshot.restoration.SnapshotRestorationManager#init(org.duracloud.snapshot.restoration.RestorationConfig)
     */
    @Override
    public void init(RestorationConfig config) {
        this.config = config;
    }
    
    /**
     * @param snapshotId
     * @return
     */
    private String getSnapshotRestorationDir(String snapshotId) {
        String contentDir =
            config.getRestorationRootDir()+ File.separator + snapshotId;
        return contentDir;
    }

}
