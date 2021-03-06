package edu.stanford.dlss.was;

import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.message.BasicStatusLine;

import org.junit.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class TestWasapiValidator {

  private StatusLine validStatusLine = new BasicStatusLine(new ProtocolVersion("HTTP 1/1", 1, 1), 200, "OK");
  private StatusLine invalidStatusLine = new BasicStatusLine(new ProtocolVersion("HTTP 1/1", 1, 1), 300, "Not Defined");
  private StatusLine invalidStatusLine2 = new BasicStatusLine(new ProtocolVersion("HTTP 1/1", 1, 1), 201, "Whatever");

  @Test(expected = ClientProtocolException.class)
  public void testNullEntity() throws ClientProtocolException, HttpResponseException {
    // testing that exception is thrown
    WasapiValidator.validateResponse(validStatusLine, true);
  }

  @Test(expected = HttpResponseException.class)
  public void test300ResponseCode() throws ClientProtocolException, HttpResponseException {
    // testing that exception is thrown
    WasapiValidator.validateResponse(invalidStatusLine, false);
  }

  @Test(expected = HttpResponseException.class)
  public void test201ResponseCode() throws ClientProtocolException, HttpResponseException {
    // testing that exception is thrown
    WasapiValidator.validateResponse(invalidStatusLine2, false);
  }

  @Test
  public void testValidResponseCode() throws ClientProtocolException, HttpResponseException {
    assertTrue(WasapiValidator.validateResponse(validStatusLine, false));
  }

  private static final char SEP = File.separatorChar;
  private static final String FIXTURE_WARC_PATH = "test" + SEP + "fixtures" + SEP + "small-file.warc.gz";
  private static final String FIXTURE_MD5 = "f08b0bf60733b61216e288cb7620bd4a";
  private static final String FIXTURE_SHA1 = "c7dff430d5d725c3d2b786d5b247eb5a8d53b228";

  @Test
  public void validateMd5Checksum_withValidChecksum() throws NoSuchAlgorithmException, IOException {
    assertTrue("md5 checksum expected to validate for small-file.warc.gz", WasapiValidator.validateMd5(FIXTURE_MD5, FIXTURE_WARC_PATH));
  }

  @Test
  @SuppressWarnings("checkstyle:LineLength")
  public void validateMd5Checksum_withInvalidChecksum() throws NoSuchAlgorithmException, IOException {
    assertFalse("md5 checksum NOT expected to validate for small-file.warc.gz", WasapiValidator.validateMd5(FIXTURE_MD5 + "9", FIXTURE_WARC_PATH));
    assertFalse("md5 checksum NOT expected to validate for small-file.warc.gz", WasapiValidator.validateMd5(FIXTURE_SHA1, FIXTURE_WARC_PATH));
  }

  @Test
  public void validateSha1Checksum_withValidChecksum() throws NoSuchAlgorithmException, IOException {
    assertTrue("sha1 checksum expected to validate for small-file.warc.gz", WasapiValidator.validateSha1(FIXTURE_SHA1, FIXTURE_WARC_PATH));
  }

  @Test
  public void validateSha1Checksum_withInvalidChecksum() throws NoSuchAlgorithmException, IOException {
    String expectationErrorMsg = "sha1 checksum NOT expected to validate for small-file.warc.gz";
    assertFalse(expectationErrorMsg, WasapiValidator.validateSha1(FIXTURE_SHA1 + "9", FIXTURE_WARC_PATH));
    assertFalse(expectationErrorMsg, WasapiValidator.validateSha1(FIXTURE_MD5, FIXTURE_WARC_PATH));
  }
}
