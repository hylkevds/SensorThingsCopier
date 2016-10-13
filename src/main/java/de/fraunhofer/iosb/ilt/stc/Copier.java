/*
 * Copyright (C) 2016 Fraunhofer Institut IOSB, Fraunhoferstr. 1, D 76131
 * Karlsruhe, Germany.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.fraunhofer.iosb.ilt.stc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author scf
 */
public class Copier {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Copier.class);

    public static class Configuration {

        public String sourceService;
        public String targetService;
        public List<DatastreamCombo> dataStreamCombos;
    }
    private final String configFileName;
    private Configuration configuration;
    private List<ObservationCopier> copiers = new ArrayList<>();

    public Copier(String configFileName) {
        this.configFileName = configFileName;
    }

    public void start() throws IOException {
        LOGGER.info("Reading configuration from {}", configFileName);
        ObjectMapper mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);
        File configFile = new File(configFileName);
        configuration = mapper.readValue(configFile, Configuration.class);

        URL sourceService = new URL(configuration.sourceService);
        URL targetService = new URL(configuration.targetService);
        LOGGER.info("Copying from {} to {}.", sourceService, targetService);

        for (DatastreamCombo combo : configuration.dataStreamCombos) {
            ObservationCopier copier = new ObservationCopier(sourceService, targetService, combo);
            copiers.add(copier);
        }
        LOGGER.info("Found {} Datastreams to copy.", copiers.size());

        for (ObservationCopier copier : copiers) {
            try {
                copier.doWork();
            } catch (URISyntaxException | ServiceFailureException | MalformedURLException e) {
                DatastreamCombo dc = copier.getDatastreamCombo();
                LOGGER.error("Failure on {} to {} from {}.", dc.sourceDatastreamId, dc.targetDatastreamId, dc.lastCopiedId);
            }
        }
        LOGGER.info("Writing config file: {}", configFile);
        mapper.writeValue(configFile, configuration);
    }

    /**
     * @param args the command line arguments
     * @throws de.fraunhofer.iosb.ilt.sta.ServiceFailureException
     * @throws java.net.URISyntaxException
     * @throws java.net.MalformedURLException
     */
    public static void main(String[] args) throws ServiceFailureException, URISyntaxException, MalformedURLException, IOException {
        String configFileName = "configuration.json";
        if (args.length > 0) {
            configFileName = args[0];
        }
        File configFile = new File(configFileName);
        if (configFile.isFile() && configFile.exists()) {
            Copier copier = new Copier(configFileName);
            copier.start();
        } else {
            LOGGER.info("No configuration found, generating sample.");
            Configuration sampleConfig = new Configuration();
            sampleConfig.sourceService = "http://localhost:8080/SensorThingsService/v1.0/";
            sampleConfig.targetService = "http://localhost:8080/SensorThingsService/v1.0/";
            sampleConfig.dataStreamCombos = new ArrayList<>();
            sampleConfig.dataStreamCombos.add(new DatastreamCombo(1, 2, 0));
            ObjectMapper mapper = new ObjectMapper()
                    .enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(configFile, sampleConfig);
            LOGGER.info("Sample configuration written to {}.", configFile);
        }
    }
}
