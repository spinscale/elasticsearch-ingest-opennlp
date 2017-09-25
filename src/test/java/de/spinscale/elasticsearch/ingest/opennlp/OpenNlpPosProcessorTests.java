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

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.Processor;
import org.elasticsearch.ingest.RandomDocumentPicks;
import org.elasticsearch.test.ESTestCase;
import org.junit.Before;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;

public class OpenNlpPosProcessorTests extends ESTestCase {

    private OpenNlpService service;

    @Before
    public void createOpenNlpService() throws IOException {
        Settings settings = Settings.builder()
                .put("ingest.opennlp.model.file.pos", "en-pos-maxent.bin")
                .build();

        service = new OpenNlpService(getDataPath("/models/en-pos-maxent.bin").getParent(), settings).start();
    }

    public void testThatTaggingWork() throws Exception {
        OpenNlpPosProcessor processor = new OpenNlpPosProcessor(service, randomAlphaOfLength(10), "source_field", "target_field",
                null, false);

        Map<String, Object> entityData = getIngestDocumentData(processor);

        assertThatHasValue(entityData, "CC", Integer.class, 1);
        assertThatHasValue(entityData, "CD", Integer.class, 3);
        assertThatHasValue(entityData, "DT", Integer.class, 5);
        assertThatHasValue(entityData, "IN", Integer.class, 4);
        assertThatHasValue(entityData, "JJ", Integer.class, 1);
        assertThatHasValue(entityData, "JJS", Integer.class, 2);
        assertThatHasValue(entityData, "NN", Integer.class, 6);
        assertThatHasValue(entityData, "NNP", Integer.class, 7);
        assertThatHasValue(entityData, "NNS", Integer.class, 3);
        assertThatHasValue(entityData, "PUNCT", Integer.class, 5);
        assertThatHasValue(entityData, "RB", Integer.class, 6);
        assertThatHasValue(entityData, "VBD", Integer.class, 1);
        assertThatHasValue(entityData, "VBN", Integer.class, 2);
        assertThatHasValue(entityData, "VBZ", Integer.class, 4);
    }

    public void testThatTagsCanBeExcluded() throws Exception {
        OpenNlpPosProcessor processor = new OpenNlpPosProcessor(service, randomAlphaOfLength(10), "source_field", "target_field",
                new HashSet<>(Arrays.asList("NN", "VBZ")), false);

        Map<String, Object> entityData = getIngestDocumentData(processor);

        assertThat(entityData, not(hasKey("CC")));
        assertThat(entityData, not(hasKey("CD")));
        assertThat(entityData, not(hasKey("DT")));
        assertThat(entityData, not(hasKey("IN")));
        assertThat(entityData, not(hasKey("JJ")));
        assertThat(entityData, not(hasKey("JJS")));
        assertThat(entityData, not(hasKey("NNP")));
        assertThat(entityData, not(hasKey("NNS")));
        assertThat(entityData, not(hasKey("PUNCT")));
        assertThat(entityData, not(hasKey("RB")));
        assertThat(entityData, not(hasKey("VBD")));
        assertThat(entityData, not(hasKey("VBN")));
        assertThatHasValue(entityData, "NN", Integer.class, 6);
        assertThatHasValue(entityData, "VBZ", Integer.class, 4);
    }

    public void testThatNormalizeWorks() throws Exception {
        OpenNlpPosProcessor processor = new OpenNlpPosProcessor(service, randomAlphaOfLength(10), "source_field", "target_field",
                null, true);

        Map<String, Object> entityData = getIngestDocumentData(processor);

        assertThatHasValue(entityData, "CD", Double.class, 0.06);
        assertThatHasValue(entityData, "DT", Double.class, 0.1);
        assertThatHasValue(entityData, "IN", Double.class,  0.08);
        assertThatHasValue(entityData, "JJ", Double.class, 0.02);
        assertThatHasValue(entityData, "JJS", Double.class, 0.04);
        assertThatHasValue(entityData, "NN", Double.class, 0.12);
        assertThatHasValue(entityData, "NNP", Double.class, 0.14);
        assertThatHasValue(entityData, "NNS", Double.class, 0.06);
        assertThatHasValue(entityData, "PUNCT", Double.class, 0.1);
        assertThatHasValue(entityData, "RB", Double.class, 0.12);
        assertThatHasValue(entityData, "VBD", Double.class, 0.02);
        assertThatHasValue(entityData, "VBN", Double.class, 0.04);
        assertThatHasValue(entityData, "VBZ", Double.class, 0.08);
    }

