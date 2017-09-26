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

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.util.Span;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.util.Supplier;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.StopWatch;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.set.Sets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OpenNLP name finders are not thread safe, so we load them via a thread local hack
 */
public class OpenNlpService {

    private final Path configDirectory;
    private final Logger logger;
    private Settings settings;

    private Map<String, TokenNameFinderModel> nameFinderModels = new ConcurrentHashMap<>();

    private POSModel posModel = null;

    public OpenNlpService(Path configDirectory, Settings settings) {
        this.logger = Loggers.getLogger(getClass(), settings);
        this.configDirectory = configDirectory;
        this.settings = settings;
    }

    public Map<String, Object> getModels() {
        return IngestOpenNlpPlugin.MODEL_FILE_SETTINGS.get(settings).getAsStructuredMap();
    }

    @FunctionalInterface
    private interface ThrowingConsumer<T> {
        void accept(T t) throws IOException;
    }

    private void loadModel(StopWatch sw, String name, String value, ThrowingConsumer<InputStream> loaderConsumer) {
        sw.start(name);
        Path path = configDirectory.resolve(value);
        try (InputStream is = Files.newInputStream(path)) {
            loaderConsumer.accept(is);
        } catch (IOException e) {
            logger.error((Supplier<?>) () -> new ParameterizedMessage("Could not load model {} with path [{}]", name, path), e);
        }
        sw.stop();
    }

    protected OpenNlpService start() {
        StopWatch sw = new StopWatch("models-loading");
        Map<String, Object> settingsMap = getModels();
        for (Map.Entry<String, Object> entry : settingsMap.entrySet()) {
            String type = entry.getKey();
            switch (type) {
                case "ner":
                    @SuppressWarnings("unchecked") Map<String, String> nerSettings = (Map)entry.getValue();
                    for (Map.Entry<String, String> nerEntry : nerSettings.entrySet()) {
                        String name = nerEntry.getKey();
                        loadModel(sw, "[" + type + "] " + name, nerEntry.getValue(),
                                (is) -> nameFinderModels.put(name, new TokenNameFinderModel(is)));
                    }
                    break;

                case "pos":
                    if (posModel == null) {
                        loadModel(sw, "[pos]", (String) entry.getValue(), (is) -> posModel = new POSModel(is));
                    }
                    break;
            }
        }

        if (settingsMap.keySet().size() == 0) {
            logger.error("Did not load any models for ingest-opennlp plugin, none configured");
        } else {
            logger.info("Read models in [{}] for {}", sw.totalTime(), settingsMap.keySet());
        }

        return this;
    }

    public Set<String> find(String content, String field) {
        if (!nameFinderModels.containsKey(field)) {
            throw new ElasticsearchException("Could not find field [{}], possible values {}", field, nameFinderModels.keySet());
        }
        TokenNameFinderModel finderModel = nameFinderModels.get(field);
        String[] tokens = SimpleTokenizer.INSTANCE.tokenize(content);
        Span spans[] = new NameFinderME(finderModel).find(tokens);
        String[] names = Span.spansToStrings(spans, tokens);
        return Sets.newHashSet(names);
    }

    public Map<String, Number> countTags(String content, @Nullable Set<String> tagSet, boolean normalize) {
        Map<String, Number> map = new TreeMap<>();
        String[] tokens = SimpleTokenizer.INSTANCE.tokenize(content);
        String tags[] = new POSTaggerME(posModel).tag(tokens);
        for (int i = 0; i < tags.length; i++) {
            String tag = fixTag(tags[i]);
            if (tagSet != null && !tagSet.contains(tag)) {
                continue;
            }
            int count = map.containsKey(tag) ? map.get(tag).intValue() : 0;
            map.put(tag, ++count);
        }
        if (normalize) {
            map.replaceAll((k, v) -> v.doubleValue() / tags.length);
        }
        return map;
    }

    private static String fixTag(String tag) {
        // Remove non-word characters from tag. A word character is a character from a-z, A-Z, 0-9, including the _ (underscore) character.
        tag = tag.replaceAll("[\\W]", "");
        return tag.isEmpty() ? "PUNCT" : tag;
    }
}
