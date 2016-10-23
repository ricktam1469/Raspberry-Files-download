@ECHO OFF
setlocal

REM
REM   Copyright (c) 2012-2015 General Electric Company. All rights reserved.
REM
REM   The copyright to the computer software herein is the property of
REM   General Electric Company. The software may be used and/or copied only
REM   with the written permission of General Electric Company or in accordance
REM   with the terms and conditions stipulated in the agreement/contract
REM   under which the software has been supplied.
REM
REM   Script for starting Http Tunnel Server

REM If Java runtime is not in your search path, set it here:
REM SET JAVA="C:\Program Files\Java\jdk1.7.0_10"

REM Make sure java is in the search path
FOR %%F IN (java.exe) DO IF NOT EXIST %%~$PATH:F (
    ECHO Error: java.exe not found in current search path.
    ECHO Make sure that JRE is correctly installed and that you have
    ECHO java.exe in your search path.
    GOTO ENDSCRIPT
)

REM Build classpath
set CLASSPATH=lib/*;.

REM Application to be run
set APP_NAME=com.ge.dspmicro.httptunnel.server.app.ServerHttpTunnelServerMain

REM Params: Keystore file location, keystore type
set KEYSTORE_FILE=
set KEYSTORE_TYPE=

REM Run publish
java -classpath %CLASSPATH% %APP_NAME% %KEYSTORE_FILE% %KEYSTORE_TYPE%

:ENDSCRIPT: