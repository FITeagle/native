[![Build Status](https://travis-ci.org/FITeagle/native.svg?branch=master)](https://travis-ci.org/FITeagle/native)

Native FITeagle Interfaces
==========================

Installation
------------

    mvn clean install wildfly:deploy


Graphical User Interface
------------------------

    http://localhost:8080/native/gui/admin

LodLive Visualisation
---------------------

To view e.g. the visualisation starting from the resource "http://federation.av.tu-berlin.de/about#MotorGarage-1":

    http://localhost:8080/native/gui/lodlive/index.html?http://federation.av.tu-berlin.de/about%23MotorGarage-1

Resource Repository
-------------------

### Requirements

    The 'api' module must be available
    The 'repo' core module and the modules of the adapters you want to use must be deployed.

### API Calls

#### List all resources

    curl -H "Accept: text/turtle" http://localhost:8080/native/api/resources
    curl -H "Accept: application/ld+json" http://localhost:8080/native/api/resources
    
#### Describe a specific resource
    
    curl -H "Accept: text/turtle" http://localhost:8080/native/api/resources/MotorGarage-1
    curl -H "Accept: application/ld+json" http://localhost:8080/native/api/resources/MotorGarage-1
    
#### Get all resource instances managed by an adapater
 
    curl -H "Accept: text/turtle" http://localhost:8080/native/api/resources/MotorGarage-1/instances
    curl -H "Accept: application/ld+json" http://localhost:8080/native/api/resources/MotorGarage-1/instances
    
#### Create new resources

    curl -k -v --request PUT --data @instancesDescription.ttl https://localhost:8443/native/api/resources/
    
#### Configure resources

    curl -k -v --request POST --data @instancesConfigureDescription.ttl https://localhost:8443/native/api/resources/

#### Release resources
  
    curl -k -v --request DELETE --data @instancesToDelete.ttl https://localhost:8443/native/api/resources/
    
Usermanagement
-------------------

### Requirements

    The 'api', 'aaa' and 'config' modules must be available.
    The 'usermanagement' module must be deployed.

### API Calls

When this module is being deployed for the first time, a new node is being created (name: TU Berlin) and a new admin user is being created (username: admin, password: admin).

Most of the following example calls are made to a user with the username 'test' and the password 'test'.

#### Add a new user
  
Request:

    curl -v -k --request PUT "https://localhost:8443/native/api/user/test" --data @exampleUser.json -H "Content-type: application/json"

Data:

    {"firstName":"test","lastName":"testlastname","password":"test","email":"test@test.de","affiliation":"exampleAffiliation","node":{"id":"1"}}}
    
If no node is specified, the user will be added to a default node.

#### Get a user

Request:

    curl -v -k --request GET "https://localhost:8443/native/api/user/test" --user test:test
    
#### Get all classes of a user

Request:

    curl -v -k --request GET "https://localhost:8443/native/api/user/test/classes" --user test:test
    
#### Get all classes owned by a user

Request:

    curl -v -k --request GET "https://localhost:8443/native/api/user/test/ownedclasses" --user test:test
    
#### Get all users

Request:

    curl -v -k --request GET "https://localhost:8443/native/api/user" --user admin:admin


#### Update a user

Request:

    curl -v -k --request POST "https://localhost:8443/native/api/user/test" --data @exampleUserUpdate.json -H "Content-type: application/json" --user test:test

Data:

    {"firstName":"hans","email":"hans@test.de"}

Possible attributes: firstName, lastName, email, affiliation, password, publicKeys (this will delete all old keys, if you want to add more keys, use the 'add publickey' method instead)

#### Change the role of a user

Request:

    curl -k -v --request POST "https://localhost:8443/native/api/user/test/role/FEDERATION_ADMIN" --user admin:admin

This will change the role of user 'test' to FEDERATION_ADMIN (can only be done by authenticating as an admin).

#### Add a new class

Request: 

	curl -k -v --request PUT "https://localhost:8443/native/api/class" --user classowner:classowner --data @exampleClass.json -H "Content-type: application/json"
    
Data:

    {"name":"Test Class","description":"a class for testing purposes","owner":{"username":"classowner"},"nodes":[{"id":"1"}]}

As a result you will get the ID of the created class. If no node is specified, the class will be added to the node of the classowner.

#### Get a class

Request:

    curl -v -k --request GET "https://localhost:8443/native/api/class/1" --user test:test

#### Get all classes

Request:

    curl -v -k --request GET "https://localhost:8443/native/api/class" --user test:test

#### Delete a class

Request:

    curl -v -k --request DELETE "https://localhost:8443/native/api/class/1" --user classowner:classowner

#### Sign up for a class

Request: 

	curl -k -v --request POST "https://localhost:8443/native/api/user/test/class/1" --user test:test
    
This will sign up the user 'test' for the class with the id '1'.

#### Leave a class

Request: 

	curl -k -v --request DELETE "https://localhost:8443/native/api/user/test/class/1" --user test:test
    
This will make the user 'test' leave the class with the id '1'.

#### Delete a user

Request:

    curl -v -k --request DELETE "https://localhost:8443/native/api/user/test" --user test:test

#### Add a new publickey

Request:

    curl -v -k --request POST "https://localhost:8443/native/api/user/test/pubkey" --data @anotherKey.json -H "Content-type: application/json" --user test:test
  
Data:

    {"publicKeyString":"ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAAgQCLq3fDWATRF8tNlz79sE4Ug5z2r5CLDG353SFneBL5z9Mwoub2wnLey8iqVJxIAE4nJsjtN0fUXC548VedJVGDK0chwcQGVinADbsIAUwpxlc2FGo3sBoGOkGBlMxLc/+5LT1gMH+XD6LljxrekF4xG6ddHTgcNO26VtqQw/VeGw== RSA-1024","description":"key1"}

#### Delete a publickey
  
Request:

    curl -v -k --request DELETE "https://localhost:8443/native/api/user/test/pubkey/key1" --user test:test

#### Rename a publickey (changes the "description" of the key)

Request:

    curl -v -k --request POST "https://localhost:8443/native/api/user/test/pubkey/key1/description" --data "my new description" -H "Content-type: text/plain" --user test:test

#### Create new keypair and get private key + certificate 
  
Request:

    curl -v -k --request POST "https://localhost:8443/native/api/user/test/certificate" --data "mypassphrase" -H "Content-type: text/plain" --user test:test

(if the passphrase is left empty, the private key won't be encrypted)

#### Create and retrieve a certificate for an existing public key
 
Request:

    curl -v -k --request GET "https://localhost:8443/native/api/user/test/pubkey/key1/certificate" --user test:test

#### Delete cookie and invalidate session (to logout)

Request:

    curl -v -k --request DELETE "https://localhost:8443/native/api/user/test/cookie" --user test:test

#### Authentication:

On the first GET-Request the Server always sends a JSESSIONID which can be used to authenticate further requests
Additionally, if the Queryparameter setCookie is set to true, the server sends a "fiteagle_user_cookie" which can be used for a "remember me" functionality (it is valid for 1 year).
The request should then look like this:

    curl -v -k --request GET "https://localhost:8443/native/api/user/test?setCookie=true" --user test:test
 
