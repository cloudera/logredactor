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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class contains the logic for redacting Strings.  It is initialized
 * from rules contained in a JSON file.
 */
public class StringRedactor {

  /**
   * The thread-local Matcher and replacement for one rule.
   */
  private static class MatcherReplacement {
    private Matcher matcher;
    private String replacement;
    private boolean caseSensitive;

    public MatcherReplacement(RedactionRule rr) {
      Pattern pattern;
      if (rr.getCaseSensitive()) {
        pattern = Pattern.compile(rr.getSearch());
      } else {
        pattern = Pattern.compile(rr.getSearch(), Pattern.CASE_INSENSITIVE);
      }
      matcher = pattern.matcher("");
      replacement = rr.getReplace();
      caseSensitive = rr.getCaseSensitive();
    }
  }

  // This <code>ThreadLocal</code> keeps and reuses the Java RegEx
  // <code>Matcher</code>s for all rules, one set per thread because
  // <code>Matcher</code> is not thread safe.
  private ThreadLocal<Map<String, List<MatcherReplacement>>> mrTL =
          new ThreadLocal<Map<String, List<MatcherReplacement>>>() {
            @Override
            protected Map<String, List<MatcherReplacement>> initialValue() {
              Map<String, List<MatcherReplacement>> matcherMap
                      = new LinkedHashMap<String, List<MatcherReplacement>>();
              for (Map.Entry<String, List<RedactionRule>> entry
                      : ruleMap.entrySet()) {
                List<MatcherReplacement> list = new ArrayList<MatcherReplacement>();
                matcherMap.put(entry.getKey(), list);
                for (RedactionRule rr : entry.getValue()) {
                  list.add(new MatcherReplacement(rr));
                }
              }
              return matcherMap;
            }
          };

  // Map of {trigger -> RedactionRule}
  private Map<String, List<RedactionRule>> ruleMap
          = new LinkedHashMap<String, List<RedactionRule>>();

  /**
   * This class is created by the JSON ObjectMapper in createFromJsonFile().
   * It holds one rule for redaction - a description and then
   * trigger-search-replace. See the comments in createFromJsonFile().
   */
  private static class RedactionRule {
    private String description;
    private boolean caseSensitive = true;
    private String trigger;
    private String search;
    private String replace;

    public void setDescription(String description) {
      this.description = description;
    }

    public void setCaseSensitive(boolean caseSensitive) {
      this.caseSensitive = caseSensitive;
    }

    public boolean getCaseSensitive() {
      return caseSensitive;
    }

    public String getTrigger() {
      return trigger;
    }

    public void setTrigger(String trigger) {
      this.trigger = trigger;
    }

    public String getSearch() {
      return search;
    }

    public void setSearch(String search) {
      this.search = search;
      // We create a Pattern here to ensure it's a valid regex.
      Pattern thrownAway = Pattern.compile(search);
    }

    public String getReplace() {
      return replace;
    }

    public void setReplace(String replace) {
      this.replace = replace;
    }

    private void validate() throws JsonMappingException {
      if ((search == null) || search.isEmpty()) {
        throw new JsonMappingException("The search regular expression cannot " +
                "be empty.");
      }
      if ((replace == null || replace.isEmpty())) {
        throw new JsonMappingException("The replacement text cannot " +
                "be empty.");
      }

    }
  }

  /**
   * This class is created by the JSON ObjectMapper in createFromJsonFile().
   * It contains a version number and an array of RedactionRules.
   */
  private static class RedactionPolicy {
    private int version = -1;
    private List<RedactionRule> rules;

    public int getVersion() {
      return version;
    }

    public void setVersion(int version) {
      this.version = version;
    }

    public List<RedactionRule> getRules() {
      return rules;
    }

    public void setRules(List<RedactionRule> rules) {
      this.rules = rules;
    }

    /**
     * Perform validation checking on the fully constructed JSON.
     * @throws JsonMappingException
     */
    private void validate() throws JsonMappingException {
      if (version == -1) {
        throw new JsonMappingException("No version specified.");
      } else if (version != 1) {
        throw new JsonMappingException("Unknown version " + version);
      }
      for (RedactionRule rule : rules) {
        rule.validate();
      }
    }
  }

