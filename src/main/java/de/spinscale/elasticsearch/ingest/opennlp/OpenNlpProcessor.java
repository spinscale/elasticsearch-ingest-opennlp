/*
 * Copyright [2016] [Alexander Reelsen]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package de.spinscale.elasticsearch.ingest.opennlp;

import org.elasticsearch.common.Strings;
import org.elasticsearch.ingest.AbstractProcessor;
import org.elasticsearch.ingest.AbstractProcessorFactory;
import org.elasticsearch.ingest.IngestDocument;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.elasticsearch.ingest.ConfigurationUtils.newConfigurationException;
import static org.elasticsearch.ingest.ConfigurationUtils.readOptionalList;
import static org.elasticsearch.ingest.ConfigurationUtils.readStringProperty;

public class OpenNlpProcessor extends AbstractProcessor {

    public static final String TYPE = "opennlp";

    private final OpenNlpService openNlpService;
    private final String field;
    private final String targetField;
    private final Set<Property> properties;

    OpenNlpProcessor(OpenNlpService openNlpService, String tag, String field, String targetField, Set<Property> properties) throws
            IOException {
        super(tag);
        this.openNlpService = openNlpService;
        this.field = field;
        this.targetField = targetField;
        this.properties = properties;
    }

    @Override
    public void execute(IngestDocument ingestDocument) throws Exception {
        String content = ingestDocument.getFieldValue(field, String.class);

        if (Strings.hasLength(content)) {
            Map<String, Set<String>> entities = new HashMap<>();
            mergeExisting(entities, ingestDocument, targetField);

            if (properties.contains(Property.NAMES)) {
                Set<String> names = openNlpService.findNames(content);
                merge(entities, "names", names);
            }

            if (properties.contains(Property.DATES)) {
                Set<String> dates = openNlpService.findDates(content);
                merge(entities, "dates", dates);
            }

            if (properties.contains(Property.LOCATIONS)) {
                Set<String> locations = openNlpService.findLocations(content);
                merge(entities, "locations", locations);
            }

            ingestDocument.setFieldValue(targetField, entities);
        }
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public static final class Factory extends AbstractProcessorFactory<OpenNlpProcessor> {

        static final Set<Property> DEFAULT_PROPERTIES = EnumSet.allOf(Property.class);
        private OpenNlpService openNlpService;

        public Factory(OpenNlpService openNlpService) {
            this.openNlpService = openNlpService;
        }

        @Override
        public OpenNlpProcessor doCreate(String processorTag, Map<String, Object> config) throws Exception {
            String field = readStringProperty(TYPE, processorTag, config, "field");
            String targetField = readStringProperty(TYPE, processorTag, config, "target_field", "entities");
            List<String> propertyNames = readOptionalList(TYPE, processorTag, config, "fields");

            final Set<Property> properties;
            if (propertyNames != null) {
                properties = EnumSet.noneOf(Property.class);
                for (String fieldName : propertyNames) {
                    try {
                        properties.add(Property.parse(fieldName));
                    } catch (Exception e) {
                        throw newConfigurationException(TYPE, processorTag, "properties", "illegal field option [" +
                                fieldName + "]. valid values are " + Arrays.toString(Property.values()));
                    }
                }
            } else {
                properties = DEFAULT_PROPERTIES;
            }

            return new OpenNlpProcessor(openNlpService, processorTag, field, targetField, properties);
        }
    }

    enum Property {

        DATES,
        LOCATIONS,
        NAMES;

        public static Property parse(String value) {
            return valueOf(value.toUpperCase(Locale.ROOT));
        }
    }

    private static void mergeExisting(Map<String,Set<String>> entities, IngestDocument ingestDocument, String targetField) {
        if (ingestDocument.hasField(targetField)) {
            @SuppressWarnings("unchecked")
            Map<String, Set<String>> existing = ingestDocument.getFieldValue(targetField, Map.class);
            entities.putAll(existing);
        } else {
            ingestDocument.setFieldValue(targetField, entities);
        }
    }

    private static void merge(Map<String,Set<String>> map, String key, Set<String> values) {
        if (values.size() == 0) return;

        if (map.containsKey(key))
            values.addAll(map.get(key));

        map.put(key, values);
    }
}
