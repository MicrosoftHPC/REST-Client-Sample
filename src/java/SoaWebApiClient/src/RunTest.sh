#/bin/bash

if [ "$#" = 0 ] ; then
    echo Usage: to compile,  run: ./RunTest.sh src
    echo Usage: to run test, run: ./RunTest.sh run
    exit 0
fi

JAVA_HOME=/etc/jdk1.6.0_23

PATH=$PATH:$JAVA_HOME/bin:/home

if [ "$1" = "src" ] ; then
	"$JAVA_HOME/bin/javac" SoaWebApiClient.java SoaWebApiClientTest.java
fi

if [ "$1" = "run" ] ; then
	"$JAVA_HOME/bin/java" SoaWebApiClientTest -Dhttp.proxyHost=157.60.216.30 -Dhttp.proxyPort=80
fi
