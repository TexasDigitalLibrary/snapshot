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
import org.duracloud.snapshot.manager.JobStatus;
import org.duracloud.snapshot.manager.JobStatus.SnapshotStatusType;
import org.duracloud.snapshot.manager.SnapshotException;
import org.duracloud.snapshot.manager.SnapshotInProcessException;
import org.duracloud.snapshot.manager.SnapshotJobManager;
import org.duracloud.snapshot.manager.SnapshotNotFoundException;
import org.duracloud.snapshot.manager.config.SnapshotConfig;
import org.duracloud.snapshot.restoration.RestorationRequest.RestoreStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Daniel Bernstein
 *         Date: Jul 15, 2014
 */
@Component
public class RestorationManagerImpl  implements RestorationManager{
    private static Logger log =
        LoggerFactory.getLogger(RestorationManagerImpl.class);
    private RestorationConfig config;
    private SnapshotJobManager jobManager;
    private NotificationManager notificationManager;
    
    @Autowired
    public RestorationManagerImpl(
        SnapshotJobManager jobManager, NotificationManager notificationManager) {
        this.jobManager = jobManager;
        this.notificationManager = notificationManager;
    }    


    /* (non-Javadoc)
     * @see org.duracloud.snapshot.restoration.SnapshotRestorationManager#restoreSnapshot(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public RestorationRequest restoreSnapshot(RestoreRequestConfig config)
        throws SnapshotNotFoundException,
            SnapshotInProcessException,
            SnapshotException {
        
        checkInitialized();
        
        String snapshotId = config.getSnapshotId();
        
        //check that snapshot exists
        JobStatus snapshotStatus = this.jobManager.getStatus(snapshotId);
        
        if(!SnapshotStatusType.COMPLETED.equals(snapshotStatus.getStatus())){
            throw new SnapshotInProcessException("Snapshot is not complete. " +
                                                 "Restoration can only occur on a " +
                                                 "completed snapshot.");
        }
        
        String restorationId = RestorationUtil.getId(config);
        RestorationRequest restoreRequest = getRestoreRequestInternal(restorationId);
        
        if(restoreRequest == null){
            //create directory
            File restoreDir = getRestoreDir(restorationId);
            restoreDir.mkdirs();
            
            //send email to DPN 
            notificationManager.sendNotification(NotificationType.EMAIL,
                                     "Snapshot Restoration Request for Snapshot ID = " + snapshotId,
                                     "Please restore the following snapshot to the following location: " + restoreDir.getAbsolutePath(),
                                     getAllEMailAddresses(this.config));

            //serialize status
            restoreRequest =
                new RestorationRequest(RestorationUtil.getId(config),
                                       config,
                                       RestoreStatus.REQUEST_ISSUED);
            restoreRequest.setMessage("request issued at "
                                                  + new Date());
            
            persist(restoreRequest);
            
            return restoreRequest;
        }
        
        RestoreStatus status = restoreRequest.getStatus();

        //verify that restore has not already started.
        if(status.equals(RestoreStatus.RESTORE_TO_BRIDGE_COMPLETE) || status.equals(RestoreStatus.REQUEST_ISSUED)){
            return restoreRequest;
        }else{
            throw new SnapshotException("Unrecognized restore state: " + status, null);
        }
        
    }

    private void persist(RestorationRequest restoreRequest)
        throws SnapshotException {
        File restoreStatusFile = getRestorationRequestStateFile(restoreRequest.getId());
        try (FileOutputStream os = new FileOutputStream(restoreStatusFile)){
            JaxbJsonSerializer<RestorationRequest> serializer = new JaxbJsonSerializer<>(RestorationRequest.class);
            String jsonString =
                serializer.serialize(restoreRequest);
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
     * @param restorationId
     * @return
     */

    public RestorationRequest getRestoreRequest(String restorationId)
        throws NoRestorationInProcessException,
            SnapshotException {
        RestorationRequest request = getRestoreRequestInternal(restorationId);
        if (request == null) {
            throw new NoRestorationInProcessException(restorationId);
        }

        return request;
    }

    private RestorationRequest getRestoreRequestInternal(String restorationId) throws SnapshotException{
        checkInitialized();
        File statusFile = getRestorationRequestStateFile(restorationId);
        
        if(!statusFile.exists()){
            return null;
        }
        
        try (FileInputStream fis = new FileInputStream(statusFile)){
            String json = IOUtil.readStringFromStream(fis);
            JaxbJsonSerializer<RestorationRequest> serializer = new JaxbJsonSerializer<>(RestorationRequest.class);
            RestorationRequest status = serializer.deserialize(json);
            return status;
        }catch(IOException ex){
            throw new SnapshotException("failed to deserialize status file for snapshot request " + config, ex);
        }
    }

    /**
     * @param snapshotId
     * @return
     */
    private File getRestorationRequestStateFile(String restorationId) {
        String restoreStatusFilePath =
            getRestorationRootDir(restorationId)
                + File.separator + "state.json";
        return new File(restoreStatusFilePath);
    }


    /**
     * @param restorationId
     * @return
     */
    private File getRestoreDir(String restorationId) {
        File restoreDir = new File(getRestorationContentDir(restorationId));
        return restoreDir;
    }



    /* (non-Javadoc)
     * @see org.duracloud.snapshot.restoration.SnapshotRestorationManager#restorationCompleted(java.lang.String)
     */
    @Override
    public RestorationRequest restorationCompleted(String restorationId)
        throws SnapshotNotFoundException,
            SnapshotInProcessException,
            NoRestorationInProcessException,
            SnapshotException {
        
        RestorationRequest request = getRestoreRequestInternal(restorationId);
        if(request == null){
            log.warn("unable to process restoration completed call:  restoration id does not exist: "
                + restorationId);
            throw new NoRestorationInProcessException("restoration "
                + restorationId + " not found. Unable to issue completed call.");
        } 
        
        RestoreStatus status = request.getStatus();
        
        if(status.equals(RestoreStatus.RESTORE_TO_BRIDGE_COMPLETE)){
            log.warn("restoration " + restorationId + " already completed. Ignoring...");
            return request;
        } else if(status.equals(RestoreStatus.REQUEST_ISSUED)){
            log.info("caller has indicated that restoration request " + restorationId + " is complete.");
            request.setMessage("completed on " + new Date());
            request.setStatus(RestoreStatus.RESTORE_TO_BRIDGE_COMPLETE);
            persist(request);
            
            RestoreRequestConfig requestConfig =  request.getConfig();
            SnapshotConfig snapshotConfig = new SnapshotConfig();
            
            snapshotConfig.setContentDir(getRestoreDir(restorationId));
            snapshotConfig.setHost(requestConfig.getHost());
            snapshotConfig.setPort(requestConfig.getPort());
            snapshotConfig.setSnapshotId(requestConfig.getSnapshotId());
            snapshotConfig.setStoreId(requestConfig.getStoreId());
            snapshotConfig.setSpaceId(requestConfig.getSpaceId());

            this.jobManager.executeRestoration(snapshotConfig);
            
            return request;
        } else{
            String message =
                "restore status type "
                    + status + " not recognized. (restorationId = "
                    + restorationId + ")";
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
    
    private String getRestorationContentDir(String restorationId) {
        String contentDir =
            getRestorationRootDir(restorationId)
                + File.separator + "data";
        return contentDir;
    }
    
    private String getRestorationRootDir(String restorationId) {
        String contentDir =
            config.getRestorationRootDir()+ File.separator + restorationId;
        return contentDir;
    }

}
