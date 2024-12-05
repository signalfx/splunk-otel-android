# Running Component Test
This is a work in progress
First, start the Otel Collector by running: ```./gradlew startOtelCollectorForTests```

Then, you'll want to update the Application under test to use the otel collector IP. Make sure
you don't point at localhost because it will try to use the device localhost, not the machines.
A build config field has been added for this, ```BuildConfig.IP_ADDRESS```. Update collector URL.

This will be an automated step eventually.

Then start running the test you want. 

The Setup only includes Otel collector and Zipkin for now. 

# End-to-End Flow
The End-to-End flow is just a flow to generate data for end-to-end tests. There will be a separate
Playwright based project [here](https://bitbucket.corp.appdynamics.com/projects/MRUM_AC/repos/mrum-e2e-tests/browse) to verify the DashUI view relating to the test data.

More on that later.
