
Northbound REST API
===================

Checkout Code
-------------
```
git clone -b dev --depth 1 https://github.com/FITeagle/api.git && \
git clone -b dev --depth 1 https://github.com/FITeagle/core.git && \
git clone -b dev --depth 1 https://github.com/FITeagle/native.git && \
git clone -b Dev --depth 1 https://github.com/FITeagle/adapters.git && \
git clone --depth 1 https://github.com/FITeagle/bootstrap.git
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
cd native && mvn clean verify wildfly:deploy && cd - \\
cd core/repo && mvn clean verify  wildfly:deployy && cd - \\
cd core/bus && mvn clean verify wildfly:deploy && cd -
```

Look at the logger
------------------

```
to be done
```

native/src/main/webapp/gui/admin/console2.html

To see what is stored in the repository:

http://localhost:3030/ds/query?query=SELECT+*+{%3Fs+%3Fp+%3Fo}&output=text&stylesheet=


Now run the Motor Adapter Microservice:

  ```
cd adapters/motor
mvn clean install wildfly:deploy
  ```


After deployment of the adapter there should be new log entries in the console (adapter registering) and in FUSEKI.


Start experimenting:

Use the following test RDF files from the MotorAdapter directory:

* createMotor.ttl
* createManyMotors.ttl
* configureMotor.ttl
* configureManyMotors.ttl
* configureDynamicMotor.ttl

Create a single new motor instance using RDF description:

  ```
curl -i -X PUT -d @createMotor.ttl http://localhost:8080/native/api/resources/garage/ARunningMotor1
  ```

Create a four new motor instances at once using RDF description:

  ```
curl -i -X PUT -d @createManyMotors.ttl http://localhost:8080/native/api/resources/garage/ARunningMotor1
  ```

Create a single new motor instance with no attached RDF description (just using the path):

  ```
curl -i -X PUT http://localhost:8080/native/api/resources/garage/ARunningMotor101
  ```

Release a single new motor instance with no attached RDF description (just using the path):

  ```
curl -i -X DELETE http://localhost:8080/native/api/resources/garage/ARunningMotor1
  ```

Configure a single motor instance using RDF description:

  ```
curl -i -X POST -d @configureMotor.ttl http://localhost:8080/native/api/resources/garage/ARunningMotor1
  ```

Configure a two new motor instances using RDF description:

  ```
curl -i -X POST -d @configureManyMotors.ttl http://localhost:8080/native/api/resources/garage/ARunningMotor1
  ```

Configure ARunningMotor1 instance so it becomes dynamic:

  ```
curl -i -X POST -d @configureDynamicMotor.ttl http://localhost:8080/native/api/resources/garage/ARunningMotor1
  ```

Now it will change its RPM property every 5 seconds and send inform messages over the bus. See log.
Keep refreshing FUSEKI service ( http://localhost:3030/ds/query?query=SELECT+*+{%3Fs+%3Fp+%3Fo}&output=text&stylesheet= ) to see the live updates in the repository.


To discover/describe resources in the Testbed (only motor at the moment):

  ```
curl -i -X GET http://localhost:8080/native/api/resources/
  ```

To discover/describe resources of the garage moter adapter:

  ```
curl -i -X GET http://localhost:8080/native/api/resources/garage/
  ```

To discover/describe a single resource:

  ```
curl -i -X GET http://localhost:8080/native/api/resources/garage/ARunningMotor1
  ```




