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

import org.duracloud.common.notification.NotificationManager;
import org.duracloud.common.notification.NotificationType;
import org.duracloud.snapshot.common.test.SnapshotTestBase;
import org.duracloud.snapshot.manager.SnapshotException;
import org.duracloud.snapshot.manager.SnapshotInProcessException;
import org.duracloud.snapshot.manager.SnapshotJobManager;
import org.duracloud.snapshot.manager.SnapshotNotFoundException;
import org.duracloud.snapshot.manager.SnapshotStatus;
import org.duracloud.snapshot.manager.SnapshotStatus.SnapshotStatusType;
import org.duracloud.snapshot.restoration.RestoreStatus.RestoreStatusType;
import org.easymock.EasyMock;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Daniel Bernstein
 *         Date: Jul 16, 2014
 */
public class SnapshotRestorationManagerTest  extends SnapshotTestBase {

    @Mock
    private SnapshotJobManager jobManager;
    
    @Mock
    private NotificationManager notificationManager;

    @TestSubject
    private SnapshotRestorationManagerImpl manager;
    

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
     * Test method for {@link org.duracloud.snapshot.restoration.SnapshotRestorationManagerImpl#restoreSnapshot(java.lang.String)}.
     * @throws SnapshotException 
     * @throws SnapshotInProcessException 
     * @throws SnapshotNotFoundException 
     */
    @Test
    public void testRestoreSnapshot() throws SnapshotNotFoundException, SnapshotInProcessException, SnapshotException {
        setupRestoreCall();
        replayAll();
        String snapshotId = "snapshot-" + System.currentTimeMillis();
        RestoreStatus status = manager.restoreSnapshot(snapshotId);
        Assert.assertNotNull(status);
        Assert.assertEquals(status.getStatusType(), RestoreStatusType.REQUEST_ISSUED);
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
     * Test method for {@link org.duracloud.snapshot.restoration.SnapshotRestorationManagerImpl#getRestoreStatus(java.lang.String)}.
     * @throws SnapshotException 
     * @throws SnapshotNotFoundException 
     */
    @Test
    public void testGetRestoreStatus() throws SnapshotNotFoundException, SnapshotException {

        replayAll();

        RestoreStatus status = this.manager.getRestoreStatus("test");
        Assert.assertNotNull(status);
        Assert.assertEquals(RestoreStatusType.NO_RESTORATION_FOUND, status.getStatusType());
    }
    /**
     * @throws SnapshotNotFoundException
     * @throws SnapshotException
     */
    private void expectCompletedSnapshotJobStatus()
        throws SnapshotNotFoundException,
            SnapshotException {
        expectCompletedSnapshotJobStatus(1);
    }

    private void expectCompletedSnapshotJobStatus(int times)
        throws SnapshotNotFoundException,
            SnapshotException {
        EasyMock.expect(this.jobManager.getStatus(EasyMock.isA(String.class)))
                .andReturn(new SnapshotStatus("test", SnapshotStatusType.COMPLETED)).times(times);
    }

    /**
     * 
     */
    private void setupManager() {
        manager = new SnapshotRestorationManagerImpl(jobManager, notificationManager);
        RestorationConfig config = new RestorationConfig();
        config.setDpnEmailAddresses(new String[] {"a"});
        config.setDuracloudEmailAddresses(new String[]{"b"});
        config.setRestorationRootDir(System.getProperty("java.io.tmpdir")
            + File.separator + System.currentTimeMillis());
        manager.init(config);
    }

    /**
     * Test method for {@link org.duracloud.snapshot.restoration.SnapshotRestorationManagerImpl#snapshotRestorationCompleted(java.lang.String)}.
     */
    @Test
    public void testSnapshotRestorationCompleted() throws SnapshotException{
        setupRestoreCall();
        //expectCompletedSnapshotJobStatus(2);

        replayAll();

        String snapshotId = "snapshot-" + System.currentTimeMillis();
        RestoreStatus status = manager.restoreSnapshot(snapshotId);
        Assert.assertNotNull(status);
        Assert.assertEquals(status.getStatusType(), RestoreStatusType.REQUEST_ISSUED);

        status = manager.snapshotRestorationCompleted(snapshotId);
        Assert.assertNotNull(status);
        Assert.assertEquals(status.getStatusType(), RestoreStatusType.COMPLETED);

    }
}
