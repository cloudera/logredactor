package org.cloudera.log4j.redactor;

import java.io.IOException;

/**
 * We throw this when we have json parsing exceptions instead of throwing
 * a jackson JsonMappingException or JsonParseException because that will
 * expose the jackson internals externally. Moreover, we relocate the
 * jackson classes to a different name, so the external jackson name would
 * be strange.
 */
public class RedactionPolicyParseException extends IOException {
  public RedactionPolicyParseException() {
    super();
  }

  public RedactionPolicyParseException(String details) {
    super(details);
  }

  public RedactionPolicyParseException(Throwable cause) {
    super(cause);
  }

  public RedactionPolicyParseException(String details, Throwable cause) {
    super(details, cause);
  }
}
