# kafkaer
[![CircleCI](https://circleci.com/gh/navdeepsekhon/kafkaer.svg?style=svg)](https://circleci.com/gh/navdeepsekhon/kafkaer)

## Table of Contents
 - [Overview](#overview)
 - [Integrate with your project](#two-ways-to-use)
 - [Define configurations](#kafka-configjson)
    - [Topic configurations](#topics)
    - [Broker configurations](#brokers)
    - [ACL configurations](#acls)
 - [Properties file](#properties-file)
 - [Kafka connection configurations](#admin-client-configs)
 - [Contributions](#contributions)

 
# Overview
Kafkaer is a deployment and configuration tool for Apache Kafka. It allows you to automate creation/update of topics and brokers across multiple environments. 

# Two ways to use:
## Executable jar
Get the jar from [releases](https://github.com/navdeepsekhon/kafkaer/releases)
```
java -jar kafkaer.jar propertiesLocation configLocation
```

## Include jar as dep in project from maven central
Gradle:
```json
compile "co.navdeep:kafkaer:0.1"
```
Maven:
```xml
<dependency>
    <groupId>co.navdeep</groupId>
    <artifactId>kafkaer</artifactId>
    <version>0.1</version>
</dependency>
```

And use it:
```java
Configurator configurator = new Configurator("src/main/resources/your.properties", "src/main/resources/kafka-config.json");
configurator.applyConfig();
```

# kafka-config.json
## Example:
```json
{
  "topics": [
    {
      "name": "withSuffix-${topic.suffix}",
      "partitions": 3,
      "replicationFactor": 3,
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

## What if the topic already exists:
### Partitions:
If the partitions listed in the config are more than the existing partitions - topic partitions will be increased to the number.

If the partitions listed in config are less than the existing - an exception will be thrown.

If they are same - nothing.

### All other configs:
All other configs will be updated to the new values from config.

## Brokers
A list of broker configs.

NOTE: If a broker id is provided, the update is made only on that broker. If no broker id is provided update is sent to each broker in the cluster. [See kafka documentation for all broker configs](https://kafka.apache.org/documentation/#brokerconfigs)

Cluster-wide configs must be without an id.

## ACLs
coming soon

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

# Contributions
Merge requests welcome. Please create an issue with change details and link it to your merge request.

Note: This project uses [lombok](https://projectlombok.org/). Please install the plugin for your IDE to avoid compilation errors.
