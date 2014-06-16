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
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public class TestRedactionNegative {

  @After
  public void cleanUp() {
    LogManager.resetConfiguration();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidRuleMustBeThreeParts() {
    PropertyConfigurator.configure(Thread.currentThread().
        getContextClassLoader().getResourceAsStream(
        "log4j-negative1.properties"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidRuleEmptyRegex() {
    PropertyConfigurator.configure(Thread.currentThread().
        getContextClassLoader().getResourceAsStream(
        "log4j-negative2.properties"));
  }

}
