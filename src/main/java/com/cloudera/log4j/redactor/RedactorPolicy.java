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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <code>RewritePolicy</code> implementation that applies the redaction
 * rules defined in the configuration of the <code>RedactorPolicy</code> in
 * the Log4j Properties configuration file.
 *
 * @see RedactorAppender for the redaction rules definition and syntax.
 */
public class RedactorPolicy implements RewritePolicy, OptionHandler {

  private static class PatternReplacement {
    Pattern pattern;
    String replacement;

    public PatternReplacement(String pattern, String replacement) {
      this.pattern = Pattern.compile(pattern);
      this.replacement = replacement;
    }
  }

  private static class MatcherReplacement {
    Matcher matcher;
    String replacement;

    public MatcherReplacement(PatternReplacement pr) {
      matcher = pr.pattern.matcher("");
      replacement = pr.replacement;
    }
  }

  private String[] rules;
  private Map<String, List<PatternReplacement>> prMap
      = new HashMap<String, List<PatternReplacement>>();

  // This <code>ThreadLocal</code> keeps and reuses the Java RegEx
  // <code>Matcher</code>s for all rules, one set per thread because
  // <code>Matcher</code> is not thread safe.
  private ThreadLocal<Map<String, List<MatcherReplacement>>> mrTL =
      new ThreadLocal<Map<String, List<MatcherReplacement>>>() {
        @Override
        protected Map<String, List<MatcherReplacement>> initialValue() {
          Map<String, List<MatcherReplacement>> matcherMap
              = new HashMap<String, List<MatcherReplacement>>();
          for (Map.Entry<String, List<PatternReplacement>> entry 
              : prMap.entrySet()) {
            List<MatcherReplacement> list = new ArrayList<MatcherReplacement>();
            matcherMap.put(entry.getKey(), list);
            for (PatternReplacement pr : entry.getValue()) {
              list.add(new MatcherReplacement(pr));
            }
          }
          return matcherMap;
        }
      };

  /**
   * Log4j configurator uses this method to set the defined
   */
  public void setRules(String rules) {
    this.rules = rules.split("\\|\\|");
  }

  @Override
  public void activateOptions() {
    for (String rule : this.rules) {
      String[] kv = rule.trim().split("::", 3);
      if (kv.length != 3) {
        throw new IllegalArgumentException(
            "Invalid rule, it should have 3 parts: " + rule);
      }
      if (kv[1].length() == 0) {
        throw new IllegalArgumentException(
            "Invalid rule, regex cannot be empty: " + rule);
      }
      List<PatternReplacement> list = prMap.get(kv[0]);
      if (list == null) {
        list = new ArrayList<PatternReplacement>();
        prMap.put(kv[0], list);
      }
      list.add(new PatternReplacement(kv[1], kv[2]));
    }
  }

  private boolean hasTrigger(String trigger, String msg) {
    //TODO use Boyer-More to make it more efficient
    //TODO http://www.cs.utexas.edu/users/moore/publications/fstrpos.pdf
    return msg.contains(trigger);
  }
  
  private String redact(String msg) {
    boolean matched = false;
    for (Map.Entry<String, List<MatcherReplacement>> entry 
        : mrTL.get().entrySet()) {
      String key = entry.getKey();
      for (MatcherReplacement mr : entry.getValue()) {
        if (hasTrigger(key, msg)) {
          mr.matcher.reset(msg);
          if (mr.matcher.find()) {
            msg = mr.matcher.replaceAll(mr.replacement);
            matched = true;
          }
        }
      }
    }
    return (matched) ? msg : null;
  }

  @Override
  public LoggingEvent rewrite(LoggingEvent source) {
    String msg = source.getMessage().toString();
    msg = redact(msg);
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
