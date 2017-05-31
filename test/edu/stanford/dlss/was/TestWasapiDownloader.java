package edu.stanford.dlss.was;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({WasapiDownloader.class, WasapiValidator.class})
public class TestWasapiDownloader {

  @Test
  public void constructor_loadsSettings() throws SettingsLoadException {
    WasapiDownloader myInstance = new WasapiDownloader(WasapiDownloader.SETTINGS_FILE_LOCATION, null);
    assertNotNull(myInstance.settings);
  }

  @Test
  public void main_callsExecutFromCmdLine() throws Exception {
    WasapiDownloader mockDownloader = PowerMockito.mock(WasapiDownloader.class);
    PowerMockito.whenNew(WasapiDownloader.class).withAnyArguments().thenReturn(mockDownloader);

    WasapiDownloader.main(null);
    verify(mockDownloader).executeFromCmdLine();
  }

  @Test
  @SuppressWarnings("checkstyle:NoWhitespaceAfter")
  public void main_withHelp_canExecuteWithoutCrashing() throws SettingsLoadException, IOException, NoSuchAlgorithmException {
    String[] args = { "-h" };
    WasapiDownloader.main(args);
  }

  @Test
  public void main_executesFileSetRequest_usesAllAppropArgsSettings() throws Exception {
    String[] args = {"--collectionId", "123", "--jobId=456", "--crawlStartAfter", "2014-03-14", "--crawlStartBefore=2017-03-14", "--username=Fred" };
    WasapiConnection mockConn = Mockito.mock(WasapiConnection.class);
    Mockito.when(mockConn.pagedJsonQuery(anyString())).thenReturn(null);
    WasapiDownloader downloaderSpy = PowerMockito.spy(new WasapiDownloader(WasapiDownloader.SETTINGS_FILE_LOCATION, args));
    PowerMockito.doReturn(mockConn).when(downloaderSpy).getWasapiConn();
    PowerMockito.whenNew(WasapiDownloader.class).withAnyArguments().thenReturn(downloaderSpy);

    WasapiDownloader.main(args);
    verify(mockConn).pagedJsonQuery(ArgumentMatchers.contains("collection=123"));
    verify(mockConn).pagedJsonQuery(ArgumentMatchers.contains("crawl=456"));
    verify(mockConn).pagedJsonQuery(ArgumentMatchers.contains("crawl-start-after=2014-03-14"));
    verify(mockConn).pagedJsonQuery(ArgumentMatchers.contains("crawl-start-before=2017-03-14"));
    // username is used in login request
    verify(mockConn, Mockito.never()).pagedJsonQuery(ArgumentMatchers.contains("username=Fred"));
    // output directory is not part of wasapi request
    verify(mockConn, Mockito.never()).pagedJsonQuery(ArgumentMatchers.contains(WasapiDownloaderSettings.OUTPUT_BASE_DIR_PARAM_NAME));
  }

  @Test
  public void main_executesFileSetRequest_onlyUsesArgsSettings() throws Exception {
    String[] args = {"--collectionId", "123" };
    WasapiConnection mockConn = Mockito.mock(WasapiConnection.class);
    Mockito.when(mockConn.pagedJsonQuery(anyString())).thenReturn(null);
    WasapiDownloader downloaderSpy = PowerMockito.spy(new WasapiDownloader(WasapiDownloader.SETTINGS_FILE_LOCATION, args));
    PowerMockito.doReturn(mockConn).when(downloaderSpy).getWasapiConn();
    PowerMockito.whenNew(WasapiDownloader.class).withAnyArguments().thenReturn(downloaderSpy);

    WasapiDownloader.main(args);
    verify(mockConn).pagedJsonQuery(ArgumentMatchers.contains("collection=123"));
    verify(mockConn, Mockito.never()).pagedJsonQuery(ArgumentMatchers.contains("crawl="));
    verify(mockConn, Mockito.never()).pagedJsonQuery(ArgumentMatchers.contains("crawl-start-after="));
    verify(mockConn, Mockito.never()).pagedJsonQuery(ArgumentMatchers.contains("crawl-start-before="));
  }

