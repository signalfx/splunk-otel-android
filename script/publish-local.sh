#!/bin/bash
echo "⬆️  Creating Maven Local Snapshot..."
chars='abcdefghijklmnopqrstuvwxyz'
n=10
str=
for ((i = 0; i < n; ++i)); do
    str+=${chars:RANDOM%${#chars}:1}
done

./gradlew publishReleasePublicationToMavenLocal -Pmaven.deploy.artifactory.snapshot=$str

echo "Done. Press any key to exit."
read  -n 1
done