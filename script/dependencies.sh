#!/bin/bash

MODULES=()

while IFS= read -r line; do
    MODULES+=("$line")
done < <(./gradlew projects | grep -- '--- Project ' | sed "s/.*Project '\(.*\)'/\1/")

CONFIGS_TO_TRY=("implementation" "api")
MODULES_TO_IGNORE=(":app" ":instrumentation:buildtime:httpurlconnection-auto:plugin" ":instrumentation:buildtime:okhttp3-auto:plugin")

ALL_DEPENDENCIES=()

echo "Module dependencies"

for module in "${MODULES[@]}"; do
    should_ignore=false

    for ignored_module in "${MODULES_TO_IGNORE[@]}"; do
      if [[ "$module" == "$ignored_module" ]]; then
        should_ignore=true
        break
      fi
    done

    if [ "$should_ignore" = true ]; then
       continue
    fi

    headerPrinted=false

    for config in "${CONFIGS_TO_TRY[@]}"; do
        OUTPUT=$(./gradlew "${module}:dependencies" --configuration "$config" 2>&1)

        if [[ "$OUTPUT" == *"Configuration '${config}' not found"* ]]; then
            continue
        fi

        DEPENDENCIES=$(echo "$OUTPUT" | grep -- '[\\+]--- ' | grep -v 'project ' | sed 's/.*--- //;s/ ->.*//;s/ (n)//')

        if [ -n "$DEPENDENCIES" ]; then
            if [ "$headerPrinted" = false ]; then
              headerPrinted=true
              echo ""
              echo "$module"
            fi

            while IFS= read -r line; do
                echo "  $line"
                ALL_DEPENDENCIES+=("$line")
            done <<< "$DEPENDENCIES"
        fi
    done
done

ALL_DEPENDENCIES_UNIQUE=()
while IFS= read -r line; do
  ALL_DEPENDENCIES_UNIQUE+=("$line")
done < <(printf "%s\n" "${ALL_DEPENDENCIES[@]}" | sort -u)

echo ""
echo "All dependencies"
printf "  %s\n" "${ALL_DEPENDENCIES_UNIQUE[@]}"
echo ""

echo "Done. Press any key to exit."
read  -n 1