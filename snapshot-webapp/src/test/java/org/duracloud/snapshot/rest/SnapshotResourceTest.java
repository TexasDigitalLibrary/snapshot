/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.snapshot.rest;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.Future;

import org.duracloud.snapshot.common.test.SnapshotTestBase;
import org.duracloud.snapshot.manager.SnapshotException;
import org.duracloud.snapshot.manager.SnapshotJobManager;
import org.duracloud.snapshot.manager.SnapshotNotFoundException;
import org.duracloud.snapshot.manager.JobStatus;
import org.duracloud.snapshot.manager.JobStatus.SnapshotStatusType;
import org.duracloud.snapshot.manager.config.SnapshotConfig;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Test;

/**
 * @author Daniel Bernstein
 *         Date: Feb 4, 2014
 */

public class SnapshotResourceTest extends SnapshotTestBase {
    
    @Mock
    private SnapshotJobManager manager;
    
    @Mock 
    private Future<JobStatus> future;
    
    @TestSubject
    private SnapshotResource resource;
    
    /* (non-Javadoc)
     * @see org.duracloud.snapshot.common.test.EasyMockTestBase#setup()
     */
    @Override
    public void setup() {
        super.setup();
        resource = new SnapshotResource(manager);
    }
    
    @Test
    public void testGetStatusSuccess() throws SnapshotException {
        EasyMock.expect(manager.getStatus(EasyMock.isA(String.class)))
                .andReturn(new JobStatus("snapshotId", SnapshotStatusType.UNKNOWN));
        replayAll();
        resource.getStatus("snapshotId");
    }

    @Test
    public void testGetStatusNotFound() throws SnapshotException {
        EasyMock.expect(manager.getStatus(EasyMock.isA(String.class)))
                .andThrow(new SnapshotNotFoundException("test"));
        replayAll();
        resource.getStatus("snapshotId");
    }

    @Test
    public void testCreate() throws SnapshotException {
        String host = "host";
        String port = "444";
        String storeId = "storeId";
        String spaceId = "spaceId";
        String snapshotId = "snapshotId";
        
        Capture<SnapshotConfig> snapshotConfigCapture = new Capture<>();
        EasyMock.expect(manager.executeSnapshotAsync(
                     EasyMock.capture(snapshotConfigCapture)))
                .andReturn(future);

        
        replayAll();
        
        resource.create(snapshotId, new SnapshotRequestParams(host, port, storeId, spaceId));

        SnapshotConfig snapshotConfig = snapshotConfigCapture.getValue();
        assertEquals(host, snapshotConfig.getHost());
        assertEquals(Integer.parseInt(port), snapshotConfig.getPort());
        assertEquals(storeId, snapshotConfig.getStoreId());
        assertEquals(spaceId, snapshotConfig.getSpaceId());
        assertEquals(snapshotId, snapshotConfig.getSnapshotId());
    }


}
