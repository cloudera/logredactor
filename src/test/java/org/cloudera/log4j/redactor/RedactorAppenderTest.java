/*
 * Copyright (c) 2015, Cloudera, Inc. All Rights Reserved.
 *
 * Cloudera, Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the
 * License.
 */
package org.cloudera.log4j.redactor;

import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Test;

public class RedactorAppenderTest extends BaseRedactorTest {
  protected static final String PRE = "log4j.appender.";

  @Override
  protected void setupLogger() {
    final String pkg = "org.cloudera.log4j.redactor.";
    defaults.setProperty(PRE + "LOG", "org.apache.log4j.ConsoleAppender");
    defaults.setProperty(PRE + "LOG.Target", "System.err");
    defaults.setProperty(PRE + "LOG.layout", "org.apache.log4j.PatternLayout");
    defaults.setProperty(PRE + "LOG.layout.ConversionPattern", "%m");
    defaults.setProperty(PRE + "redactor", pkg + "RedactorAppender");
    defaults.setProperty(PRE + "redactor.appenderRefs", "LOG");
    defaults.setProperty(PRE + "redactor.policy", pkg + "RedactorPolicy");
    defaults.setProperty("log4j.rootLogger", "ALL, LOG, redactor");
  }

  @Override
  public void cleanUp() {
    super.cleanUp();
    LogManager.resetConfiguration();
  }

  /**
   * Validate that redaction does in fact occur. Rigorous testing of the
   * redaction rules file and redaction itself is over in the
   * <code>StringRedactorTest</code>.
   */
  @Test
  public void testRedaction() {
    Properties logProps = new Properties(defaults);
    logProps.setProperty(PRE + "redactor.policy.rules",
            resourcePath + "/real-1.json");
    PropertyConfigurator.configure(logProps);
    final Logger log = Logger.getLogger(RedactorAppenderTest.class);

    testRedactionRules(new LoggerWrapper() {
      public void info(String message) {
        log.info(message);
      }
    });
  }

  /**
   * Ensure expected exception behavior with a file that doesn't exist
   */
  @Test
  public void testNoFile() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("thisfiledoesnotexist.json");
    Properties logProps = new Properties(defaults);
    logProps.setProperty(PRE + "redactor.policy.rules",
            resourcePath + "/tmp/thisfiledoesnotexist.json");
    PropertyConfigurator.configure(logProps);
    Logger log = Logger.getLogger(RedactorAppenderTest.class);
    log.info("This is a test");
  }

  /**
   * Ensure expected exception behavior with an invalid file
   */
  @Test
  public void testBadFile() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("non-json.json");
    Properties logProps = new Properties(defaults);
    logProps.setProperty(PRE + "redactor.policy.rules",
            resourcePath + "/non-json.json");
    PropertyConfigurator.configure(logProps);
    Logger log = Logger.getLogger(RedactorAppenderTest.class);
    log.info("This is a test");
  }

}
