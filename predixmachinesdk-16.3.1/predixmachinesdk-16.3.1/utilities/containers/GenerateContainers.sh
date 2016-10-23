#!/bin/bash

# Copyright (c) 2012-2016 General Electric Company. All rights reserved."
# The copyright to the computer software herein is the property of"
# General Electric Company. The software may be used and/or copied only"
# with the written permission of General Electric Company or in accordance"
# with the terms and conditions stipulated in the agreement/contract"
# under which the software has been supplied"


#
# There must be an argument for the downloaded eclipse.tar.gz
# 
MACHINE_BUILD_VERSION=16.3.1

# Input Parameters
CONTAINER_TYPE=default
CUSTOM_IMAGE_FILE=
DOCKERIZE=false
ECLIPSE_PATH=
DOCKER_HOST=
DOCKER_FTP_PROXY=
DOCKER_HTTP_PROXY=
DOCKER_HTTPS_PROXY=
DOCKER_NO_PROXY=

# GLobals
DOCKER_FOLDER=./docker
MACHINE_FOLDER=PredixMachine-$MACHINE_BUILD_VERSION

if [ -e "$HOME/.mbsruntime" ]; then 
    echo "The file $HOME/.mbsruntime must not exist. Please rename or remove it."
    exit 1
fi

# Print usage instructions
PrintUsage () {
    echo ""
    echo "NAME:"
    echo "   GenerateContainers.sh - Generate a Predix Machine container"
    echo ""
    echo "USAGE:"
    echo "   GenerateContainers [OPTIONS]"
    echo ""
    echo "EXAMPLES:"
    echo "   sh GenerateContainers.sh -e ~/eclipse-jee-mars-4.5.0-macosx-cocoa-x86_64.tar.gz -c PROV"
    echo "   sh GenerateContainers.sh -e \$ECLIPSE_PATH -c CUSTOM /Users/user1/myMachine.img"
    echo "   sh GenerateContainers.sh -e \$ECLIPSE_PATH -c AGENT -d --docker_host myDockerHost --arch x86_64 --http_proxy http://proxy-src.research.ge.com:8080 --https_proxy http://proxy-src.research.ge.com:8080 --no_proxy localhost,127.0.0.1,*.ge.com"
    echo ""
    echo "REQUIRED:"
    echo "   -e <ECLIPSE_PATH>                    Path of downloaded Eclipse archive"
    echo ""
    echo "OPTIONS:"
    echo "   -c <CONTAINER_TYPE>                  Type of Predix Machine container to create"
    echo "   -d                                   Creates Predix Machine as a Docker image in a tar.gz file"
    echo ""
    echo "DOCKER BUILD OPTIONS (only used with -d):"    
    echo "   --docker_host <DOCKER_HOST>          Name of Docker host to use"
    echo "   --arch <ARCHITECTURE>                The target architecture for the docker image. Default: x86_64"
    echo "   --ftp_proxy <FTP_PROXY_SERVER>       FTP proxy server setting for Dockerized Predix Machine"
    echo "   --http_proxy <PROXY_SERVER>          HTTP proxy server setting for Dockerized Predix Machine"
    echo "   --https_proxy <PROXY_SERVER>         HTTPS proxy server setting for Dockerized Predix Machine"
    echo "   --no_proxy <PROXY_EXCEPTIONS>        A set of comma-separated domains that do not go through the proxy"
    echo ""
    echo "CONTAINER TYPES:"
    echo "   [not specified]                      Predefined Predix Machine"
    echo "   AGENT                                Predefined Predix Machine with agent support (docker)"
    echo "   AGENT_DEBUG                          Predefined Predix Machine with agent support (docker) with Web Console "
    echo "   DEBUG                                Predefined Predix Machine with Web Console"
    echo "   PROV                                 Predefined Predix Machine with provisioning support"
    echo "   TECH                                 Predefined Predix Machine with Technician Console"    
    echo "   CUSTOM <IMAGE_FILE_FULL_PATH>        Customized Predix Machine with user-provided image (.img)"
    echo ""
}

# Print error message and exit with status 1
PrintError () {
    echo ""
    echo "####################  E R R O R ######################"
    echo "$1"
    echo "######################################################"
}

# Parse input arguments and set globals
ParseArguments () {
    echo ""
    echo "Parsing arguments ..."
    while [[ $# -gt 0 ]]; do   
        case $1 in
            -c | --container) 
                shift
                CONTAINER_TYPE="$1"
                echo CONTAINER_TYPE=$CONTAINER_TYPE
                if [ "$CONTAINER_TYPE" = "CUSTOM" ]; then
                    shift
                    CUSTOM_IMAGE_FILE="$1"
                    echo CUSTOM_IMAGE_FILE=$CUSTOM_IMAGE_FILE
                fi
                ;;
            -d | --dockerize)
                DOCKERIZE=true
                echo DOCKERIZE=$DOCKERIZE
                ;;
            -e | --eclipse)
                shift
                ECLIPSE_PATH="$1"
                echo ECLIPSE_PATH=$ECLIPSE_PATH
                ;;
            -h | -help | --help | -usage | ?)
                PrintUsage
                exit
                ;;
            --docker_host)
                shift
                DOCKER_HOST="$1"
                echo DOCKER_HOST=$DOCKER_HOST
                ;;    
            --arch)
            	shift
            	ARCHITECTURE="$1"
            	echo ARCHITECTURE=$ARCHITECTURE
            	;;
            --ftp_proxy)
                shift
                DOCKER_FTP_PROXY="$1"
                echo DOCKER_FTP_PROXY=$DOCKER_FTP_PROXY
                ;;    
            --http_proxy)
                shift
                DOCKER_HTTP_PROXY="$1"
                echo DOCKER_HTTP_PROXY=$DOCKER_HTTP_PROXY
                ;;
            --https_proxy)
                shift
                DOCKER_HTTPS_PROXY="$1"
                echo DOCKER_HTTPS_PROXY=$DOCKER_HTTPS_PROXY
                ;;
            --no_proxy)
                shift
                DOCKER_NO_PROXY="$1"
                echo DOCKER_NO_PROXY=$DOCKER_NO_PROXY
                ;;
            *)
                PrintError "Invalid command $1"
                PrintUsage
                exit 1
        esac
        shift
    done
}

# Validate environment before generation
ValidateEnvironment () {

    echo ""
    echo "Validating environment ..."

    if [ -z "$ECLIPSE_PATH" ]; then
        PrintError "Eclipse path required"
        PrintUsage
        exit 1
    fi
    
    if [ ! -e "$ECLIPSE_PATH" ]; then 
        PrintError "Eclipse archive "$ECLIPSE_PATH" not found"
        exit 1
    fi
    
    if [[ "$CONTAINER_TYPE" != "AGENT" && 
          "$CONTAINER_TYPE" != "AGENT_DEBUG" && 
          "$CONTAINER_TYPE" != "CUSTOM" && 
          "$CONTAINER_TYPE" != "DEBUG" && 
          "$CONTAINER_TYPE" != "PROV" && 
          "$CONTAINER_TYPE" != "TECH" && 
          "$CONTAINER_TYPE" != "default" ]]; then
        PrintError "Invalid container type "$CONTAINER_TYPE""
        PrintUsage
        exit 1
    fi
    
    if [ "$CONTAINER_TYPE" = "CUSTOM" ]; then
        if [ -z "$CUSTOM_IMAGE_FILE" ]; then
            PrintError "Custom image file required"
            PrintUsage
            exit 1
        fi
        if [ ! -e "$CUSTOM_IMAGE_FILE" ]; then 
            PrintError "Custom image file "$CUSTOM_IMAGE_FILE" not found"
            exit 1
        fi
    fi
    
    if [ "$DOCKERIZE" = true ]; then
        command -v "docker" >/dev/null 2>&1 || {
            PrintError "Docker not found";
            exit 1;
        }
    fi

    echo "Environment OK"
}

# Generate Predix Machine
GenerateMachine () {

    echo ""
    echo "Generating Predix Machine ..."

    if [ "$CONTAINER_TYPE" = "AGENT" ]; then
        CONTAINER_NAME=agent
        MACHINE_FOLDER=PredixMachine-$CONTAINER_NAME-$MACHINE_BUILD_VERSION
        mvn clean install -Declipse.archive="$ECLIPSE_PATH"
        mvn exec:exec -Dimage.file=tid_predix-agent.img -Doutput.folder=$MACHINE_FOLDER 
        echo "Removing files not supported in agent ..."
        rm -r PredixMachine-agent-$MACHINE_BUILD_VERSION/yeti
        rm -r PredixMachine-agent-$MACHINE_BUILD_VERSION/bin/service_installation

    elif [ "$CONTAINER_TYPE" = "AGENT_DEBUG" ]; then
        CONTAINER_NAME=agent-debug
        MACHINE_FOLDER=PredixMachine-$CONTAINER_NAME-$MACHINE_BUILD_VERSION
        mvn clean install -Declipse.archive="$ECLIPSE_PATH"
        mvn exec:exec -Dimage.file=tid_predix-agent-debug.img -Doutput.folder=$MACHINE_FOLDER 
        echo "Removing files not supported in agent ..."
        rm -r PredixMachine-agent-debug-$MACHINE_BUILD_VERSION/yeti
        rm -r PredixMachine-agent-debug-$MACHINE_BUILD_VERSION/bin/service_installation

    elif [ "$CONTAINER_TYPE" = "CUSTOM" ]; then
        CONTAINER_NAME=$(basename $CUSTOM_IMAGE_FILE)
        MACHINE_FOLDER=PredixMachine-$CONTAINER_NAME-$MACHINE_BUILD_VERSION
        mvn clean install -Declipse.archive="$ECLIPSE_PATH"
        mvn exec:exec -Dimage.file="$(basename $CUSTOM_IMAGE_FILE)" -Doutput.folder=$MACHINE_FOLDER -Dimage.folder="$(dirname $CUSTOM_IMAGE_FILE)"

    elif [ "$CONTAINER_TYPE" = "DEBUG" ]; then
        CONTAINER_NAME=debug
        MACHINE_FOLDER=PredixMachine-$CONTAINER_NAME-$MACHINE_BUILD_VERSION
        mvn clean install -Declipse.archive="$ECLIPSE_PATH"
        mvn exec:exec -Dimage.file=tid_predix-debug.img -Doutput.folder=$MACHINE_FOLDER 

    elif [ "$CONTAINER_TYPE" = "PROV" ]; then
        CONTAINER_NAME=provision
        MACHINE_FOLDER=PredixMachine-$CONTAINER_NAME-$MACHINE_BUILD_VERSION
        mvn clean install -Declipse.archive=$ECLIPSE_PATH
        mvn exec:exec -Dimage.file=tid_predix-provision.img -Doutput.folder=$MACHINE_FOLDER

    elif [ "$CONTAINER_TYPE" = "TECH" ]; then
        CONTAINER_NAME=technician
        MACHINE_FOLDER=PredixMachine-$CONTAINER_NAME-$MACHINE_BUILD_VERSION
        mvn clean install -Declipse.archive="$ECLIPSE_PATH"
        mvn exec:exec -Dimage.file=tid_predix-tech.img -Doutput.folder=$MACHINE_FOLDER

    elif  [ "$CONTAINER_TYPE" = "default" ]; then
        CONTAINER_NAME=default
        MACHINE_FOLDER=PredixMachine-$MACHINE_BUILD_VERSION
        mvn clean install -Declipse.archive="$ECLIPSE_PATH"
        mvn exec:exec -Dimage.file=tid_predix-release.img -Doutput.folder=$MACHINE_FOLDER
    fi

    if [ -e "$MACHINE_FOLDER" ]; then
        echo "Generated Predix Machine `pwd`/$MACHINE_FOLDER"
    else
        PrintError "Generate Predix Machine failed"
        exit 1
    fi   
}

# Invoke DockerizeContainer.sh to create a compressed Docker image for the generated Predix Machine
DockerizeMachine () {

    # Construct command
    COMMAND=". DockerizeContainer.sh -m ./$MACHINE_FOLDER --container_name $CONTAINER_NAME --tar_name PredixMachine"
    if [ ! -z "$DOCKER_HOST" ]; then
        COMMAND+=" --docker_host $DOCKER_HOST"
    fi
    if [ ! -z "$ARCHITECTURE" ]; then
    	COMMAND+=" --arch $ARCHITECTURE"
    fi
    if [ ! -z "$DOCKER_FTP_PROXY" ]; then
        COMMAND+=" --ftp_proxy $DOCKER_FTP_PROXY"
    fi
    if [ ! -z "$DOCKER_HTTP_PROXY" ]; then
        COMMAND+=" --http_proxy $DOCKER_HTTP_PROXY"
    fi
    if [ ! -z "$DOCKER_HTTPS_PROXY" ]; then
        COMMAND+=" --https_proxy $DOCKER_HTTPS_PROXY"
    fi
    if [ ! -z "$DOCKER_NO_PROXY" ]; then
        COMMAND+=" --no_proxy $DOCKER_NO_PROXY"
    fi

    # Execute command
    eval $COMMAND
}

##################################################
# main()
##################################################

ParseArguments $@
ValidateEnvironment
GenerateMachine

if [ "$DOCKERIZE" = true ]; then
    DockerizeMachine
fi