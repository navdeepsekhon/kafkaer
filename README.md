# kafkaer
<a href="https://circleci.com/gh/navdeepsekhon/kafkaer" target="_blank">![CircleCI](https://circleci.com/gh/navdeepsekhon/kafkaer.svg?style=svg)</a>
<a href="https://search.maven.org/search?q=g:%22co.navdeep%22%20AND%20a:%22kafkaer%22" target="_blank">![Maven Central](https://img.shields.io/maven-central/v/co.navdeep/kafkaer.svg?label=Maven%20Central)</a>

## Table of Contents
 - [Overview](#overview)
 - [Integrate with your project](#two-ways-to-use)
 - [Define configurations](#kafka-configjson)
    - [Topic configurations](#topics)
    - [Broker configurations](#brokers)
    - [ACL configurations](#acls)
 - [Properties file](#properties-file)
 - [Kafka connection configurations](#admin-client-configs)
 - [Delete created topics (--wipe)](#delete-created-topics)
 - [Delete schemas from schema registry (--wipe-schemas)](#delete-schemas)
 - [Debug (--debug)](#debug)
 - [Preserve Partition Count (--preserve-partition-count)](#preserve-partition-count)
 - [Contributions](#contributions)

 
# Overview
Kafkaer is a deployment and configuration tool for Apache Kafka. It allows you to automate creation/update of topics and brokers across multiple environments. Create one template configration file and control using different properties files.



Current features:
* Create topics
* Update configurations and partitions for existing topics
* Update configs for a specific broker
* Update configs for entire kafka cluster
* Create/update Access control lists (ACLs)
* Delete all topics created by tool
* Delete all schemas from schema registry when deleting topics


# Two ways to use:
## Executable jar
Get the jar from [releases](https://github.com/navdeepsekhon/kafkaer/releases)
```
java -jar kafkaer.jar --properties propertiesLocation --config configLocation
```

## Include jar as dep in project from maven central
Gradle:
```json
compile "co.navdeep:kafkaer:1.1"
```
Maven:
```xml
<dependency>
    <groupId>co.navdeep</groupId>
    <artifactId>kafkaer</artifactId>
    <version>1.1</version>
</dependency>
```

And use it:
```java
Configurator configurator = new Configurator("src/main/resources/your.properties", "src/main/resources/kafka-config.json");
configurator.applyConfig();
```
###### The jar published to maven is also an executable.

# kafka-config.json
## Example:
```json
{
  "topics": [
    {
      "name": "withSuffix-${topic.suffix}",
      "partitions": 3,
      "replicationFactor": 3,
      "description": "This description is just for documentation. It does not affect the kafka cluster.",
      "configs": {
        "compression.type": "gzip",
        "cleanup.policy": "delete",
        "delete.retention.ms": "86400000"
      }
    },
    {
      "name": "test",
      "partitions": 1,
      "replicationFactor": 1,
      "configs": {
        "compression.type": "gzip",
        "cleanup.policy": "compact"
      }
    }
  ],
    "brokers": [
      {
        "id": "1",
        "config": {
          "sasl.login.refresh.window.jitter": "0.05"
        }
      }
    ],
    "aclStrings": [
        "User:joe,Topic,LITERAL,test,Read,Allow,*",
        "User:jon,Cluster,LITERAL,kafka-cluster,Create,Allow,*"
      ]
}

```

## Topics:
A list of topics. Required for each topic:
```json
name,
partitions,
replicationFactor
```

Rest of all the configs go inside the `configs` map. You can specify any/all of the [topic configurations listed in the kafka documentation](https://kafka.apache.org/documentation/#topicconfigs)

`description` is optional. It is just for documentation purpose and is ignored by kafkaer.

## What if the topic already exists:
### Partitions:
If the partitions listed in the config are more than the existing partitions - topic partitions will be increased to the number.

If the partitions listed in config are less than the existing - an exception will be thrown.

If they are same - nothing.

If flag `--preserve-partition-count` is used, partitions will not be updated.

### All other configs:
All other configs will be updated to the new values from config.

## Brokers
A list of broker configs.

NOTE: If a broker id is provided, the update is made only on that broker. If no broker id is provided update is sent to each broker in the cluster. [See kafka documentation for all broker configs](https://kafka.apache.org/documentation/#brokerconfigs)

Cluster-wide configs must be without an id.

## ACLs
You can provide the ACLs to create in one of two formats:

Structured list:
```json
"acls" : [
    {
      "principal": "User:joe",
      "resourceType": "Topic",
      "patternType": "LITERAL",
      "resourceName": "test",
      "operation": "Read",
      "permissionType": "Allow",
      "host": "*"
    },
    {
          "principal": "User:jon",
          "resourceType": "Cluster",
          "patternType": "LITERAL",
          "resourceName": "kafka-cluster",
          "operation": "Create",
          "permissionType": "Allow",
          "host": "*"
        }
  ]
```

As a list of strings:
```json
//Format: "principal,resourceType,patternType,resourceName,operation,permissionType,host"

"aclStrings": [
    "User:joe,Topic,LITERAL,test,Read,Allow,*",
    "User:jon,Cluster,LITERAL,kafka-cluster,Create,Allow,*"
  ]
```

All the values are case insensitive.

## Variables in kafka-config.json 
To allow for deployments across different environments, kafka-config.json allows you to specify variables for values that will be replaced with values from the properties file. In the example above the topic name `withSuffix-${topic.suffix}` will be replaced with `withSuffix-iamasuffix` using the value of `topic.suffix` from props. 

Why is it useful?

Use case 1: You want to setup multiple instances of your application on same kafka cluster. You can name all your topics with `${topic.suffix}` and use different value for each instance `john`, `jane` etc.

Use case 2: You might need 50 partitions for your topics in production but only 3 for dev. You create two properties files with different values and use the same `kafka-config.json`.

# Properties file
Standard java properties file.
```json
#admin client configs
kafkaer.bootstrap.servers=localhost:29092
kafkaer.client.id=kafkaer

#variables
topic.suffix=iamasuffix
```

# Admin Client configs
Kafkaer uses `AdminClient` API to connect to Kafka.
All the admin client configs can be provided in the same properties file. Property name must have prefix `kafkaer.` followed by one of `AdminClientConfig`. For example, to specify `bootstrap.servers` add a property called `kafkaer.bootstrap.servers`. All the admin client configs are supported. [See the list of configs here](https://github.com/apache/kafka/blob/trunk/clients/src/main/java/org/apache/kafka/clients/admin/AdminClientConfig.java)

# Delete created topics

Provide the `--wipe` flag to delete all the topics listed in the config.json

# Delete Schemas

If you're using [confluent schema registry](https://docs.confluent.io/current/schema-registry/develop/api.html) or other compatible schema registry to store topic schemas, kafkaer can delete the associated schemas when deleting the topics.

Use flag `--wipe-schemas` with `--wipe` to delete schemas.

Provide the schema registry url with property `kafkaer.schema.registry.url`. Other schema registry properties can be provided by prefixing `kafkaer.`.
```
kafkaer.schema.registry.security.protocol=SSL
kafkaer.schema.registry.ssl.truststore.location=...
...
```

# Debug

Use flag `--debug` for detailed logging

# Preserve Partition Count

If a topic already exists and it's partition count is different from what is defined in the config, kafkaer will try update the partitions as described above. In order to ignore the partition count and keep the existing partitions, `--preserve-partition-count` flag can be used. When used, the difference is partition count will only be logged.

# Contributions
Merge requests welcome. Please create an issue with change details and link it to your merge request.

Note: This project uses [lombok](https://projectlombok.org/). Please install the plugin for your IDE to avoid compilation errors.
