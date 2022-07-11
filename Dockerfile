FROM docker.elastic.co/elasticsearch/elasticsearch:8.3.2

ADD ./src/test/resources/models/en-ner-persons.bin /usr/share/elasticsearch/config/ingest-opennlp/
ADD ./src/test/resources/models/en-ner-locations.bin /usr/share/elasticsearch/config/ingest-opennlp/
ADD ./src/test/resources/models/en-ner-dates.bin /usr/share/elasticsearch/config/ingest-opennlp/

ENV ES_SETTING_INGEST_OPENNLP_MODEL_FILE_NAMES=en-ner-persons.bin
ENV ES_SETTING_INGEST_OPENNLP_MODEL_FILE_LOCATIONS=en-ner-locations.bin
ENV ES_SETTING_INGEST_OPENNLP_MODEL_FILE_DATES=en-ner-dates.bin

ADD build/distribution/elasticsearch-ingest-opennlp.zip /elasticsearch-ingest-opennlp.zip
RUN /usr/share/elasticsearch/bin/elasticsearch-plugin install --batch file:///elasticsearch-ingest-opennlp.zip
EXPOSE 9200
