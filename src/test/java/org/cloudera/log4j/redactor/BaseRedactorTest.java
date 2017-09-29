/*
 * Copyright (c) 2017, Cloudera, Inc. All Rights Reserved.
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Properties;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

public abstract class BaseRedactorTest {
  protected static String resourcePath = "/";

  protected Properties defaults = new Properties();
  private PrintStream originalOut;
  private PrintStream originalErr;
  private ByteArrayOutputStream memOut;
  private FilterOut filterOut;
  private PrintStream capturedOut;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    originalOut = System.out;
    originalErr = System.err;
    memOut = new ByteArrayOutputStream();
    filterOut = new FilterOut(memOut);
    capturedOut = new PrintStream(filterOut);
    System.setOut(capturedOut);
    System.setErr(capturedOut);

    URL resourceUrl = getClass().getResource("/real-1.json");
    File resourceFile = new File(resourceUrl.toURI());
    resourcePath = resourceFile.getParent();

    setupLogger();
  }

  protected abstract void setupLogger();

  @After
  public void cleanUp() {
    System.setErr(originalErr);
    System.setOut(originalOut);
  }

  protected String getAndResetLogOutput() {
    capturedOut.flush();
    String logOutput = new String(memOut.toByteArray());
    memOut = new ByteArrayOutputStream();
    filterOut.setOutputStream(memOut);
    return logOutput;
  }

  protected void testRedactionRules(LoggerWrapper log) {
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

  private static class FilterOut extends FilterOutputStream {
    public FilterOut(OutputStream out) {
      super(out);
    }

    public void setOutputStream(OutputStream out) {
      this.out = out;
    }
  }

  // Since log4j and log4j2 use different Logger classes, we put a wrapper around them to avoid
  // test code duplication
  protected static abstract class LoggerWrapper {
    public abstract void info(String message);
  }
}
