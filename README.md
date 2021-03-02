# SensorThingsCopier [![Build Status](https://github.com/FraunhoferIOSB/SensorThingsCopier/workflows/Maven%20Build/badge.svg)](https://github.com/FraunhoferIOSB/SensorThingsCopier/actions)
A simple program that copies observations from one OGC SensorThings API compatible
service/Datastream to another service/Datastream.

## Use
Simply run it from the command line, or using cron.

`java -jar SensorThingsCopier-<VERSION>-jar-with-dependencies.jar`

If it finds no configuration file, it generates an example configuration file.
The default configuration file name is configuration.json, but a different file
name can be passed as the first argument.

## Configuration
The config file is updated after each run, with the new IDs of the last copied observations.
To edit the configuration file, use
`java -cp SensorThingsCopier-<VERSION>-jar-with-dependencies.jar de.fraunhofer.iosb.ilt.stc.ConfigGui`
