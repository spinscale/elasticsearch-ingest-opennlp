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
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.Processor;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.elasticsearch.ingest.ConfigurationUtils.readOptionalList;
import static org.elasticsearch.ingest.ConfigurationUtils.readStringProperty;

public class OpenNlpProcessor extends AbstractProcessor {

    public static final String TYPE = "opennlp";

    private final OpenNlpService openNlpService;
    private final String sourceField;
    private final String targetField;
    private final Set<String> fields;

    OpenNlpProcessor(OpenNlpService openNlpService, String tag, String sourceField, String targetField, Set<String> fields) throws
            IOException {
        super(tag);
        this.openNlpService = openNlpService;
        this.sourceField = sourceField;
        this.targetField = targetField;
        this.fields = fields;
    }

    @Override
    public void execute(IngestDocument ingestDocument) throws Exception {
        String content = ingestDocument.getFieldValue(sourceField, String.class);

        if (Strings.hasLength(content)) {
            Map<String, Set<String>> entities = new HashMap<>();
            mergeExisting(entities, ingestDocument, targetField);

            for (String field : fields) {
                Set<String> data = openNlpService.find(content, field);
                merge(entities, field, data);
            }

            ingestDocument.setFieldValue(targetField, entities);
        }
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public static final class Factory implements Processor.Factory {

        private OpenNlpService openNlpService;

        public Factory(OpenNlpService openNlpService) {
            this.openNlpService = openNlpService;
        }

        @Override
        public OpenNlpProcessor create(Map<String, Processor.Factory> registry, String processorTag, Map<String, Object> config)
                throws Exception {
            String field = readStringProperty(TYPE, processorTag, config, "field");
            String targetField = readStringProperty(TYPE, processorTag, config, "target_field", "entities");
            List<String> fields = readOptionalList(TYPE, processorTag, config, "fields");
            final Set<String> foundFields = fields == null || fields.size() == 0 ? openNlpService.getModels() : new HashSet<>(fields);
            return new OpenNlpProcessor(openNlpService, processorTag, field, targetField, foundFields);
        }
    }

    private static void mergeExisting(Map<String, Set<String>> entities, IngestDocument ingestDocument, String targetField) {
        if (ingestDocument.hasField(targetField)) {
            @SuppressWarnings("unchecked")
            Map<String, Set<String>> existing = ingestDocument.getFieldValue(targetField, Map.class);
            entities.putAll(existing);
        } else {
            ingestDocument.setFieldValue(targetField, entities);
        }
    }

    private static void merge(Map<String, Set<String>> map, String key, Set<String> values) {
        if (values.size() == 0) return;

        if (map.containsKey(key))
            values.addAll(map.get(key));

        map.put(key, values);
    }
}
