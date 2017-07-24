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
import de.fraunhofer.iosb.ilt.configurable.Configurable;
import de.fraunhofer.iosb.ilt.configurable.EditorFactory;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorClass;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorList;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorMap;
import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
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

    private EditorMap<Object, Object, Map<String, Object>> editor;
    private EditorClass<Object, Object, StaServer> editorSourceService;
    private EditorClass<Object, Object, StaServer> editorTargetService;
    private EditorList<Object, Object, DatastreamCombo, EditorClass<Object, Object, DatastreamCombo>> editorDatastreamCombos;

    private final File configFile;

    public List<DatastreamCombo> dataStreamCombos;

    private List<ObservationCopier> copiers = new ArrayList<>();

    public Copier() {
        this.configFile = null;
    }

    public Copier(File configFile) throws IOException {
        this.configFile = configFile;
    }

    @Override
    public void configure(JsonElement config, Object context, Object edtCtx) {
        getConfigEditor(context, edtCtx).setConfig(config, context, edtCtx);
    }

    @Override
    public EditorMap<Object, Object, Map<String, Object>> getConfigEditor(Object context, Object edtCtx) {
        if (editor == null) {
            editor = new EditorMap<>();

            editorSourceService = new EditorClass<>(StaServer.class, "Source Service", "The service to copy observations from.");
            editor.addOption("sourceService", editorSourceService, false);

            editorTargetService = new EditorClass<>(StaServer.class, "Target Service", "The service to copy observations to.");
            editor.addOption("targetService", editorTargetService, false);

            EditorFactory<EditorClass<Object, Object, DatastreamCombo>> factory = () -> {
                return new EditorClass<>(DatastreamCombo.class);
            };
            editorDatastreamCombos = new EditorList<>(factory, "Datastreams", "The source and target datastreams to copy.");
            editor.addOption("dataStreamCombos", editorDatastreamCombos, false);
        }
        return editor;
    }

    public void start() throws IOException, MalformedURLException, URISyntaxException {
        LOGGER.info("Reading configuration from {}", configFile);
        String configString = FileUtils.readFileToString(configFile, "UTF-8");
        JsonElement json = new JsonParser().parse(configString);
        configure(json, null, null);

        dataStreamCombos = editorDatastreamCombos.getValue();

        SensorThingsService sourceService = editorSourceService.getValue().createService();
        SensorThingsService targetService = editorTargetService.getValue().createService();
        LOGGER.info("Copying from {} to {}.", sourceService, targetService);

        for (DatastreamCombo combo : dataStreamCombos) {
            ObservationCopier copier = new ObservationCopier(sourceService, targetService, combo);
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
    public static void main(String[] args) throws ServiceFailureException, URISyntaxException, MalformedURLException, IOException {
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
