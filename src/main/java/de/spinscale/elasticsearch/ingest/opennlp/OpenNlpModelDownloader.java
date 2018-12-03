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

import joptsimple.OptionSet;
import org.elasticsearch.cli.EnvironmentAwareCommand;
import org.elasticsearch.cli.SuppressForbidden;
import org.elasticsearch.cli.Terminal;
import org.elasticsearch.env.Environment;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

public class OpenNlpModelDownloader extends EnvironmentAwareCommand {

    public static void main(String[] args) throws Exception {
        exit(new OpenNlpModelDownloader().main(args, Terminal.DEFAULT));
    }

    public OpenNlpModelDownloader() {
        super("Downloads three sample models for named entity recognition for dates, locations and persons");
    }

    @Override
    protected void execute(Terminal terminal, OptionSet options, Environment env) throws Exception {
        Path configDirectoryPath = env.configFile().resolve(IngestOpenNlpPlugin.NAME).normalize().toAbsolutePath();
        if (Files.exists(configDirectoryPath) == false) {
            Files.createDirectories(configDirectoryPath);
        }

        String baseUrl = "http://opennlp.sourceforge.net/models-1.5/";
        download(baseUrl + "en-ner-person.bin", configDirectoryPath.resolve("en-ner-persons.bin"), terminal);
        download(baseUrl + "en-ner-location.bin", configDirectoryPath.resolve("en-ner-locations.bin"), terminal);
        download(baseUrl + "en-ner-date.bin", configDirectoryPath.resolve("en-ner-dates.bin"), terminal);

        terminal.println("\nyou can use the following configuration settings now\n");

        terminal.println("ingest.opennlp.model.file.persons: en-ner-persons.bin");
        terminal.println("ingest.opennlp.model.file.dates: en-ner-dates.bin");
        terminal.println("ingest.opennlp.model.file.locations: en-ner-locations.bin");
    }

    @SuppressForbidden(reason = "we have to download the models, so we have to open a socket")
    private void download(String url, Path filename, Terminal terminal) throws Exception {
        terminal.print(Terminal.Verbosity.NORMAL,"Downloading " + filename.getFileName() + " model... ");
        if (Files.exists(filename)) {
            terminal.println("not downloading, existed already.");
        } else {
            URLConnection connection = new URL(url).openConnection();
            connection.connect();

            try (InputStream inputStream = connection.getInputStream()) {
                Files.copy(inputStream, filename);
            }
            terminal.println("done");
        }
    }
}
