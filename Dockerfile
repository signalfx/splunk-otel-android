
FROM openjdk:11.0.11-9-jdk

WORKDIR /app

RUN apt-get --quiet update --yes
RUN apt-get --quiet install --yes wget tar unzip lib32stdc++6 lib32z1
RUN wget --quiet --output-document=android-sdk.zip https://dl.google.com/android/repository/commandlinetools-linux-7302050_latest.zip
RUN unzip -d android-sdk-linux android-sdk.zip
RUN echo y | android-sdk-linux/cmdline-tools/bin/sdkmanager --sdk_root=. "platforms;android-30" >/dev/null
RUN echo y | android-sdk-linux/cmdline-tools/bin/sdkmanager --sdk_root=. "platform-tools" >/dev/null
RUN echo y | android-sdk-linux/cmdline-tools/bin/sdkmanager --sdk_root=. "build-tools;30.0.3" >/dev/null
RUN export ANDROID_SDK_ROOT=/app
RUN export PATH=$PATH:$PWD/platform-tools/

RUN android-sdk-linux/cmdline-tools/bin/sdkmanager --sdk_root=. --licenses
RUN git clone https://github.com/signalfx/splunk-otel-android.git

WORKDIR /app/splunk-otel-android

RUN git checkout -t origin/test_publishing
RUN touch local.properties

RUN ANDROID_SDK_ROOT=/app ./gradlew tasks

COPY build.gradle /app/splunk-otel-android
COPY splunk-otel-android/build.gradle /app/splunk-otel-android/splunk-otel-android

RUN echo "" >> gradle.properties
RUN echo "org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1024m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8" >> gradle.properties
RUN echo "gradle.daemon=false" >> gradle.properties

RUN ANDROID_SDK_ROOT=/app ./gradlew -x test build

RUN echo 'export ANDROID_SDK_ROOT=/app' > do_signing.sh
# RUN echo './gradlew -x test -PsigningKeyId=${GPG_KEY_ID} -PsigningInMemoryKey=$GPG_SECRET_KEY -PsigningInMemoryKeyPassword=$GPG_PASSWORD' signMavenPublication >> ./do_signing.sh

RUN echo './gradlew -x test signMavenPublication' >> ./do_signing.sh
RUN chmod 755 /app/splunk-otel-android/do_signing.sh


CMD './do_signing.sh'
