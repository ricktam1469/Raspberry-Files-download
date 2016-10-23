

Top level project contains samples for the Machine product.

===================================================================================================
Folder Structure
===================================================================================================

/--
    /appdata/httpriver
        - Sample data for sample-httpriver.

    /configuration/machine
        - Configuration files for some of the samples.

    /machine/bin/vms/solution.ini
        - Sample solution.ini that includes all the samples.

    /sample-basicmachineadapter
        - Implements IMachineAdapter interface and shows how to develop a basic machine adapter.

    /sample-configuration
        - Shows how to use Meta Mapping for files with configuration files.

    /sample-custompolling
        - Shows how to poll the cloud for updates and commands on demand (on a programmatic schedule 
            instead of periodic schedule defined in the configuration file).

    /sample-gitrepository
        - Git Repository Sample facilitates the use of JGit.

    /sample-healthmachineadapter
        - Demonstrate receiving data from health monitor adapter.

    /sample-hoover
        - Shows how to implement a Processor to manipulate data and then send it to the
        data river.

    /sample-httpclient
        - Shows how to use the HTTP Client service to make a REST call. Requires keystores and
          truststores to be configured. See section below for the /security folder.

    /sample-httpriver
        - This sample consists of two parts:
          - Predix Machine component: sample-httpriver
          - Predix Cloud component:   httpdata (see sample-cloud-apps.zip)
        - Together, these components show how data can be sent from Predix Machine to Predix Cloud
          using HTTP River service. Requires sample data in /appdata/httpriver (see /appdata
          section above.)

    /sample-mqttclient
        - Shows how to use MQTT client to publish a message and subscribe to a topic.

    /sample-mqttmachineadapter
        - Shows how to use MQTT machine adapter to receive data from an MQTT broker.

    /sample-databus
        - Shows how to use the databus to publish/subscribe to machine adapter. This sample assumes the 
        the docker environment with a MQTT broker installed.

    /sample-security
        - Shows how to use the SecurityAdmin service to encrypt and decrypt a string containing
          sensitive information.

    /sample-storeforwardclient
        - Shows how to use the StoreForward service to pass the data through a persistence queue
          after receiving from source and before sending it to it's destination.

    /sample-subscriptionmachineadapter
        - Implements ISubscriptionMachineAdapter interface and shows how to develop subscription
          functionality of machine adapter.

    /sample-websocketclient
        - Shows how to use the wsclient service to connect to a public websocket endpoint.

    /sample-websocketserver
        - Creates a local web socket server for sample-websocketclient.

    /sample-websocketriver
        - Shows how to use websocket river service to send data to the cloud over websockets.

    /security
        - Security config files. This also contains the flag to enable HTTP. If you are running a
          sample that requires keys setup in keystore/truststore (eg. sample-httpclient), you must
          copy all contents of this folder into your running container.


===================================================================================================
Building Samples
===================================================================================================

Before building samples:
========================
Make sure your environment has the following components installed and setup properly before
building the samples. Please refer to Predix Machine Getting Started Guide and Developers Guide for
details on how to setup your development environment. Make sure you have access to the maven
repository at https://artifactory.predix.io/artifactory/PREDIX-EXT

You will need to update your maven settings.xml to include the username/password. You must edit the
.m2/settings.xml file.

    1. Login into https://artifactory.predix.io
    2. Under profile generate an API Key
    3. Add the following into your settings.xml:

       <server>
           <id>artifactory.external</id>
           <username>predix cloud login</username>
           <password>{encrypted password - API key that you generated in step 2}</password>
       </server>

To build all samples from the command line:
===========================================
    1. Navigate to <sample-root> directory
    2. Run 'mvn clean install'

To build individual samples from the command line:
==================================================
    1. Navigate to <sample-root>/<sample-name> directory
    2. Run 'mvn clean install'

To build samples using eclipse IDE:
===================================

We recommend that you use Eclipse JEE version as IDE for your solution development.
Please connect to http://www.eclipse.org/downloads/ for detail information on how to download.
Install the Predix Machine SDK into the Eclipse (Follow the InstallationGuide.pdf)
To explore and get a hands on these samples:

    1.  Launch Eclipse and create your own workspace.
    2.  Import samples by select: File->Import->Maven->Existing Maven Projects, click "Next".
    3.  Browse and select "<Predix-Machine>/sample-apps" as root folder
    4.  Click "Finish" to finish importing all samples into your workspace.
    5.  Select the sample-apps, select "Run->Run As->Maven Install"
    6.  Add the maven project into a Predix Machine SDK image editor.
    7.  Add any configuration files in the image editor.
    8.  Export and run the container.

===================================================================================================
Running Samples
===================================================================================================

To run a sample:
================
    1. Make sure the sample project is built (see above)
    2. Copy the com.ge.dspmicro.{sample-name}-{version}.jar
       from <sample-root>/<sample-name>/target/
       to   <Predix-Machine>/machine/bundles
    3. Copy any configuration files needed by the sample
       from <sample-root>/configuration/
       to   <Predix-Machine>/configuration/machine/
    4. Modify <Predix-Machine>/machine/bin/vms/solution.ini file by adding a <bundle> tag for each
       sample in the following format:
           <bundle>
               <name>com.ge.dspmicro.{sample-name}-{version}.jar </name>
           </bundle>
       Or you can copy and paste the existing solution.ini file for all samples and modify that
       file. solutions.ini file is located in <sample-root>/machine/bin/vms/
    5. Start Predix Machine:
       Navigate to <Predix-Machine>/bin/
       Run command 'sh ./start_predixmachine.sh clean' ('start_predixmachine.bat clean' for Windows)

NOTE: Some samples may need more configuration not mentioned here. Please refer to the individual
sample's README for additional instructions.


