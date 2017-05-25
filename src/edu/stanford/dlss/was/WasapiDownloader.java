package edu.stanford.dlss.was;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.validator.routines.IntegerValidator;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;

@SuppressWarnings("checkstyle:MultipleStringLiterals")
public class WasapiDownloader {
  public static final String SETTINGS_FILE_LOCATION = "config/settings.properties";
  private static final char SEP = File.separatorChar;

  // TODO:  use setting (see wasapi-downloader#93 in github)
  public static final int NUM_RETRIES = 3;

  public WasapiDownloaderSettings settings;

  private WasapiConnection wasapiConn;


  public WasapiDownloader(String settingsFileLocation, String[] args) throws SettingsLoadException {
    settings = new WasapiDownloaderSettings(settingsFileLocation, args);
  }

  public void executeFromCmdLine() throws IOException, NoSuchAlgorithmException {
    if (settings.shouldDisplayHelp()) {
      System.out.print(settings.getHelpAndSettingsMessage());
      return;
    }

    downloadSelectedWarcs();
  }

  // package level method for testing
  void downloadSelectedWarcs() throws IOException, NoSuchAlgorithmException {
    // System.out.println("DEBUG: about to request " + getFileSetRequestUrl());
    List<WasapiResponse> wasapiRespList = getWasapiConn().pagedJsonQuery(getFileSetRequestUrl());
    // System.out.println(wasapiResp.toString());

    if (wasapiRespList != null && wasapiRespList.get(0) != null) {
      WasapiCrawlSelector crawlSelector = new WasapiCrawlSelector(wasapiRespList);
      for (Integer crawlId : desiredCrawlIds(crawlSelector)) {
        for (WasapiFile file : crawlSelector.getFilesForCrawl(crawlId)) {
          downloadAndValidateFile(file);
        }
      }
    }
  }

  // package level method for testing
  WasapiConnection getWasapiConn() throws IOException {
    if (wasapiConn == null)
      wasapiConn = new WasapiConnection(new WasapiClient(settings));
    return wasapiConn;
  }

  // package level method for testing
  @SuppressWarnings("checkstyle:MethodLength")
  void downloadAndValidateFile(WasapiFile file) throws NoSuchAlgorithmException {
    String fullFilePath = prepareOutputLocation(file);
    int attempts = 0;
    boolean notValidated = true;
    do {
      attempts++;
      try {
        // System.out.println("DEBUG: trying to get " + file.getLocations()[0]);
        boolean downloadSuccess = getWasapiConn().downloadQuery(file.getLocations()[0], fullFilePath);
        if (downloadSuccess && checksumValidate("md5", file, fullFilePath)) {
          System.out.println("file retrieved successfully: " + file.getLocations()[0]);
          notValidated = false; // break out of loop
        }
      } catch (HttpResponseException e) {
        System.err.println("ERROR: HttpResponseException downloading file (will not retry) " + fullFilePath);
        System.err.println(" HTTP ResponseCode is " + e.getStatusCode());
        e.printStackTrace(System.err);
        attempts = NUM_RETRIES + 1;  // no more attempts
      } catch (ClientProtocolException e) {
        System.err.println("ERROR: ClientProtocolException downloading file (will not retry) " + fullFilePath);
        e.printStackTrace(System.err);
        attempts = NUM_RETRIES + 1;  // no more attempts
      } catch (IOException e) {
        // swallow exception and try again - it may be a network issue
        System.err.println("WARNING: exception downloading file (will retry) " + fullFilePath);
        e.printStackTrace(System.err);
      }
    } while (attempts <= NUM_RETRIES && notValidated);

    if (attempts == NUM_RETRIES)
      System.err.println("file not retrieved: " + file.getLocations()[0]);
    else if (notValidated)
      System.err.println("file has invalid checksum: " + file.getLocations()[0]);
  }

  // package level method for testing
  String prepareOutputLocation(WasapiFile file) {
    String outputPath = settings.outputBaseDir() + "AIT_" + file.getCollectionId() + SEP + file.getCrawlId() + SEP + file.getCrawlStartDateStr();
    new File(outputPath).mkdirs();
    return outputPath + SEP + file.getFilename();
  }

  private boolean checksumValidate(String algorithm, WasapiFile file, String fullFilePath) throws NoSuchAlgorithmException, IOException {
    // TODO:  use setting to decide md5 vs sha1 (see wasapi-downloader#92 in github)
    String checksum = file.getChecksums().get(algorithm);
    if (checksum == null) {
      System.err.println("No checksum of type: " + algorithm + " available.  Options are " + file.getChecksums().keySet().toString());
      return false;
    }

    if ("md5".equals(algorithm))
      return WasapiValidator.validateMd5(checksum, fullFilePath);
    else if ("sha1".equals(algorithm))
      return WasapiValidator.validateSha1(checksum, fullFilePath);
    return false;
  }

  private List<Integer> desiredCrawlIds(WasapiCrawlSelector crawlSelector) {
    // TODO: want cleaner grab of int from settings: wasapi-downloader#83
    Integer myInteger = IntegerValidator.getInstance().validate(settings.jobIdLowerBound());
    if (myInteger != null) {
      int jobsAfter = myInteger.intValue();
      return crawlSelector.getSelectedCrawlIds(jobsAfter);
    }
    else
      return crawlSelector.getSelectedCrawlIds(0); // all returns all crawl ids from FileSet
  }

  private String getFileSetRequestUrl() {
    StringBuilder sb = new StringBuilder(settings.baseUrlString() + "webdata?");
    List<String> params = requestParams();
    if (!params.isEmpty()) {
      for (String paramArg : params) {
        sb.append(paramArg + "&");
      }
      sb.deleteCharAt(sb.length() - 1);
    }
    return sb.toString();
  }

  private List<String> requestParams() {
    List<String> params = new ArrayList<String>();

    // If a filename is provided, other arguments are ignored
    if (settings.filename() != null) {
      params.add("filename=" + settings.filename());
      return params;
    }

    if (settings.collectionId() != null)
      params.add("collection=" + settings.collectionId());
    if (settings.crawlStartAfter() != null)
      params.add("crawl-start-after=" + settings.crawlStartAfter());
    if (settings.crawlStartBefore()!= null)
      params.add("crawl-start-before=" + settings.crawlStartBefore());
    if (settings.jobId() != null)
      params.add("crawl=" + settings.jobId());
    return params;
  }

  @SuppressWarnings("checkstyle:UncommentedMain")
  public static void main(String[] args) throws SettingsLoadException, IOException, NoSuchAlgorithmException {
    WasapiDownloader downloader = new WasapiDownloader(SETTINGS_FILE_LOCATION, args);
    downloader.executeFromCmdLine();
  }
}
