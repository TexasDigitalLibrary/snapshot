/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.snapshot.manager.spring.batch;

import org.duracloud.snapshot.common.test.SnapshotTestBase;
import org.duracloud.snapshot.db.model.Restoration;
import org.duracloud.snapshot.db.model.Snapshot;
import org.duracloud.snapshot.db.repo.RestorationRepo;
import org.duracloud.snapshot.db.repo.SnapshotRepo;
import org.duracloud.snapshot.manager.SnapshotConstants;
import org.duracloud.snapshot.manager.SnapshotException;
import org.duracloud.snapshot.manager.SnapshotNotFoundException;
import org.duracloud.snapshot.manager.config.SnapshotJobManagerConfig;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Daniel Bernstein Date: Feb 19, 2014
 */
public class SnapshotJobManagerImplTest extends SnapshotTestBase {

    private String snapshotName = "test-id";
    private Long snapshotId = 10101l;

    @TestSubject
    private SnapshotJobManagerImpl manager;

    @Mock
    private JobExecutionListener snapshotJobListener;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private TaskExecutor taskExecutor;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobExplorer jobExplorer;

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private JobExecution jobExecution;

    @Mock
    private SnapshotJobBuilder jobBuilder;

    @Mock
    private Job job;

    @Mock
    private Snapshot snapshot;

    @Mock
    private Restoration restoration;

    @Mock
    private ApplicationContext context;

    @Mock
    private SnapshotJobManagerConfig config;
    
    @Mock
    private RestorationRepo restorationRepo;
    
    @Mock
    private SnapshotRepo snapshotRepo;



    @Before
    public void setup() {
        manager =
            new SnapshotJobManagerImpl(snapshotJobListener,
                                       transactionManager,
                                       taskExecutor,
                                       snapshotRepo,
                                       restorationRepo);
        manager.setApplicationContext(context);
    }

    @After
    public void tearDown() {
        verifyAll();
    }

    /*
     * @Test public void testInit() {
     * EasyMock.expect(context.getBean(SnapshotJobManager
     * .JOB_REPOSITORY_KEY)).andReturn(jobRepository);
     * EasyMock.expect(context.getBean
     * (SnapshotJobManager.JOB_LAUNCHER_KEY)).andReturn(jobLauncher);
     * replayAll();
     * 
     * //somehow the private fields (jobRepository and jobManager) are getting
     * //set by the test framework - so initialize can't be tested.
     * 
     * manager.init(config);
     * 
     * }
     */

    @Test
    public void testExecuteSnapshot() throws Exception {

        EasyMock.expect(jobBuilder.build(snapshot,
                                         config,
                                         snapshotJobListener,
                                         jobRepository,
                                         jobLauncher,
                                         transactionManager,
                                         taskExecutor)).andReturn(job);

        Capture<JobParameters> jobParams = new Capture<>();

        EasyMock.expect(jobLauncher.run(EasyMock.isA(Job.class),
                                        EasyMock.capture(jobParams)))
                .andReturn(jobExecution);

        EasyMock.expect(jobExecution.getStatus())
                .andReturn(BatchStatus.COMPLETED);
        setupSnapshotRepo();
        
        replayAll();

        
        manager.executeSnapshot(snapshotName);

        JobParameters jobParameters = jobParams.getValue();

        Assert.assertEquals(snapshotId,
                            jobParameters.getLong(SnapshotConstants.OBJECT_ID));

    }

    /**
     * 
     */
    private void setupSnapshotRepo() {
        EasyMock.expect(snapshotRepo.findByName(snapshotName)).andReturn(snapshot);
        EasyMock.expect(snapshot.getId()).andReturn(snapshotId);
    }

    
    @Test
    public void testGetSnapshotStatus() throws SnapshotNotFoundException, SnapshotException{
        EasyMock.expect(jobRepository.getLastJobExecution(EasyMock.isA(String.class),
                                                          EasyMock.isA(JobParameters.class)))
                .andReturn(jobExecution);
        
        EasyMock.expect(jobExecution.getStatus()).andReturn(BatchStatus.COMPLETED);
        
        setupSnapshotRepo();
        
        replayAll();
        Assert.assertNotNull(this.manager.getStatus(snapshotName));
    }

}
