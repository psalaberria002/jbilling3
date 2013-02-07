/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.util;

import com.sapienter.jbilling.client.process.JobScheduler;
import com.sapienter.jbilling.client.process.Trigger;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.process.task.IScheduledTask;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import org.apache.log4j.Logger;
import org.quartz.SchedulerException;

/**
 * Spring bean that bootstraps jBilling services on application start.
 *
 * @author Brian Cowdery
 * @since 22-09-2010
 */
public class Bootstrap {
    private static final Logger LOG = Logger.getLogger(Bootstrap.class);

    public void init() {
        scheduleBatchJobs();
        schedulePluggableTasks();
                                          
        LOG.debug("Starting the job scheduler");
        JobScheduler.getInstance().start();
    }

    public void destroy() {
        LOG.debug("Shutting down the job scheduler");
        JobScheduler.getInstance().shutdown();
    }

    /**
     * Schedule all core jBilling batch processes.
     */
    private void scheduleBatchJobs() {
        // todo: refactor "Trigger" into separate scheduled Job classes.
        Trigger.Initialize();
    }

    /**
     * Schedule all configured {@link IScheduledTask} plug-ins for each entity.
     */
    private void schedulePluggableTasks() {
        JobScheduler scheduler = JobScheduler.getInstance();
        try {
            for (CompanyDTO entity : new CompanyDAS().findEntities()) {
                PluggableTaskManager<IScheduledTask> manager =
                        new PluggableTaskManager<IScheduledTask>
                                (entity.getId(), com.sapienter.jbilling.server.util.Constants.PLUGGABLE_TASK_SCHEDULED);

                LOG.debug("Processing " + manager.getAllTasks().size() + " scheduled tasks for entity " + entity.getId());
                for (IScheduledTask task = manager.getNextClass(); task != null; task = manager.getNextClass()) {
                    try {
                        scheduler.getScheduler().scheduleJob(task.getJobDetail(), task.getTrigger());
                        LOG.debug("Scheduled: [" + task.getTaskName() + "]");
                    } catch (PluggableTaskException e) {
                        LOG.warn("Failed to schedule pluggable task [" + task.getTaskName() + "]");
                    } catch (SchedulerException e) {
                        LOG.warn("Failed to schedule pluggable task [" + task.getTaskName() + "]");
                    }                    
                }
            }
        } catch (PluggableTaskException e) {
            LOG.error("Exception occurred scheduling pluggable tasks.", e);
        }
    }
}
