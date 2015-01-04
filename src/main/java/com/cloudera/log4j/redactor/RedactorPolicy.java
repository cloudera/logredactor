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

import org.apache.log4j.rewrite.RewritePolicy;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.OptionHandler;

/**
 * <code>RewritePolicy</code> implementation that applies the redaction
 * rules defined in the configuration of the <code>RedactorPolicy</code> in
 * the Log4j Properties configuration file.
 *
 * @see RedactorAppender for the redaction rules definition and syntax.
 */
public class RedactorPolicy implements RewritePolicy, OptionHandler {

  private String rules;
  private StringRedactor redactor;

  /**
   * Log4j configurator uses this method to set the defined
   */
  public void setRules(String rules) {
    this.rules = rules;
  }

  /**
   * Called after all options are read in (in our case this is only setRules())
   * so that they can be acted on at one time.  The rules are either in the
   * log4j config file directly, or the rules is a full path to a file
   * containing rules.  This implements the OptionHandler interface.
   */
  @Override
  public void activateOptions() {
    if (rules.startsWith("/")) {
      redactor = StringRedactor.createFromFile(rules);
    } else {
      redactor = StringRedactor.createFromString(rules);
    }
  }

  /**
   * Given a LoggingEvent, potentially modify it and return an altered copy.
   * This implements the RewritePolicy interface.
   * @param source LoggingEvent to examine
   * @return Either the original (no changes) or a redacted copy.
   */
  @Override
  public LoggingEvent rewrite(LoggingEvent source) {
    String msg = source.getMessage().toString();
    msg = redactor.redact(msg);
    if (msg != null) {
      Throwable throwable = (source.getThrowableInformation() != null)
                            ? source.getThrowableInformation().getThrowable()
                            : null;
      source = new LoggingEvent(source.getFQNOfLoggerClass(),
          source.getLogger(), source.getTimeStamp(), source.getLevel(), msg,
          throwable);
    }
    return source;
  }
}
