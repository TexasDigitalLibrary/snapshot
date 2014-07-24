/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.snapshot.manager.spring.batch;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.duracloud.snapshot.db.model.Restoration;
import org.duracloud.snapshot.db.model.Snapshot;
import org.duracloud.snapshot.db.repo.RestorationRepo;
import org.duracloud.snapshot.db.repo.SnapshotRepo;
import org.duracloud.snapshot.manager.SnapshotConstants;
import org.duracloud.snapshot.manager.SnapshotException;
import org.duracloud.snapshot.manager.SnapshotJobManager;
import org.duracloud.snapshot.manager.SnapshotNotFoundException;
import org.duracloud.snapshot.manager.config.SnapshotJobManagerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The default implementation of the <code>SnapshotJobManager</code> interface.
 * Uses spring-batch componentry.
 * 
 * @author Daniel Bernstein Date: Feb 11, 2014
 */

public class SnapshotJobManagerImpl
    implements SnapshotJobManager, ApplicationContextAware {

    private static final Logger log =
        LoggerFactory.getLogger(SnapshotJobManagerImpl.class);
    private JobExecutionListener jobListener;
    private JobLauncher jobLauncher;
    private JobRepository jobRepository;

    private PlatformTransactionManager transactionManager;
    private TaskExecutor taskExecutor;
    private ApplicationContext context;
    private ExecutorService executor;
    private SnapshotRepo snapshotRepo;
    private RestorationRepo restorationRepo;
    private SnapshotJobManagerConfig config;
    private SnapshotJobBuilder snapshotJobBuilder;
    private RestorationJobBuilder restorationJobBuilder;
    
    @Autowired
    public SnapshotJobManagerImpl(JobExecutionListener jobListener,
                                    PlatformTransactionManager transactionManager,
                                    TaskExecutor taskExecutor, 
                                    SnapshotRepo snapshotRepo,
                                    RestorationRepo restorationRepo) {
        super();
        this.jobListener = jobListener;
        this.transactionManager = transactionManager;
        this.taskExecutor = taskExecutor;
        this.executor = Executors.newFixedThreadPool(10);
        this.snapshotJobBuilder = new SnapshotJobBuilder();
        this.restorationJobBuilder = new RestorationJobBuilder();
        this.restorationRepo = restorationRepo;
        this.snapshotRepo = snapshotRepo;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.context.ApplicationContextAware#setApplicationContext
     * (org.springframework.context.ApplicationContext)
     */
    @Override
    public void setApplicationContext(ApplicationContext context)
        throws BeansException {
        this.context = context;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.duracloud.snapshot.rest.SnapshotJobManager#initialize(org.duracloud
     * .snapshot.rest.InitParams)
     */
    @Override
    public void init(SnapshotJobManagerConfig config) {
        
        if (isInitialized()) {
            log.warn("Already initialized. Ignorning");
            return;
        }

        this.config = config;
        
        this.jobRepository = (JobRepository) context.getBean(JOB_REPOSITORY_KEY);
        this.jobLauncher = (JobLauncher) context.getBean(JOB_LAUNCHER_KEY);
    }

    /**
     * 
     */
    private boolean isInitialized() {
        return this.jobLauncher != null;
    }


    private Snapshot getSnapshot(String snapshotId) throws SnapshotNotFoundException{
        Snapshot snapshot = this.snapshotRepo.findByName(snapshotId);
        if(snapshot == null){
            throw new SnapshotNotFoundException(snapshotId);
        }
        
        return snapshot;
    }

     private BatchStatus executeJob(JobRequest jobRequest)
        throws SnapshotException {
         Job job = jobRequest.getJob();
         JobParameters params = jobRequest.getJobParameters();    
         try {
            JobExecution execution = jobLauncher.run(job, params);
            return execution.getStatus();
        } catch (Exception e) {
            String message =
                "Error running job: " + job.getName() + ", params=" + params + ": " + e.getMessage();
            log.error(message, e);
            throw new SnapshotException(e.getMessage(), e);
        }
    }
    

    /* (non-Javadoc)
     * @see org.duracloud.snapshot.manager.SnapshotJobManager#executeRestoration(java.lang.Long)
     */
    @Override
    public BatchStatus executeRestoration(Long restorationId)
        throws SnapshotException {
        return executeJob(buildJob(getRestoration(restorationId)));
    }
    

    /**
     * @param restorationId
     * @return
     */
    private Snapshot getRestoration(Long restorationId)  throws RestorationNotFoundException {
        Restoration restoration = this.restorationRepo.getOne(restorationId);
        if(restoration == null){
            throw new RestorationNotFoundException(restorationId);
        }
        return null;
    }

    /**
     * @param snapshot
     * @return
     * @throws SnapshotException
     */
    private JobRequest buildJob(Snapshot snapshot) throws SnapshotException {
             Job job= snapshotJobBuilder.build(snapshot,
                                    config,
                                    jobListener,
                                    jobRepository,
                                    jobLauncher,
                                    transactionManager,
                                    taskExecutor);

             
             return new JobRequest(job, createJobParameters(snapshot.getId()));
    }

    /**
     * @param snapshot
     * @return
     * @throws SnapshotException
     */
    private JobRequest buildJob(Restoration restoration) throws SnapshotException {
        
        Job job =  restorationJobBuilder.build(restoration,
                                    config,
                                    jobListener,
                                    jobRepository,
                                    jobLauncher,
                                    transactionManager,
                                    taskExecutor);
        return new JobRequest(job, createJobParameters(restoration.getId()));

    }
 
    /* (non-Javadoc)
     * @see org.duracloud.snapshot.manager.SnapshotJobManager#executeSnapshot(java.lang.String)
     */
    @Override
    public BatchStatus executeSnapshot(String snapshotId)
        throws SnapshotException {
        checkInitialized();
        JobRequest job = buildJob(getSnapshot(snapshotId));
        return executeJob(job);
    }
 
    /**
     * 
     */
    private void checkInitialized() throws SnapshotException {
        if (!isInitialized()) {
            throw new SnapshotException("The application must be initialized "
                + "before it can be invoked!", null);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.duracloud.snapshot.rest.SnapshotJobManager#getJobExecution(java.lang
     * .String)
     */
    @Override
    public BatchStatus getStatus(String snapshotId)
        throws SnapshotNotFoundException,
            SnapshotException {

        checkInitialized();
        Snapshot snapshot = getSnapshot(snapshotId);
        JobParameters params = new JobParameters(createIdentifyingJobParameters(snapshot.getId()));
        JobExecution ex =
            this.jobRepository.getLastJobExecution(SnapshotConstants.SNAPSHOT_JOB_NAME, params);
        if (ex == null) {
            return BatchStatus.UNKNOWN;
        }else{
            return ex.getStatus();
        }
    }

    /**
     * @param snapshotId
     * @return
     */
    private String getSnapshotContentDir(String snapshotId) {
        String contentDir =
            config.getContentRootDir()
                + File.separator + "snapshots" + File.separator + snapshotId;
        return contentDir;
    }
    
    /**
     * @param snapshotId
     * @param contentDir 
     * @return
     */
    private JobParameters createJobParameters(Long id) {
        Map<String, JobParameter> map = createIdentifyingJobParameters(id);
        JobParameters params = new JobParameters(map);
        return params;
    }

    private Map<String,JobParameter> createIdentifyingJobParameters(Long id) {
        Map<String, JobParameter> map = new HashMap<>();
        map.put(SnapshotConstants.OBJECT_ID, new JobParameter(id, true));
        return map;
    }

    
}
