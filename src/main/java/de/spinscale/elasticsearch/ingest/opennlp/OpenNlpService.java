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
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.util.Span;
import org.elasticsearch.common.StopWatch;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.set.Sets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * OpenNLP name finders are not thread safe, so we load them via a thread local hack
 */
public class OpenNlpService {

    private final Path configDirectory;
    private final ESLogger logger;
    private Settings settings;

    private ThreadLocal<NameFinderME> nameFinderMEThreadLocal = new ThreadLocal<>();
    private ThreadLocal<NameFinderME> dateFinderMEThreadLocal = new ThreadLocal<>();
    private ThreadLocal<NameFinderME> locationFinderMEThreadLocal = new ThreadLocal<>();
    private TokenNameFinderModel nameFinderModel;
    private TokenNameFinderModel dateFinderModel;
    private TokenNameFinderModel locationFinderModel;

    public OpenNlpService(Path configDirectory, Settings settings) {
        this.logger = Loggers.getLogger(getClass(), settings);
        this.configDirectory = configDirectory;
        this.settings = settings;
    }

    protected OpenNlpService start() throws IOException {
        StopWatch sw = new StopWatch("models-loading").start("names");
        Path namePath = configDirectory.resolve(IngestOpenNlpPlugin.MODEL_NAME_FILE_SETTING.get(settings));
        try (InputStream is = Files.newInputStream(namePath)) {
            nameFinderModel = new TokenNameFinderModel(is);
        }

        sw.stop().start("dates");

        Path datePath = configDirectory.resolve(IngestOpenNlpPlugin.MODEL_DATE_FILE_SETTING.get(settings));
        try (InputStream is = Files.newInputStream(datePath)) {
            dateFinderModel = new TokenNameFinderModel(is);
        }

        sw.stop().start("locations");

        Path locationPath = configDirectory.resolve(IngestOpenNlpPlugin.MODEL_LOCATION_FILE_SETTING.get(settings));
        try (InputStream is = Files.newInputStream(locationPath)) {
            locationFinderModel = new TokenNameFinderModel(is);
        }

        sw.stop();
        logger.info("Read models in [{}]", sw.totalTime());

        return this;
    }

    public Set<String> findNames(String content) {
        return find(content, nameFinderMEThreadLocal, nameFinderModel);
    }

    public Set<String> findDates(String content) {
        return find(content, dateFinderMEThreadLocal, dateFinderModel);
    }

    public Set<String> findLocations(String content) {
        return find(content, locationFinderMEThreadLocal, locationFinderModel);
    }

    private Set<String> find(String content, ThreadLocal<NameFinderME> finder, TokenNameFinderModel model) {
        String[] tokens = SimpleTokenizer.INSTANCE.tokenize(content);
        if (finder.get() == null) {
            finder.set(new NameFinderME(model));
        }
        Span spans[] = finder.get().find(tokens);
        String[] names = Span.spansToStrings(spans, tokens);
        return Sets.newHashSet(names);
    }
}
