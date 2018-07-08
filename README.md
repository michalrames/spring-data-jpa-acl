# spring-data-jpa-acl
Provides the entities required for manipulating spring ACL tables using JPA. 

## Why this was forked
Adds foregin key names matching those of sql script embedded in the ACL jars. This allows retrofitting of the JPA entities into ACL on MySQL previously implemented via spring-acl only.

Spring ACL database setup scripts define the foregin key names (https://github.com/spring-projects/spring-security/blob/master/acl/src/main/resources/createAclSchemaMySQL.sql) 

## TODO:
* Implement dynamic or configurable foregin key names to match other database types (https://github.com/spring-projects/spring-security/tree/master/acl/src/main/resources)
