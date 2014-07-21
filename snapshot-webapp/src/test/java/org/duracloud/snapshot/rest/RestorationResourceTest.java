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
import org.duracloud.snapshot.restoration.RestorationRequest;
import org.duracloud.snapshot.restoration.RestoreRequestConfig;
import org.duracloud.snapshot.restoration.RestorationRequest.RestoreStatus;
import org.duracloud.snapshot.restoration.RestorationManager;
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
    private RestorationManager manager;
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
        
        EasyMock.expect(manager.restoreSnapshot(EasyMock.isA(RestoreRequestConfig.class)))
                .andReturn(EasyMock.createMock(RestorationRequest.class));
        replayAll();
        RestoreParams params = new RestoreParams();
        params.setHost("hoset");
        params.setPort("443");
        params.setSnapshotId("snapshot");
        params.setSpaceId("space");
        resource.restoreSnapshot(params);
        
        
    }

    @Test
    public void testSnapshotRestorationComplete() throws SnapshotException {
        String restorationId = "restoreId";
        EasyMock.expect(manager.restorationCompleted(restorationId))
                .andReturn(EasyMock.createMock(RestorationRequest.class));
        replayAll();
        resource.restoreComplete(restorationId);
        
    }

}
