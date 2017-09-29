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
package org.cloudera.log4j2.redactor;

import java.io.IOException;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.cloudera.log4j.redactor.StringRedactor;

/**
 * <code>RewritePolicy</code> implementation that applies the redaction
 * rules defined in the configuration of the <code>RedactorPolicy</code> in
 * the Log4j Properties configuration file. Use with RewriteAppender.
 */
@Plugin(name = "RedactorPolicy", category = "Core", elementType = "layout", printObject = true)
public class RedactorPolicy implements RewritePolicy {

  // 'rules' is really the name of the file containing the rules
  private String rules;
  private StringRedactor redactor;

  @PluginFactory
  public static RedactorPolicy createPolicy(@PluginAttribute("name") String name,
                                            @PluginAttribute("rules") String rules) {
    return new RedactorPolicy(rules);
  }

  protected RedactorPolicy(String rules) {
    this.rules = rules;
    try {
      this.redactor = StringRedactor.createFromJsonFile(rules);
    } catch (IOException e) {
      // Changing the exception, since activateOptions can't throw an IOException
      throw new IllegalArgumentException("Problem with rules file " + rules, e);
    }
  }

  /**
   * Given a LoggingEvent, potentially modify it and return an altered copy.
   * This implements the RewritePolicy interface.
   * @param source LoggingEvent to examine
   * @return Either the original (no changes) or a redacted copy.
   */
  @Override
  public LogEvent rewrite(LogEvent source) {
    String original = source.getMessage().getFormattedMessage();
    String redacted = redactor.redact(original);
    if (!redacted.equals(original)) {
      source = new Log4jLogEvent.Builder(source)
          .setMessage(new SimpleMessage(redacted))
          .build();
    }
    return source;
  }
}
