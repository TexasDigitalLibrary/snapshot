/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.snapshot.manager.spring.batch;

import org.duracloud.common.notification.NotificationManager;
import org.duracloud.common.notification.NotificationType;
import org.duracloud.snapshot.manager.SnapshotConstants;
import org.duracloud.snapshot.manager.config.SnapshotNotifyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;

/**
 * @author Erik Paulsson
 *         Date: 2/10/14
 */
public class SnapshotExecutionListener implements JobExecutionListener {

    private static final Logger log =
        LoggerFactory.getLogger(SnapshotExecutionListener.class);

    private NotificationManager notificationManager;
    private SnapshotNotifyConfig snapshotNotifyConfig;

    public SnapshotExecutionListener(NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
    }

    public void init(SnapshotNotifyConfig snapshotNotifyConfig) {
        this.snapshotNotifyConfig = snapshotNotifyConfig;
    }

    public void beforeJob(JobExecution jobExecution) {

    }

    public void afterJob(JobExecution jobExecution) {
        JobParameters jobParams = jobExecution.getJobParameters();
        BatchStatus status = jobExecution.getStatus();

        String snapshotId = jobParams.getString(SnapshotConstants.SNAPSHOT_ID);
        String snapshotPath = jobParams.getString(SnapshotConstants.CONTENT_DIR);

        log.debug("Completed snapshot: {} with status: {}", snapshotId, status);
        String jobName = jobExecution.getJobInstance().getJobName();
        
        if(jobName.equals(SnapshotConstants.SNAPSHOT_JOB_NAME)){
            handleAfterSnapshotJob(status, snapshotId, snapshotPath);
        }else if(jobName.equals(SnapshotConstants.RESTORE_JOB_NAME)){
            handleAfterRestorationJob(status, snapshotId, snapshotPath);
        }
    }

    /**
     * @param status
     * @param snapshotId
     * @param snapshotPath
     */
    private void handleAfterRestorationJob(BatchStatus status,
                                           String snapshotId,
                                           String snapshotPath) {
        log.debug("Completed snapshot: {} with status: {}", snapshotId, status);
        if(BatchStatus.COMPLETED.equals(status)) {
            // Job success. Email duracloud team as well as restoration requestor
            // TODO Figure out where the duracloud user's email is coming from.
            // TODO make sure that duracloud space location (host,port,space,store) info
            //  is included in the email.
            String subject =
                "DuraCloud snapshot has been restored!";
            String message =
                "A DuraCloud content snapshot has been transferred from " +
                "bridge storage to DuraCloud";
            sendEmail(subject, message, this.snapshotNotifyConfig.getDuracloudEmailAddresses());
        } else {
            // Job failed.  Email DuraSpace team about failed snapshot attempt.
            String subject =
                "DuraCloud snapshot restoration failed to complete";
            String message =
                "A DuraCloud snapshot restoration has failed to complete.\n" +
                "\nsnapshot-id=" + snapshotId +
                "\nsnapshot-path=" + snapshotPath;
                // TODO: Add details of failure in message
            sendEmail(subject, message,
                      snapshotNotifyConfig.getDuracloudEmailAddresses());
        }
        
    }

    /**
     * @param status
     * @param snapshotId
     * @param snapshotPath
     */
    private void handleAfterSnapshotJob(BatchStatus status,
                                        String snapshotId,
                                        String snapshotPath) {
        if(BatchStatus.COMPLETED.equals(status)) {
            // Job success. Email Chronopolis/DPN AND DuraSpace teams about
            // snapshot ready for transfer into preservation storage.
            String subject =
                "DuraCloud content snapshot ready for preservation";
            String message =
                "A DuraCloud content snapshot has been transferred from " +
                "DuraCloud to bridge storage and ready to move into " +
                "preservation storage.\n" +
                "\nsnapshot-id=" + snapshotId +
                "\nsnapshot-path=" + snapshotPath;
            sendEmail(subject, message,
                      snapshotNotifyConfig.getAllEmailAddresses());
        } else {
            // Job failed.  Email DuraSpace team about failed snapshot attempt.
            String subject =
                "DuraCloud content snapshot failed to complete";
            String message =
                "A DuraCloud content snapshot has failed to complete.\n" +
                "\nsnapshot-id=" + snapshotId +
                "\nsnapshot-path=" + snapshotPath;
                // TODO: Add details of failure in message
            sendEmail(subject, message,
                      snapshotNotifyConfig.getDuracloudEmailAddresses());
        }
    }

    private void sendEmail(String subject, String msg, String... destinations) {
        notificationManager.sendNotification(NotificationType.EMAIL,
                                             subject,
                                             msg.toString(),
                                             destinations);
    }
}
