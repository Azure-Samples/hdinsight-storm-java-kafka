---
services: hdinsight
platforms: java
author: blackmist
---

# Use Apache Kafka with Apache Storm on HDInsight

This is a basic example of reading and writing string data to a Kafka on HDInsight cluster from Storm on HDInsight cluster.

__NOTE__: Apache Kafka and Storm are available as two different cluster types. HDInsight cluster types are tuned for the performance of a specific technology; in this case, Kafka and Storm. To use both together, you must create an Azure Virtual Network and then create both a Kafka and Storm cluster on the virtual network. For an example on how to do this using an Azure Resource Manager template, see [https://hditutorialdata.blob.core.windows.net/armtemplates/create-linux-based-kafka-storm-cluster-in-vnet.json](https://hditutorialdata.blob.core.windows.net/armtemplates/create-linux-based-kafka-storm-cluster-in-vnet.json). For an example of using the template with this example, see [Use Apache Storm with Kafka on HDInsight (preview)](https://docs.microsoft.com/azure/hdinsight/hdinsight-apache-storm-with-kafka).

## Understanding the code

This project contains two topologys:

* __KafkaWriter__: Defined by the __writer.yaml__ file, this topology writes random sentences to Kafka using the KafkaBolt provided with Apache Storm.

    This topology uses a custom __SentenceSpout__ component to generate random sentences.

* __KafkaReader__: Defined by the __reader.yaml__ file, this topology reads data from Kafka using the KafkaSpout provided with Apache Storm, then logs the data to stdout.

    This topology uses a custom __LoggerBolt__ component to log data read from Kafka.

### Flux

The topologies are defined using [Flux](https://storm.apache.org/releases/0.10.0/flux.html). Flux is new with Storm 0.10.x (available on HDInsight version 3.3 and 3.4,) and allows you to separate the topology configuration from the code. For Topologies that use the Flux framework, the topology is defined in a YAML file. This can be included as part of the topology, or can be specified when you submit the topology to the Storm server. Flux also supports variable substitution at run-time, which is used in this example.

Both topologies read several environment variables at run-time to determine what the Kafka topic name is, as well as your Zookeeper and Kafka broker hosts. The following are the environment variables that the topologies use:

* __KAFKATOPIC__: The name of the Kafka topic that the topologies read/write to.
* __KAFKABROKERS__: The hosts that the kafka brokers are running on. This is used by the KafkaBolt when writing to Kafka.
* __KAFKAZKHOSTS__: The Zookeeper hosts. Used by the KafkaReader component to read messages from a topic.

## To use

NOTE: These steps assume that you have a functioning Java development environment, including Maven 3.x

1. Create a Storm on HDInsight cluster 3.3 or 3.4 (Linux-based)

2. Download this repository

4. From a command line in the project directory, use the following to build and package the topology.

        mvn clean package

    This will create a file named KafkaTopology-1.0-SNAPSHOT.jar in the target directory.

5. Copy the KafkaTopology-1.0-SNAPSHOT.jar file to your Storm on HDInsight cluster. For example, `scp KafkaTopology-1.0-SNAPSHOZT.jar myname@mystormcluster-ssh.azurehdinsight.net:`

6. Also copy the `set-env-variables.sh` script from the scripts directory:

        scp ./scripts/set-env-variables.sh
        myname@mystormcluster-ssh.azurehdinsight.net:

7. Connect to your Kafka on HDInsight cluster. For example, `ssh myname@mykafkacluster-ssh.azurehdinsight.net`. Once connected, use the `set-env-variables.sh` script to set the environment variables used by the Storm topology:

        chmod +x set-env-variables.sh
        . ./set-env-variables.sh KAFKACLUSTERNAME PASSWORD
    
    Replace __KAFKACLUSTERNAME__ with the name of the Kafka cluster. Replace __PASSWORD__ with the admin login password for the Kafka cluster. The output of the script is similar to the following:

        Checking for jq: install ok installed
        Exporting variables:
        $KAFKATOPIC=stormtest
        $KAFKABROKERS=wn0-storm.4rf4ncirvydube02fuj0gpxp4e.ex.internal.cloudapp.net:9092,wn1-storm.4rf4ncirvydube02fuj0gpxp4e.ex.internal.cloudapp.net:9092
        $KAFKAZKHOSTS=zk1-storm.4rf4ncirvydube02fuj0gpxp4e.ex.internal.cloudapp.net:2181,zk3-storm.4rf4ncirvydube02fuj0gpxp4e.ex.internal.cloudapp.net:2181,zk5-storm.4rf4ncirvydube02fuj0gpxp4e.ex.internal.cloudapp.net:2181

8. Use the following command to start the writer topology:

        storm jar KafkaTopology-1.0-SNAPSHOT.jar org.apache.storm.flux.Flux --remote -R /writer.yaml -e

    The parameters used with this command are:

    * __org.apache.storm.flux.Flux__: Use Flux to configure and run this topology.
    * __--remote__: Submit the topology to Nimbus. This runs the topology in a distributed fashion using the worker nodes in the cluster.
    * __-R /writer.yaml__: Use the __writer.yaml__ to configure the topology. `-R` indicates that this is a resource that is included in the jar file. It's in the root of the jar, so `/writer.yaml` is the path to it.
    * __-e__: Use environment variable substitution. This allows Flux to pick up the $KAFKABROKERS and $KAFKATOPIC values you set previously, and use them in the reader.yaml file in place of the `${ENV-KAFKABROKER}` and `${ENV-KAFKATOPIC}` entries.

9. Once the topology has started, use the following command from the SSH connection to the Kafka cluster to view messages written to the __stormtest__ topic:

         /usr/hdp/current/kafka-broker/bin/kafka-console-consumer.sh --zookeeper $KAFKAZKHOSTS --from-beginning --topic stormtest

    This will start listing the random sentences that the topology is writing to the topic. Use Ctrl-c to stop the script.

10. use the following command to start the reader topology:

        storm jar KafkaTopology-1.0-SNAPSHOT.jar org.apache.storm.flux.Flux --remote -R /reader.yaml -e

11. Once the topology starts, it should start logging information similar to the following to the Storm log:

        2016-11-04 17:47:14.907 c.m.e.LoggerBolt [INFO] Received data: four score and seven years ago
        2016-11-04 17:47:14.907 STDIO [INFO] the cow jumped over the moon
        2016-11-04 17:47:14.908 c.m.e.LoggerBolt [INFO] Received data: the cow jumped over the moon
        2016-11-04 17:47:14.911 STDIO [INFO] snow white and the seven dwarfs
        2016-11-04 17:47:14.911 c.m.e.LoggerBolt [INFO] Received data: snow white and the seven dwarfs
        2016-11-04 17:47:14.932 STDIO [INFO] snow white and the seven dwarfs
        2016-11-04 17:47:14.932 c.m.e.LoggerBolt [INFO] Received data: snow white and the seven dwarfs
        2016-11-04 17:47:14.969 STDIO [INFO] an apple a day keeps the doctor away
        2016-11-04 17:47:14.970 c.m.e.LoggerBolt [INFO] Received data: an apple a day keeps the doctor away

    To view this information, use the Storm UI for the cluster (https://CLUSTERNAME.azurehdinsight.net/stormui) and drill down into the LoggerBolt component for the topology

13. Use the following command to stop the writer:

        storm stop kafka-writer
        storm stop kafka-reader
