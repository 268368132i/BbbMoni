# Moni - monitoring and managing BigBlueButton recordings
This is a simple Apache Tomcat webapp that allows you to easily check free space, list, delete, download recordings.

**Features:**
1. See how much space is used by published, unpublished and in-process recordings.
2. Check size and get playback URL of each recording.
3. Download a recording as a bundle in zip archive for storage and future deployments.

**Not implemented**
1. Authentication.
2. Localization: the interface is a mix of Russian and English.

### Installation
1. Clone this repository.
2. Modify web.xml file. You'll need to specify BBB API URL as well as API SECRET.
3. run `mvn package`
4. Deploy .war file to your BigBlueButton Tomcat server.