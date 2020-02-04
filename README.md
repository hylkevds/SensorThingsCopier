# SensorThingsCopier [![Build Status](https://travis-ci.org/FraunhoferIOSB/SensorThingsCopier.svg?branch=master)](https://travis-ci.org/FraunhoferIOSB/SensorThingsCopier)
A simple program that copies observations from one OGC SensorThings API compatible
service/Datastream to another service/Datastream.

## Use
Simply run it from the command line, or using cron.

`java -jar SensorThingsCopier-1.0.jar`

If it finds no configuration file, it generates an example configuration file.
The default configuration file name is configuration.json, but a different file
name can be passed as the first argument.

## Configuration
The config file is updated after each run, with the new IDs of the last copied observations.
To edit the configuration file, use
`java -cp SensorThingsCopier-0.2-jar-with-dependencies.jar de.fraunhofer.iosb.ilt.stc.ConfigGui`
