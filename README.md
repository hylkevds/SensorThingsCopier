# SensorThingsCopier
A simple program that copies observations from one OGC SensorThings API compatible
service/Datastream to another service/Datastream.

## Use
Simply run it from the command line, or using cron.

`java -jar SensorThingsCopier-1.0.jar`

If it finds no configuration file, it generates an example configuration file.
The default configuration file name is configuration.json, but a different file
name can be passed as the first argument.

## Configuration
The configuration file is a json file.
```
{
  "sourceService" : "http://source.server:8080/SensorThingsService/v1.0/",
  "targetService" : "http://target.server:80/SensorThingsService/v1.0/",
  "dataStreamCombos" : [ {
    "sourceDatastreamId" : 94,
    "targetDatastreamId" : 405,
    "lastCopiedId" : 0
  }, {
    "sourceDatastreamId" : 96,
    "targetDatastreamId" : 407,
    "lastCopiedId" : 0
  } ]
}
```
The config file is updated after each run, with the new IDs of the last copied observations.
