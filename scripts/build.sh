#!/bin/bash

cd /usr/src/project

echo "Installing Node dependencies..."
npm install
# TODO: This shouldn't be needed. Find out why it's not normally being executed after npm install
npm run-script postinstall
echo "Node dependencies installed"

echo "Installing jspm dependencies..."
jspm install --yes
echo "jspm dependencies installed"

# echo "Linking related projects..."
# echo "Linking carbonldp-panel..."
# jspm link carbonldp-panel/dist --yes > /dev/null
# echo "carbonldp-panel linked"
# echo "Related projects linked"

echo "Installing definition files..."
typings install
echo "Definition files installed"

gulp build --profile prod