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

package org.apache.hadoop.hoya.yarn.cluster

import groovy.transform.CompileStatic
import groovy.util.logging.Commons
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hoya.yarn.CommonArgs
import org.apache.hadoop.hoya.yarn.KeysForTests
import org.apache.hadoop.hoya.yarn.MicroZKCluster
import org.apache.hadoop.hoya.yarn.client.ClientArgs
import org.apache.hadoop.hoya.yarn.client.HoyaClient
import org.apache.hadoop.yarn.conf.YarnConfiguration
import org.apache.hadoop.yarn.server.MiniYARNCluster
import org.apache.hadoop.yarn.server.resourcemanager.ResourceManager
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.ResourceScheduler
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.fifo.FifoScheduler
import org.apache.hadoop.yarn.service.ServiceOperations
import org.apache.hadoop.yarn.service.launcher.ServiceLauncher
import org.apache.hadoop.yarn.service.launcher.ServiceLauncherBaseTest
import org.junit.After
import org.junit.Before

/**
 * Base class for mini cluster tests -creates a field for the
 * mini yarn cluster
 */
@Commons
@CompileStatic

public class YarnMiniClusterTestBase extends ServiceLauncherBaseTest
implements KeysForTests {

  /**
   * Mini YARN cluster only
   */
  protected MiniYARNCluster miniCluster;
  protected MicroZKCluster microZKCluster

  @Before
  public void setup() {

  }

  @After
  public void teardown() {
    ServiceOperations.stopQuietly(log, miniCluster)
    microZKCluster?.close();
  }

  /**
   * Create and start a minicluster
   * @param name cluster/test name
   * @param conf configuration to use
   * @param noOfNodeManagers #of NMs
   * @param numLocalDirs #of local dirs
   * @param numLogDirs #of log dirs
   */
  protected void createMiniCluster(String name,
                               YarnConfiguration conf,
                               int noOfNodeManagers,
                               int numLocalDirs,
                               int numLogDirs,
                               boolean startZK) {
    conf.setInt(YarnConfiguration.RM_SCHEDULER_MINIMUM_ALLOCATION_MB, 64);
    conf.setClass(YarnConfiguration.RM_SCHEDULER,
                  FifoScheduler.class, ResourceScheduler.class);
    miniCluster = new MiniYARNCluster(name, noOfNodeManagers, numLocalDirs, numLogDirs)
    miniCluster.init(conf)
    miniCluster.start();
    //now the ZK cluster
    if (startZK) {
      microZKCluster = new MicroZKCluster(new Configuration(conf))
      microZKCluster.createCluster();
    }

  }

  /**
   * Create and start a minicluster
   * @param name cluster/test name
   * @param conf configuration to use
   * @param noOfNodeManagers #of NMs
   * @param numLocalDirs #of local dirs
   * @param numLogDirs #of log dirs
   */
  protected void createMiniCluster(String name, YarnConfiguration conf, int noOfNodeManagers, boolean startZK) {
    createMiniCluster(name, conf, noOfNodeManagers, 1, 1, startZK)
  }

  /**
   * Launch the hoya client with the specific args
   * @param conf configuration
   * @param args arg list
   * @return the service launcher that launched it, containing exit codes
   * and the service itself
   */
  protected ServiceLauncher launchHoyaClient(Configuration conf, List<String> args) {
    return launch(HoyaClient, conf, args);
  }

  /**
   * Launch the hoya client with the specific args
   * @param conf configuration
   * @param args arg list
   * @return the return code
   */
  protected ServiceLauncher launchHoyaClientAgainstMiniMR(Configuration conf,
                                                          List<String> args) {
    ResourceManager rm = miniCluster.resourceManager
    log.info("Connecting to rm at ${rm}")

    return launch(HoyaClient, conf, args);
  }


  public String getHBaseHome() {
    YarnConfiguration conf = getTestConfiguration()
    String hbaseHome = conf.getTrimmed(HOYA_TEST_HBASE_HOME)
    return hbaseHome
  }

  public YarnConfiguration getTestConfiguration() {
    YarnConfiguration conf = new YarnConfiguration()

    conf.addResource(HOYA_TEST)
    return conf
  }

  protected String getRMAddr() {
    assert miniCluster != null
    String addr = miniCluster.config.get(YarnConfiguration.RM_ADDRESS)
    assert addr != null;
    assert addr != "";
    return addr
  }

  protected String getZKBinding() {
    if (!microZKCluster) {
      return "localhost:1"
    } else {
      return microZKCluster.zkBindingString
    }
  }

  /**
   * Create an AM without a master
   * @param name AM name
   * @param size # of nodes
   * @return launcher which will have executed the command.
   */
  public ServiceLauncher createMasterlessAM(String name, int size) {
    assert name != null
    assert miniCluster != null
    assert microZKCluster != null
    List<String> args = [
        ClientArgs.ACTION_CREATE, name,
        CommonArgs.ARG_MIN, Integer.toString(size),
        CommonArgs.ARG_MAX, Integer.toString(size),
        ClientArgs.ARG_MANAGER, RMAddr,
        CommonArgs.ARG_USER, USERNAME,
        CommonArgs.ARG_HBASE_HOME, HBaseHome,
        CommonArgs.ARG_ZOOKEEPER, ZKBinding,
        CommonArgs.ARG_HBASE_ZKPATH, "/test/" + name,
        CommonArgs.ARG_X_TEST,
        CommonArgs.ARG_X_NO_MASTER
    ]
    ServiceLauncher launcher = launchHoyaClientAgainstMiniMR(
        new YarnConfiguration(miniCluster.config),
        args
    )
    assert launcher.serviceExitCode == 0
    return launcher
  }
}
