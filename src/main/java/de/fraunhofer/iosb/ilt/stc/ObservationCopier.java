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

import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.model.Datastream;
import de.fraunhofer.iosb.ilt.sta.model.Observation;
import de.fraunhofer.iosb.ilt.sta.model.ext.EntityList;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author scf
 */
public class ObservationCopier {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ObservationCopier.class);
    private final URL sourceBaseUrl;
    private final URL targetBaseUrl;
    private final DatastreamCombo datastreamCombo;

    public ObservationCopier(URL sourceBaseUrl, URL targetBaseUrl, DatastreamCombo combo) {
        this.sourceBaseUrl = sourceBaseUrl;
        this.targetBaseUrl = targetBaseUrl;
        this.datastreamCombo = combo;
    }

    public synchronized long doWork() throws URISyntaxException, ServiceFailureException, MalformedURLException {
        LOGGER.debug("Copying {} to {}.", datastreamCombo.sourceDatastreamId, datastreamCombo.targetDatastreamId);
        SensorThingsService sourceService = new SensorThingsService(sourceBaseUrl);
        SensorThingsService targetService = new SensorThingsService(targetBaseUrl);

        Datastream sourceDatastream = sourceService.datastreams().find(datastreamCombo.sourceDatastreamId);
        Datastream targetDatastream = targetService.datastreams().find(datastreamCombo.targetDatastreamId);

        EntityList<Observation> list = sourceDatastream
                .observations().query()
                .filter("id gt " + datastreamCombo.lastCopiedId)
                .orderBy("id asc")
                .top(1000)
                .list();
        long count = 0;
        Iterator<Observation> i = list.fullIterator();
        while (i.hasNext()) {
            Observation observation = i.next();
            Long sourceId = observation.getId();
            observation.setService(null);
            observation.setSelfLink(null);
            observation.setId(null);
            targetDatastream.observations().create(observation);
            Long targetId = observation.getId();
            LOGGER.trace("Copied Obs {}. New Id: {}.", sourceId, targetId);
            datastreamCombo.lastCopiedId = sourceId;
            count++;
        }
        LOGGER.info("Copied {} observations from {} to {}. LastId={}.",
                count,
                datastreamCombo.sourceDatastreamId,
                datastreamCombo.targetDatastreamId,
                datastreamCombo.lastCopiedId);
        return datastreamCombo.lastCopiedId;
    }

    public DatastreamCombo getDatastreamCombo() {
        return datastreamCombo;
    }

}