  @Test
  public void main_singleFileDownload_onlyUsesFilename() throws Exception {
    String[] args = {"--collectionId", "123", "--filename", "ARCHIVEIT-5425-MONTHLY-JOB302671-20170526114117181-00049.warc.gz" };
    WasapiConnection mockConn = Mockito.mock(WasapiConnection.class);
    Mockito.when(mockConn.pagedJsonQuery(anyString())).thenReturn(null);
    WasapiDownloader downloaderSpy = PowerMockito.spy(new WasapiDownloader(WasapiDownloader.SETTINGS_FILE_LOCATION, args));
    PowerMockito.doReturn(mockConn).when(downloaderSpy).getWasapiConn();
    PowerMockito.whenNew(WasapiDownloader.class).withAnyArguments().thenReturn(downloaderSpy);

    WasapiDownloader.main(args);
    verify(mockConn).pagedJsonQuery(ArgumentMatchers.contains("filename=ARCHIVEIT-5425-MONTHLY-JOB302671-20170526114117181-00049.warc.gz"));
    verify(mockConn, Mockito.never()).pagedJsonQuery(ArgumentMatchers.contains("crawl="));
    verify(mockConn, Mockito.never()).pagedJsonQuery(ArgumentMatchers.contains("crawl-start-after="));
    verify(mockConn, Mockito.never()).pagedJsonQuery(ArgumentMatchers.contains("crawl-start-before="));
    verify(mockConn, Mockito.never()).pagedJsonQuery(ArgumentMatchers.contains("collection="));
  }

  @Test
  public void downloadSelectedWarcs_requestsFileSetResponse() throws Exception {
    WasapiConnection mockConn = Mockito.mock(WasapiConnection.class);
    Mockito.when(mockConn.pagedJsonQuery(anyString())).thenReturn(null);
    WasapiDownloader downloaderSpy = Mockito.spy(new WasapiDownloader(WasapiDownloader.SETTINGS_FILE_LOCATION, null));
    Mockito.doReturn(mockConn).when(downloaderSpy).getWasapiConn();

    downloaderSpy.downloadSelectedWarcs();
    WasapiDownloaderSettings mySettings = new WasapiDownloaderSettings(WasapiDownloader.SETTINGS_FILE_LOCATION, null);
    verify(mockConn).pagedJsonQuery(ArgumentMatchers.startsWith(mySettings.baseUrlString()));
  }

  private List<WasapiResponse> getWasapiRespList() {
    List<WasapiResponse> wasapiRespList = new ArrayList<WasapiResponse>();
    wasapiRespList.add(new WasapiResponse());
    return wasapiRespList;
  }

  @Test
  public void downloadSelectedWarcs_usesCrawlSelector() throws Exception {
    WasapiConnection mockConn = Mockito.mock(WasapiConnection.class);
    List<WasapiResponse> wasapiRespList = getWasapiRespList();
    Mockito.when(mockConn.pagedJsonQuery(anyString())).thenReturn(wasapiRespList);

    WasapiCrawlSelector mockCrawlSelector = PowerMockito.mock(WasapiCrawlSelector.class);
    List<Integer> desiredCrawlIds = new ArrayList<Integer>();
    desiredCrawlIds.add(Integer.valueOf("666"));
    PowerMockito.when(mockCrawlSelector.getSelectedCrawlIds(0)).thenReturn(desiredCrawlIds);
    PowerMockito.whenNew(WasapiCrawlSelector.class).withArguments(wasapiRespList).thenReturn(mockCrawlSelector);

    WasapiDownloader downloaderSpy = Mockito.spy(new WasapiDownloader(WasapiDownloader.SETTINGS_FILE_LOCATION, null));
    Mockito.doReturn(mockConn).when(downloaderSpy).getWasapiConn();

    downloaderSpy.downloadSelectedWarcs();
    verify(mockCrawlSelector).getSelectedCrawlIds(0); // no command line args means it gets all crawl ids like this
    verify(mockCrawlSelector).getFilesForCrawl(anyInt());
  }

