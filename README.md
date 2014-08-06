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

