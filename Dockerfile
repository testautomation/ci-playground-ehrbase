# FROM postgres:11.5-alpine

# # Set default values for database user and passwords
# ARG EHRBASE_USER="ehrbase"
# ARG EHRBASE_PASSWORD="ehrbase"
# ENV EHRBASE_USER=${EHRBASE_USER}
# ENV EHRBASE_PASSWORD=${EHRBASE_PASSWORD}

# # Set Postgres data directory to custom folder
# ENV PGDATA="/var/lib/postgresql/pgdata"

# # Create custom data directory and change ownership to postgres user
# RUN mkdir -p ${PGDATA}
# RUN chown postgres: ${PGDATA}
# RUN chmod 0700 ${PGDATA}

# # Define Postgres version for easier upgrades for the future
# ENV PG_MAJOR=11.9

# # Adding locales to an alpine container as described
# # here: https://github.com/Auswaschbar/alpine-localized-docker
# # set our environment variable
# ENV MUSL_LOCPATH="/usr/share/i18n/locales/musl"

# # install libintl
# # then install dev dependencies for musl-locales
# # clone the sources
# # build and install musl-locales
# # remove sources and compile artifacts
# # lastly remove dev dependencies again
# RUN apk --no-cache add libintl && \
# 	apk --no-cache --virtual .locale_build add cmake make musl-dev gcc gettext-dev git && \
# 	git clone https://gitlab.com/rilian-la-te/musl-locales && \
# 	cd musl-locales && cmake -DLOCALE_PROFILE=OFF -DCMAKE_INSTALL_PREFIX:PATH=/usr . && make && make install && \
# 	cd .. && rm -r musl-locales && \
# 	apk del .locale_build

# # Copy init scripts to init directory
# COPY ./scripts/create-ehrbase-user.sh /docker-entrypoint-initdb.d/

# # Initialize basic database cluster
# RUN sh -c "/usr/local/bin/docker-entrypoint.sh postgres & " && \
#     sleep 20 && \
#     echo "Database initialized"

# # Allow connections from all adresses & Listen to all interfaces
# RUN echo "host  all  all   0.0.0.0/0  scram-sha-256" >> ${PGDATA}/pg_hba.conf
# RUN echo "listen_addresses='*'" >> ${PGDATA}/postgresql.conf

# # Install python and dependencies
# RUN apk add --update postgresql-dev \
#                      build-base \
#                      git \
#                      flex \
#                      bison

# # Install temporary_tables plugin
# COPY ./scripts/install-temporal-tables.sh .
# RUN chmod +x ./install-temporal-tables.sh
# RUN sh -c "./install-temporal-tables.sh"

# # Install jsquery plugin
# COPY ./scripts/install-jsquery.sh .
# RUN chmod +x ./install-jsquery.sh 
# RUN sh -c "./install-jsquery.sh"

# # Prepare database schemas
# COPY ./scripts/start-databases.sh .
# RUN chmod +x ./start-databases.sh
# RUN sh -c "./start-databases.sh"

# # Cleanup
# RUN rm -f -r ./jsquery
# RUN rm -f -r ./temporal_tables

# EXPOSE 5432

# # INSTALL JAVA 11 JDK
# RUN apk --no-cache add openjdk11 --repository=http://dl-cdn.alpinelinux.org/alpine/edge/community \
#   && java --version

# # INSTALL MAVEN
# ENV MAVEN_VERSION 3.6.3
# ENV MAVEN_HOME /usr/lib/mvn
# ENV PATH $MAVEN_HOME/bin:$PATH
# RUN wget http://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz && \
#   tar -zxvf apache-maven-$MAVEN_VERSION-bin.tar.gz && \
#   rm apache-maven-$MAVEN_VERSION-bin.tar.gz && \
#   mv apache-maven-$MAVEN_VERSION /usr/lib/mvn \
#   && mvn --version

# RUN ls -la
# RUN ls -la /home
# RUN ifconfig
# RUN cat /etc/hosts

FROM maven:3.6.3-openjdk-11-slim AS builder
WORKDIR /ehrbase

COPY ./pom.xml ./pom.xml
COPY ./api/pom.xml ./api/pom.xml
COPY ./application/pom.xml ./application/pom.xml
COPY ./base/pom.xml ./base/pom.xml
COPY ./jooq-pq/pom.xml ./jooq-pq/pom.xml
COPY ./rest-ehr-scape/pom.xml ./rest-ehr-scape/pom.xml
COPY ./rest-openehr/pom.xml ./rest-openehr/pom.xml
COPY ./service/pom.xml ./service/pom.xml
COPY ./api/src ./api/src
COPY ./application/src ./application/src
COPY ./base/src ./base/src
COPY ./jooq-pq/src ./jooq-pq/src
COPY ./rest-ehr-scape/src ./rest-ehr-scape/src
COPY ./rest-openehr/src ./rest-openehr/src
COPY ./service/src ./service/src
RUN mvn compile dependency:go-offline

# BUILD EHRBASE (AND STORE ALL DEPENDENCIES)
COPY . .
RUN ls -la ./
RUN mvn package -Dmaven.javadoc.skip=true
RUN cp application/target/application-0.13.0.jar /tmp/app.jar



FROM openjdk:11-jre-slim AS pusher
WORKDIR /ehrbase
COPY --from=builder /tmp/app.jar .

EXPOSE 8080
CMD ["java", "-jar", "/ehrbase/app.jar"]
