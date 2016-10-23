

This folder contains:
	solution.ini - 		This file is copied into the container to load the bundles listed.
	
To change the samples tested, modify the solution.ini file.
For example, if you choose to not test the gitrepository sample:
	
	remove:
		<bundle>
			<name>com.ge.dspmicro.sample-gitrepository-16.3.1.jar</name>
		</bundle>
		
