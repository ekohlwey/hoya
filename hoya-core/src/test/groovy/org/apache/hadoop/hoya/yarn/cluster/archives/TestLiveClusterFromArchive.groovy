/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */



package org.apache.hadoop.hoya.yarn.cluster.archives

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.hadoop.hbase.ClusterStatus
import org.apache.hadoop.hoya.yarn.client.HoyaClient
import org.apache.hadoop.hoya.yarn.providers.hbase.HBaseMiniClusterTestBase
import org.apache.hadoop.yarn.service.launcher.ServiceLauncher
import org.junit.Test

/**
 * create a live cluster from the image
 */
@CompileStatic
@Slf4j
class TestLiveClusterFromArchive extends HBaseMiniClusterTestBase {

  @Test
  public void testLiveClusterFromArchive() throws Throwable {
    String clustername = getTestClusterName()
    int regionServerCount = 1
    createMiniCluster(clustername, createConfiguration(), regionServerCount + 1, 1, 1, true,
                      startHDFS())

    //now launch the cluster
    setupImageToDeploy()
    ServiceLauncher launcher = createHBaseCluster(clustername, regionServerCount, [], true, true)

    HoyaClient hoyaClient = (HoyaClient) launcher.service
    ClusterStatus clustat = basicHBaseClusterStartupSequence(hoyaClient)

    //get the hbase status
    waitForHBaseRegionServerCount(hoyaClient, clustername, regionServerCount, HBASE_CLUSTER_STARTUP_TO_LIVE_TIME)
    waitForHoyaWorkerCount(hoyaClient, regionServerCount, HBASE_CLUSTER_STARTUP_TO_LIVE_TIME)

    clusterActionFreeze(hoyaClient, clustername,"end of run")
  }

  public void setupImageToDeploy() {
    switchToImageDeploy = true
  }

  public String getTestClusterName() {
    return "TestLiveClusterFromArchive"
  }

  public boolean startHDFS() {
    return false
  }

}
