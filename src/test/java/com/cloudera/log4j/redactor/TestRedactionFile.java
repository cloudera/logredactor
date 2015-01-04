/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudera.log4j.redactor;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.Properties;

/**
 * Tests the usage of a file for rules redaction purposes, instead of the
 * rules being inside the properties file.
 */
public class TestRedactionFile {
  private Properties defaults = new Properties();
  private final String pre = "log4j.appender.";
  private String resourcePath = "/";

  @Before
  public void setDefaults() throws Exception {
    final String pkg = "com.cloudera.log4j.redactor.";
    defaults.setProperty(pre + "LOG", "org.apache.log4j.ConsoleAppender");
    defaults.setProperty(pre + "LOG.Target", "System.err");
    defaults.setProperty(pre + "LOG.layout", "org.apache.log4j.PatternLayout");
    defaults.setProperty(pre + "LOG.layout.ConversionPattern", "%m");
    defaults.setProperty(pre + "redactor", pkg + "RedactorAppender");
    defaults.setProperty(pre + "redactor.appenderRefs", "LOG");
    defaults.setProperty(pre + "redactor.policy", pkg + "RedactorPolicy");
    defaults.setProperty("log4j.rootLogger", "ALL, LOG, redactor");

    URL resourceUrl = getClass().getResource("/test1.redaction");
    File resourceFile = new File(resourceUrl.toURI());
    resourcePath = resourceFile.getParent();
  }

  @After
  public void cleanUp() {
    LogManager.resetConfiguration();
  }

  @Test
  public void testpositiveLoad() {
    Properties logProps = new Properties(defaults);
    logProps.setProperty(pre + "redactor.policy.rules",
            resourcePath + "/test1.redaction");
    PropertyConfigurator.configure(logProps);
    Logger log = Logger.getLogger(TestRedactionFile.class);
    log.info("WHERE x=123-45-6789");
    log.info("blah blah ABC Blah");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadLine() {
    Properties logProps = new Properties(defaults);
    logProps.setProperty(pre + "redactor.policy.rules",
            resourcePath + "/test2.redaction");
    PropertyConfigurator.configure(logProps);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNoRegex() {
    Properties logProps = new Properties(defaults);
    logProps.setProperty(pre + "redactor.policy.rules",
            resourcePath + "/test3.redaction");
    PropertyConfigurator.configure(logProps);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNoFile() {
    Properties logProps = new Properties(defaults);
    logProps.setProperty(pre + "redactor.policy.rules",
            resourcePath + "/filenotfound");
    PropertyConfigurator.configure(logProps);
  }
}
