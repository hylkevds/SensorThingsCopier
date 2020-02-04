FROM openjdk:8

# Copy to images tomcat path
ARG JAR_FILE
WORKDIR /usr/local/FROST

ADD target/${JAR_FILE} /usr/local/FROST/SensorThingsCopier.jar
ADD configuration.json configuration.json
ADD run.sh run.sh

RUN chmod +x ./run.sh
CMD ./run.sh
