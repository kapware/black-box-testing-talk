# "Black boxy" testing approach using test containers

This is a show case how to set up tests using test containers in a "black boxy" style

## Running
Specify TESTED_IMAGE env variable to point to a docker image of the blackbox to be tested (tests are prepared for xenia-api: https://github.com/TorunJUG/xenia-api)
```
export TESTED_IMAGE=xenia-api:0.0.1
mvn failsafe:integration-test
```
