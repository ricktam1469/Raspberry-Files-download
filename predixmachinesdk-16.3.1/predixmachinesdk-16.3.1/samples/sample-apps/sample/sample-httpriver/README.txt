HTTP river sample - Shows how to use the HTTP river service to connect to an 
endpoint to send data.  This simple implementation shows how to inject the HTTP
river send service, create a HTTP river request, and send the data.In order for
the edge devices to talk to the cloud, a micro service must be deployed in the 
cloud to handle the data received. A sample micro service is provided in the 
sample cloud apps called predixmachine-http-data. Before running the HTTP river
sample you must build and deploy this microservice.  Next, get the url for the 
service and configure it in the http river configuration file.

Note:
In the predixmachine-http-data project the default trusted token issuer is set 
to all issuers. This is insecure and should instead be set to a regular expression 
matching only the issuers which your application should trust.  Change this
issuer in the HTTPDataApplication.java FastTokeServices method before using.