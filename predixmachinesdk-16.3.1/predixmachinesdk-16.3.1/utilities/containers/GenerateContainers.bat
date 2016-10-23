@echo off

rem  Copyright (c) 2012-2016 General Electric Company. All rights reserved.
rem  The copyright to the computer software herein is the property of
rem  General Electric Company. The software may be used and/or copied only
rem  with the written permission of General Electric Company or in accordance
rem  with the terms and conditions stipulated in the agreement/contract
rem  under which the software has been supplied

setlocal EnableDelayedExpansion

set MACHINE_BUILD_VERSION=16.3.1

rem Input Parameters
set CONTAINER_TYPE="default"
set CUSTOM_IMAGE_FILE=
set DOCKERIZE=false
set ECLIPSE_PATH=
set DOCKER_HOST=
set DOCKER_FTP_PROXY=
set DOCKER_HTTP_PROXY=
set DOCKER_HTTPS_PROXY=
set DOCKER_NO_PROXY=


if exist "%UserProfile%\.mbsruntime" (
    echo The file %UserProfile%\.mbsruntime must not exist. Please rename or remove it.
    goto :EOF
)

echo.
echo Parsing arguments ...
:ParseArguments
    if "%1"=="" goto EndParseArguments
    if "%1"=="-c" (
       set CONTAINER_TYPE="%~2"
       rem echo CONTAINER_TYPE="%~2"
       shift
       if "%~2"=="CUSTOM" (
          set CUSTOM_IMAGE_FILE="%~3"
          rem echo CUSTOM_IMAGE_FILE="%~3"
          shift
       )
    ) else if "%1"=="-d" (
        set DOCKERIZE=true
        rem echo DOCKERIZE=true
    ) else if "%1"=="-e" (
        set ECLIPSE_PATH="%~2"
        rem echo ECLIPSE_PATH="%~2"
        shift
    ) else if "%1"=="-h" (
        call :Usage
        exit /b 0
    ) else if "%1"=="--docker_host" (
        set DOCKER_HOST="%~2"
        rem echo DOCKER_HOST="%~2"
        shift
    ) else if "%1"=="--arch" (
        set ARCHITECTURE="%~2"
        rem echo ARCHITECTURE="%~2"
        shift
    ) else if "%1"=="--ftp_proxy" (
        set DOCKER_FTP_PROXY="%~2"
        rem echo DOCKER_FTP_PROXY="%~2"
        shift
    ) else if "%1"=="--http_proxy" (
        set DOCKER_HTTP_PROXY="%~2"
        rem echo DOCKER_HTTP_PROXY="%~2"
        shift
    ) else if "%1"=="--https_proxy" (
        set DOCKER_HTTPS_PROXY="%~2"
        rem echo DOCKER_HTTPS_PROXY="%~2"
        shift
    ) else if "%1"=="--no_proxy" (
        set DOCKER_NO_PROXY="%~2"
        rem echo DOCKER_NO_PROXY="%~2"
        shift
    ) else (
        call :PrintError "Invalid command %1"
        call :Usage
        exit /b 1
    ) 
    shift
    goto ParseArguments
:EndParseArguments


echo CONTAINER_TYPE=%CONTAINER_TYPE%
echo CUSTOM_IMAGE_FILE=%CUSTOM_IMAGE_FILE%
echo DOCKERIZE=%DOCKERIZE%
echo ECLIPSE_PATH=%ECLIPSE_PATH%
echo DOCKER_HOST=%DOCKER_HOST%
echo DOCKER_FTP_PROXY=%DOCKER_FTP_PROXY%
echo DOCKER_HTTP_PROXY=%DOCKER_HTTP_PROXY%
echo DOCKER_HTTPS_PROXY=%DOCKER_HTTPS_PROXY%
echo DOCKER_NO_PROXY=%DOCKER_NO_PROXY%


:ValidateEnvironment
    echo.
    echo Validating environment ...

    if [%ECLIPSE_PATH%]==[] (
        call :PrintError "Eclipse path required"
        call :Usage
        exit /b 1
    )

    if not exist %ECLIPSE_PATH% (
        call :PrintError "Eclipse archive %ECLIPSE_PATH% not found"
        exit /b 1
    )
    
    if not [%CONTAINER_TYPE%]==["AGENT"] (
        if not [%CONTAINER_TYPE%]==["AGENT_DEBUG"] (
            if not [%CONTAINER_TYPE%]==["CUSTOM"] (
                if not [%CONTAINER_TYPE%]==["DEBUG"] (
                    if not [%CONTAINER_TYPE%]==["PROV"] (
                        if not [%CONTAINER_TYPE%]==["TECH"] (
                            if not [%CONTAINER_TYPE%]==["default"] (
                                call :PrintError "Invalid container type %CONTAINER_TYPE%"
                                call :Usage
                                exit /b 1
                            )
                        )
                    )
                )
            )
        )
    )    

    if [%CONTAINER_TYPE%]==["CUSTOM"] (
        if [%CUSTOM_IMAGE_FILE%]==[] (
            call :PrintError "Custom image file required"
            call :Usage
            exit /b 1
        )
        if not exist "%CUSTOM_IMAGE_FILE%" (
            call :PrintError "Custom image file %CUSTOM_IMAGE_FILE% not found"
            exit /b 1
        )
    )

    if [%DOCKERIZE%]==[true] (
        where docker >nul 2>nul
        if !errorlevel!==1 (
            call :PrintError "Docker not found"
            exit /b 1
        )
    )

    echo Environment OK 
