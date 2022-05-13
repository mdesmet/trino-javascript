Trino Connector
===============

[![Build Status](https://github.com/mdesmet/trino-javascript/actions/workflows/release.yaml/badge.svg)](https://github.com/mdesmet/trino-javascript/actions/workflows/release.yaml)

This is a [Trino](http://trino.io/) connector.

# Quick Start

To run a Docker container with the connector, run the following:
```bash
docker run \
  -d \
  --name trino-javascript \
  -p 8080:8080 \
  mdesmet/trino-javascript:0.1
```

Then use your favourite SQL client to connect to Trino running at http://localhost:8080

# Usage

Download one of the ZIP packages, unzip it and copy the `trino-javascript-0.16` directory to the plugin directory on every node in your Trino cluster.
Create a `trino-javascript.properties` file in your Trino catalog directory and set all the required properties.

```
connector.name=trino-javascript
```

After reloading Trino, you should be able to connect to the `trino-javascript` catalog.

# Build

Run all the unit test classes.
```
mvn test
```

Creates a deployable jar file
```
mvn clean compile package
```

Copy jar files in target directory to use git connector in your Trino cluster.
```
cp -p target/*.jar ${PLUGIN_DIRECTORY}/trino-javascript/
```

# Deploy

An example command to run the Trino server with the trino-javascript plugin and catalog enabled:

```bash
src=$(git rev-parse --show-toplevel)
docker run \
  -v $src/target/trino-javascript-0.1-SNAPSHOT:/usr/lib/trino/plugin/trino-javascript \
  -v $src/catalog:/usr/lib/trino/default/etc/catalog \
  -p 8080:8080 \
  --name trino \
  -d \
  trinodb/trino:375
```

Connect to that server using:
```bash
docker run -it --rm --link trino trinodb/trino:375 trino --server trino:8080 --catalog trino-javascript --schema default
```
