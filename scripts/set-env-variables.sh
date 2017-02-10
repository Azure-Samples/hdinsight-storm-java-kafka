# Used to print out usage information
usage() {
  echo >&2 "$@"
}

# Check that the basename and password were provided 
[ "$#" -eq 2 ] || usage "2 arguments (the Kafka cluster name and password) are required, $# provided."
# Save to variables if they were provided
KAFKANAME=$1
PASSWORD=$2

# Install jq if not already installed
PKG_OK=`dpkg-query -W --showformat='${Status}\n' jq | grep "install ok installed"`
echo Checking for jq: $PKG_OK
if [ "" == "$PKG_OK" ]; then
  echo "No jq. Installing jq."
  sudo apt -y install jq
fi

echo 'Exporting variables:'

export KAFKATOPIC=stormtest
echo '$KAFKATOPIC='$KAFKATOPIC

export KAFKABROKERS=`curl --silent -u admin:$PASSWORD -G https://$KAFKANAME.azurehdinsight.net/api/v1/clusters/$KAFKANAME/services/HDFS/components/DATANODE | jq -r '["\(.host_components[].HostRoles.host_name):9092"] | join(",")'`
echo '$KAFKABROKERS='$KAFKABROKERS

export KAFKAZKHOSTS=`curl --silent -u admin:$PASSWORD -G https://$KAFKANAME.azurehdinsight.net/api/v1/clusters/$KAFKANAME/services/ZOOKEEPER/components/ZOOKEEPER_SERVER | jq -r '["\(.host_components[].HostRoles.host_name):2181"] | join(",")'`
echo '$KAFKAZKHOSTS='$KAFKAZKHOSTS