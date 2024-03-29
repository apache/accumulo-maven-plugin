/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.accumulo.plugin.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.accumulo.core.client.Accumulo;
import org.apache.accumulo.core.client.AccumuloClient;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.BatchWriterConfig;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.TableExistsException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.minicluster.MiniAccumuloCluster;
import org.apache.accumulo.plugin.CustomFilter;
import org.junit.BeforeClass;
import org.junit.Test;

public class PluginIT {

  private static Properties props;
  private static AccumuloClient client;

  @BeforeClass
  public static void setUp() throws Exception {
    String instanceName = "plugin-it-instance";
    props = MiniAccumuloCluster.getClientProperties(new File("target/accumulo2-maven-plugin/" + instanceName));
    client = Accumulo.newClient().from(props).build();
  }

  @Test
  public void testConnection() {
    assertTrue(props != null);
    assertTrue(client != null);
    assertTrue(client instanceof AccumuloClient);
  }

  @Test
  public void testCreateTable() throws AccumuloException, AccumuloSecurityException, TableExistsException, IOException {
    String tableName = "testCreateTable";
    client.tableOperations().create(tableName);
    assertTrue(client.tableOperations().exists(tableName));
    assertTrue(new File("target/accumulo2-maven-plugin/" + props.getProperty("instance.name") + "/testCreateTablePassed").createNewFile());
  }

  @Test
  public void writeToTable() throws AccumuloException, AccumuloSecurityException, TableExistsException, TableNotFoundException, IOException {
    String tableName = "writeToTable";
    client.tableOperations().create(tableName);
    BatchWriter bw = client.createBatchWriter(tableName, new BatchWriterConfig());
    Mutation m = new Mutation("ROW");
    m.put("CF", "CQ", "V");
    bw.addMutation(m);
    bw.close();
    Scanner scanner = client.createScanner(tableName, Authorizations.EMPTY);
    int count = 0;
    for (Entry<Key,Value> entry : scanner) {
      count++;
      assertEquals("ROW", entry.getKey().getRow().toString());
      assertEquals("CF", entry.getKey().getColumnFamily().toString());
      assertEquals("CQ", entry.getKey().getColumnQualifier().toString());
      assertEquals("V", entry.getValue().toString());
    }
    assertEquals(1, count);
    assertTrue(new File("target/accumulo2-maven-plugin/" + props.getProperty("instance.name") + "/testWriteToTablePassed").createNewFile());
  }

  @Test
  public void checkIterator() throws IOException, AccumuloException, AccumuloSecurityException, TableExistsException, TableNotFoundException {
    String tableName = "checkIterator";
    client.tableOperations().create(tableName);
    BatchWriter bw = client.createBatchWriter(tableName, new BatchWriterConfig());
    Mutation m = new Mutation("ROW1");
    m.put("allowed", "CQ1", "V1");
    m.put("denied", "CQ2", "V2");
    m.put("allowed", "CQ3", "V3");
    bw.addMutation(m);
    m = new Mutation("ROW2");
    m.put("allowed", "CQ1", "V1");
    m.put("denied", "CQ2", "V2");
    m.put("allowed", "CQ3", "V3");
    bw.addMutation(m);
    bw.close();

    // check filter
    Scanner scanner = client.createScanner(tableName, Authorizations.EMPTY);
    IteratorSetting is = new IteratorSetting(5, CustomFilter.class);
    scanner.addScanIterator(is);
    int count = 0;
    for (Entry<Key,Value> entry : scanner) {
      count++;
      assertEquals("allowed", entry.getKey().getColumnFamily().toString());
    }
    assertEquals(4, count);

    // check filter negated
    scanner.clearScanIterators();
    CustomFilter.setNegate(is, true);
    scanner.addScanIterator(is);
    count = 0;
    for (Entry<Key,Value> entry : scanner) {
      count++;
      assertEquals("denied", entry.getKey().getColumnFamily().toString());
    }
    assertEquals(2, count);
    assertTrue(new File("target/accumulo2-maven-plugin/" + props.getProperty("instance.name") + "/testCheckIteratorPassed").createNewFile());
  }

}
