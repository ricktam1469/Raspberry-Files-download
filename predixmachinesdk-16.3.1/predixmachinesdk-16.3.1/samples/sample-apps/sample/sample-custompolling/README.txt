

Predix Machine Custom Cloud Polling Sample - This is an example of how to poll the cloud for updates and commands on demand. This allows for 
a programmatic schedule instead of the periodic schedule defined in com.proximetry.osgiagent.impl.DevicesService.cfg.

This example will force the configuration file to change to "ON_DEMAND" on the activate. Normally modifying the configuration file manually and saving
would be the solution you want.

NOTE: this samples requires Proximetry APIs. This means this item should be pushed to a local artifactory so that these can be resolved:

        <dependency>
            <groupId>com.proximetry</groupId>
            <artifactId>agent-api</artifactId>
            <version>${com.proximetry.agent-api.version}</version>
            <scope>provided</scope>
        </dependency>

        
The jar can be taken from any machine container built with proximetry support.

For example:
mvn install:install-file -DgroupId=com.proximetry \
    -DartifactId=agent-api \
    -Dversion=0.3.3 \
    -Dfile=agent-api-0.3.3.jar \
    -Dpackaging=jar \
    -DgeneratePom=true
