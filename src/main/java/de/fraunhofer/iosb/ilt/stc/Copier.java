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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.fraunhofer.iosb.ilt.configurable.ConfigEditor;
import de.fraunhofer.iosb.ilt.configurable.Configurable;
import de.fraunhofer.iosb.ilt.configurable.ConfigurationException;
import de.fraunhofer.iosb.ilt.configurable.EditorFactory;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorClass;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorInt;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorList;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorMap;
import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.Utils;
import de.fraunhofer.iosb.ilt.sta.model.Datastream;
import de.fraunhofer.iosb.ilt.sta.model.Observation;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author scf
 */
public class Copier implements Configurable<Object, Object> {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Copier.class);

    private EditorMap<Map<String, Object>> editor;
    private EditorClass<Object, Object, StaServer> editorSourceService;
    private EditorClass<Object, Object, StaServer> editorTargetService;
    private EditorInt editorDelayTime;
    private EditorInt editorPerRequest;
    private EditorList<DatastreamCombo, EditorClass<Object, Object, DatastreamCombo>> editorDatastreamCombos;

    private final File configFile;

    public List<DatastreamCombo> dataStreamCombos;
    private long delay = 1;
    /**
     * The number of observations to fetch per request.
     */
    private int perRequest = 1000;
    private List<ObservationCopier> copiers = new ArrayList<>();

    public Copier() {
        this.configFile = null;
    }

    public Copier(File configFile) throws IOException {
        this.configFile = configFile;
    }

    @Override
    public void configure(JsonElement config, Object context, Object edtCtx, ConfigEditor<?> configEditor) {
        getConfigEditor(context, edtCtx).setConfig(config);
        delay = editorDelayTime.getValue();
        perRequest = editorPerRequest.getValue();
    }

    @Override
    public EditorMap<Map<String, Object>> getConfigEditor(Object context, Object edtCtx) {
        if (editor == null) {
            editor = new EditorMap<>();

            editorSourceService = new EditorClass<>(context, edtCtx, StaServer.class, "Source Service", "The service to copy observations from.");
            editor.addOption("sourceService", editorSourceService, false);

            editorTargetService = new EditorClass<>(context, edtCtx, StaServer.class, "Target Service", "The service to copy observations to.");
            editor.addOption("targetService", editorTargetService, false);

            EditorFactory<EditorClass<Object, Object, DatastreamCombo>> factory = () -> {
                return new EditorClass<>(context, edtCtx, DatastreamCombo.class);
            };
            editorDatastreamCombos = new EditorList<>(factory, "Datastreams", "The source and target datastreams to copy.");
            editor.addOption("dataStreamCombos", editorDatastreamCombos, false);

            editorDelayTime = new EditorInt(0, 100000, 1, 1, "Delay", "The number of milliseconds to wait after each inserted observation.");
            editor.addOption("delay", editorDelayTime, true);

            editorPerRequest = new EditorInt(0, 100000, 1, 1000, "Per Request", "The number of Observations to request at the same time.");
            editor.addOption("perRequest", editorPerRequest, true);
        }
        return editor;
    }

    private List<DatastreamCombo> findCombos(SensorThingsService sourceService, SensorThingsService targetService) throws ServiceFailureException {
        List<DatastreamCombo> value = new ArrayList<>();
        for (Iterator<Datastream> it = sourceService.datastreams().query().list().fullIterator(); it.hasNext();) {
            Datastream sourceStream = it.next();

            List<Datastream> targetStreams = targetService.datastreams().query().filter("name eq '" + Utils.escapeForStringConstant(sourceStream.getName()) + "'").list().toList();
            if (targetStreams.size() > 1) {
                LOGGER.error("Found multiple Datastreams with name {} in target Service.", sourceStream.getName());
                continue;
            }
            if (targetStreams.isEmpty()) {
                LOGGER.debug("No Satastream with name {} found in target service.", sourceStream.getName());
                continue;
            }
            Datastream targetStream = targetStreams.get(0);

            Observation lastTarget = targetStream.observations()
                    .query()
                    .orderBy("id desc")
                    .select("id", "phenomenonTime", "validTime")
                    .top(1).first();
            StringBuilder filter = new StringBuilder();
            Observation lastSource = null;
            if (lastTarget != null) {
                filter.append("phenomenonTime eq ").append(lastTarget.getPhenomenonTime().toString());
                if (lastTarget.getValidTime() != null) {
                    filter.append(" and validTime eq ").append(lastTarget.getValidTime().toString());
                }

                lastSource = sourceStream.observations()
                        .query()
                        .orderBy("id desc")
                        .select("id")
                        .filter(filter.toString())
                        .top(1).first();
            }

            DatastreamCombo combo = new DatastreamCombo();
            combo.getConfigEditor(null, null);
            combo.setSourceDatastreamId((Long) sourceStream.getId().getValue());
            combo.setTargetDatastreamId((Long) targetStream.getId().getValue());
            if (lastSource != null) {
                combo.setLastCopiedId((Long) lastSource.getId().getValue() + 1);
            }
            value.add(combo);
            LOGGER.info("Found combo for Datastream {} as: {}", sourceStream.getName(), combo);
        }
        return value;
    }

    public void start() throws IOException, MalformedURLException, URISyntaxException, ConfigurationException {
        LOGGER.info("Reading configuration from {}", configFile);
        String configString = FileUtils.readFileToString(configFile, "UTF-8");
        JsonElement json = JsonParser.parseString(configString);
        configure(json, null, null, null);

        SensorThingsService sourceService = editorSourceService.getValue().createService();
        SensorThingsService targetService = editorTargetService.getValue().createService();
        LOGGER.info("Copying from {} to {}.", sourceService.getEndpoint(), targetService.getEndpoint());

        dataStreamCombos = editorDatastreamCombos.getValue();
        if (dataStreamCombos.isEmpty()) {
            try {
                dataStreamCombos = findCombos(sourceService, targetService);
            } catch (ServiceFailureException ex) {
                LOGGER.error("Failed to find datastreams.", ex);
                return;
            }
        }

        for (DatastreamCombo combo : dataStreamCombos) {
            ObservationCopier copier = new ObservationCopier(sourceService, targetService, combo);
            copier.setDelay(delay);
            copier.setPerRequest(perRequest);
            copiers.add(copier);
        }
        LOGGER.info("Found {} Datastreams to copy.", copiers.size());

        for (ObservationCopier copier : copiers) {
            try {
                copier.doWork();
            } catch (URISyntaxException | ServiceFailureException | MalformedURLException e) {
                DatastreamCombo dc = copier.getDatastreamCombo();
                LOGGER.error("Failure on {}.", dc);
            }
        }
        LOGGER.info("Writing config file: {}", configFile);
        json = editor.getConfig();
        configString = new GsonBuilder().setPrettyPrinting().create().toJson(json);
        FileUtils.writeStringToFile(configFile, configString, "UTF-8");
    }

    /**
     * @param args the command line arguments
     * @throws de.fraunhofer.iosb.ilt.sta.ServiceFailureException
     * @throws java.net.URISyntaxException
     * @throws java.net.MalformedURLException
     */
    public static void main(String[] args) throws ServiceFailureException, URISyntaxException, MalformedURLException, IOException, ConfigurationException {
        String configFileName = "configuration.json";
        if (args.length > 0) {
            configFileName = args[0];
        }
        File configFile = new File(configFileName);
        if (configFile.isFile() && configFile.exists()) {
            Copier copier = new Copier(configFile);
            copier.start();
        } else {
            LOGGER.info("No configuration found, generating sample.");
            JsonElement sampleConfig = new Copier().getConfigEditor(null, null).getConfig();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String configString = gson.toJson(sampleConfig);
            FileUtils.writeStringToFile(configFile, configString, "UTF-8");

            LOGGER.info("Sample configuration written to {}.", configFile);
        }
    }
}
