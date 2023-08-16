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
package org.apache.accumulo.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.accumulo.minicluster.MiniAccumuloCluster;
import org.apache.accumulo.minicluster.MiniAccumuloConfig;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Goal which starts an instance of {@link MiniAccumuloCluster}.
 */
@Mojo(name = "start", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
    requiresDependencyResolution = ResolutionScope.TEST)
public class StartMojo extends AbstractAccumuloMojo {

  /**
   * Specifies the output directory in which this plugin will create files for its runtime use. This
   * plugin will create a client properties file inside a subdirectory named after this plugin and
   * the specified instance name. This property defaults to your Maven target directory.
   *
   * <p>
   * For example: <code>${outputDirectory}/accumulo2-maven-plugin/instanceName</code>
   *
   * <p>
   * The client properties file can be used to construct an Accumulo client in your test code like:
   *
   * <code>
   * <pre>
   * String instanceName = "plugin-it-instance";
   * String outputDir = "target";
   * File propsFile = new File(outputDir + "/accumulo2-maven-plugin/" + instanceName);
   * Properties props = MiniAccumuloCluster.getClientProperties(propsFile);
   * AccumuloClient client = Accumulo.newClient().from(props).build();
   * </pre>
   * </code>
   *
   * @since 1.0.0
   */
  @Parameter(defaultValue = "${project.build.directory}", alias = "outputDirectory",
      property = "accumulo.outputDirectory", required = true)
  private File outputDirectory;

  /**
   * Specifies the instance name the Accumulo instance will use for itself, which clients use to
   * connect. It is also used to create the working directory for MiniAccumuloCluster, which will
   * contain the client properties file which clients can use to connect. See
   * <a href="#outputDirectory">{@link #outputDirectory}</a>.
   *
   * @since 1.0.0
   */
  @Parameter(defaultValue = "testInstance", alias = "instanceName",
      property = "accumulo.instanceName", required = true)
  private String instanceName;

  /**
   * Specifies the root user's initial password for clients to connect and perform additional
   * operations.
   *
   * @since 1.0.0
   */
  @Parameter(defaultValue = "secret", alias = "rootPassword", property = "accumulo.rootPassword",
      required = false)
  private String rootPassword;

  /**
   * Specifies the client port on which ZooKeeper listens. If not specified, MiniAccumuloCluster
   * will select an available port on its own.
   *
   * @since 1.0.0
   */
  @Parameter(defaultValue = "0", alias = "zooKeeperPort", property = "accumulo.zooKeeperPort",
      required = false)
  private int zooKeeperPort;

  static Set<MiniAccumuloCluster> runningClusters = Collections.synchronizedSet(new HashSet<>());

  @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN",
      justification = "could restrict outputDirectory to target/ in future")
  @Override
  public void execute() throws MojoExecutionException {
    if (shouldSkip()) {
      return;
    }

    if (!instanceName.matches("^[a-zA-Z0-9_-]+$")) {
      throw new MojoExecutionException("instanceName must be only letters and numbers");
    }

    File subdir = new File(new File(outputDirectory, "accumulo2-maven-plugin"), instanceName);
    try {
      subdir = subdir.getCanonicalFile();
      if (subdir.exists()) {
        FileUtils.forceDelete(subdir);
      }
      if (!subdir.mkdirs() && !subdir.isDirectory()) {
        throw new MojoExecutionException(subdir + " cannot be created as a directory");
      }
      MiniAccumuloConfig cfg = new MiniAccumuloConfig(subdir, rootPassword);
      cfg.setInstanceName(instanceName);
      cfg.setZooKeeperPort(zooKeeperPort);
      configureMiniClasspath(cfg);
      MiniAccumuloCluster mac = new MiniAccumuloCluster(cfg);
      getLog().info("Starting MiniAccumuloCluster: " + mac.getInstanceName() + " in "
          + mac.getConfig().getDir());
      mac.start();
      runningClusters.add(mac);
    } catch (IOException | InterruptedException e) {
      throw new MojoExecutionException(
          "Unable to start " + MiniAccumuloCluster.class.getSimpleName(), e);
    }
  }
}
