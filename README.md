[![Build Status](https://travis-ci.org/FITeagle/native.svg?branch=master)](https://travis-ci.org/FITeagle/native)

Native FITeagle Interfaces
==========================

Installation
------------

    mvn clean wildfly:deploy


Graphical User Interface
------------------------

    http://localhost:8080/native/gui/admin

Resource Repository
-------------------

Requirements

    The 'api' module must be available
    The 'resourcerepository' core module must be deployed.

List resources via REST API

    curl -H "Accept: application/ld+json" http://localhost:8080/native/api/rest/resources
    curl -H "Accept: text/turtle" http://localhost:8080/native/api/rest/resources
    curl -H "Accept: application/ld+json" http://localhost:8080/native/api/rest/resources/testMe
    curl -H "Accept: text/turtle" http://localhost:8080/native/api/rest/resources/anotherTest
    
Usermanagement
-------------------

### Requirements

  The 'api' module must be available.
  
  The 'usermanagement', 'aaa' and 'config' core modules must be deployed.

### API Calls

#### Get a user

Request:

    curl -v -k --request GET "http://localhost:8080/native/api/user/mnikolaus" --user mnikolaus:mitja

#### Add a new user
  
Request:

    curl -v -k --request PUT "http://localhost:8080/native/api/user/mnikolaus" --data @exampleUser.json -H "Content-type: application/json"

Data:

    {"firstName":"mitja","lastName":"nikolaus","password":"mitja","email":"mnikolaus@test.de","affiliation":"exampleAffiliation"}

#### Update a user

Request:

    curl -v -k --request POST "http://localhost:8080/native/api/user/mnikolaus" --data @exampleUserUpdate.json -H "Content-type: application/json" --user mnikolaus:mitja

Data:

    {"firstName":"hans","email":"hans@test.de"}

Possible attributes: firstName, lastName, email, affiliation, password, publicKeys (this will delete all old keys, if you want to add more keys, use the /pubkey method instead)

#### Change the role of a user

Request:

    curl -k -v --request POST "http://localhost:8080/native/api/user/mnikolaus/role/ADMIN" --user admin:admin

this will change the role of user "mnikolaus" to ADMIN (can only be done by authenticating as an admin)

#### Delete a user

Request:

    curl -v -k --request DELETE "http://localhost:8080/native/api/user/mnikolaus" --user mnikolaus:mitja

#### Add a new publickey

Request:

    curl -v -k --request POST "http://localhost:8080/native/api/user/mnikolaus/pubkey" --data @anotherKey.json -H "Content-type: application/json" --user mnikolaus:mitja
  
Data:

    {"publicKeyString":"ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAAgQCLq3fDWATRF8tNlz79sE4Ug5z2r5CLDG353SFneBL5z9Mwoub2wnLey8iqVJxIAE4nJsjtN0fUXC548VedJVGDK0chwcQGVinADbsIAUwpxlc2FGo3sBoGOkGBlMxLc/+5LT1gMH+XD6LljxrekF4xG6ddHTgcNO26VtqQw/VeGw== RSA-1024","description":"key1"}

#### Delete a publickey
  
Request:

    curl -v -k --request DELETE "http://localhost:8080/native/api/user/mnikolaus/pubkey/key1" --user mnikolaus:mitja

#### Rename a publickey (changes the "description" of the key)

Request:

    curl -v -k --request POST "http://localhost:8080/native/api/user/mnikolaus/pubkey/key1/description" --data "my new description" -H "Content-type: text/plain" --user mnikolaus:mitja

#### Create new keypair and get private key + certificate (if the passphrase is left empty, the private key won't be encrypted)
  
Request:

    curl -v -k --request POST "http://localhost:8080/native/api/user/mnikolaus/certificate" --data "mypassphrase" -H "Content-type: text/plain" --user mnikolaus:mitja

#### Create and retrieve a certificate for an existing public key
 
Request:

    curl -v -k --request GET "http://localhost:8080/native/api/user/mnikolaus/pubkey/key1/certificate" --user mnikolaus:mitja

#### Delete cookie and invalidate session (to logout)

Request:

    curl -v -k --request DELETE "http://localhost:8080/native/api/user/mnikolaus/cookie" --user mnikolaus:mitja

#### Authentication:

On the first GET-Request the Server always sends a JSESSIONID which can be used to authenticate further requests
Additionally, if the Queryparameter setCookie is set to true, the server sends a "fiteagle_user_cookie" which can be used for a "remember me" functionality (it is valid for 1 year).
The request should then look like this:

    curl -v -k --request GET "http://localhost:8080/native/api/user/mnikolaus?setCookie=true" --user mnikolaus:mitja
 