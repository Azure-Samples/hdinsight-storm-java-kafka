---
services: hdinsight
platforms: java
author: blackmist
---

# Use Apache Kafka with Apache Storm on HDInsight

This is a basic example of reading and writing string data to Kafka on a Linux-based Storm on HDInsight cluster.

__NOTE__: Apache Kafka is currently provided as a preview with Linux-based Storm on HDInsight clusters.

## Understanding the code

This project contains two topologys:

* __KafkaWriter__: Defined by the __writer.yaml__ file, this topology writes random sentences to Kafka using the KafkaBolt provided with Apache Storm.

    This topology uses a custom __SentenceSpout__ component to generate random sentences.

* __KafkaReader__: Defined by the __reader.yaml__ file, this topology reads data from Kafka using the KafkaSpout provided with Apache Storm, then logs the data to stdout.

    This topology uses a custom __PrinterBolt__ component to log data read from Kafka.

### Flux

The topologies are defined using [Flux](https://storm.apache.org/releases/0.10.0/flux.html). Flux is new with Storm 0.10.x (available on HDInsight version 3.3 and 3.4,) and allows you to separate the topology configuration from the code. For Topologies that use the Flux framework, the topology is defined in a YAML file. This can be included as part of the topology, or can be specified when you submit the topology to the Storm server. Flux also supports variable substitution at run-time, which is used in this example.

Both topologies read several environment variables at run-time to determine what the Kafka topic name is, as well as your Zookeeper and Kafka broker hosts. The following are the environment variables that the topologies use:

* __KAFKATOPIC__: The name of the Kafka topic that the topologies read/write to.
* __KAFKABROKERS__: The hosts that the kafka brokers are running on. This is used by the KafkaBolt when writing to Kafka.

    Each worker node in the HDInsight cluster runs a Kafka broker. To set this environment variable, you can use the following statement from an SSH session to a cluster head node:

        export KAFKABROKERS=`sudo bash -c 'ls /var/lib/ambari-agent/data/command-[0-9]*.json' | tail -n 1 | xargs sudo cat | jq -r '["\(.clusterHostInfo.kafka_broker_hosts[]):9092"] | join(",")'`

    This reads the latest `command-*.json` file from `/var/lib/ambari-agent/data`, and uses the `jq` utility to pull out the broker hosts. The port that the broker runs on (9092) is appended to each of the host names.

    > [AZURE.IMPORTANT] The Kafka broker runs on all worker nodes in a cluster. Since worker nodes can be added and removed by scaling your cluster, the list of brokers will be changed by scaling operations. There doesn't appear to be a way to dynamically update the list used by KafkaBolt other than stopping the topology and then starting again with the new list of brokers.

* __ZKHOSTS__: The Zookeeper hosts. Used by the KafkaReader component to read messages from a topic.

    To set this environment variable, you can use the following statement from an SSH session to a cluster head node:

        export ZKHOSTS=`grep -R zk /etc/hadoop/conf/yarn-site.xml | grep 2181 | grep -oPm1 "(?<=<value>)[^<]+"`
    
    This reads the `yarn-site.xml` file, extracting entries that contain a port of 2181 (Zookeeper.)

## To use

NOTE: These steps assume that you have a functioning Java development environment, including Maven 3.x

1. Create a Storm on HDInsight cluster 3.3 or 3.4 (Linux-based)

2. Download this repository

4. From a command line in the project directory, use the following to build and package the topology.

        mvn clean package

    This will create a file named KafkaTopology-1.0-SNAPSHOT.jar in the target directory.

5. Copy the KafkaTopology-1.0-SNAPSHOT.jar file to your Stom on HDInsight cluster. For example, `scp KafkaTopology-1.0-SNAPSHOZT.jar myname@myhdinsight-ssh.azurehdinsight.net:`

6. Connect to HDInsight using SSH. Once connected, use the following to install jq, and then export a few environment variables that will be used by the topology:

        wget https://github.com/stedolan/jq/releases/download/jq-1.5/jq-linux64 --output-document jq
        chmod +x jq
        sudo mv jq /usr/bin

        export KAFKATOPIC=stormtest
        export KAFKABROKERS=`sudo bash -c 'ls /var/lib/ambari-agent/data/command-[0-9]*.json' | tail -n 1 | xargs sudo cat | jq -r '["\(.clusterHostInfo.kafka_broker_hosts[]):9092"] | join(",")'`
        export ZKHOSTS=`grep -R zk /etc/hadoop/conf/yarn-site.xml | grep 2181 | grep -oPm1 "(?<=<value>)[^<]+"`

7. Next, create a Kafka topic. The topologies will write, and then read from this.

        /usr/hdp/current/kafka-broker/bin/kafka-topics.sh --create --replication-factor 2 --partitions 8 --topic $KAFKATOPIC --zookeeper $ZKHOSTS

8. Use the following command to start the writer topology:

        storm jar KafkaTopology-1.0-SNAPSHOT.jar org.apache.storm.flux.Flux --remote -R /writer.yaml -e

    The parameters used with this command are:

    * __org.apache.storm.flux.Flux__: Use Flux to configure and run this topology.
    * __--remote__: Submit the topology to Nimbus. This runs the topology in a distributed fashion using the worker nodes in the cluster.
    * __-R /writer.yaml__: Use the __writer.yaml__ to configure the topology. `-R` indicates that this is a resource that is included in the jar file. It's in the root of the jar, so `/writer.yaml` is the path to it.
    * __-e__: Use environment variable substitution. This allows Flux to pick up the $KAFKABROKERS and $KAFKATOPIC values you set previously, and use them in the reader.yaml file in place of the `${ENV-KAFKABROKER}` and `${ENV-KAFKATOPIC}` entries.

9. Once the topology has started, use the following command to view messages written to the __input__ topic:

         /usr/hdp/current/kafka-broker/bin/kafka-console-consumer.sh --zookeeper $ZKHOSTS --from-beginning --topic $KAFKATOPIC

    This will start listing the random sentences that the topology is writing to the topic. Use Ctrl-c to stop the script.

10. use the following command to start the reader topology:

        storm jar KafkaTopology-1.0-SNAPSHOT.jar org.apache.storm.flux.Flux --local -R /reader.yaml -e

    The parameters used with this command are:

    * __org.apache.storm.flux.Flux__: Use Flux to configure and run this topology.
    * __--local__: Submit the topology to in local mode. This runs the topology locally on the head node. Usually this is used for testing; in this case, we're using it so we can easily see the data that the topology logs to stdout as it reads data from Kafka.
    * __-R /reader.yaml__: Use the __writer.yaml__ to configure the topology. `-R` indicates that this is a resource that is included in the jar file. It's in the root of the jar, so `/writer.yaml` is the path to it.
    * __-e__: Use environment variable substitution. This allows Flux to pick up the $ZKHOSTS and $KAFKATOPIC values you set previously, and use them in the writer.yaml file in place of the `${ENV-ZKHOSTS}` and `${ENV-KAFKATOPIC}` entries.

11. Once the topology starts, it should start logging information similar to the following:

        18:15:22.521 [Thread-24-printerbolt] INFO  c.m.e.PrinterBolt - Received data: i am at two with nature
        18:15:22.528 [Thread-24-printerbolt] INFO  c.m.e.PrinterBolt - Received data: four score and seven years ago
        18:15:22.530 [Thread-24-printerbolt] INFO  c.m.e.PrinterBolt - Received data: the cow jumped over the moon
        18:15:22.531 [Thread-36-printerbolt] INFO  c.m.e.PrinterBolt - Received data: four score and seven years ago
        18:15:22.533 [Thread-24-printerbolt] INFO  c.m.e.PrinterBolt - Received data: four score and seven years ago
        18:15:22.535 [Thread-36-printerbolt] INFO  c.m.e.PrinterBolt - Received data: i am at two with nature
        18:15:22.537 [Thread-16-printerbolt] INFO  c.m.e.PrinterBolt - Received data: the cow jumped over the moon
        18:15:22.538 [Thread-16-printerbolt] INFO  c.m.e.PrinterBolt - Received data: four score and seven years ago

12. Use Ctrl+c to stop the topology.

13. Use the following command to stop the writer:

        storm stop kafka-writer
