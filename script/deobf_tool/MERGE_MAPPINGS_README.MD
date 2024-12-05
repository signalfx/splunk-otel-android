```markdown
# Merge Mappings Script

This script is used to merge Android SDK mappings from a specified path.

## Script Overview

The `merge_mappings.sh` script is designed to merge SDK mapping files found within a directory structure. It expects the directories to contain version subdirectories in the format "X.Y.Z" (where X, Y, and Z are integers), and that each of these version subdirectories may contain a "mapping.txt" file.

The script takes at least one parameter, which is the path where the merged mappings will be saved. It can optionally take a second path; if provided, the merged mappings will also be saved here. If no second path is provided, the merged mappings will be saved in the directory where the script resides.

## Usage

Run the script with the required output path as a parameter:

```bash
./merge_mappings.sh /path/to/directory
```

To specify a different save directory for the merged mappings, add a second path parameter:

```bash
./merge_mappings.sh /path/to/directory /different/directory
```

The script also supports a `-h` or `--help` parameter. If you use this parameter, or run the script with no parameters, it will display usage information:

```bash
./merge_mappings.sh -h
```

## Expected Directory Structure

The script expects a specific directory structure in the supplied `output_path`. It should contain a `com/smartlook/android` directory, within which there are one or more version directories in the "X.Y.Z" format. Each version directory might contain a "mapping.txt" file, the contents of which will be included in the merged mapping file.

## Output

The script generates a merged mapping file named `merged_mapping_X.Y.Z.txt`, where `X.Y.Z` is the version number, and saves it in the `output_path` directory or in the script's own directory if no second path parameter is provided.

