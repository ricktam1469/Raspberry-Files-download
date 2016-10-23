HTTP Data 
=========
This sample cloud application receives data sent from Predix Machine and stores it into a relational database.
Note: the Predix Machine device must be enrolled with the cloud for this sample cloud to receive data.

You will need:
==============
- A Cloud Foundry account
- Cloud Foundry (CF) Command Line Interface (CLI)
  - Downloadable at https://github.com/cloudfoundry/cli/releases

Setup
=====

Before You Begin
----------------

Make sure you have access to the maven repository: https://artifactory.predix.io/artifactory/PREDIX-EXT

You will need to update your maven settings.xml to include the username/password. You must edit the .m2/settings.xml file.
        
        1. Login into https://artifactory.predix.io
        2. Under profile generate a API Key
    
        Add an entry into the settings.xml with this information:
        <server>
            <id>artifactory.external</id>
            <username>predix cloud login</username>
            <password>{encrypted password - API key}</password>
        </server>


You must be logged into Cloud Foundry and have the Cloud Foundry Command Line Interface installed.
If a service is available in Cloud Foundry, and you have the required access for that service, you 
can use it for your application development. Perform the following procedure to create an HTTP Data
service database instance and bind it to the service.


Setting up PostgreSQL on CF to store your data
----------------------------------------------

1. From command line, using CF CLI, login to your Cloud Foundry system as follows.
   
      cf login [-a API_URL] [-u USERNAME] [-p PASSWORD] [-o ORG] [-s SPACE]

   For example: cf login -a api.grc-apps.svc.ice.ge.com -u [cf-user-name] -o predix-mst -s dev
   
2. Create a database instance for use by HTTP Data.

      cf create-service [service-name] [plan-name] [your-service-instance-name]
      
   [service-name]                For example "postgresql93". You can find the appropriate name with "cf marketplace".
   [plan-name]                   For example "free". You can find the appropriate name with "cf marketplace".
   [your-service-instance-name]  Name it something you can remember (for example, "my-httpdata-postgres").
   
3. Verify your database instance is created.

      cf services
      
   You should now see your service instance in the listing.
      
      
Setting up HTTP Data
--------------------

1. From command line, change directory to sample/httpdata

2. Update manifest.yml as needed, for example

      - Customize application name so it doesn't conflict with other applications in CF (for example, "my-httpdata").
      - Change service name to your PostgreSQL instance name (for example, "my-httpdata-postgres" used above).
      
3. From command line, deploy HTTP Data to CF by entering the following CLI command

      cf push
      
4. Verify HTTP Data is deployed and running

      cf env my-httpdata
      
   Note the application_uris under VCAP_APPLICATION.
   In a web browser, enter the URL and append it with /v1/welcome (for example, http://my-httpdata.grc-apps.svc.ice.ge.com/v1/welcome). 
   If you see the welcome message, HTTP Data is running.
      
      
Administering data in your PostgreSQL (optional)
------------------------------------------------
The phpPgAdmin application allows you to access and manage data in your PostgreSQL database.

1. Verify phpPgAdmin is installed and running

      cf app pm-phpPgAdmin
      
   Note the URL in the output.
      
2. If phpPgAdmin is not installed:

   a Download phpPgAdmin

         git clone https://github.com/skibum55/phppgadmin
         
   b. Customize phpPgAdmin
   
         cd to phppgadmin directory
         edit manifest.yml and change the name as needed (for example, "pm-phppgadmin").
         
   c. Deploy phpPgAdmin to CF and bind it to your PostgreSQL instance
   
         cf push
         cf bind-service pm-phppgadmin my-httpdata-postgres
         cf restart pm-phppgadmin

3. Get the username and password for your PostgreSQL instance

      cf env pm-phppgadmin
      
   Notice the username and password listed under VCAP_SERVICES.
   
4. Enter the URL you got from step 1 in a browser.

5. Click on the database instance on the left pane. When prompted for credentials,
   enter the database username and password you got from step 3.