  @Test
  @SuppressWarnings("checkstyle:MethodLength")
  public void downloadSelectedWarcs_callsDownloadAndValidateFile() throws Exception {
    WasapiConnection mockConn = Mockito.mock(WasapiConnection.class);
    Mockito.when(mockConn.jsonQuery(anyString())).thenReturn(new WasapiResponse());

    WasapiCrawlSelector mockCrawlSelector = PowerMockito.mock(WasapiCrawlSelector.class);
    List<Integer> desiredCrawlIds = new ArrayList<Integer>();
    desiredCrawlIds.add(Integer.valueOf("666"));
    PowerMockito.when(mockCrawlSelector.getSelectedCrawlIds(0)).thenReturn(desiredCrawlIds);
    List<WasapiFile> filesForCrawl = new ArrayList<WasapiFile>();
    WasapiFile wfile = new WasapiFile();
    String filename = "i_is_a_warc_file";
    wfile.setFilename(filename);
    filesForCrawl.add(wfile);
    PowerMockito.when(mockCrawlSelector.getFilesForCrawl(666)).thenReturn(filesForCrawl);
    PowerMockito.whenNew(WasapiCrawlSelector.class).withAnyArguments().thenReturn(mockCrawlSelector);

    WasapiDownloader downloaderSpy = Mockito.spy(new WasapiDownloader(WasapiDownloader.SETTINGS_FILE_LOCATION, null));
    Mockito.doReturn(mockConn).when(downloaderSpy).getWasapiConn();
    Mockito.doNothing().when(downloaderSpy).downloadAndValidateFile(wfile);

    downloaderSpy.downloadSelectedWarcs();
    verify(downloaderSpy).downloadAndValidateFile(wfile);
  }

  @Test
  @SuppressWarnings("checkstyle:NoWhitespaceAfter")
  public void downloadSelectedWarcs_byJobIdLowerBound() throws Exception {
    String argValue = "666";
    String[] args = { "--jobIdLowerBound=" + argValue };

    WasapiCrawlSelector mockCrawlSelector = PowerMockito.mock(WasapiCrawlSelector.class);
    List<WasapiResponse> wasapiRespList = getWasapiRespList();
    PowerMockito.whenNew(WasapiCrawlSelector.class).withArguments(wasapiRespList).thenReturn(mockCrawlSelector);

    WasapiConnection mockConn = Mockito.mock(WasapiConnection.class);
    Mockito.when(mockConn.pagedJsonQuery(anyString())).thenReturn(wasapiRespList);
    WasapiDownloader downloaderSpy = Mockito.spy(new WasapiDownloader(WasapiDownloader.SETTINGS_FILE_LOCATION, args));
    Mockito.doReturn(mockConn).when(downloaderSpy).getWasapiConn();

    downloaderSpy.downloadSelectedWarcs();
    verify(mockCrawlSelector).getSelectedCrawlIds(Integer.valueOf(argValue));
  }

  @Test
  public void prepareOutputLocation_correctLocation() throws SettingsLoadException {
    WasapiDownloader wd = new WasapiDownloader(WasapiDownloader.SETTINGS_FILE_LOCATION, null);
    WasapiFile wfile = new WasapiFile();
    String collId = "123";
    wfile.setCollectionId(Integer.parseInt(collId));
    String crawlStartTime = "2017-01-01T00:00:00Z";
    wfile.setCrawlStartDateStr(crawlStartTime);
    String filename = "i_is_a_warc_file";
    wfile.setFilename(filename);
    String crawlId = "666";
    wfile.setCrawlId(Integer.parseInt(crawlId));
    WasapiDownloaderSettings mySettings = new WasapiDownloaderSettings(WasapiDownloader.SETTINGS_FILE_LOCATION, null);

    String result = wd.prepareOutputLocation(wfile);
    String expected = mySettings.outputBaseDir() + "AIT_" + collId + "/" + crawlId + "/" + crawlStartTime + "/" + filename;
    assertEquals("Incorrect output location", expected, result);
  }

  @Test
  public void prepareOutputLocation_whenMissingJsonValues() throws SettingsLoadException {
    WasapiDownloader wd = new WasapiDownloader(WasapiDownloader.SETTINGS_FILE_LOCATION, null);
    String result = wd.prepareOutputLocation(new WasapiFile());
    assertThat(result, org.hamcrest.CoreMatchers.containsString("/AIT_0/")); // when missing collectionId
    assertThat(result, org.hamcrest.CoreMatchers.containsString("/0/")); // when missing crawlId
    assertThat(result, org.hamcrest.CoreMatchers.containsString("/null/")); // when missing crawlStartTime
    assertThat(result, org.hamcrest.CoreMatchers.endsWith("/null")); // when missing filename - gah!
  }

