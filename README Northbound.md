
Northbound REST API
===================

Checkout Code
-------------
```
git clone --recursive -b dev --depth 1 https://github.com/FITeagle/api.git && \
git clone --recursive -b dev --depth 1 https://github.com/FITeagle/core.git && \
git clone --recursive -b dev --depth 1 https://github.com/FITeagle/native.git && \
git clone --recursive -b Dev --depth 1 https://github.com/FITeagle/adapters.git && \
git clone --recursive --depth 1 https://github.com/FITeagle/bootstrap.git
```
The native package contains the lodlive lib as a git submodule, hints: git submodule init, git submodule update

Start Servers
-------------

Assuming you've run the bootstrap script from http://github.com/fiteagle/bootstrap:

```
./bootstrap/fiteagle.sh startJ2EE
./bootstrap/fiteagle.sh startSPARQL
```

Start Microservices
-------------------

```
cd api && mvn clean verify install && cd - && \
cd native && mvn clean verify wildfly:deploy && cd - && \
cd core/repo && mvn clean verify wildfly:deploy && cd - && \
cd core/bus && mvn clean verify wildfly:deploy && cd -
```


Look at the logger
------------------

To view a text version of the logger open:

[http://localhost:8080/native/gui/admin/log-viewer-text.html](http://localhost:8080/native/gui/admin/log-viewer-text.html)

A list version can be found at:

[http://localhost:8080/native/gui/admin/log-viewer-list.html](http://localhost:8080/native/gui/admin/log-viewer-list.html)


Look at the repository
----------------------

To see all the content of the repository open the FUSEKI web interface:

[http://localhost:3030/ds/query?query=SELECT+*+{%3Fs+%3Fp+%3Fo}&output=text&stylesheet=](http://localhost:3030/ds/query?query=SELECT+*+{%3Fs+%3Fp+%3Fo}&output=text&stylesheet=)

To see a graph visualization of the repository content open:

[http://localhost:8080/native/api/lodlive/query?output=json&format=application/json&timeout=0&query=Select%20*%20%7B?s%20?p%20?o%7D&callback=lodlive](http://localhost:8080/native/api/lodlive/query?output=json&format=application/json&timeout=0&query=Select%20*%20%7B?s%20?p%20?o%7D&callback=lodlive)


Run the Testbend adapter
---------------------

The testbed adapter is managing the testbed by keeping track of all deployed adapters. Deploy it first:

```
cd adapters/testbed && mvn clean verify wildfly:deploy
```


Run the Motor adapter
---------------------

Now deploy all three dummy adapters:

```
cd adapters/motor && mvn clean verify wildfly:deploy
```
```
cd adapters/stopwatch && mvn clean verify wildfly:deploy
```
```
cd adapters/mightyrobot && mvn clean verify wildfly:deploy
```

After deployment of the adapter there should be new log entries in the log viewer (adapter registering) and new resources in FUSEKI.


Open the at the adapter manager
------------------

Open the adapter manager to see a list of the deployed adapters and manage them:

[http://localhost:8080/native/gui/admin/adapter-manager.html](http://localhost:8080/native/gui/admin/adapter-manager.html)

Monitor, create, terminate and configure resource adapters.


Experimenting
-------------------

Experiments (create, configure, monitor, release) can be done via direct calls to the REST API with a tool such as curl or via the Adapter Manager Web GUI ( [http://localhost:8080/native/gui/admin/adapter-manager.html](http://localhost:8080/native/gui/admin/adapter-manager.html) ).


Experimenting via curl REST calls
----------------------------------

Send a global discover message into the message bus. The first response will appear as the REST response, the other responses are visible via the Message Bus Logger:

```
curl -i -X GET http://localhost:8080/native/api/resources/discover```
```

Get all adapters and their type that are currently deployed in the testbed:

```
curl -i -X GET http://localhost:8080/native/api/resources/```
```

### Example: Motor Adapter

The example RDF files that are used in some calls can be found in the adapters/motor directory.

Possible adapter names:

* Create a single new motor instance using an attached, detailed RDF description:
  * ```curl -i -X PUT -d @createMotor.ttl http://localhost:8080/native/api/resources/ADeployedMotorAdapter1```
  * Response should be HTTP 201 + New motor instance RDF description
  
  
* Create four new motor instances at once using an attached, detailed RDF description:
  * ```curl -i -X PUT -d @createManyMotors.ttl http://localhost:8080/native/api/resources/ADeployedMotorAdapter1```
  * Response should be HTTP 201 + New motor instance RDF description
   
* Create a single new motor resource instance named "ARunningMotor01" with no attached RDF description and default parameters (just using the path):
  * ```curl -i -X PUT http://localhost:8080/native/api/resources/ADeployedMotorAdapter1/ARunningMotor01```
  * Response should be HTTP 201 + New motor instance RDF description

* To get a description of all resources instances managed by the adapter:
  * ```curl -i -X GET http://localhost:8080/native/api/resources/ADeployedMotorAdapter1```

* To get a description of the properties of a single resource instance managed by the adapter:
  * ```curl -i -X GET http://localhost:8080/native/api/resources/ADeployedMotorAdapter1/ARunningMotor01```
  
* Release a single motor resource instance (just using the path):
  * ```curl -i -X DELETE http://localhost:8080/native/api/resources/ADeployedMotorAdapter1/ARunningMotor01```
  * Response should be HTTP 200 + motor instance release RDF description
  
* Configure a single motor resource instance using attached RDF description:
  * ```curl -i -X POST -d @configureMotor.ttl http://localhost:8080/native/api/resources/ADeployedMotorAdapter1```
  * Response should be HTTP 200 + motor instance updated properties RDF description

* Configure a two motor resource instances at the same time using attached RDF description:
  * ```curl -i -X POST -d @configureManyMotors.ttl http://localhost:8080/native/api/resources/ADeployedMotorAdapter1 ```
  * Response should be HTTP 200 + motor instances updated properties RDF description
 
* Configure "Motor1" instance so it becomes dynamic:
  * ```curl -i -X POST -d @configureDynamicMotorTrue.ttl http://localhost:8080/native/api/resources/ADeployedMotorAdapter1 ```
  * Response should be HTTP 200 + motor instances updated properties RDF description
  * A motor resource instance that is configured with the property isDynamic = true will randomly change its RPM property every 5 seconds and send a corresponding notification (fitealge:inform Message).
  * Open the log viewer to see those notifications. Alternatively keep requesting the motor instances details using GET (see above) to see the updated RPM values. Also keep refreshing the FUSEKI web interface to see the live updates made in the repository.
  
* Configure "Motor1" instance so it it no longer dynamic:
  * ```curl -i -X POST -d @configureDynamicMotorFalse.ttl http://localhost:8080/native/api/resources/ADeployedMotorAdapter1 ```


### Example: Stopwatch Adapter

Essentially the same commands as the motor adapter, but instead of the name "ADeployedMotorAdapter1" use "ADeployedStopwatchAdapter1".

A stopwatch resource has three properties:

 * currentTime: The time since starting the stopwatch.
 * refreshInterval: The time between notifications of currenTime (while the stopwatch has been started and running).
 * isRunning: Set to true to start the stopwatch and false to stop it. 


Experimenting via Adapter Manager Web GUI
------------------------------------------

Open the Adapter Manager Web GUI ( [http://localhost:8080/native/gui/admin/adapter-manager.html](http://localhost:8080/native/gui/admin/adapter-manager.html). Choose the adapter to manage in the list and click "Manage Adapter" on the right.

This will show a list of all currently managed resource instances.

##### Create:
Create a new resource instance by using the box at the top. Fill in the boxes with the required data types and click create.
##### Monitor:
Monitor resource instances automatically. All notifications from the message bus (fiteagle:inform Messages) are automatically evaluated and the corresponding values refreshed live in the Web GUI. For example, creating new instances via curl REST calls will automatically show them in the Web GUI without manually refreshing. Updated values from dynamic resource properties (motor RPM, stopwatch currentTime) are updated live as well.
##### Release:
Release a single resource instance by clicking the release button on the right.
##### Configure
Configure a resource by clicking the configure button on the right. The resource's properties will become editable. Enter the new values and click apply to apply the changes or click cancel to reset to the original properties.


408 Timeout Problem
-----------------------------
Whenever a command via REST API or Web GUI leads to a 408 Timeout or seemingly no response, just refresh the page (Web GUI) or do the operation again (REST API). Usually the command has been executed, but the response has not been registered correctly. This is a bug that needs to be fixed.


Implemented HTTP Status Codes
-----------------------------

* PUT create an already existing resource: 400
* GET discover a non-existing resource: 404
* GET/PUT/POST/DELETE discover from a non-existing adapter: 404
* POST configure a non-existing resource: 400
* DELETE an non-existing resource: 404
* Timeout: 408
  


LodLive visualization
---------------------

To browse through the data open http://localhost:8080/native/gui/lodlive/index.html?http://fiteagleinternal%23FITEAGLE_Testbed
Keep in mind that the testbed adapter has to be deployed.
You can start at every positin in  the graph stored at the repo by replacing the  http://fiteagleinternal%23FITEAGLE_Testbed part of the url with a known resource.
E.g. http://localhost:8080/native/gui/lodlive/index.html?http://fiteagleinternal%23ADeployedMotorAdapter1 when it is deployed.