  /**
   * Given a RedactionPolicy created by the ObjectMapper, take the rules
   * it contains and organize them into the ruleMap
   * @param policy RedactionPolicy created by the ObjectMapper
   */
  private void populateRuleMap(RedactionPolicy policy) {
    for (RedactionRule rule : policy.getRules()) {
      String trigger = (rule.getTrigger() != null) ? rule.getTrigger() : "";
      List<RedactionRule> list = ruleMap.get(trigger);
      if (list == null) {
        list = new ArrayList<RedactionRule>();
        ruleMap.put(trigger, list);
      }
      list.add(rule);
    }
  }

  /**
   * Create a StringRedactor based on the JSON found in a file. The file
   * format looks like this:
   * {
   *   "version": 1,
   *   "rules": [
   *     { "description": "This is the first rule",
   *       "trigger": "triggerstring 1",
   *       "search": "regex 1",
   *       "replace": "replace 1"
   *     },
   *     { "description": "This is the second rule",
   *       "trigger": "triggerstring 2",
   *       "search": "regex 2",
   *       "replace": "replace 2"
   *     }
   *   ]
   * }
   * @param fileName The name of the file to read
   * @return A freshly allocated StringRedactor
   * @throws JsonParseException, JsonMappingException, IOException
   */
  public static StringRedactor createFromJsonFile(String fileName)
          throws IOException {
    StringRedactor sr = new StringRedactor();
    if (fileName == null) {
      return sr;
    }
    File file = new File(fileName);
    // An empty file is explicitly allowed as "no rules"
    if (file.exists() && file.length() == 0) {
      return sr;
    }

    ObjectMapper mapper = new ObjectMapper();
    RedactionPolicy policy = mapper.readValue(file, RedactionPolicy.class);
    policy.validate();
    sr.populateRuleMap(policy);
    return sr;
  }

  /**
   * Create a StringRedactor based on the JSON found in the given String.
   * The format is identical to that described in createFromJsonFile().
   * @param json String containing json formatted rules.
   * @return A freshly allocated StringRedactor
   * @throws JsonParseException, JsonMappingException, IOException
   */
  public static StringRedactor createFromJsonString(String json)
          throws IOException {
    StringRedactor sr = new StringRedactor();
    if ((json == null) || json.isEmpty()) {
        return sr;
    }

    ObjectMapper mapper = new ObjectMapper();
    RedactionPolicy policy = mapper.readValue(json, RedactionPolicy.class);
    policy.validate();
    sr.populateRuleMap(policy);
    return sr;
  }

  private boolean hasTrigger(String trigger, String msg, boolean caseSensitive) {
    //TODO use Boyer-More to make it more efficient
    //TODO http://www.cs.utexas.edu/users/moore/publications/fstrpos.pdf
    if (caseSensitive) {
      return msg.contains(trigger);
    }

    // As there is no case-insensitive contains(), our options are to
    // tolower() the strings (creates and throws away objects), use a regex
    // (slow) or write our own using regionMatches(). We take the latter
    // option, as it's fast.
    final int len = trigger.length();
    final int max = msg.length() - len;
    for (int i = 0; i <= max; i++) {
      if (msg.regionMatches(true, i, trigger, 0, len)) {
        return true;
      }
    }
    return false;
  }

  /**
   * The actual redaction - given a message, look through the list of
   * redaction rules and apply if matching. If so, return the redacted
   * String, else return the original string.
   * @param msg The message to examine.
   * @return The (potentially) redacted message.
   */
  public String redact(String msg) {
    String original = msg;
    boolean matched = false;
    for (Map.Entry<String, List<MatcherReplacement>> entry
            : mrTL.get().entrySet()) {
      String key = entry.getKey();
      for (MatcherReplacement mr : entry.getValue()) {
        if (hasTrigger(key, msg, mr.caseSensitive)) {
          mr.matcher.reset(msg);
          if (mr.matcher.find()) {
            msg = mr.matcher.replaceAll(mr.replacement);
            matched = true;
          }
        }
      }
    }
    return matched ? msg : original;
  }
}
