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
package com.sapienter.jbilling.server.process.task;

import java.util.HashMap;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;

import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;

/**
 * @author Brian Cowdery
 * @since 04-02-2010
 */
public abstract class ScheduledTask extends PluggableTask implements IScheduledTask {

    /**
     * Constructs the JobDetail for this scheduled task, and copies the plug-in parameter
     * map into the detail JobDataMap for use when the task is executed by quartz.
     *
     * @return job detail
     * @throws com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException
     */
    public JobDetail getJobDetail() throws PluggableTaskException {
        JobDetail detail = new JobDetail(getTaskName() + " job", Scheduler.DEFAULT_GROUP, this.getClass());
        detail.getJobDataMap().put("entityId", getEntityId());
        detail.getJobDataMap().put("taskId", getTaskId());
        detail.getJobDataMap().putAll(parameters);
        return detail;
    }

    /**
     * Copies plug-in parameters from the JobDetail map into the plug-in's working
     * parameter map. This is a compatibility step so that we don't have to write
     * separate parameter handling code specifically for scheduled tasks.
     * 
     * @param context executing job context
     * @throws JobExecutionException thrown if an exception occurs while initializing parameters
     */
    protected void _init(JobExecutionContext context) throws JobExecutionException {
        JobDataMap map = context.getJobDetail().getJobDataMap();
        setEntityId(map.getInt("entityId"));

        parameters = new HashMap<String, String>();
        for (Object key : map.keySet())
            parameters.put((String) key, map.get(key).toString());
    }

    /**
     * Return this plug-ins schedule as a readable string. Can be used as part of
     * {@link IScheduledTask#getTaskName()} to make the task name unique to the schedule
     * allowing multiple plug-ins of the same type to be added with different schedules.
     *
     * @return schedule string
     */
    public abstract String getScheduleString();
}
