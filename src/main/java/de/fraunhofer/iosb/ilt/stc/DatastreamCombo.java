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

import com.google.gson.JsonElement;
import de.fraunhofer.iosb.ilt.configurable.ConfigEditor;
import de.fraunhofer.iosb.ilt.configurable.Configurable;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorLong;
import de.fraunhofer.iosb.ilt.configurable.editor.EditorMap;
import java.util.Map;

/**
 *
 * @author scf
 */
public class DatastreamCombo implements Configurable<Object, Object> {

    private EditorMap<Object, Object, Map<String, Object>> editor;
    private EditorLong editorSourceDatastreamId;
    private EditorLong editorTargetDatastreamId;
    private EditorLong editorLastCopiedId;

    @Override
    public void configure(JsonElement config, Object context, Object edtCtx) {
        getConfigEditor(context, edtCtx).setConfig(config, context, edtCtx);
    }

    @Override
    public ConfigEditor<Object, Object, ?> getConfigEditor(Object context, Object edtCtx) {
        if (editor == null) {
            editor = new EditorMap<>();

            editorSourceDatastreamId = new EditorLong(Long.MIN_VALUE, Long.MAX_VALUE, 1, "Source ID", "The source datastream id.");
            editor.addOption("sourceDatastreamId", editorSourceDatastreamId, false);

            editorTargetDatastreamId = new EditorLong(Long.MIN_VALUE, Long.MAX_VALUE, 1, "Target ID", "The target datastream id.");
            editor.addOption("targetDatastreamId", editorTargetDatastreamId, false);

            editorLastCopiedId = new EditorLong(Long.MIN_VALUE, Long.MAX_VALUE, 0, "Last Copied", "The id of the last copied observation.");
            editor.addOption("lastCopiedId", editorLastCopiedId, false);
        }
        return editor;

    }

    public long getSourceDatastreamId() {
        return editorSourceDatastreamId.getValue();
    }

    public long getTargetDatastreamId() {
        return editorTargetDatastreamId.getValue();
    }

    public long getLastCopiedId() {
        return editorLastCopiedId.getValue();
    }

    public void setLastCopiedId(long lastCopiedId) {
        this.editorLastCopiedId.setValue(lastCopiedId);
    }

    @Override
    public String toString() {
        return editorSourceDatastreamId + " to " + editorTargetDatastreamId + " from " + getLastCopiedId();
    }

}
