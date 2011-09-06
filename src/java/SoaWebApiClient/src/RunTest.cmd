@echo off
setlocal

if [%1]==[] (
    echo Usage: to compile,  run: RunTest.cmd src
    echo Usage: to run test, run: RunTest.cmd run
    exit /b 0
)    

set JAVA_HOME=c:\Program Files\Java\jdk1.6.0_23

if [%1]==[src] (
    "%JAVA_HOME%\bin\javac" SoaWebApiClient.java SoaWebApiClientTest.java
)
if [%1]==[run] (
    "%JAVA_HOME%\bin\java" SoaWebApiClientTest -Dhttp.proxyHost=157.60.216.30 -Dhttp.proxyPort=80
)


