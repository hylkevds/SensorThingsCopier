/*
 * Copyright (C) 2017 Fraunhofer Institut IOSB, Fraunhoferstr. 1, D 76131
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

import com.google.gson.JsonElement;
import de.fraunhofer.iosb.ilt.configurable.ConfigEditor;
import de.fraunhofer.iosb.ilt.configurable.Configurable;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorMap;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorString;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorSubclass;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;
import de.fraunhofer.iosb.ilt.stc.auth.AuthMethod;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

/**
 *
 * @author scf
 */
public class StaServer implements Configurable<Object, Object> {

    private EditorMap<Map<String, Object>> editor;
    private EditorString editorBaseUrl;
    private EditorSubclass<Object, Object, AuthMethod> editorAuthMethod;

    @Override
    public void configure(JsonElement config, Object context, Object edtCtx) {
        getConfigEditor(context, edtCtx).setConfig(config);
    }

    @Override
    public ConfigEditor<?> getConfigEditor(Object context, Object edtCtx) {
        if (editor == null) {
            editor = new EditorMap<>();

            editorBaseUrl = new EditorString("http://localhost:8080/SensorThingsService/v1.0", 1, "Url", "Base url of the SensorThings API service.");
            editor.addOption("baseUrl", editorBaseUrl, false);

            editorAuthMethod = new EditorSubclass(context, edtCtx, AuthMethod.class, "Auth Method", "Authentication method to use for this service.");
            editor.addOption("auth", editorAuthMethod, true);
        }
        return editor;
    }

    public SensorThingsService createService() throws MalformedURLException, URISyntaxException {
        SensorThingsService service = new SensorThingsService(new URL(editorBaseUrl.getValue()));
        AuthMethod auth = editorAuthMethod.getValue();
        if (auth != null) {
            auth.setAuth(service);
        }
        return service;
    }

}
