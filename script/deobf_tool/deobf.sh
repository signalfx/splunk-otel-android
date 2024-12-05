#!/bin/bash

# Check if the user requested help
usage() {
    echo "Usage: ./deobf.sh [-help] <path_to_merged_mappings_file> <path_to_crash_file>"
    echo ""
    echo "This tool deobfuscates a given crash file using ProGuard's retrace tool and outputs the result to a text file."
    echo ""
    echo "Options:"
    echo "    --help                               Display this help text and exit"
    echo ""
    echo "Arguments:"
    echo "    <path_to_merged_mappings_file>       The path to the ProGuard mappings file"
    echo "    <path_to_crash_file>                 The path to the crash file you want to deobfuscate"
    echo ""
    echo "Example:"
    echo "    ./deobf.sh /path/to/mappings.txt /path/to/crash.txt"
    echo ""
    echo "The script will create a text file named 'deobfuscation<timestamp>.txt' in the current directory, "
    echo "where <timestamp> is the current date and time in the format 'year_month_day_hour_minutes'. "
    echo "This file will contain the deobfuscated output."
    echo ""
    echo "NOTE: The script assumes that 'retrace.sh' is located at '\$ANDROID_HOME/tools/proguard/bin/retrace.sh'. "
    echo "If your 'retrace.sh' file is located elsewhere, you'll need to modify the script accordingly."
    
}
if  [ "$#" -ne 2 ] || [ "$1" == "-h" ] || [ "$1" == "--help" ]; then
    usage
    exit 1
fi

path_to_merged_mappings_file=$1
path_to_crash_file=$2

# Check if the mapping file exists
if [ ! -f "$path_to_merged_mappings_file" ]; then
    echo "Error: mapping file was not found"
    exit 1
fi

path_to_retrace=$ANDROID_HOME/tools/proguard/bin/retrace.sh

# Check if retrace.sh really exists at the given path
if [ ! -f "$path_to_retrace" ]; then
    echo "Error: retrace.sh was not found at the path: $path_to_retrace\nPlease check if ANDROID_HOME is set correctly"
    exit 1
fi

# Preprocess the crash file to trim leading whitespaces, add "at " to lines that don't start with "at ", and add four spaces before each "at "
awk '{$1=$1};1' "$path_to_crash_file" | awk '!/^at / {print "at " $0; next} 1' | awk '{gsub(/^at /, "    at ", $0); print}' > processed_file.txt

# Run retrace and echo its result
"$path_to_retrace" -verbose "$path_to_merged_mappings_file" "$(pwd)/processed_file.txt"

# Remove the processed file
rm -f processed_file.txt


