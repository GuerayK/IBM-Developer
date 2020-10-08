#! 
#
# Script to run the MlpNetworkTrainer program.
#
# Program version
PROGRAM_VERSION=1.1.0
# Set DEBUG to something other than true to turn it off
DEBUG=true
# Add network.properties.file system property to JAVA_OPTS to use an external properties file.
# Lame, but at least gives you a way to run the code without having to rebuild the network
# every time you tweak the network.properties file.
# You can also specify this property in the shell (but I probably didn't need to tell you that).
#EXAMPLE_JAVA_OPTS="-Dnetwork.properties.file=/Users/sperry/home/network.properties"

function usage {
  echo "Usage: $0 "
  echo "Description: Runs the DeepLearning4J network trainer and runner"
  echo
}

## Process number of arguments
#NUMARGS=$#
#if [[ "$DEBUG" == "true" ]]; then echo -e \\n"Number of arguments: $NUMARGS"; fi
#if [[ "$NUMARGS" -eq 0 ]]; then
#  usage
#  exit 1
#fi
#if [[ "$DEBUG" == "true" ]]; then echo "Script arguments: $@"; fi

# Below is an example that works on my Mac.
# Change this to match your source location (so your .class files can be found).
ROOT_DIR=.
# Set the location of your network.properties
NETWORK_PROPERTIES_FILE=./network.properties
if [[ "$DEBUG" == "true" ]]; then echo "Network properties file: $NETWORK_PROPERTIES_FILE"; fi
JAVA_OPTS=-Dnetwork.properties.file=$NETWORK_PROPERTIES_FILE
#JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_121.jdk/Contents/Home

# Make sure ROOT_DIR is set or bail out
if [[ -z "$ROOT_DIR" ]]
then
  echo "ROOT_DIR is not set! This variable should be set to the source root of your project."
  echo "Make sure that you run a Maven build to create the necessary class files"
  echo "and library dependencies"
  exit 1
fi

if [[ "$DEBUG" == "true" ]]; then echo "ROOT_DIR = ${ROOT_DIR}"; fi

echo "JAVA_OPTS=$JAVA_OPTS"

# Fire up the program
java $JAVA_OPTS -jar $ROOT_DIR/MarchMadness-${PROGRAM_VERSION}.jar $@
