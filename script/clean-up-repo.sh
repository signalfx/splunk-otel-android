#!/bin/bash

echo "⬆️  Cleaning up local repo folders...."

# Delete every "repo" folder in the current directory and its subdirectories
find . -type d -name "repo" -exec rm -rf {} \;

echo "Done"