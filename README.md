# EHRbase

EHRbase is an [openEHR](openehr.org) Clinical Data Repository, providing a standard-based backend for interoperable clinical applications. It implements the latest version of the openEHR Reference Model (RM 1.0.4) and version 1.4 of the Archetype Definition Language (ADL). Applications can use the capabilities of EHRbase through the latest version of the [openEHR REST API](https://specifications.openehr.org/releases/ITS-REST/latest/) and model-based queries using the [Archetype Query Language](https://specifications.openehr.org/releases/QUERY/latest/AQL.html).

## Release Notes
##### 2020, May 14
This release of EHRbase (v0.13.0) is the first **beta** release. Please see [EHRbase Documentation](https://ehrbase.readthedocs.io/en/latest/) for further details.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. Please read these instructions carefully. See [deployment](#deployment) for notes on how to deploy the project on a live system.

### Prerequisites

You will need Java JDK/JRE 11 (preferably openJDK: e.g. from https://adoptopenjdk.net/)

You will need a Postgres Database (Docker image or local installation). We recommend the Docker image to get started quickly.

When installing locally, the Postgres Database (at least Version 10.4) needs the following extensions:
 * [temporal tables](https://github.com/arkhipov/temporal_tables) 
    ```bash
     git clone https://github.com/arkhipov/temporal_tables.git
     make
     sudo make install
     make installcheck
     ```
 * [jsquery](https://github.com/postgrespro/jsquery) 
     ```bash
     git clone https://github.com/postgrespro/jsquery.git
     cd jsquery
     make USE_PGXS=1
     sudo make USE_PGXS=1 install
     make USE_PGXS=1 installcheck
    ```

### Installing

#### 1. Setup database

> NOTE: Building EHRbase requires a properly set up and running DB for the following steps.

Run `./db-setup/createdb.sql` as `postgres` User.

You can also use this Docker image which is a preconfigured Postgres database:
```shell
    docker network create ehrbase-net
    docker run --name ehrdb --network ehrbase-net -e POSTGRES_PASSWORD=postgres -d -p 5432:5432 ehrbaseorg/ehrbase-postgres:latest
```

(For a preconfigured EHRbase application Docker image and its usage see [below](#Docker))
#### 2. Setup Maven environment

Edit the database properties in  `./pom.xml` if necessary

#### 3. Build EHRbase
Run `mvn package`

#### 4. Run EHRbase

Replace the * with the current version, e.g. `application/target/application-0.9.0.jar`

`java -jar application/target/application-*.jar`

### Authentication Types

#### 1. Basic Auth

EHRbase can use Basic Authentication for all resources. This means you have to send an 'Authorization' header
set with keyword `Basic` followed by the authentication information in Base64 encoded username and password. To
generate the Base64 encoded username and password combination create the string after the following schema:
`username:password`.

The Basic Auth mechanism is implemented as "opt-in" and can be activated either by providing an environment variable
`SECURITY_AUTHTYPE=BASIC` with the start command or by adding the value into the target application.yml file.

Currently we have support one user with password which can be set via environment variables `SECURITY_AUTHUSER` and
`SECURITY_AUTHPASSWORD`. By default these values are set with `ehrbase-user` and `authPassword=SuperSecretPassword`
and can be overridden by environment values. Alternatively you can set them inside the corresponding application.yml
file.

The same applies to the *admin* user, via `SECURITY_AUTHADMINUSER`, `SECURITY_AUTHADMINPASSWORD` 
and their default values of `ehrbase-admin` and `EvenMoreSecretPassword`.

#### 2. OAuth2

Environment variable `SECURITY_AUTHTYPE=OAUTH` is enabling OAuth2 authentication.

Additionally, setting the following variable to point to the existing OAuth2 server and realm is necessary:
`SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUERURI=http://localhost:8081/auth/realms/ehrbase`

In the given OAuth2 server configuration the roles `user` and `admin` are expected to be configured. 
Users should have their roles assigned accordingly.

## Running the tests

This command will run all tests from `tests/robot` folder.
DB and server application will be started/stopped by the tests accordingly. You *must not* start them by hand.

> NOTE: Make sure you meet the PREREQUISITES mentioned in tests/README.md prior to test execution.
>
> Please Check the README in `tests` folder for more details.

```bash
cd tests
./run_local_tests.sh
```


## Deployment

 1. `java -jar application/target/application-*.jar` You can override the application properties (like database settings) using the normal spring boot mechanism: [Command-Line Arguments in Spring Boot](https://www.baeldung.com/spring-boot-command-line-arguments)
 2. Browse to Swagger UI --> http://localhost:8080/ehrbase/swagger-ui.html


## Docker

### Locally Build Docker Image

First build the application as described in [Installing](#Installing)

To create a Docker image run the following command and provide the correct build output file name created in the previous step, e.g. application-0.10.0.jar for version 0.10.0.

`docker build -f application/Dockerfile --build-arg JAR_FILE=application-*.jar -t ehrbaseorg/ehrbase:latest .`

To run the built container image use the following command:

`docker run --name ehrbase --network ehrbase-net -d -p 8080:8080 -e DB_URL=jdbc:postgresql://ehrdb:5432/ehrbase -e DB_USER=ehrbase -e DB_PASS=ehrbase -e SYSTEM_NAME=local.ehrbase.org ehrbaseorg/ehrbase`.

Adopt the parameters by your needs. The following parameters for `-e` must be set to start the EHRbase container:

| Parameter     | Usage                                                    | Example                              |
| ------------- | -------------------------------------------------------- | ------------------------------------ |
| DB_URL        | Database URL. Must point to the running database server. | jdbc:postgresql://ehrdb:5432/ehrbase |
| DB_USER       | Database user configured for the ehr schema.             | ehrbase                              |
| DB_PASS       | Password for the database user                           | ehrbase                              |
| AUTH_TYPE     | Set HTTP security method                                 | BASIC                                |
| AUTH_USER     | Username for Basic Auth                                  | myuser                               |
| AUTH_PASSWORD | Password for Basic Auth                                  | myPassword432                        |
| SYSTEM_NAME   | Name for the local system                                | local.ehrbase.org                    |

### Pre-build Docker Image

See: https://hub.docker.com/r/ehrbaseorg/ehrbase

There is also a preconfigured `docker-compose.yml` file, which sets up and starts the necessary database and EHRbase 
application with, for instance:

```shell script
cd application
docker-compose up
```

Notes: It is not necessary to have the whole repository on your machine, just copy the `docker-compose.yml` file to
a local working directory and run it. Using the `-d` argument starts both containers detached, without blocking the 
terminal. And the DB data is saved in `application/.pgdata` for easier access.

## Built With

* [Maven](https://maven.apache.org/) - Dependency Management



## Continuous Integration (CI/CD with CircleCI)
EHRbase uses CircleCI for continuous integration and deployment. The CI pipeline consists of the following workflows:

### workflow 1/3 - build-and-test
- trigger: commit to any branch (except - `release/v*`, `master`, `sync/*`, `feature/sync/*`)
- jobs:
  - build artifacts
  - run unit tests
  - run sdk integraiton tests
  - run robot integration tests
  - perform sonarcloud analysis and OWASP dependency check

### workflow 2/3 - release
- trigger: commit to `release/v` or `master` branch
- jobs:
  - build artifacts
  - run unit tests
  - run sdk integraiton tests
  - run robot integration tests
  - perform sonarcloud analysis and OWASP dependency check
  - TODO: deploy to Maven Central
  - TODO: deploy to Docker Hub

### workflow 3/3 - synced-feature-check

:warning: This is a special workflow to catch errors that can occur when code changes introduced to EHRbase AND openEHR_SDK repository are related in a way that they have to be tested together and otherwise can't be catched in workflow 1 or 2. 

- trigger: commit to `sync/*` branch
- jobs:
  - pull, build, and test SDK from `sync/*` branch of openEHR_SDK repo
  - build and test ehrbase (w/ SDK installed in previous step)
  - start ehrbase server (from .jar packaged in previous step)
  - run SDK's (java) integration tests
  - run EHRbase's (robot) integration tests
  
  
```
HOW TO USE WORKFLOW 3/3
=======================

1. create TWO branches following the naming convention `sync/[issue-id]_some-desciption`
   in both repositories (EHRbase and openEHR_SDK) with exact the same name:

  - ehrbase repo       --> i.e.    sync/123_example-issue
  - openehr_sdk repo   --> i.e.    sync/123_example-issue

2. apply your code changes
3. push to openehr_sdk repo (NO CI will be triggered)
4. push to ehrbase repo (CI will trigger this workflow)
5. create TWO PRs (one in EHRbase, one in openEHR_SDK)
6. merge BOTH PRs considering below notes:
  - make sure both PRs are reviewed and ready to be merged
    at the same time!
  - make sure to sync both PRs w/ develop before merging!
  - MERGE BOTH PRs AT THE SAME TIME!
```

## License

EHRbase uses the Apache License, Version 2.0 (https://www.apache.org/licenses/LICENSE-2.0)

## Acknowledgments

EHRbase contains code and derived code from EtherCIS (ethercis.org) which has been developed by Christian Chevalley (ADOC Software Development Co.,Ltd).
Dr. Tony Shannon and Phil Berret of the [Ripple Foundation CIC Ltd, UK](https://ripple.foundation/) and Dr. Ian McNicoll (FreshEHR Ltd.) greatly contributed to EtherCIS. 

EHRbase heavily relies on the openEHR Reference Model implementation ([Archie](https://github.com/openEHR/archie)) made by Nedap. Many thanks to Pieter Bos and his team for their work!

EHRbase is jointly developed by [Vitasystems GmbH](https://www.vitagroup.ag/de_DE/Ueber-uns/vitasystems) and [Peter L. Reichertz Institute for Medical Informatics of TU Braunschweig and Hannover Medical School](plri.de)


## Stargazers over time

[![Stargazers over time](https://starchart.cc/ehrbase/ehrbase.svg)](https://starchart.cc/ehrbase/ehrbase)
