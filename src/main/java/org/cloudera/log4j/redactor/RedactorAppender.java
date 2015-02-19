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

import org.apache.log4j.Appender;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.rewrite.RewriteAppender;
import org.apache.log4j.spi.LoggingEvent;

import java.util.Enumeration;

/**
 * <code>RewriteAppender</code> that redacts the message of
 * <code>LoggingEvent</code>s and delegates to the referenced
 * <code>Appender</code>.
 * <p/>
 * The redaction rules are enforced by a {@link RedactorPolicy}.
 * <p/>
 * The <code>RedactorAppender</code> configuration is as follows:
 * <p/>
 * <pre>
 * log4j.appender.redactor=org.cloudera.log4j.redactor.RedactorAppender
 * log4j.appender.redactor.appenderRefs=[APPENDERS]
 * log4j.appender.redactor.policy=org.cloudera.log4j.redactor.RedactorPolicy
 * log4j.appender.redactor.policy.rules=/full/path/to/rule/file.json
 * </pre>
 * <p/>
 * [APPENDERS] should be the list of appenderRefs, comma separated, to wrap for
 * redaction.
 * <p/>
 * All the appenderRefs listed in [APPENDERS] must match the appenders listed
 * for the logger that you wish to redact.
 * <p/>
 * The <code>redactor</code> appender itself must be added to the logger that
 * you wish to redact.
 * <p/>
 * The format of the rules file in policy.rules is described in the
 * <code>StringRedactor</code> class.
 */
public class RedactorAppender extends RewriteAppender {
  private RedactorPolicy policy;
  private String[] appenderRefs;

  /**
   * Log4j configurator calls this with the contents found in the config file.
   */
  public void setPolicy(RedactorPolicy policy) {
    this.policy = policy;
  }

  /**
   * Log4j configurator calls this with the contents found in the config file.
   */
  public void setAppenderRefs(String refs) {
    this.appenderRefs = refs.split(",");
  }

  /**
   * For each of the given appenderRefs that are attached to the given logger,
   * place a RedactorAppender "in front of" the real appender so that it can
   * do redaction magic.
   * @param logger The logger to operate on.
   * @param appenders The appenderRefs to wrap.
   */
  private void wrapAppender(Logger logger, String[] appenders) {
    for (String appenderName : appenders) {
      appenderName = appenderName.trim();
      if (!appenderName.isEmpty()) {
        Appender appender = logger.getAppender(appenderName);
        if (appender != null) {
          logger.removeAppender(appenderName);
          RedactorAppender maskingAppender = new RedactorAppender();
          maskingAppender.setRewritePolicy(policy);
          maskingAppender.addAppender(appender);
          logger.addAppender(maskingAppender);
        }
      }
    }
  }

  /**
   * Called after all options are read in so that they can be acted on
   * at one time. Here we wrap all the necessary appenderRefs with
   * RedactorAppender()s.
   */
  @Override
  public void activateOptions() {
    super.activateOptions();
    Enumeration e = LogManager.getCurrentLoggers();
    while (e.hasMoreElements()) {
      Logger logger = (Logger) e.nextElement();
      wrapAppender(logger, appenderRefs);
    }
    wrapAppender(LogManager.getRootLogger(), appenderRefs);
  }

  @Override
  protected void append(LoggingEvent event) {
    super.append(event);
  }
}
