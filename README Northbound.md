
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
cd native && mvn clean verify wildfly:deploy && cd - && \
cd core/repo && mvn clean verify wildfly:deploy && cd - && \
cd core/bus && mvn clean verify wildfly:deploy && cd -
```


Look at the logger
------------------

To view a text version of the logger open:

http://localhost:8080/native/gui/admin/log-viewer-text.html

A list version can be found at:

http://localhost:8080/native/gui/admin/log-viewer-list.html


Look at the repository
----------------------

To see all the content of the repository open the FUSEKI web interface:

http://localhost:3030/ds/query?query=SELECT+*+{%3Fs+%3Fp+%3Fo}&output=text&stylesheet=

To see a graph visualization of the repository content open:

http://localhost:8080/native/api/lodlive/query?output=json&format=application/json&timeout=0&query=Select%20*%20%7B?s%20?p%20?o%7D&callback=lodlive


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

http://localhost:8080/native/gui/admin/adapter-manager.html

Monitor, create, terminate and configure resource adapters.


Experimenting
-------------------

Experiments (create, configure, monitor, release) can be done via direct calls to the REST API with a tool such as curl or via the Adapter Manager Web GUI ( http://localhost:8080/native/gui/admin/adapter-manager.html ).



Use the following test RDF files from the MotorAdapter directory.

Possible adapter names:

[ADAPTER_NAME]: ADeployedStopwatchAdapter1, ADeployedMotorAdapter1

[RESOURCE_NAME]: Any name you want

* Create a single new motor instance using RDF description:
  * ```curl -i -X PUT -d @createMotor.ttl http://localhost:8080/native/api/resources/[ADAPTER_NAME]/ARunningMotor1```
* Create four new motor instances at once using RDF description:
  * ```curl -i -X PUT -d @createManyMotors.ttl http://localhost:8080/native/api/resources/[ADAPTER_NAME]```
* Create a single new motor instance with no attached RDF description (just using the path):
  * ```curl -i -X PUT http://localhost:8080/native/api/resources/[ADAPTER_NAME]/ARunningMotor101```
* Release a single new motor instance with no attached RDF description (just using the path):
  * ```curl -i -X DELETE http://localhost:8080/native/api/resources/[ADAPTER_NAME]/ARunningMotor1```
* Configure a single motor instance using RDF description:
  * ```curl -i -X POST -d @configureMotor.ttl http://localhost:8080/native/api/resources/[ADAPTER_NAME]```
* Configure a two new motor instances using RDF description:
  * ```curl -i -X POST -d @configureManyMotors.ttl http://localhost:8080/native/api/resources/[ADAPTER_NAME] ```
* Configure ARunningMotor1 instance so it becomes dynamic:
  * ```curl -i -X POST -d @configureDynamicMotor.ttl http://localhost:8080/native/api/resources/[ADAPTER_NAME] ```
  * Now it will change its RPM property every 5 seconds and send inform messages over the bus. See log. Keep refreshing FUSEKI serviceto see the live updates in the repository.
* To discover/describe resources in the Testbed (only motor at the moment):
  * ```curl -i -X GET http://localhost:8080/native/api/resources/```
* To discover/describe resources of the garage moter adapter:
  * ```curl -i -X GET http://localhost:8080/native/api/resources/[ADAPTER_NAME]/```
* To discover/describe a single resource:
  * ```curl -i -X GET http://localhost:8080/native/api/resources/[ADAPTER_NAME]/ARunningMotor1```
 

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
