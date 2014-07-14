/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.snapshot.rest;

import org.duracloud.snapshot.common.test.SnapshotTestBase;
import org.duracloud.snapshot.manager.SnapshotException;
import org.duracloud.snapshot.restoration.RestoreStatus;
import org.duracloud.snapshot.restoration.RestoreStatus.RestoreStatusType;
import org.duracloud.snapshot.restoration.SnapshotRestorationManager;
import org.easymock.EasyMock;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Test;

/**
 * @author Daniel Bernstein
 *         Date: Feb 4, 2014
 */

public class RestorationResourceTest extends SnapshotTestBase {
    
    @Mock
    private SnapshotRestorationManager manager;
    @TestSubject
    private RestorationResource resource;
    
    /* (non-Javadoc)
     * @see org.duracloud.snapshot.common.test.EasyMockTestBase#setup()
     */
    @Override
    public void setup() {
        super.setup();
        resource = new RestorationResource(manager);
    }
    
  


    @Test
    public void testRestoreSnapshot() throws SnapshotException {
        String snapshotId = "mysnapshot";
        EasyMock.expect(manager.restoreSnapshot(snapshotId))
                .andReturn(new RestoreStatus(RestoreStatusType.REQUEST_ISSUED,"request issued."));
        replayAll();
        resource.restoreSnapshot(snapshotId);
        
    }

    @Test
    public void testSnapshotRestorationComplete() throws SnapshotException {
        String snapshotId = "mysnapshot";
        EasyMock.expect(manager.snapshotRestorationCompleted(snapshotId))
                .andReturn(new RestoreStatus(RestoreStatusType.COMPLETED,"request issued."));
        replayAll();
        resource.restoreComplete(snapshotId);
        
    }

}
