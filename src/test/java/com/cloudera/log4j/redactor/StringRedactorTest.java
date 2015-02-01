package com.cloudera.log4j.redactor;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class StringRedactorTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private String resourcePath;
  private static final String MESSAGE = "This string is not redacted";

  @Before
  public void setUp() throws Exception {
    URL resourceUrl = getClass().getResource("/good-1.json");
    File resourceFile = new File(resourceUrl.toURI());
    resourcePath = resourceFile.getParent();
  }

  @After
  public void tearDown() throws Exception {

  }

  @Test
  public void testFileDNE() throws Exception {
    final String fileName = "thisfiledoesnotexist.json";
    thrown.expect(FileNotFoundException.class);
    thrown.expectMessage(fileName);
    StringRedactor sr = StringRedactor.createFromJsonFile(fileName);
  }

  @Test
  public void testFileIsDir() throws Exception {
    final String fileName = "/tmp";
    thrown.expect(FileNotFoundException.class);
    thrown.expectMessage(fileName);
    thrown.expectMessage("directory");
    StringRedactor sr = StringRedactor.createFromJsonFile(fileName);
  }

  @Test
  public void testNotJSON() throws Exception {
    final String fileName = resourcePath + "/non-json.json";
    thrown.expect(JsonParseException.class);
    thrown.expectMessage("#");
    thrown.expectMessage("Unexpected");
    thrown.expectMessage(fileName);
    StringRedactor sr = StringRedactor.createFromJsonFile(fileName);
  }

  @Test
  public void testNoVersion() throws Exception {
    final String fileName = resourcePath + "/no-version.json";
    thrown.expect(JsonMappingException.class);
    thrown.expectMessage("No version specified");
    thrown.expectMessage(fileName);
    StringRedactor sr = StringRedactor.createFromJsonFile(fileName);
  }

  @Test
  public void testUnknownVersion() throws Exception {
    final String fileName = resourcePath + "/unknown-version.json";
    thrown.expect(JsonMappingException.class);
    thrown.expectMessage("Unknown version");
    thrown.expectMessage(fileName);
    StringRedactor sr = StringRedactor.createFromJsonFile(fileName);
  }

  @Test
  public void testAlphaVersion() throws Exception {
    final String fileName = resourcePath + "/alpha-version.json";
    thrown.expect(JsonMappingException.class);
    thrown.expectMessage("string");
    thrown.expectMessage(fileName);
    StringRedactor sr = StringRedactor.createFromJsonFile(fileName);
  }

  @Test
  public void testExtraAttr() throws Exception {
    final String fileName = resourcePath + "/extra-attr.json";
    thrown.expect(JsonMappingException.class);
    thrown.expectMessage("extra");
    thrown.expectMessage(fileName);
    StringRedactor sr = StringRedactor.createFromJsonFile(fileName);
  }

  @Test
  public void testNoSearch() throws Exception {
    final String fileName = resourcePath + "/no-search.json";
    thrown.expect(JsonMappingException.class);
    thrown.expectMessage("search");
    thrown.expectMessage("cannot be empty");
    thrown.expectMessage(fileName);
    StringRedactor sr = StringRedactor.createFromJsonFile(fileName);
  }

  @Test
  public void testNoReplace() throws Exception {
    final String fileName = resourcePath + "/no-replace.json";
    thrown.expect(JsonMappingException.class);
    thrown.expectMessage("replace");
    thrown.expectMessage("cannot be empty");
    thrown.expectMessage(fileName);
    StringRedactor sr = StringRedactor.createFromJsonFile(fileName);
  }

  @Test
  public void testEmptyFile() throws Exception {
    final String fileName = resourcePath + "/empty.json";
    StringRedactor sr = StringRedactor.createFromJsonFile(fileName);
    String result = sr.redact(MESSAGE);
    Assert.assertEquals(MESSAGE, result);
  }

  @Test
  public void testEmptyRules() throws Exception {
    final String fileName = resourcePath + "/empty-rules.json";
    StringRedactor sr = StringRedactor.createFromJsonFile(fileName);
    String result = sr.redact(MESSAGE);
    Assert.assertEquals(MESSAGE, result);
  }

  @Test
  public void testBasicGood1() throws Exception {
    String fileName = resourcePath + "/good-1.json";
    StringRedactor sr = StringRedactor.createFromJsonFile(fileName);
    String redacted = sr.redact("Hello, world");
    Assert.assertEquals("Hxllx, wxrld", redacted);
  }

  @Test
  public void testRealRules() throws Exception {
    String fileName = resourcePath + "/real-1.json";
    StringRedactor sr = StringRedactor.createFromJsonFile(fileName);
    List<String[]> tests = new ArrayList<String[]>();
    // tests are a list of {"input", "expected"} pairs.
    tests.add(new String[]{"Hello, world", "Hello, world"});
    tests.add(new String[]{"CC 1234-2345-3456-4576", "CC XXXX-XXXX-XXXX-XXXX"});
    tests.add(new String[]{"CC 1234234534654576", "CC XXXXXXXXXXXXXXXX"});
    tests.add(new String[]{"CC 1234,2345,3456,4576", "CC XXXX-XXXX-XXXX-XXXX"});
    tests.add(new String[]{"SSN 123-45-6789", "SSN XXX-XX-XXXX"});
    tests.add(new String[]{"SSN 123456789", "SSN XXXXXXXXX"});
    tests.add(new String[]{"My password=Hello123", "My password=xxxxx"});
    tests.add(new String[]{"Host www.cloudera.com", "Host HOSTNAME.REDACTED"});
    tests.add(new String[]{"www.c1-foo.org rules!", "HOSTNAME.REDACTED rules!"});
    tests.add(new String[]{"IP1 8.8.8.8", "IP1 0.0.0.0"});
    tests.add(new String[]{"IP2 192.168.0.1", "IP2 0.0.0.0"});
    tests.add(new String[]{"My email is myoder@cloudera.com", "My email is email@redacted.host"});
    tests.add(new String[]{"hello.world@ex.x-1.fr is interesting", "email@redacted.host is interesting"});

    String redacted;
    for (String[] test : tests) {
      redacted = sr.redact(test[0]);
      Assert.assertEquals("Failed redacting: " + test[0], test[1], redacted);
    }
  }

  @Test
  public void testHugeRules() throws Exception {
    String fileName = resourcePath + "/huge-1.json";
    StringRedactor sr = StringRedactor.createFromJsonFile(fileName);
    String redacted = sr.redact(MESSAGE);
    Assert.assertEquals("This string is not redadted", redacted);
  }
}
