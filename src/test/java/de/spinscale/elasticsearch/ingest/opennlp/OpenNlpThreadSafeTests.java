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

import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.util.Supplier;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ESTestCase;
import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.Is.is;

public class OpenNlpThreadSafeTests extends ESTestCase {

    private OpenNlpService service;
    private ExecutorService executorService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Settings settings = Settings.builder()
                .put("ingest.opennlp.model.file.locations", "en-ner-locations.bin")
                .build();
        service = new OpenNlpService(getDataPath("/models/en-ner-persons.bin").getParent(), settings).start();
        executorService = Executors.newFixedThreadPool(10);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        terminate(executorService);
    }

    public void testThatOpenNlpServiceIsThreadSafe() throws Exception {
        int runs = 1000;
        CountDownLatch latch = new CountDownLatch(runs);
        List<OpennlpRunnable> runnables = new ArrayList<>();

        for (int i = 0; i < runs; i++) {
            String city = randomFrom("Munich", "Stockholm", "Madrid", "San Francisco", "Cologne", "Paris", "London", "Amsterdam");

            OpennlpRunnable runnable = new OpennlpRunnable(i, city, latch);
            runnables.add(runnable);
            executorService.submit(runnable);
        }

        latch.await(30, TimeUnit.SECONDS);
        for (OpennlpRunnable runnable : runnables) {
            runnable.assertResultIsCorrect();
        }
    }

    private class OpennlpRunnable implements Runnable {

        private int idx;
        final String city;
        private CountDownLatch latch;
        String result;

        OpennlpRunnable(int idx, String city, CountDownLatch latch) {
            this.idx = idx;
            this.city = city;
            this.latch = latch;
        }

        @Override
        public void run() {
            try {
                Set<String> locations = service.find(city + " is really an awesome city, but others are as well.", "locations");
                // logger.info("Got {}, expected {}, index {}", locations, city, idx);
                if (locations.size() > 0) {
                    result = locations.stream().findFirst().get();
                }
            } catch (Exception e) {
                logger.error((Supplier<?>) () -> new ParameterizedMessage("Unexpected exception"), e);
            } finally {
                latch.countDown();
            }
        }

        private void assertResultIsCorrect() {
            assertThat(String.format(Locale.ROOT, "Expected task %s to have result %s", idx, city), result, is(city));
        }
    }
}
