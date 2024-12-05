#!/bin/bash

usage() {
    echo "Usage: ./merge_mappings.sh [-h] output_path [output_path]"
    echo "Merge Android SDK mappings from a specified path"
    echo
    echo "Options:"
    echo "  -h, --help    Show help"
    echo "Arguments:"
    echo "  output_path   The path where the merged mappings will be saved"
    echo "  [output_path] Optional second path. If supplied, merged mappings will be saved here too."
}

if [ $# -eq 0 ] || [ "$1" == "-h" ] || [ "$1" == "--help" ]
then
    usage
    exit 1
fi

SCRIPT_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Parameters
path_to_output_with_mergings=$1
output_path=${2:-$SCRIPT_PATH}

modules_path="$path_to_output_with_mergings/com/smartlook/android"
merged_mapping=""
sdk_version=""

echo "Merging mappings from $modules_path to $output_path"

for directory in "$modules_path"/*; do
    if [ -d "$directory" ]; then
        for sub_directory in "$directory"/*; do
            if [[ -d "$sub_directory" ]]; then
                # Try to identify if sub_directory is a version folder
                potential_version=$(basename "$sub_directory")
                if [[ ! $potential_version =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
                   continue
                fi

                sdk_version=$potential_version
                                if [[ -f "$sub_directory/mapping.txt" ]]; then
                    merged_mapping+=$'\n'"$(cat "$sub_directory"/mapping.txt)"
                fi
            fi
        done
    fi
done

echo "Merging mappings for SDK version $sdk_version finished"
echo "$merged_mapping" >  "${output_path}/merged_mapping_${sdk_version}.txt"
