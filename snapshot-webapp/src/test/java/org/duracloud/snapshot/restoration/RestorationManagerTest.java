/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.snapshot.restoration;

import static org.easymock.EasyMock.isA;

import java.io.File;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.duracloud.common.notification.NotificationManager;
import org.duracloud.common.notification.NotificationType;
import org.duracloud.snapshot.common.test.SnapshotTestBase;
import org.duracloud.snapshot.manager.JobStatus;
import org.duracloud.snapshot.manager.JobStatus.SnapshotStatusType;
import org.duracloud.snapshot.manager.SnapshotException;
import org.duracloud.snapshot.manager.SnapshotInProcessException;
import org.duracloud.snapshot.manager.SnapshotJobManager;
import org.duracloud.snapshot.manager.SnapshotNotFoundException;
import org.duracloud.snapshot.manager.config.SnapshotConfig;
import org.duracloud.snapshot.restoration.RestorationRequest.RestoreStatus;
import org.easymock.EasyMock;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Daniel Bernstein
 *         Date: Jul 16, 2014
 */
public class RestorationManagerTest  extends SnapshotTestBase {

    @Mock
    private SnapshotJobManager jobManager;
    
    @Mock
    private NotificationManager notificationManager;

    @TestSubject
    private RestorationManagerImpl manager;
    

    /* (non-Javadoc)
     * @see org.duracloud.snapshot.common.test.SnapshotTestBase#setup()
     */
    @Override
    public void setup() {
        super.setup();
        setupManager();
    }
    /**

    /**
     * Test method for {@link org.duracloud.snapshot.restoration.RestorationManagerImpl#restoreSnapshot(java.lang.String)}.
     * @throws SnapshotException 
     * @throws SnapshotInProcessException 
     * @throws SnapshotNotFoundException 
     */
    @Test
    public void testRestoreSnapshot() throws SnapshotNotFoundException, SnapshotInProcessException, SnapshotException {
        setupRestoreCall();
        replayAll();
        RestorationRequest status = manager.restoreSnapshot(createRestoreRequestConfig());
        Assert.assertNotNull(status);
        Assert.assertEquals(status.getStatus(), RestoreStatus.REQUEST_ISSUED);
    }
    /**
     * @throws SnapshotNotFoundException
     * @throws SnapshotException
     */
    private void setupRestoreCall()
        throws SnapshotNotFoundException,
            SnapshotException {
        expectCompletedSnapshotJobStatus();
        notificationManager.sendNotification(isA(NotificationType.class),
                                             isA(String.class),
                                             isA(String.class),
                                             EasyMock.isA(String.class),
                                             EasyMock.isA(String.class));
    }

    /**
     * Test method for {@link org.duracloud.snapshot.restoration.RestorationManagerImpl#getRestoreRequest(java.lang.String)}.
     * @throws SnapshotException 
     * @throws SnapshotNotFoundException 
     */
    @Test
    public void testGetRestoreStatus() throws SnapshotException {

        replayAll();

        try { 
            this.manager.getRestoreRequest("test");
            Assert.fail();
        }catch(NoRestorationInProcessException ex){
            Assert.assertTrue(true);
        }
    }
    /**
     * @throws SnapshotNotFoundException
     * @throws SnapshotException
     */
    private void expectCompletedSnapshotJobStatus()
        throws SnapshotException {
        expectCompletedSnapshotJobStatus(1);
    }

    private void expectCompletedSnapshotJobStatus(int times)
        throws SnapshotException {
        EasyMock.expect(this.jobManager.getStatus(EasyMock.isA(String.class)))
                .andReturn(new JobStatus("test", SnapshotStatusType.COMPLETED)).times(times);
    }

    /**
     * 
     */
    private void setupManager() {
        manager = new RestorationManagerImpl(jobManager, notificationManager);
        RestorationConfig config = new RestorationConfig();
        config.setDpnEmailAddresses(new String[] {"a"});
        config.setDuracloudEmailAddresses(new String[]{"b"});
        config.setRestorationRootDir(System.getProperty("java.io.tmpdir")
            + File.separator + System.currentTimeMillis());
        manager.init(config);
    }

    /**
     * Test method for {@link org.duracloud.snapshot.restoration.RestorationManagerImpl#restorationCompleted(java.lang.String)}.
     */
    @Test
    public void testSnapshotRestorationCompleted() throws SnapshotException{
        setupRestoreCall();
        Future<JobStatus> future = null;
        EasyMock.expect(this.jobManager.executeRestoration(EasyMock.isA(SnapshotConfig.class)))
                .andReturn(future);
        
        replayAll();
        RestoreRequestConfig config = createRestoreRequestConfig();
        String restorationId = RestorationUtil.getId(config);
        RestorationRequest status = manager.restoreSnapshot(config);
        Assert.assertNotNull(status);
        Assert.assertEquals(status.getStatus(), RestoreStatus.REQUEST_ISSUED);

        status = manager.restorationCompleted(restorationId);
        Assert.assertNotNull(status);
        Assert.assertEquals(status.getStatus(), RestoreStatus.RESTORE_TO_BRIDGE_COMPLETE);

    }
    /**
     * @return
     */
    private RestoreRequestConfig createRestoreRequestConfig() {
        RestoreRequestConfig config = new RestoreRequestConfig("test", 443, "0", "space", "snapshot");
        return config;
    }
}