:EndValidateEnvironment


:GenerateMachine
    echo.
    echo Generating Predix Machine ...

    if [%CONTAINER_TYPE%]==["AGENT"] (
        set CONTAINER_NAME=agent
        set MACHINE_FOLDER=PredixMachine-agent-%MACHINE_BUILD_VERSION%
        call mvn clean install -Declipse.archive="%ECLIPSE_PATH%"
        call mvn exec:exec -Dimage.file=tid_predix-agent.img -Doutput.folder=PredixMachine-agent-%MACHINE_BUILD_VERSION%
        echo Removing files not supported in agent ... 
        rmdir /q /s PredixMachine-agent-%MACHINE_BUILD_VERSION%\yeti >nul 2>nul
        rmdir /q /s PredixMachine-agent-%MACHINE_BUILD_VERSION%\bin\service_installation >nul 2>nul

    ) else if [%CONTAINER_TYPE%]==["AGENT_DEBUG"] (
        set CONTAINER_NAME=agent-debug
        set MACHINE_FOLDER=PredixMachine-agent-debug-%MACHINE_BUILD_VERSION%
        call mvn clean install -Declipse.archive="%ECLIPSE_PATH%"
        call mvn exec:exec -Dimage.file=tid_predix-agent-debug.img -Doutput.folder=PredixMachine-agent-debug-%MACHINE_BUILD_VERSION%
        echo Removing files not supported in agent ... 
        rmdir /q /s PredixMachine-agent-debug-%MACHINE_BUILD_VERSION%\yeti >nul 2>nul
        rmdir /q /s PredixMachine-agent-debug-%MACHINE_BUILD_VERSION%\bin\service_installation >nul 2>nul

    ) else if [%CONTAINER_TYPE%]==["PROV"] (
        set CONTAINER_NAME=provision
        set MACHINE_FOLDER=PredixMachine-provision-%MACHINE_BUILD_VERSION%
        call mvn clean install -Declipse.archive="%ECLIPSE_PATH%"
        call mvn exec:exec -Dimage.file=tid_predix-provision.img -Doutput.folder=PredixMachine-provision-%MACHINE_BUILD_VERSION%

    ) else if [%CONTAINER_TYPE%]==["DEBUG"] (
        set CONTAINER_NAME=debug
        set MACHINE_FOLDER=PredixMachine-debug-%MACHINE_BUILD_VERSION%
        call mvn clean install -Declipse.archive="%ECLIPSE_PATH%"
        call mvn exec:exec -Dimage.file=tid_predix-debug.img -Doutput.folder=PredixMachine-debug-%MACHINE_BUILD_VERSION%

    ) else if [%CONTAINER_TYPE%]==["TECH"]  (
        set CONTAINER_NAME=technician
        set MACHINE_FOLDER=PredixMachine-technician-%MACHINE_BUILD_VERSION%
        call mvn clean install -Declipse.archive="%ECLIPSE_PATH%"
        call mvn exec:exec -Dimage.file=tid_predix-tech.img -Doutput.folder=PredixMachine-technician-%MACHINE_BUILD_VERSION%

    ) else if [%CONTAINER_TYPE%]==["CUSTOM"] (
        for /f %%i in (%CUSTOM_IMAGE_FILE%) do set BN=%%~nxi
        for %%F in (%CUSTOM_IMAGE_FILE%) do set DN=%%~dpF
        set CONTAINER_NAME="!BN!"
        set MACHINE_FOLDER="!DN!"
        echo  mvn exec:exec -Dimage.file="!BN!" -Doutput.folder="PredixMachine-!BN!-%MACHINE_BUILD_VERSION%" -Dimage.folder="!DN!" 
        call mvn clean install -Declipse.archive="%ECLIPSE_PATH%"
        call mvn exec:exec -Dimage.file="!BN!" -Doutput.folder="PredixMachine-!BN!-%MACHINE_BUILD_VERSION%" -Dimage.folder="!DN!"

    ) else if [%CONTAINER_TYPE%]==["default"]  (
        set CONTAINER_NAME=default
        set MACHINE_FOLDER=PredixMachine-%MACHINE_BUILD_VERSION%
        call mvn clean install -Declipse.archive="%ECLIPSE_PATH%"
        call mvn exec:exec -Dimage.file=tid_predix-release.img -Doutput.folder=PredixMachine-%MACHINE_BUILD_VERSION%
    )
    
    if exist %MACHINE_FOLDER% (
        echo Generated Predix Machine %MACHINE_FOLDER%
    ) else (
        call :PrintError "Generate Predix Machine failed"
        exit /b 1
    )
