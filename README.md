[![Build Status](https://travis-ci.org/sul-dlss/wasapi-downloader.svg?branch=master)](https://travis-ci.org/sul-dlss/wasapi-downloader)
[![Coverage Status](https://coveralls.io/repos/github/sul-dlss/wasapi-downloader/badge.svg?branch=master)](https://coveralls.io/github/sul-dlss/wasapi-downloader?branch=master)
[![GitHub version](https://badge.fury.io/gh/sul-dlss%2Fwasapi-downloader.svg)](https://badge.fury.io/gh/sul-dlss%2Fwasapi-downloader)

# wasapi-downloader
Java command line application to download crawls from WASAPI.

## Local Setup

You'll need the following prerequisites installed on your local computer:

- Java (7)
- Ruby (we use Capistrano for deployment)

The minimal sequence of steps to verify that you can work with the code is:

1. `git clone https://github.com/sul-dlss/wasapi-downloader.git`
2. `cd wasapi-downloader`
3. `./gradlew installDist`  (compile and test the code and create a script to execute it)
4. `./build/install/wasapi-downloader/bin/wasapi-downloader --help` (explain usage)

An example invocation of the downloader:
```
./build/install/wasapi-downloader/bin/wasapi-downloader --collectionId 123 --crawlStartAfter 2014-03-14
```

### Configuration

This repository contains an example `config/settings.properties` file with dummy values for the required configuration settings. In order to successfully execute the Java application, you will need to override these default settings.

### Usage

#### Building

wasapi-downloader uses the gradle wrapper (https://docs.gradle.org/3.3/userguide/gradle_wrapper.html) so users don't have to worry about installing gradle.  However, using the gradle wrapper once (`gradlew [task]`) installs gradle on your system and from then forward you can simply execute `gradle [tasks]` rather than `gradlew [tasks]` (though either will work).


wasapi-downloader is built using [Gradle](https://gradle.org/docs).  To create a runnable installation with all needed jars and shell script (cleaning out old builds first):

`./gradle clean installDist`

List all available build tasks:

`./gradle tasks`

#### Running

To run:

`./build/install/wasapi-downloader/bin/wasapi-downloader --help` (explain usage)

An example invocation of the downloader:
```
./build/install/wasapi-downloader/bin/wasapi-downloader --collectionId 123 --crawlStartAfter 2014-03-14
```

## Deployment

Capistrano is used for deployment to Stanford VMs.

1. On your laptop, run

    `bundle`

  to install the Ruby capistrano gems and other dependencies for deployment.

2. Deploy code to remote VM:

    `cap <environment> deploy`

   `<environment>` is either `dev`, `stage` or `prod`, as specified in `config/deploy/`.

   This will also get our (Stanford's) latest configuration settings.

## (Stanford) Production Use

The deployment command shown above creates an executable Java application. After logging onto the production server you may run wasapi-downloader by following these steps:
```cd wasapi-downloader/current/
./build/install/wasapi-downloader/bin/wasapi-downloader <args>
```

The `--help` option will display a message listing all of the arguments:

`./build/install/wasapi-downloader/bin/wasapi-downloader --help`

Some of the arguments have a default value in `config/settings.properties`. `--help` will display the current configuration.

Download all crawl files created after 2014:

`./build/install/wasapi-downloader/bin/wasapi-downloader --crawlStartAfter 2014-01-01`

Download crawl files created before 2012, into /tmp/:

`./build/install/wasapi-downloader/bin/wasapi-downloader --crawlStartBefore 2012-01-01 --outputBaseDir /tmp/`

Download a single file:

`./build/install/wasapi-downloader/bin/wasapi-downloader --filename ARCHIVEIT-5425-MONTHLY-JOB302671-20170526114117181-00049.warc.gz`

**Note:** When a `--filename` argument is present, all other request parameters (crawl start/end, collection ID, job ID) are ignored.
