Websocket river sample - Shows how to use the websocket river service to 
connect to an external websocket endpoint to send data.  This simple 
implementation shows how to inject the websocket river send service, create
a websocket river request, and send the data.  The example shows transfer of a
preformatted JSON test string as in a file as well as a PDataValue in a byte 
stream.  These data types are both compatible with the Predix Cloud TimeSeries
service.  The WebSocket River service configuration in Predix Machine must be 
compatible with the configuration of the TimeSeries service running in the 
cloud.

CONFIGURATION
1. The UAA token URL in the Predix Machine Client Identity Service 
configuration MUST be a trusted issuer for the TimeSeries service instance in 
the cloud
2. The header name and value for the Predix Zone ID must match the 
configuration of the TimeSeries service instance in the cloud. These values can
be retrieved by running cf env for the application bound to the TimeSeries 
service. Sample values are shown below.
"ingest": {
     "uri": "wss://ingestion_url/v1/stream/messages",
     "zone-http-header-name": "Predix-Zone-Id",
     "zone-http-header-value": "9378e3db-e683-46a2-97c2-ccd11d75869d",
     "zone-token-scope": ["timeseries.zones.9378e3db-e683-46a2-97c2-ccd11d75869d.user","timeseries.zones.9378e3db-e683-46a2-97c2-ccd11d75869d.ingest"]
}
 
3. The client that is assigned to the Predix Machine instance MUST have the 
authorities listed in the zone-token-scope section of the above output. In this
example, these authorities are 
timeseries.zones.9378e3db-e683-46a2-97c2-ccd11d75869d.user and timeseries.zones.9378e3db-e683-46a2-97c2-ccd11d75869d.ingest

The Predix TimeSeries Service is deployed on Predix Cloud. To obtain an 
instance of this service for your Predix Cloud space, follow the instructions
at https://predix.ge.com/docs/#BcWpExrv.