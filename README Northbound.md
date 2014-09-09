
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

Open http://localhost:8080/native/gui/admin/console2.html


Look at the repository
----------------------

Open http://localhost:3030/ds/query?query=SELECT+*+{%3Fs+%3Fp+%3Fo}&output=text&stylesheet=


Run the Motor adapter
---------------------


```
cd adapters/motor && mvn clean verify wildfly:deploy
```

After deployment of the adapter there should be new log entries in the console (adapter registering) and in FUSEKI.


Start experimenting
-------------------

Use the following test RDF files from the MotorAdapter directory.

* Create a single new motor instance using RDF description:
  * ```curl -i -X PUT -d @createMotor.ttl http://localhost:8080/native/api/resources/motor/ARunningMotor1```
* Create four new motor instances at once using RDF description:
  * ```curl -i -X PUT -d @createManyMotors.ttl http://localhost:8080/native/api/resources/motor```
* Create a single new motor instance with no attached RDF description (just using the path):
  * ```curl -i -X PUT http://localhost:8080/native/api/resources/motor/ARunningMotor101```
* Release a single new motor instance with no attached RDF description (just using the path):
  * ```curl -i -X DELETE http://localhost:8080/native/api/resources/motor/ARunningMotor1```
* Configure a single motor instance using RDF description:
  * ```curl -i -X POST -d @configureMotor.ttl http://localhost:8080/native/api/resources/motor```
* Configure a two new motor instances using RDF description:
  * ```curl -i -X POST -d @configureManyMotors.ttl http://localhost:8080/native/api/resources/motor ```
* Configure ARunningMotor1 instance so it becomes dynamic:
  * ```curl -i -X POST -d @configureDynamicMotor.ttl http://localhost:8080/native/api/resources/motor ```
  * Now it will change its RPM property every 5 seconds and send inform messages over the bus. See log. Keep refreshing FUSEKI serviceto see the live updates in the repository.
* To discover/describe resources in the Testbed (only motor at the moment):
  * ```curl -i -X GET http://localhost:8080/native/api/resources/```
* To discover/describe resources of the garage moter adapter:
  * ```curl -i -X GET http://localhost:8080/native/api/resources/motor/```
* To discover/describe a single resource:
  * ```curl -i -X GET http://localhost:8080/native/api/resources/motor/ARunningMotor1```
 

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

To browse through the data open http://localhost:8080/native/gui/lodlive/ and select a pre-defined resource from the grey box called "fiteagleinternal".
