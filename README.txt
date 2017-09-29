Log Redactor

This Log4j Appender redacts log messages using redaction rules
before delegating to other Appenders.

INSTALL AND CONFIGURATION:

Install the Log Redactor JAR file in the classpath.

For each logger whose content you wish to redact, add the following to the
log4j.properties file:

 log4j.appender.redactor=org.cloudera.log4j.redactor.RedactorAppender
 log4j.appender.redactor.appenderRefs=[APPENDERS]
 log4j.appender.redactor.policy=org.cloudera.log4j.redactor.RedactorPolicy
 log4j.appender.redactor.policy.rules=[RULES FILE]

* "redactor" is an arbitrary name for this particular redaction appender
* [APPENDERS] is the list of appenders belonging to the logger whose content
  you wish to redact.  
* [RULES FILE] is a full path to a file specifying redaction rules

The redactor appender itself must be added to the logger whose content you
wish to redact as the last appender.

The redaction rules specified in the [RULES FILE] above are specified in
JSON format. The file looks like

{
  "version": 1,
  "rules": [
    {
      "description": "This is the first rule",
      "trigger": "triggerstring 1",
      "search": "regex 1",
      "replace": "replace 1"
    },
    {
      "description": "This is the second rule",
      "trigger": "triggerstring 2",
      "caseSensitive": "false",
      "search": "regex 2",
      "replace": "replace 2"
    }
  ]
}

If the log message contains the "trigger", the "search" will be searched
and all occurrences will be replaced with "replace"

"Trigger" is a simple string compare and exists for performance reasons:
a simple string compare is much faster than a regular expression. The 
trigger is optional. If it does not exist, the message will always have
"search" applied.

"caseSensitive" is a boolean indicating if the trigger and search are
to be used in case sensitive or case insensitive matching. It is optional
and defaults to true (case sensitive matching).

The "search" field is a regular expression, and is required. Make sure that
proper escaping is used.

The "replace" field is a simple string and is also required. In practice,
it usually looks something like "XXXXXXX".

The "description" field is optional and is intended for self-documentation
purposes.

The ordering of the rules is significant. The rules are evaluated strictly
in the order given. Thus, in theory later rules might be influenced by
earlier rules.

Working example of a simple log4j.properties:

-----
# STDOUT Appender
log4j.appender.stdout=org.apache.log4j.ConsoleAppender
log4j.appender.stdout.Target=System.out
log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
log4j.appender.stdout.layout.ConversionPattern=%d{ISO8601} %-5p %c{1} - %m%n

log4j.appender.redactor=org.cloudera.log4j.redactor.RedactorAppender
log4j.appender.redactor.appenderRefs=stdout
log4j.appender.redactor.policy=org.cloudera.log4j.redactor.RedactorPolicy
log4j.appender.redactor.policy.rules=/full/path/to/rules.json

log4j.rootLogger=stdout, redactor
-----

Working example of rules.json:
{
  "version": "1",
  "rules": [
    {
      "description": "No more vowels",
      "search": "[aeiou]",
      "replace": "x"
    },
    {
      "description": "Passwords",
      "trigger": "password",
      "search": "password=.*",
      "replace": "password=xxxxx"
    }
  ]
}

This example has 2 rules. The first banishes lowercase vowels from all
log messages, and replaces them with x's. The second looks for lines
containing "password", and then replaces any password=.... occurrences
with password=xxxxx.

For more extensive and appropriate rules, see the "real-1.json" file in
the test resources directory.

USING REDACTOR APPENDERS IN MULTIPLE LOGGERS:

One individual redacting appender can be used in only one logger. To
redact more than one logger, create multiple redacting appenders, like so:

----
# CONS Appender (for the console)
log4j.appender.CONS=org.apache.log4j.ConsoleAppender
log4j.appender.CONS.Target=System.out
log4j.appender.CONS.layout=org.apache.log4j.PatternLayout
log4j.appender.CONS.layout.ConversionPattern=CONS %m%n

# RFA - Rolling File Appender
log4j.appender.RFA=org.apache.log4j.RollingFileAppender
log4j.appender.RFA.File=/var/log/file.out
log4j.appender.RFA.layout=org.apache.log4j.PatternLayout
log4j.appender.RFA.layout.ConversionPattern=%d{ISO8601} %p %c: %m%n
log4j.appender.RFA.MaxFileSize=200MB
log4j.appender.RFA.MaxBackupIndex=10

# Redactor for rootLogger
log4j.appender.rootRedactor=org.cloudera.log4j.redactor.RedactorAppender
log4j.appender.rootRedactor.appenderRefs=CONS
log4j.appender.rootRedactor.policy=org.cloudera.log4j.redactor.RedactorPolicy
log4j.appender.rootRedactor.policy.rules=/full/path/to/rules.json

# Redactor for org.cloudera
log4j.appender.clouderaRedactor=org.cloudera.log4j.redactor.RedactorAppender
log4j.appender.clouderaRedactor.appenderRefs=RFA
log4j.appender.clouderaRedactor.policy=org.cloudera.log4j.redactor.RedactorPolicy
log4j.appender.clouderaRedactor.policy.rules=/full/path/to/rules.json

log4j.rootLogger=CONS, rootRedactor
log4j.logger.org.cloudera=RFA, clouderaRedactor

LOG4J2 SUPPORT:

Log redaction is supported in log4j2 via the
org.cloudera.log4j2.redactor.RedactorPolicy class (note the "log4j2" in the
class name, which differentiates it from the log4j version). This class is an
implementation of RewritePolicy and is meant to be used with the built-in
RewriteAppender class in log4j2. Simply instantiate a RewriteAppender, specify
the RedactorPolicy with the correct rules file, and specify in the
RewriteAppender the appenders that should receive the redacted log messages.

An example, which redacts messages and passes them on to the console and a
rolling file appender, is provided below:

appender.redactorForRootLogger.name=redactorForRootLogger
appender.redactorForRootLogger.type=Rewrite
appender.redactorForRootLogger.appenderRef-console.type=AppenderRef
appender.redactorForRootLogger.appenderRef-console.ref=console
appender.redactorForRootLogger.appenderRef-console.level=ERROR
appender.redactorForRootLogger.appenderRef-DRFA.type=AppenderRef
appender.redactorForRootLogger.appenderRef-DRFA.ref=DRFA
appender.redactorForRootLogger.rewritePolicy.name=redactorForRootLoggerPolicy
appender.redactorForRootLogger.rewritePolicy.type=RedactorPolicy
appender.redactorForRootLogger.rewritePolicy.rules=/full/path/to/rules.json
