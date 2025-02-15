/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.cluster;

/**
 * A basic implementation for batch executors that simply need to execute the tasks in the batch iteratively,
 * producing a cluster state after each task. This allows executing the tasks in the batch as a
 * series of executions, each taking an input cluster state and producing a new cluster state that serves as the
 * input of the next task in the batch.
 */
public abstract class SimpleBatchedExecutor<T extends ClusterStateTaskListener> implements ClusterStateTaskExecutor<T> {

    /**
     * Executes the provided task from the batch.
     *
     * @param task The task to be executed.
     * @param clusterState    The cluster state on which the task should be executed.
     * @return The resulting cluster state after executing this task.
     */
    public abstract ClusterState executeTask(T task, ClusterState clusterState) throws Exception;

    /**
     * Called once all tasks in the batch have finished execution. It should return a cluster state that reflects
     * the execution of all the tasks.
     *
     * @param clusterState The cluster state resulting from the execution of all the tasks.
     * @param clusterStateChanged Whether {@code clusterState} is different from the cluster state before executing the tasks in the batch.
     * @return The resulting cluster state after executing all the tasks.
     */
    public ClusterState afterBatchExecution(ClusterState clusterState, boolean clusterStateChanged) {
        return clusterState;
    }

    /**
     * Called if executing a task in the batch finished successfully, and before the execution of the next
     * task in the batch.
     *
     * @param task The task that successfully finished execution.
     */
    public abstract void taskSucceeded(T task);

    @Override
    public final void clusterStatePublished(ClusterState newClusterState) {
        clusterStatePublished();
    }

    /**
     * Called after the new cluster state is published. Note that this method is not invoked if the cluster state was not updated.
     */
    public void clusterStatePublished() {}

    @Override
    public final ClusterState execute(BatchExecutionContext<T> batchExecutionContext) throws Exception {
        var initState = batchExecutionContext.initialState();
        var clusterState = initState;
        for (final var taskContext : batchExecutionContext.taskContexts()) {
            try (var ignored = taskContext.captureResponseHeaders()) {
                var task = taskContext.getTask();
                clusterState = executeTask(task, clusterState);
                taskContext.success(() -> taskSucceeded(task));
            } catch (Exception e) {
                taskContext.onFailure(e);
            }
        }
        try (var ignored = batchExecutionContext.dropHeadersContext()) {
            return afterBatchExecution(clusterState, clusterState != initState);
        }
    }
}
