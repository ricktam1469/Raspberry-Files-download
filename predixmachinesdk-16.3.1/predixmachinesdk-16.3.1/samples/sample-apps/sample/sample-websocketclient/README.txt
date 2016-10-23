

Web Socket Client Sample - Shows how to use the wsclient service connect to a 
local and external websocket endpoint.  This simple implementation shows how to
inject the websocket container and use annotated POJOs to interact with the
WebSocket life cycle events.  The example shows transfer of a text string as 
well as binary data in a byte buffer.  If running on the GE network, setting 
up the proxy is required for external connections.  Follow the steps below:

This sample requires HTTP enabled, copy the security files, i.e.
security/com.ge.dspmicro.securityadmin.cfg 

Note: the com.ge.dspmicro.securityadmin.cfg will set the client certificates and enable http browsing. See properties:
    org.apache.felix.http.enable=true
    org.apache.felix.https.clientcertificate=needs
    
    If you add web/technician console support for testing, the browser needs to go through the http address and not the https because of the client certificates.
    I.e. http://localhost:8181/system/console

Once the container starts with the sample-configuration bundle, 
	1- Navigate to		http://localhost:8181/system/console/configMgr
	
	2- Select the "Predix Machine Websocket Client".
	
	3- Edit the "Proxy host" configuration to be "proxy-src.research.ge.com".
	
Save the configuration to complete the setup.