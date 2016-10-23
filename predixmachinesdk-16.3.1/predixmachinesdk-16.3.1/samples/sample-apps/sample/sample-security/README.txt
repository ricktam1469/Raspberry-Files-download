

Security Sample - Shows how to use the SecurityAdmin service to encrypt and decrypt a string containing sensitive information.

This Sample Requires Configuration of keystores environment.
	Copy security configuration and keystores files, com.ge.dspmicro.securityadmin.cfg & 
	dspmicro_truststore.jks 
		From		<Predix-Machine>/sample-apps/security
		to 			<Predix-Machine>/security

Note: the com.ge.dspmicro.securityadmin.cfg will set the client certificates and enable http browsing. See properties:
    org.apache.felix.http.enable=true
    org.apache.felix.https.clientcertificate=needs
    
    If you add web/technician console support for testing, the browser needs to go through the http address and not the https because of the client certificates.
    I.e. http://localhost:8181/system/console

		