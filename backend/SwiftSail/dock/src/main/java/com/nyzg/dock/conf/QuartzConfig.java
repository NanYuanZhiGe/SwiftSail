package com.nyzg.dock.conf;

import org.quartz.Scheduler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

import javax.sql.DataSource;

@Configuration
public class QuartzConfig {
    @Bean
    public SpringBeanJobFactory springBeanJobFactory() {
        return new QuartzSpringBeanFactory();
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(
            DataSource dataSource,
            ApplicationContext applicationContext
    ) {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setOverwriteExistingJobs(true);
        factory.setJobFactory(springBeanJobFactory());
        factory.setApplicationContext(applicationContext);
        factory.setApplicationContextSchedulerContextKey("applicationContext");
        factory.setWaitForJobsToCompleteOnShutdown(true);
        factory.setAutoStartup(true);
        return factory;
    }
}
