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

    public void initialize(SnapshotNotifyConfig snapshotNotifyConfig) {
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
