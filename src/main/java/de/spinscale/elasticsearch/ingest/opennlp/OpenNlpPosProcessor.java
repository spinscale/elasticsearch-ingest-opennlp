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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.elasticsearch.ingest.ConfigurationUtils.readOptionalList;
import static org.elasticsearch.ingest.ConfigurationUtils.readStringProperty;
import static org.elasticsearch.ingest.ConfigurationUtils.readBooleanProperty;

public class OpenNlpPosProcessor extends AbstractProcessor {

    public static final String TYPE = "opennlp_pos";

    private final OpenNlpService openNlpService;
    private final String sourceField;
    private final String targetField;
    private final Set<String> tagSet;
    private final boolean normalize;

    OpenNlpPosProcessor(OpenNlpService openNlpService, String processorTag, String sourceField, String targetField,
                        Set<String> tagSet, boolean normalize) throws
            IOException {
        super(processorTag);
        this.openNlpService = openNlpService;
        this.sourceField = sourceField;
        this.targetField = targetField;
        this.tagSet = tagSet;
        this.normalize = normalize;
    }

    @Override
    public void execute(IngestDocument ingestDocument) throws Exception {
        String content = ingestDocument.getFieldValue(sourceField, String.class);

        if (Strings.hasLength(content)) {

            Map<String, Object> map = new TreeMap<>();
            mergeExisting(map, ingestDocument, targetField);

            Map<String, Number> tags = openNlpService.countTags(content, tagSet, normalize);
            for (Map.Entry<String, Number> entry : tags.entrySet()) {
                merge(map, entry.getKey(), entry.getValue());
            }

            ingestDocument.setFieldValue(targetField, map);
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
        public OpenNlpPosProcessor create(Map<String, Processor.Factory> registry, String processorTag, Map<String, Object> config)
                throws Exception {
            String field = readStringProperty(TYPE, processorTag, config, "field");
            List<String> tags = readOptionalList(TYPE, processorTag, config, "tags");
            boolean normalize = readBooleanProperty(TYPE, processorTag, config, "normalize", false);
            final Set<String> tagSet = tags == null || tags.size() == 0 ? null : new HashSet<>(tags);
            String targetField = readStringProperty(TYPE, processorTag, config, "target_field", "tags");
            return new OpenNlpPosProcessor(openNlpService, processorTag, field, targetField, tagSet, normalize);
        }
    }

    private static void mergeExisting(Map<String, Object> map, IngestDocument ingestDocument, String targetField) {
        if (ingestDocument.hasField(targetField)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> existing = ingestDocument.getFieldValue(targetField, Map.class);
            map.putAll(existing);
        }
    }

    private static void merge(Map<String, Object> map, String key, Number value) {
        // Overwrite
        map.put(key, value);
    }
}
