

StoreForward sample -- demonstrates how to use StoreForward service to pass the data through a persistence queue after receiving 
from source and before sending it to it's destination.

Execution process of the sample:
  - The sample injects the StoreForward service by the name in sample's cofig file, which matches the Storeforwards name in one of it's config file. 
  - The sample registers a callback with the storeforward. 
  - The sample receives the data in it's callback and writes it to log file.

Configurations for the sample:
  Default configurations should work. Make sure storeforward name matches in sample 
   - <Predix-Machine>/configuration/machine/com.ge.dspmicro.sample.storeforwardclient.cfg
  and StoreForward factory instance configuration files, like:
   - <Predix-Machine>/configuration/machine/com.ge.dspmicro.storeforward-0.cfg