/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.accumulo.maven.plugin;

import java.net.MalformedURLException;
import java.util.ArrayList;

import org.apache.accumulo.minicluster.MiniAccumuloConfig;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

public abstract class AbstractAccumuloMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject project;

  /**
   * Instructs this plugin to skip execution.
   *
   * @since 1.0.0
   */
  @Parameter(defaultValue = "false", alias = "skip", property = "accumulo.skip", required = false)
  private boolean skip;

  protected boolean shouldSkip() {
    if (skip) {
      getLog().info("Skipping execution of accumulo2-maven-plugin");
    }
    return skip;
  }

  void configureMiniClasspath(MiniAccumuloConfig macConfig) throws MalformedURLException {
    ArrayList<String> classpathItems = new ArrayList<>();
    if (project != null) {
      classpathItems.add(project.getBuild().getOutputDirectory());
      classpathItems.add(project.getBuild().getTestOutputDirectory());
      for (Artifact artifact : project.getArtifacts()) {
        classpathItems.add(artifact.getFile().toURI().toURL().toString());
      }
    }

    // Hack to prevent sisu-guava, a maven 3.0.4 dependency, from effecting normal accumulo
    // behavior.
    String sisuGuava = null;
    for (String items : classpathItems)
      if (items.contains("sisu-guava"))
        sisuGuava = items;

    if (sisuGuava != null)
      classpathItems.remove(sisuGuava);

    macConfig.setClasspath(classpathItems.toArray(new String[classpathItems.size()]));
  }
}
