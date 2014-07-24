/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.snapshot.manager.spring.batch;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.duracloud.common.notification.NotificationManager;
import org.duracloud.common.notification.NotificationType;
import org.duracloud.snapshot.common.test.SnapshotTestBase;
import org.duracloud.snapshot.db.model.Snapshot;
import org.duracloud.snapshot.db.repo.RestorationRepo;
import org.duracloud.snapshot.db.repo.SnapshotRepo;
import org.duracloud.snapshot.manager.SnapshotConstants;
import org.duracloud.snapshot.manager.config.ExecutionListenerConfig;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;

/**
 * @author Bill Branan
 *         Date: 2/18/14
 */
public class SnapshotExecutionListenerTest extends SnapshotTestBase {

    private Long snapshotID = 101010101l;
    private String snapshotName = "snapshot-name";
    private String contentDir = "content-dir";
    private JobParameters jobParams;

    @Mock
    private NotificationManager notificationManager;

    @Mock
    private ExecutionListenerConfig executionConfig;

    @Mock
    private JobExecution jobExecution;

    @Mock
    private SnapshotRepo snapshotRepo;
    
    @Mock
    private RestorationRepo restorationRepo;
    
    @Mock
    private JobInstance job;

    @Mock 
    private Snapshot snapshot;
    
    @TestSubject
    private SnapshotExecutionListener executionListener;

    @Before
    public void setup() {
        super.setup();
        executionListener =
            new SnapshotExecutionListener();
        
        executionListener.setNotificationManager(notificationManager);

        Map<String, JobParameter> jobParamMap = new HashMap<>();
        jobParamMap.put(SnapshotConstants.OBJECT_ID,
                        new JobParameter(snapshotID));
        jobParams = new JobParameters(jobParamMap);
        executionListener.init(executionConfig);
        
        
    }

    @Test
    public void testAfterJobSuccess() {
        String dpnEmail = "dpn-email";
        String duracloudEmail = "duracloud-email";

        setupJob(SnapshotConstants.SNAPSHOT_JOB_NAME);

        EasyMock.expect(jobExecution.getStatus())
                .andReturn(BatchStatus.COMPLETED);

        Capture<String> messageCapture = new Capture<>();
        notificationManager.sendNotification(
            EasyMock.eq(NotificationType.EMAIL),
            EasyMock.<String>anyObject(),
            EasyMock.capture(messageCapture),
            EasyMock.eq(dpnEmail),
            EasyMock.eq(duracloudEmail));
        EasyMock.expectLastCall();

        EasyMock.expect(executionConfig.getAllEmailAddresses())
                .andReturn(new String[]{dpnEmail, duracloudEmail});

        replayAll();

        executionListener.afterJob(jobExecution);
        String message = messageCapture.getValue();
        assertTrue(message.contains(snapshotName));
        assertTrue(message.contains(contentDir));
        assertTrue(message.contains("preservation"));
    }

    /**
     * 
     */
    private void setupJob(String jobName) {
        expect(jobExecution.getJobParameters())
                .andReturn(jobParams);
        
        expect(job.getJobName()).andReturn(jobName);
        
        EasyMock.expect(jobExecution.getJobInstance()).andReturn(job);
        
        expect(snapshotRepo.getOne(snapshotID)).andReturn(snapshot);

        expect(executionConfig.getContentRoot()).andReturn(new File(contentDir));
        
        expect(snapshot.getName()).andReturn(snapshotName).atLeastOnce();

    }

    @Test
    public void testAfterJobFailure() {
        setupJob(SnapshotConstants.SNAPSHOT_JOB_NAME);

        String duracloudEmail = "duracloud-email";

        EasyMock.expect(jobExecution.getStatus())
                .andReturn(BatchStatus.FAILED);

        Capture<String> messageCapture = new Capture<>();
        notificationManager.sendNotification(
            EasyMock.eq(NotificationType.EMAIL),
            EasyMock.<String>anyObject(),
            EasyMock.capture(messageCapture),
            EasyMock.eq(duracloudEmail));
        EasyMock.expectLastCall();

        EasyMock.expect(executionConfig.getDuracloudEmailAddresses())
                .andReturn(new String[]{duracloudEmail});

        replayAll();

        executionListener.afterJob(jobExecution);
        String message = messageCapture.getValue();
        assertTrue(message.contains(snapshotName));
        assertTrue(message.contains(contentDir));
        assertTrue(message.contains("failed"));
    }

}
