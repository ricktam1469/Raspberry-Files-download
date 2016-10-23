
Http Tunnel Server Sample - Shows how to start the Http Tunnel Server using a script

A script is provided to start the application. In the script there are two properties to set,
the KEYSTORE_FILE and the KEYSTORE_TYPE. The keystore file must be generated and saved in the
same place the script is located. Export the certificate, it will need to be imported to the
Predix Machine truststore.

In addition to the properties in the script, you will also need to update the server configurations.
These are located in the etc folder.
    1) By default, the com.ge.dspmicro.httptunnel.server.config file is configured to listen on port 8087.
    2) If transferring data from the Client to the Server, you must update
        the com.ge.dspmicro.httptunnel.server.protocol-0.config file.

Generate a container to use the Http Tunnel Client. Make sure to select the Predix Http Tunnel Feature Group.

Before starting the Predix Machine OSGi container to run the Http Tunnel Client:
    1) Import the certificate to the Predix Machine truststore in the security folder.
    2) Open the com.ge.dspmicro.httptunnel.client-0.config file and configure the properties.
    3) Open the com.ge.dspmicro.httptunnel.client.protocol-ssh.config file and configure
        according to what action you want to take. Upload or download data to and from the server respectively.

Starting services:
    1) Start the Predix Http Tunnel Server by running "sh tunnelServer". Enter the keystore password when prompted.
    2) Start the Predix Machine by navigating to the <container>/bin and running "./start_predixmachine.sh"

When the Http Tunnel Client makes a connection to the server, a 'device.properties' file is
created in the current script directory. 
The Device information saved in the file are the different connections made to devices in the 
format <Device Name>|<Cloud Device ID>|<Protocol>=<Port Server Opens>
	ex Device_1|Cloud_ID|ssh=55420

The Device Name, Device ID, and the Protocol is information passed on by the Client. The Port the server opens
is to listen to connections to tunnel to the client.

The machine documentation provides information on how to configure the HttpTunnelClient.