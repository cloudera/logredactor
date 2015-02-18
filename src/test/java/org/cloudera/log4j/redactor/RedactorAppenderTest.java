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
package org.cloudera.log4j.redactor;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Properties;

public class RedactorAppenderTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private PrintStream originalOut;
  private ByteArrayOutputStream memOut;
  private FilterOut filterOut;
  private PrintStream capturedOut;

  private static class FilterOut extends FilterOutputStream {
    public FilterOut(OutputStream out) {
      super(out);
    }

    public void setOutputStream(OutputStream out) {
      this.out = out;
    }
  }

  private Properties defaults = new Properties();
  private static final String PRE = "log4j.appender.";
  private String resourcePath = "/";

  @Before
  public void setUp() throws Exception {
    originalOut = System.err;
    memOut = new ByteArrayOutputStream();
    filterOut = new FilterOut(memOut);
    capturedOut = new PrintStream(filterOut);
    System.setErr(capturedOut);

    final String pkg = "org.cloudera.log4j.redactor.";
    defaults.setProperty(PRE + "LOG", "org.apache.log4j.ConsoleAppender");
    defaults.setProperty(PRE + "LOG.Target", "System.err");
    defaults.setProperty(PRE + "LOG.layout", "org.apache.log4j.PatternLayout");
    defaults.setProperty(PRE + "LOG.layout.ConversionPattern", "%m");
    defaults.setProperty(PRE + "redactor", pkg + "RedactorAppender");
    defaults.setProperty(PRE + "redactor.appenderRefs", "LOG");
    defaults.setProperty(PRE + "redactor.policy", pkg + "RedactorPolicy");
    defaults.setProperty("log4j.rootLogger", "ALL, LOG, redactor");

    URL resourceUrl = getClass().getResource("/real-1.json");
    File resourceFile = new File(resourceUrl.toURI());
    resourcePath = resourceFile.getParent();
  }

  @After
  public void cleanUp() {
    System.setErr(originalOut);
    LogManager.resetConfiguration();
  }

  private String getAndResetLogOutput() {
    capturedOut.flush();
    String logOutput = new String(memOut.toByteArray());
    memOut = new ByteArrayOutputStream();
    filterOut.setOutputStream(memOut);
    return logOutput;
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
    Logger log = Logger.getLogger(RedactorAppenderTest.class);

    log.info("WHERE x=123-45-6789");
    String out = getAndResetLogOutput();
    Assert.assertEquals("WHERE x=XXX-XX-XXXX", out);

    log.info("x=123-45-6789 WHERE");
    out = getAndResetLogOutput();
    Assert.assertEquals("x=XXX-XX-XXXX WHERE", out);

    log.info("WHERE x=123-45-6789 or y=000-00-0000");
    out = getAndResetLogOutput();
    Assert.assertEquals("WHERE x=XXX-XX-XXXX or y=XXX-XX-XXXX", out);

    log.info("x=1234-1234-1234-1234 or y=000-00-0000");
    out = getAndResetLogOutput();
    Assert.assertEquals("x=XXXX-XXXX-XXXX-XXXX or y=XXX-XX-XXXX", out);

    log.info("xxx password=\"hi\"");
    out = getAndResetLogOutput();
    Assert.assertEquals("xxx password=xxxxx", out);

    log.info("Mail me at myoder@cloudera.com, dude.");
    out = getAndResetLogOutput();
    Assert.assertEquals("Mail me at email@redacted.host, dude.", out);
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