    public void testThatExistingValuesAreOverwritten() throws Exception {
        OpenNlpPosProcessor processor = new OpenNlpPosProcessor(service, randomAlphaOfLength(10), "source_field", "target_field",
                new HashSet<>(Arrays.asList("NN", "VBZ")), false);

        IngestDocument ingestDocument = getIngestDocument();

        Map<String, Object> entityData = new HashMap<>();
        entityData.put("NN", 101);
        entityData.put("VBD", 99.9);
        entityData.put("VBZ", 102);

        ingestDocument.setFieldValue("target_field", entityData);

        processor.execute(ingestDocument);

        entityData = getIngestDocumentData(ingestDocument);

        assertThatHasValue(entityData, "NN", Integer.class, 6);
        assertThatHasValue(entityData, "VBZ", Integer.class, 4);
        assertThatHasValue(entityData, "VBD", Double.class, 99.9);
    }

    public void testConstructorNoTagsSpecified() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("field", "source_field");
        config.put("target_field", "target_field");

        OpenNlpPosProcessor.Factory factory = new OpenNlpPosProcessor.Factory(service);
        Map<String, Processor.Factory> registry = Collections.emptyMap();
        OpenNlpPosProcessor processor = factory.create(registry, randomAlphaOfLength(10), config);

        Map<String, Object> entityData = getIngestDocumentData(processor);

        assertThat(entityData, hasKey("CC"));
        assertThat(entityData, hasKey("CD"));
        assertThat(entityData, hasKey("DT"));
        assertThat(entityData, hasKey("IN"));
        assertThat(entityData, hasKey("JJ"));
        assertThat(entityData, hasKey("JJS"));
        assertThat(entityData, hasKey("NNP"));
        assertThat(entityData, hasKey("NNS"));
        assertThat(entityData, hasKey("PUNCT"));
        assertThat(entityData, hasKey("RB"));
        assertThat(entityData, hasKey("VBD"));
        assertThat(entityData, hasKey("VBN"));
        assertThat(entityData, hasKey("NN"));
        assertThat(entityData, hasKey("VBZ"));
    }

    public void testConstructorTagsSpecified() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("field", "source_field");
        config.put("target_field", "target_field");
        config.put("tags", Arrays.asList("NN", "VBZ"));

        OpenNlpPosProcessor.Factory factory = new OpenNlpPosProcessor.Factory(service);
        Map<String, Processor.Factory> registry = Collections.emptyMap();
        OpenNlpPosProcessor processor = factory.create(registry, randomAlphaOfLength(10), config);

        Map<String, Object> entityData = getIngestDocumentData(processor);

        assertThat(entityData, not(hasKey("CC")));
        assertThat(entityData, not(hasKey("CD")));
        assertThat(entityData, not(hasKey("DT")));
        assertThat(entityData, not(hasKey("IN")));
        assertThat(entityData, not(hasKey("JJ")));
        assertThat(entityData, not(hasKey("JJS")));
        assertThat(entityData, not(hasKey("NNP")));
        assertThat(entityData, not(hasKey("NNS")));
        assertThat(entityData, not(hasKey("PUNCT")));
        assertThat(entityData, not(hasKey("RB")));
        assertThat(entityData, not(hasKey("VBD")));
        assertThat(entityData, not(hasKey("VBN")));
        assertThat(entityData, hasKey("NN"));
        assertThat(entityData, hasKey("VBZ"));
    }

    private Map<String, Object> getIngestDocumentData(OpenNlpPosProcessor processor) throws Exception {
        IngestDocument ingestDocument = getIngestDocument();
        processor.execute(ingestDocument);
        return getIngestDocumentData(ingestDocument);
    }

    private IngestDocument getIngestDocument() throws Exception {
        return getIngestDocument("Kobe Bryant was one of the best basketball players of all times. Not even Michael Jordan has ever " +
                "scored 81 points in one game. Munich is really an awesome city, but New York is as well. Yesterday has been the " +
                "hottest day of the year.");
    }

    private IngestDocument getIngestDocument(String content) throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("source_field", content);
        return RandomDocumentPicks.randomIngestDocument(random(), document);
    }

    private Map<String, Object> getIngestDocumentData(IngestDocument ingestDocument) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) ingestDocument.getSourceAndMetadata().get("target_field");
        return data;
    }

    private <T extends java.lang.Comparable<T>> void assertThatHasValue(Map<String, Object> entityData, String field,
                                                                        Class<T> clazz, T value) {
        assertThat(entityData, hasKey(field));
        assertThat(entityData.get(field), instanceOf(clazz));
        @SuppressWarnings("unchecked")
        T v = (T) entityData.get(field);
        assertThat(v, comparesEqualTo(value));
    }
}
