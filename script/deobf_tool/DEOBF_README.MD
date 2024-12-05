# Deobfuscation Bash Script

This bash script deobfuscates given crash files using ProGuard's retrace tool and outputs the result to a text file.

## Usage

```bash
./deobf.sh [-h] [--help] <path_to_merged_mappings_file> <path_to_crash_file>
```

## Options

- `-help` : Displays the help text and exits the script.

## Arguments

- `<path_to_merged_mappings_file>` : The path to the ProGuard mappings file.
- `<path_to_crash_file>` : The path to the crash file you want to deobfuscate.

## Example

```bash
./deobf.sh /path/to/mappings.txt /path/to/crash.txt
```

The script will create a text file named `deobfuscation<timestamp>.txt` in the current directory, where `<timestamp>` is the current date and time in the format 'year_month_day_hour_minutes'. This file will contain the deobfuscated output.

## Requirements

The script assumes that 'retrace.sh' is located at `$ANDROID_HOME/tools/proguard/bin/retrace.sh`. If your 'retrace.sh' file is located elsewhere, you'll need to modify the script accordingly.

Please note, this script will modify crash logs by adding "at " at the beginning of lines that don't already start with it, which improves the output of the retrace tool.

## Known Limitations

This script is not intended for complex use cases and does not support multiple input files at once. It is designed to be used as a simple tool to deobfuscate individual crash files.

For advanced use cases, it is recommended to use ProGuard's retrace tool directly or explore other deobfuscation tools that offer more features and flexibility.
