

HTTP Client Sample - Shows how to use the HTTP Client service to make a rest call 

This sample requires HTTP enabled, copy the security files, i.e.
security/com.ge.dspmicro.securityadmin.cfg 

Note: the com.ge.dspmicro.securityadmin.cfg will set the client certificates and enable http browsing. See properties:
    org.apache.felix.http.enable=true
    org.apache.felix.https.clientcertificate=needs
    
    If you add web/technician console support for testing, the browser needs to go through the http address and not the https because of the client certificates.
    I.e. http://localhost:8181/system/console

If you choose to not use code to modify the configurations, you can modify the
com.ge.dspmicro.httpclient.cfg file for the environment settings.
	File location: <Predix-Machine>/configuration/machine/