:EndGenerateMachine

rem Invoke DockerizeContainer.bat to create a compressed Docker image for the generated Predix Machine
:DockerizeMachine
    if [%DOCKERIZE%]==[false] (
        exit /b 0
    )

    rem Construct command
    set COMMAND=DockerizeContainer.bat -m .\%MACHINE_FOLDER% --container_name %CONTAINER_NAME% --tar_name PredixMachine
    if not [%DOCKER_HOST%]==[] (
        set COMMAND=%COMMAND% --docker_host %DOCKER_HOST%
    )
    if not [%ARCHITECTURE%]==[] (
        set COMMAND=%COMMAND% --arch %ARCHITECTURE%
    )
    if not [%DOCKER_FTP_PROXY%]==[] (
        set COMMAND=%COMMAND% --ftp_proxy %DOCKER_FTP_PROXY%
    )
    if not [%DOCKER_HTTP_PROXY%]==[] (
        set COMMAND=%COMMAND% --http_proxy %DOCKER_HTTP_PROXY%
    )
    if not [%DOCKER_HTTPS_PROXY%]==[] (
        set COMMAND=%COMMAND% --https_proxy %DOCKER_HTTPS_PROXY%
    )
    if not [%DOCKER_NO_PROXY%]==[] (
        set COMMAND=%COMMAND% --no_proxy %DOCKER_NO_PROXY%
    )

    rem Execute command
    rem echo Calling command: %COMMAND%
    call %COMMAND%

    goto :EOF
:EndDockerizeMachine


:PrintError
    echo.
    echo ####################  E R R O R ######################
    echo %~1
    echo ######################################################
    goto :EOF
 
:Usage
    echo.
    echo NAME:
    echo    GenerateContainers - Generate a Predix Machine container
    echo.
    echo USAGE:
    echo    GenerateContainers [OPTIONS]
    echo.
    echo EXAMPLES:
    echo    GenerateContainers -e C:\eclipse-jee-mars-R-win32-x86_64.zip -c PROV
    echo    GenerateContainers -e %%ECLIPSE_PATH%% -c CUSTOM /Users/user1/myMachine.img
    echo    GenerateContainers -e %%ECLIPSE_PATH%% -c AGENT -d --docker_host myDockerHost --arch x86_64 --http_proxy http://proxy-src.research.ge.com:8080 --https_proxy http://proxy-src.research.ge.com:8080 --no_proxy "localhost,127.0.0.1,*.ge.com"
    echo. 
    echo REQUIRED:
    echo    -e ^<ECLIPSE_PATH^>                    Path of downloaded Eclipse archive
    echo.
    echo OPTIONS:
    echo    -c ^<CONTAINER_TYPE^>                  Type of Predix Machine container to create
    echo    -d                                   Creates Predix Machine as a Docker image in a tar.gz file
    echo.
    echo DOCKER OPTIONS (only used with -d):
    echo    --docker_host ^<DOCKER_HOST^>          Name of Docker host to use, default to 'default'
    echo    --arch ^<ARCHITECTURE^>                The target architecture for the docker image. Default: x86_64
    echo    --ftp_proxy ^<FTP_PROXY_SERVER^>       FTP proxy server setting for Dockerized Predix Machine
    echo    --http_proxy ^<PROXY_SERVER^>          HTTP proxy server setting for Dockerized Predix Machine
    echo    --https_proxy ^<PROXY_SERVER^>         HTTPS proxy server setting for Dockerized Predix Machine
    echo    --no_proxy ^<PROXY_EXCEPTIONS^>        A set of comma-separated domains that do not go through the proxy
    echo.
    echo CONTAINER TYPES:
    echo    [not specified]                      Predefined Predix Machine
    echo    AGENT                                Predefined Predix Machine with agent support (docker)
    echo    AGENT_DEBUG                          Predefined Predix Machine with agent support (docker) with Web Console
    echo    DEBUG                                Predefined Predix Machine with Web Console
    echo    PROV                                 Predefined Predix Machine with provisioning support
    echo    TECH                                 Predefined Predix Machine with Technician Console    
    echo    CUSTOM ^<IMAGE_FILE_FULL_PATH^>        Customized Predix Machine with user-provided image (.img)
    echo.
    goto :EOF
