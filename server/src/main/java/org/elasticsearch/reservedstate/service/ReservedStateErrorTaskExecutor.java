/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.reservedstate.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.SimpleBatchedExecutor;

/**
 * Reserved cluster error state task executor
 * <p>
 * We use this task executor to record any errors while updating and reserving the cluster state
 */
class ReservedStateErrorTaskExecutor extends SimpleBatchedExecutor<ReservedStateErrorTask> {

    private static final Logger logger = LogManager.getLogger(ReservedStateErrorTaskExecutor.class);

    @Override
    public ClusterState executeTask(ReservedStateErrorTask task, ClusterState clusterState) {
        if (task.shouldUpdate(clusterState)) {
            return task.execute(clusterState);
        }
        return clusterState;
    }

    @Override
    public void taskSucceeded(ReservedStateErrorTask task) {
        task.listener().onResponse(ActionResponse.Empty.INSTANCE);
    }

    @Override
    public void clusterStatePublished() {
        logger.debug("Wrote new error state in reserved cluster state metadata");
    }
}