  @Test
  public void prepareOutputLocation_createsDirsAsNecessary() throws SettingsLoadException {
    WasapiDownloader wd = new WasapiDownloader(WasapiDownloader.SETTINGS_FILE_LOCATION, null);
    WasapiFile wfile = new WasapiFile();
    String collId = "111";
    wfile.setCollectionId(Integer.parseInt(collId));
    String crawlStartTime = "2017-01-01T00:01:02Z";
    wfile.setCrawlStartDateStr(crawlStartTime);
    String crawlId = "888";
    wfile.setCrawlId(Integer.parseInt(crawlId));

    WasapiDownloaderSettings mySettings = new WasapiDownloaderSettings(WasapiDownloader.SETTINGS_FILE_LOCATION, null);
    String expected = mySettings.outputBaseDir() + "AIT_" + collId + "/" + crawlId + "/" + crawlStartTime;
    File expectedDir = new File(expected);

    wd.prepareOutputLocation(wfile);
    assertTrue("Directory should exist: " + expected, expectedDir.exists());
  }

  @Test
  public void checksumValidate_md5_calls_wasapiValidator_validateMd5() throws SettingsLoadException, NoSuchAlgorithmException, IOException {
    WasapiFile wfile = new WasapiFile();
    String expectedChecksum = "666";
    HashMap<String, String> checksumsMap = new HashMap<String, String>();
    checksumsMap.put("md5", expectedChecksum);
    wfile.setChecksums(checksumsMap);

    PowerMockito.mockStatic(WasapiValidator.class);
    Mockito.when(WasapiValidator.validateMd5(expectedChecksum, anyString())).thenReturn(anyBoolean());

    WasapiDownloader wd = new WasapiDownloader(WasapiDownloader.SETTINGS_FILE_LOCATION, null);
    wd.checksumValidate("md5", wfile, anyString());

    PowerMockito.verifyStatic();
    WasapiValidator.validateMd5(expectedChecksum, "");
  }

  @Test
  public void checksumValidate_sha1_calls_wasapiValidator_validateSha1() throws SettingsLoadException, NoSuchAlgorithmException, IOException {
    WasapiFile wfile = new WasapiFile();
    String expectedChecksum = "666";
    HashMap<String, String> checksumsMap = new HashMap<String, String>();
    checksumsMap.put("sha1", expectedChecksum);
    wfile.setChecksums(checksumsMap);

    PowerMockito.mockStatic(WasapiValidator.class);
    Mockito.when(WasapiValidator.validateSha1(expectedChecksum, anyString())).thenReturn(anyBoolean());

    WasapiDownloader wd = new WasapiDownloader(WasapiDownloader.SETTINGS_FILE_LOCATION, null);
    wd.checksumValidate("sha1", wfile, anyString());

    PowerMockito.verifyStatic();
    WasapiValidator.validateSha1(expectedChecksum, "");
  }

  @Test
  public void checksumValidate_missingChecksumPrintsErrorAndReturnsFalse() throws SettingsLoadException, NoSuchAlgorithmException, IOException {
    WasapiFile wfile = new WasapiFile();
    String expectedChecksum = "666";
    HashMap<String, String> checksumsMap = new HashMap<String, String>();
    checksumsMap.put("sha1", expectedChecksum);
    wfile.setChecksums(checksumsMap);

    ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    System.setErr(new PrintStream(errContent));

    WasapiDownloader wd = new WasapiDownloader(WasapiDownloader.SETTINGS_FILE_LOCATION, null);
    assertFalse("result of checksumValidate for missing checksum should be false", wd.checksumValidate("md5", wfile, any()));
    assertEquals("Wrong SYSERR output", "No checksum of type: md5 available: {sha1=666}\n", errContent.toString());
  }

  @Test
  public void checksumValidate_unsupportedAlgorithmReturnsFalse() throws SettingsLoadException, NoSuchAlgorithmException, IOException {
    WasapiFile wfile = new WasapiFile();
    String expectedChecksum = "666";
    HashMap<String, String> checksumsMap = new HashMap<String, String>();
    checksumsMap.put("foo", expectedChecksum);
    wfile.setChecksums(checksumsMap);

    ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    System.setErr(new PrintStream(errContent));

    WasapiDownloader wd = new WasapiDownloader(WasapiDownloader.SETTINGS_FILE_LOCATION, null);
    assertFalse("result of checksumValidate for unsupported algorithm should be false", wd.checksumValidate("foo", wfile, any()));
    assertEquals("Wrong SYSERR output", "Unsupported checksum algorithm: foo.  Options are 'md5' or 'sha1'\n", errContent.toString());
  }
}
