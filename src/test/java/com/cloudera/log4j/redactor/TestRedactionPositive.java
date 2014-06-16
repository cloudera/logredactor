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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public class TestRedactionPositive {
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
  
  @Before
  public void setUp() {
    originalOut = System.err;
    memOut = new ByteArrayOutputStream();
    filterOut = new FilterOut(memOut);
    capturedOut = new PrintStream(filterOut);
    System.setErr(capturedOut);
    PropertyConfigurator.configure(Thread.currentThread().
        getContextClassLoader().getResourceAsStream("log4j-positive.properties"));
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

  @Test
  public void testRedaction() {
    Logger log = Logger.getLogger(TestRedactionPositive.class);

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
    Assert.assertEquals("x=1234-1234-1234-1234 or y=000-00-0000", out);

    log.info("xxx password=\"hi\"");
    out = getAndResetLogOutput();
    Assert.assertEquals("xxx password=\"?????\"", out);

    log.info("blah blah ABC Blah");
    out = getAndResetLogOutput();
    Assert.assertEquals("blah blah ??? Blah", out);
  }

}
