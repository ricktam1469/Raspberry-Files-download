

Configuration Sample - Shows how to use Meta Mapping for files with configuration files

A sample com.ge.dspmicro.sample.configuration.cfg file is located in the 
<Predix-Machine>/configuration/machine/ folder to show how these configuration fields can be modified from the
interface. 

This sample requires HTTP enabled, copy the security files, i.e.
security/com.ge.dspmicro.securityadmin.cfg 

Note: the com.ge.dspmicro.securityadmin.cfg will set the client certificates and enable http browsing. See properties:
    org.apache.felix.http.enable=true
    org.apache.felix.https.clientcertificate=needs
    
    If you add web/technician console support for testing, the browser needs to go through the http address and not the https because of the client certificates.
    I.e. http://localhost:8181/system/console

Once the container starts with the sample-configuration bundle, 
	1- Navigate to		http://localhost:8181/system/console/bundles
	
	2- Select the "Configuration" tab on the top menu.
	
	3- Find the "Predix Machine Use Configuration Sample" service and select it.
	
You will see a pop-up showing the interface to modify the metadata from the configuration file 
located in the <Predix-Machine>/configuration/machine/ folder.