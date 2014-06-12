Log Redactor

Log4j Appender that redacts log messages using redaction rules
before delegating to other Appenders.

How to use:

Install the Log Redactor JAR file in the classpath.

Edit the log4j.properties file adding the following appender definition:

 log4j.appender.redactor=com.cloudera.log4j.redactor.RedactorAppender
 log4j.appender.redactor.appenderRefs=[APPENDERS]
 log4j.appender.redactor.policy=com.cloudera.log4j.redactor.RedactorPolicy
 log4j.appender.redactor.policy.rules=[RULES]

[APPENDERS] should be the list of appenders, comma separated, to wrap for
redaction. All these appenders must be added to the rootLogger.

The redactor appender itself must also be to the rootLogger as the last
appender.

[RULES] are a list of [TRIGGER]::[REGEX]::[REDACTION_MASK] separated by '||'

If the log message contains the [TRIGGER], starting from the [TRIGGER] position
in the log message, the [REGEX] will be searched and all occurrences will be
replaced with the [REDACTION_MASK].

All rules for which the [TRIGGER] is found will be applied. If the [TRIGGER] is
empty, the rule will be applied to all log messages.

IMPORTANT: [REGEX] are Java regular expressions. Make sure escaping of \ is
properly done.

Working example of log4j.properties:

-----
# STDOUT Appender
log4j.appender.stdout=org.apache.log4j.ConsoleAppender
log4j.appender.stdout.Target=System.out
log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
log4j.appender.stdout.layout.ConversionPattern=%d{ISO8601} %-5p %c{1} - %m%n

log4j.appender.redactor=com.cloudera.log4j.redactor.RedactorAppender
log4j.appender.redactor.appenderRefs=stdout
log4j.appender.redactor.policy=com.cloudera.log4j.redactor.RedactorPolicy
log4j.appender.redactor.policy.rules=\
password=::password=\\".*\\"::password=\"?????\"||\
::\\d\\d\\d\\d-\\d\\d\\d\\d-\\d\\d\\d\\d-\\d\\d\\d\\d::XXXX-XXXX-XXXX-XXXX||\
::\\d\\d\\d-\\d\\d-\\d\\d\\d\\d::XXX-XX-XXXX||

log4j.rootLogger=ALL, stdout, redactor
-----

This example has 3 rules. The first one is triggered when 'password=' is found
and it replaces the password value (assumed between double quotes) with ?????.
The second and third rule are applied to all log messages, redacting credit
card numbers and SSN numbers.