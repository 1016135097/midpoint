/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json.reader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.namespace.QName;

import com.fasterxml.jackson.core.JsonFactory;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.schema.SchemaRegistry;

public class JsonReader extends AbstractReader {

    public JsonReader(@NotNull SchemaRegistry schemaRegistry) {
        super(schemaRegistry);
    }

    @Override
    public boolean canRead(@NotNull File file) {
        return file.getName().endsWith(".json");
    }

    @Override
    public boolean canRead(@NotNull String dataString) {
        // Second for is for multiple objects
        return dataString.startsWith("{") || dataString.startsWith("[");
    }

    @Override
    protected com.fasterxml.jackson.core.JsonParser createJacksonParser(InputStream stream) throws IOException {
        return new JsonFactory().createParser(stream);
    }

    @Override
    protected QName tagToTypeName(Object tid, JsonReadingContext ctx) {
        return null;
    }

    @Override
    boolean supportsMultipleDocuments() {
        return false;
    }
}
