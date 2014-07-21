/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.snapshot.manager.spring.batch;

import java.io.File;

import org.duracloud.client.ContentStore;
import org.duracloud.retrieval.util.StoreClientUtil;
import org.duracloud.snapshot.manager.SnapshotConstants;
import org.duracloud.snapshot.manager.SnapshotException;
import org.duracloud.snapshot.manager.config.SnapshotConfig;
import org.duracloud.snapshot.manager.config.SnapshotJobManagerConfig;
import org.duracloud.sync.endpoint.DuraStoreSyncEndpoint;
import org.duracloud.sync.endpoint.SyncEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.builder.SimpleJobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.factory.SimpleStepFactoryBean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Daniel Bernstein 
 *         Date: Feb 19, 2014
 */
public class RestorationJobBuilder {
    private static Logger log = LoggerFactory.getLogger(RestorationJobBuilder.class);

    public Job build(SnapshotConfig restoreConfig,
                     SnapshotJobManagerConfig jobManagerConfig,
                     JobExecutionListener jobListener,
                     JobRepository jobRepository,
                     JobLauncher jobLauncher,
                     PlatformTransactionManager transactionManager,
                     TaskExecutor taskExecutor) throws SnapshotException {
        Job job;
        try {
            StoreClientUtil clientUtil = new StoreClientUtil();

            ContentStore contentStore =
                clientUtil.createContentStore(restoreConfig.getHost(),
                                              restoreConfig.getPort(),
                                              restoreConfig.getContext(),
                                              jobManagerConfig.getDuracloudUsername(),
                                              jobManagerConfig.getDuracloudPassword(),
                                              restoreConfig.getStoreId());

            SyncEndpoint endpoint =
                new DuraStoreSyncEndpoint(contentStore,
                                          jobManagerConfig.getDuracloudUsername(),
                                          restoreConfig.getSpaceId(),
                                          false);
            
            File watchDir = restoreConfig.getContentDir();

            FileSystemReader reader =
                new FileSystemReader(watchDir);

            SyncWriter writer =
                new SyncWriter(watchDir,
                               endpoint,
                               contentStore,
                               restoreConfig.getSpaceId());

            SimpleStepFactoryBean<File, File> stepFactory =
                new SimpleStepFactoryBean<>();
            stepFactory.setJobRepository(jobRepository);
            stepFactory.setTransactionManager(transactionManager);
            stepFactory.setBeanName("step1");
            stepFactory.setItemReader(reader);
            stepFactory.setItemWriter(writer);
            stepFactory.setCommitInterval(1);
            stepFactory.setThrottleLimit(20);
            stepFactory.setTaskExecutor(taskExecutor);
            Step step = (Step) stepFactory.getObject();

            JobBuilderFactory jobBuilderFactory =
                new JobBuilderFactory(jobRepository);
            JobBuilder jobBuilder = jobBuilderFactory.get(SnapshotConstants.RESTORE_JOB_NAME);
            SimpleJobBuilder simpleJobBuilder = jobBuilder.start(step);
            simpleJobBuilder.listener(jobListener);

            job = simpleJobBuilder.build();

        } catch (Exception e) {
            log.error("Error creating job: {}", e.getMessage(), e);
            throw new SnapshotException(e.getMessage(), e);
        }
        return job;
    }
}
