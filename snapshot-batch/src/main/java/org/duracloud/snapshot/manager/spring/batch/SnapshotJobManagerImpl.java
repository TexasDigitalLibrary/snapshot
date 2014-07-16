/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.snapshot.manager.spring.batch;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.duracloud.snapshot.manager.SnapshotConstants;
import org.duracloud.snapshot.manager.SnapshotException;
import org.duracloud.snapshot.manager.SnapshotJobManager;
import org.duracloud.snapshot.manager.SnapshotNotFoundException;
import org.duracloud.snapshot.manager.SnapshotStatus;
import org.duracloud.snapshot.manager.SnapshotStatus.SnapshotStatusType;
import org.duracloud.snapshot.manager.SnapshotSummary;
import org.duracloud.snapshot.manager.config.SnapshotConfig;
import org.duracloud.snapshot.manager.config.SnapshotJobManagerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
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
    private JobExplorer jobExplorer;

    private PlatformTransactionManager transactionManager;
    private TaskExecutor taskExecutor;
    private ApplicationContext context;
    private ExecutorService executor;
    private SnapshotJobManagerConfig config;
    private SnapshotJobBuilder jobBuilder;
    
    @Autowired
    public SnapshotJobManagerImpl(
        JobExecutionListener jobListener,
        PlatformTransactionManager transactionManager, TaskExecutor taskExecutor) {
        super();
        this.jobListener = jobListener;
        this.transactionManager = transactionManager;
        this.taskExecutor = taskExecutor;
        this.executor = Executors.newFixedThreadPool(10);
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
        this.jobExplorer = (JobExplorer) context.getBean(JOB_EXPLORER_KEY);
        this.jobLauncher = (JobLauncher) context.getBean(JOB_LAUNCHER_KEY);
    }

    /**
     * 
     */
    private boolean isInitialized() {
        return this.jobLauncher != null;
    }

    /*
     * (non-Javadoc)
     * @see org.duracloud.snapshot.manager.SnapshotJobManager#executeSnapshotAsync(org.duracloud.snapshot.manager.config.SnapshotConfig)
     */
    @Override
    public Future<SnapshotStatus> executeSnapshotAsync(final SnapshotConfig config)
        throws SnapshotException {
        checkInitialized();

        
        final Job job = buildJob(config);

        FutureTask<SnapshotStatus> future = new FutureTask(new Callable() {
            public SnapshotStatus call() throws SnapshotException {
                    return executeJob(job, config);
            }
        });
        

        this.executor.execute(future);
        
        return future;
    }

    /**
     * @param snapshotConfig
     * @return
     * @throws SnapshotException
     */
    private SnapshotStatus executeJob(Job job, SnapshotConfig snapshotConfig)
        throws SnapshotException {

        String snapshotId = snapshotConfig.getSnapshotId();
        
        File contentDir = SnapshotUtils.resolveContentDir(snapshotConfig,this.config);
        JobParameters params =
            createJobParameters(snapshotId, contentDir.getAbsolutePath());
        try {
            JobExecution execution = jobLauncher.run(job, params);
            return createSnapshotStatus(snapshotId, execution);
        } catch (Exception e) {
            String message =
                "Error running job: " + snapshotId + ": " + e.getMessage();
            log.error(message, e);
            throw new SnapshotException(e.getMessage(), e);
        }
    }

    /**
     * @param snapshotConfig
     * @return
     * @throws SnapshotException
     */
    public Job buildJob(SnapshotConfig snapshotConfig) throws SnapshotException {
        if(jobBuilder == null) {
            jobBuilder = new SnapshotJobBuilder();
        }
        
        return jobBuilder.build(snapshotConfig,
                                config,
                                jobListener,
                                jobRepository,
                                jobLauncher,
                                transactionManager,
                                taskExecutor);
    }

 

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.duracloud.snapshot.spring.batch.SnapshotJobManager#executeSnapshotSync
     * (org.duracloud.snapshot.spring.batch.config.SnapshotConfig)
     */
    @Override
    public SnapshotStatus executeSnapshot(SnapshotConfig config)
        throws SnapshotException {
        checkInitialized();
        Job job = buildJob(config);
        return executeJob(job,config);
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
    public SnapshotStatus getStatus(String snapshotId)
        throws SnapshotNotFoundException,
            SnapshotException {

        checkInitialized();

        String contentDir = getSnapshotContentDir(snapshotId);
        JobParameters params = createJobParameters(snapshotId, contentDir);
        JobExecution ex =
            this.jobRepository.getLastJobExecution(SnapshotConstants.SNAPSHOT_JOB_NAME, params);
        if (ex != null) {
            return createSnapshotStatus(snapshotId, ex);
        }

        throw new SnapshotNotFoundException("Snapshot ["
            + snapshotId + "] not found.");

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
    
    /* (non-Javadoc)
     * @see org.duracloud.snapshot.spring.batch.SnapshotJobManager#getSnapshotList()
     */
    @Override
    public List<SnapshotSummary> getSnapshotList() throws SnapshotException {
        checkInitialized();
        //FIXME - make sure you're pulling all the job instances, not just the first 1000.
        List<JobInstance> results = this.jobExplorer.getJobInstances(SnapshotConstants.SNAPSHOT_JOB_NAME, 0, 1000);
        List<SnapshotSummary> list = new ArrayList<>(results.size());
        for(JobInstance job : results){
            List<JobExecution> executions = this.jobExplorer.getJobExecutions(job);
            if(executions.size() > 0) {
                JobExecution execution =  executions.get(0);
                JobParameters params = execution.getJobParameters();
                String snapshotId = params.getString(SnapshotConstants.SNAPSHOT_ID);
                list.add(new SnapshotSummary(snapshotId));
                continue;
            }
        }
        
        return list;
    }


    /**
     * @param snapshotId
     * @param contentDir 
     * @return
     */
    private JobParameters createJobParameters(String snapshotId, String contentDir) {
        Map<String, JobParameter> map = new HashMap<>();
        map.put(SnapshotConstants.SNAPSHOT_ID, new JobParameter(snapshotId, true));
        map.put(SnapshotConstants.CONTENT_DIR, new JobParameter(contentDir));
        JobParameters params = new JobParameters(map);
        return params;
    }

    /**
     * @param snapshotId
     * @param ex
     * @return
     */
    private SnapshotStatus createSnapshotStatus(String snapshotId,
                                                JobExecution ex) {
        return new SnapshotStatus(snapshotId, SnapshotStatusType.valueOf(ex.getStatus().name()));
    }
    
}
