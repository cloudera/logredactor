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

import java.net.URI;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.junit.Assert;
import org.junit.Test;

public class Log4j2RedactorTest extends BaseRedactorTest {

  protected void setupLogger() {
    // Setup happens before each test
  }

  /**
   * Runs all log4j2 tests.
   *
   * Due to being unable to reset the logger properly between test cases, all the tests need to
   * run in the same test method, and the good test must run first.
   */
  @Test
  public void testLog4j2() {
    testRedaction();

    testNoFile();

    testBadFile();
  }

  /**
   * Validate that redaction does in fact occur. Rigorous testing of the
   * redaction rules file and redaction itself is over in the
   * <code>StringRedactorTest</code>.
   */
  public void testRedaction() {
    ConfigurationFactory.setConfigurationFactory(new TestConfigurationFactory());
    ((LoggerContext)LogManager.getContext()).reconfigure();
    final Logger log = LogManager.getLogger("testRedaction");

    testRedactionRules(new LoggerWrapper() {
      public void info(String message) {
        log.info(message);
      }
    });
  }

  /**
   * Ensure expected exception behavior with a file that doesn't exist. In log4j2, the exception
   * is no longer propagated, but instead logged to stdout, so we scan stdout for the exception.
   */
  public void testNoFile() {
    ConfigurationFactory.setConfigurationFactory(new NoFileConfigurationFactory());
    ((LoggerContext)LogManager.getContext()).reconfigure();
    Logger log = LogManager.getLogger("testNoFile");
    log.info("WHERE x=123-45-6789");

    String out = getAndResetLogOutput();
    Assert.assertTrue(out.contains(IllegalArgumentException.class.getCanonicalName()));
    Assert.assertTrue(out.contains("/thisfiledoesnotexist.json"));
  }

  /**
   * Ensure expected exception behavior with an invalid file. In log4j2, the exception is no
   * longer propagated, but instead logged to stdout, so we scan stdout for the exception.
   */
  public void testBadFile() {
    ConfigurationFactory.setConfigurationFactory(new BadFileConfigurationFactory());
    ((LoggerContext)LogManager.getContext()).reconfigure();
    Logger log = LogManager.getLogger("testBadFile");
    log.info("WHERE x=123-45-6789");

    String out = getAndResetLogOutput();
    Assert.assertTrue(out.contains(IllegalArgumentException.class.getCanonicalName()));
    Assert.assertTrue(out.contains("/non-json.json"));
  }

  /**
   * Creates a custom log4j2 configuration which registers a RewriteAppender and a
   * RedactorPolicy.
   */
  protected static class TestConfigurationFactory extends ConfigurationFactory {
    @Override
    protected String[] getSupportedTypes() {
      return new String[]{"*"};
    }

    @Override
    public Configuration getConfiguration(LoggerContext loggerContext,
                                          ConfigurationSource source) {
      return buildConfiguration();
    }

    @Override
    public Configuration getConfiguration(LoggerContext loggerContext, String name, URI configLocation) {
      return buildConfiguration();
    }

    @Override
    public Configuration getConfiguration(LoggerContext loggerContext, String name, URI configLocation, ClassLoader loader) {
      return buildConfiguration();
    }

    protected Configuration buildConfiguration() {
      ConfigurationBuilder<BuiltConfiguration> builder = newConfigurationBuilder();
      builder.setConfigurationName(Log4j2RedactorTest.class.getName());
      builder.setStatusLevel(Level.INFO);

      AppenderComponentBuilder appenderBuilder = builder.newAppender("Stderr", "CONSOLE")
          .addAttribute("target", ConsoleAppender.Target.SYSTEM_ERR);
      appenderBuilder.add(builder.newLayout("PatternLayout")
          .addAttribute("pattern", "%msg"));
      builder.add(appenderBuilder);

      AppenderComponentBuilder rewriteBuilder = builder.newAppender("Redactor", "Rewrite")
          .addComponent(builder.newComponent("RedactorPolicy", "RedactorPolicy")
              .addAttribute("rules", resourcePath + getPolicyFilename()))
          .addComponent(builder.newAppenderRef("Stderr"));
      builder.add(rewriteBuilder);

      builder.add(builder.newRootLogger(Level.INFO)
          .add(builder.newAppenderRef("Redactor")));

      return builder.build();
    }

    /**
     * Returns the filename of the redaction policy file.
     *
     * This is silly, but ConfigurationFactory subclasses must have a default no-argument
     * constructor, which prevents us from passing the filename as an argument. Thus, we
     * subclass and override this method so that we can test different policy files.
     */
    protected String getPolicyFilename() {
      return "/real-1.json";
    }
  }

  protected static final class NoFileConfigurationFactory extends TestConfigurationFactory {
    protected String getPolicyFilename() {
      return "/thisfiledoesnotexist.json";
    }
  }

  protected static final class BadFileConfigurationFactory extends TestConfigurationFactory {
    protected String getPolicyFilename() {
      return "/non-json.json";
    }
  }
}
