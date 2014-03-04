Native FITeagle Interfaces
==========================

Resource Repository
-------------------

0. Requirements: The 'resourcerepository' core module must be deployed.

1. List resources via Message Driven Bean

  curl http://localhost:8080/native/repo/mdb/resources.rdf
  curl http://localhost:8080/native/repo/mdb/resources.ttl

2. List resources via Enterprise Java Bean

  curl http://localhost:8080/native/repo/ejb/resources.rdf
  curl http://localhost:8080/native/repo/ejb/resources.ttl
