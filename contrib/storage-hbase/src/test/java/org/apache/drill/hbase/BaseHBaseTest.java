/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.hbase;

import java.io.IOException;
import java.util.List;

import org.apache.drill.BaseTestQuery;
import org.apache.drill.common.util.FileUtils;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.exec.store.hbase.HBaseStoragePlugin;
import org.apache.drill.exec.store.hbase.HBaseStoragePluginConfig;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class BaseHBaseTest extends BaseTestQuery {

  public static final String HBASE_STORAGE_PLUGIN_NAME = "hbase";

  protected static Configuration conf = HBaseConfiguration.create();

  protected static HBaseStoragePlugin storagePlugin;

  protected static HBaseStoragePluginConfig storagePluginConfig;

  @BeforeClass
  public static void setupDefaultTestCluster() throws Exception {
    /*
     * Change the following to HBaseTestsSuite.configure(false, true)
     * if you want to test against an externally running HBase cluster.
     */
    HBaseTestsSuite.configure(true /*manageHBaseCluster*/, true /*createTables*/);
    HBaseTestsSuite.initCluster();

    BaseTestQuery.setupDefaultTestCluster();

    final StoragePluginRegistry pluginRegistry = getDrillbitContext().getStorage();
    storagePlugin = (HBaseStoragePlugin) pluginRegistry.getPlugin(HBASE_STORAGE_PLUGIN_NAME);
    storagePluginConfig = storagePlugin.getConfig();
    storagePluginConfig.setEnabled(true);
    storagePluginConfig.setZookeeperPort(HBaseTestsSuite.getZookeeperPort());
    pluginRegistry.createOrUpdate(HBASE_STORAGE_PLUGIN_NAME, storagePluginConfig, true);
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    HBaseTestsSuite.tearDownCluster();
  }

  protected String getPlanText(String planFile, String tableName) throws IOException {
    return Files.toString(FileUtils.getResourceAsFile(planFile), Charsets.UTF_8)
        .replaceFirst("\"hbase\\.zookeeper\\.property\\.clientPort\".*:.*\\d+", "\"hbase.zookeeper.property.clientPort\" : " + HBaseTestsSuite.getZookeeperPort())
        .replace("[TABLE_NAME]", tableName);
  }

  protected void runHBasePhysicalVerifyCount(String planFile, String tableName, int expectedRowCount) throws Exception{
    String physicalPlan = getPlanText(planFile, tableName);
    List<QueryDataBatch> results = testPhysicalWithResults(physicalPlan);
    printResultAndVerifyRowCount(results, expectedRowCount);
  }

  protected List<QueryDataBatch> runHBaseSQLlWithResults(String sql) throws Exception {
    sql = canonizeHBaseSQL(sql);
    System.out.println("Running query:\n" + sql);
    return testSqlWithResults(sql);
  }

  protected void runHBaseSQLVerifyCount(String sql, int expectedRowCount) throws Exception{
    List<QueryDataBatch> results = runHBaseSQLlWithResults(sql);
    printResultAndVerifyRowCount(results, expectedRowCount);
  }

  private void printResultAndVerifyRowCount(List<QueryDataBatch> results, int expectedRowCount) throws SchemaChangeException {
    int rowCount = printResult(results);
    if (expectedRowCount != -1) {
      Assert.assertEquals(expectedRowCount, rowCount);
    }
  }

  protected String canonizeHBaseSQL(String sql) {
    return sql.replace("[TABLE_NAME]", HBaseTestsSuite.TEST_TABLE_1.getNameAsString());
  }

}
