---
services: hdinsight
platforms: java
author: blackmist
---

# Use Apache Kafka with Apache Storm on HDInsight

This is a basic example of reading and writing string data to a Kafka on HDInsight cluster from Storm on HDInsight cluster.

__NOTE__: Apache Kafka and Storm are available as two different cluster types. HDInsight cluster types are tuned for the performance of a specific technology; in this case, Kafka and Storm. To use both together, you must create an Azure Virtual Network and then create both a Kafka and Storm cluster on the virtual network. For an example on how to do this using an Azure Resource Manager template, see the [create-kafka-storm-clusters-in-vnet.json](create-kafka-storm-clusters-in-vnet.json) document. For an example of using the template with this example, see [Use Apache Storm with Kafka on HDInsight](https://docs.microsoft.com/azure/hdinsight/hdinsight-apache-storm-with-kafka).

## Understanding the code

This project contains two topologys:

* __KafkaWriter__: Defined by the __writer.yaml__ file, this topology writes random sentences to Kafka using the KafkaBolt provided with Apache Storm.

    This topology uses a custom __SentenceSpout__ component to generate random sentences.

* __KafkaReader__: Defined by the __reader.yaml__ file, this topology reads data from Kafka using the KafkaSpout provided with Apache Storm, then logs the data to stdout.

    This topology uses a custom __LoggerBolt__ component to log data read from Kafka.

### Flux

The topologies are defined using [Flux](https://storm.apache.org/releases/0.10.0/flux.html). Flux was introduced in Storm 0.10.x (available on HDInsight version 3.3 and 3.4,) and allows you to separate the topology configuration from the code. For Topologies that use the Flux framework, the topology is defined in a YAML file. This can be included as part of the topology, or can be specified when you submit the topology to the Storm server. Flux also supports variable substitution at run-time, which is used in this example.

Both topologies use the `dev.properties` file at run time to provide the values for the following entries in the topologies:

* `${kafka.topic}`: The name of the Kafka topic that the topologies read/write to.

* `${kafka.broker.hosts}`: The hosts that the Kafka brokers run on. The broker information is used by the KafkaBolt when writing to Kafka.

* `${kafka.zookeeper.hosts}`: The hosts that Zookeeper runs on in the Kafka cluster.

## To use

NOTE: These steps assume that you have a functioning Java development environment, including Maven 3.x

1. Create an Azure Virtual Network that contains a Storm and Kafka cluster. Both clusters must be HDInsight 3.6.

2. Download this repository

4. From a command line in the project directory, use the following to build and package the topology.

        mvn clean package

    This will create a file named KafkaTopology-1.0-SNAPSHOT.jar in the target directory.

5. Copy the KafkaTopology-1.0-SNAPSHOT.jar file to your Storm on HDInsight cluster. For example, `scp KafkaTopology-1.0-SNAPSHOZT.jar myname@mystormcluster-ssh.azurehdinsight.net:`

6. Find the Zookeeper and Broker hosts for the **Kafka** cluster. The following examples show how to do this using PowerShell and Curl:

    * Brokers:

        ```powershell
        $creds = Get-Credential -UserName "admin" -Message "Enter the HDInsight login"
        $clusterName = Read-Host -Prompt "Enter the Kafka cluster name"
        $resp = Invoke-WebRequest -Uri "https://$clusterName.azurehdinsight.net/api/v1/clusters/$clusterName/services/KAFKA/components/KAFKA_BROKER" `
            -Credential $creds
        $respObj = ConvertFrom-Json $resp.Content
        $brokerHosts = $respObj.host_components.HostRoles.host_name
        ($brokerHosts -join ":9092,") + ":9092"
        ```

        ```bash
        curl -su admin -G "https://$CLUSTERNAME.azurehdinsight.net/api/v1/clusters/$CLUSTERNAME/services/KAFKA/components/KAFKA_BROKER" | jq -r '["\(.host_components[].HostRoles.host_name):9092"] | join(",")'
        ```

    * Zookeeper:

        ```powershell
        $creds = Get-Credential -UserName "admin" -Message "Enter the HDInsight login"
        $clusterName = Read-Host -Prompt "Enter the Kafka cluster name"
        $resp = Invoke-WebRequest -Uri "https://$clusterName.azurehdinsight.net/api/v1/clusters/$clusterName/services/ZOOKEEPER/components/ZOOKEEPER_SERVER" `
            -Credential $creds
        $respObj = ConvertFrom-Json $resp.Content
        $zookeeperHosts = $respObj.host_components.HostRoles.host_name
        ($zookeeperHosts -join ":2181,") + ":2181"
        ```

        ```bash
        curl -su admin -G "https://$CLUSTERNAME.azurehdinsight.net/api/v1/clusters/$CLUSTERNAME/services/ZOOKEEPER/components/ZOOKEEPER_SERVER" | jq -r '["\(.host_components[].HostRoles.host_name):2181"] | join(",")'
        ```

7. Modify the `dev.properties` file to add the Zookeeper and Broker values from the previous step. Just copy and paste the string of comma-delimited fully qualified domain names that were returned.

8. Upload the `dev.properties` file to the Storm cluster.

8. Connect to Storm cluster using SSH. For example, `ssh sshuser@clustername-ssh.azurehdinsight.net`.

9. Use the following command to start the writer topology:

        storm jar KafkaTopology-1.0-SNAPSHOT.jar org.apache.storm.flux.Flux --remote -R /writer.yaml --filter dev.properties

    The parameters used with this command are:

    * __org.apache.storm.flux.Flux__: Use Flux to configure and run this topology.
    * __--remote__: Submit the topology to Nimbus. This runs the topology in a distributed fashion using the worker nodes in the cluster.
    * __-R /writer.yaml__: Use the __writer.yaml__ to configure the topology. `-R` indicates that this is a resource that is included in the jar file. It's in the root of the jar, so `/writer.yaml` is the path to it.
    * __--filter dev.properties__: Use the contents of dev.properties when starting the topology. This allows Flux to pick up the Kafka broker, Zookeeper, and topic values from the `dev.properties`. file. Flux uses these values in the reader.yaml file in place of the `${...}` entries. For example, `${kafka.topic}` is replaced by the value of `kafka.topic:` from the `dev.properties` file.

10. Once the topology has started, use the following command from the SSH connection to view messages written to the __stormtopic__ topic:

    __NOTE__: Replace `$KAFKAZKHOSTS` with the Zookeeper host information for the __Kafka__ cluster.

         /usr/hdp/current/kafka-broker/bin/kafka-console-consumer.sh --zookeeper $KAFKAZKHOSTS --from-beginning --topic stormtopic

    This will start listing the random sentences that the topology is writing to the topic. Use Ctrl-c to stop the script.

10. use the following command to start the reader topology:

        storm jar KafkaTopology-1.0-SNAPSHOT.jar org.apache.storm.flux.Flux --remote -R /reader.yaml --filter dev.properties

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


## Troubleshooting

By default, Kafka on HDInsight does not enable the automatic creation of Kafka topics. This example assumes that you have configured Kafka to enable automatic topic creation; the Azure Resource Manager Template included in this project demonstrates how to enable this using a template:

```json
"kafka-broker": {
    "auto.create.topics.enable": "true"
}
```

If you have not enabled the auto-creation of topics, then you must manually create the topic before starting the Storm topologies. For information on creating Kafka topics, see the [Start with Kafka on HDInsight](https://docs.microsoft.com/azure/hdinsight/kafka/apache-kafka-get-started) document.