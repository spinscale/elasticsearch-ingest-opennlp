# Elasticsearch OpenNLP Ingest Processor

I wrote a [opennlp mapping plugin](https://github.com/spinscale/elasticsearch-opennlp-plugin) a couple of years ago and people asked me, why I did not update it. The main reason was, that it was a bad architectural choice as mentioned in the [openlp plugin README](https://github.com/spinscale/elasticsearch-opennlp-plugin#elasticsearch-opennlp-plugin). With the introduction of ingest processors in Elasticsearch 5.0 this problem has been resolved.

This processor is doing named/date/location/'whatever you have a model for' entity recognition and stores the output in the JSON before it is being stored.

This plugin is also intended to show you, that using gradle as a build system makes it very easy to reuse the testing facilities that elasticsearch already provides. First, you can run regular tests, but by adding a rest test, the plugin will be packaged and unzipped against elasticsearch, allowing you to execute a real end-to-end test, by just adding a java test class.

## Installation

| ES    | Command |
| ----- | ------- |
| 7.4.2 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/7.4.2.1/ingest-opennlp-7.4.2.1.zip` |
| 7.4.1 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/7.4.1.1/ingest-opennlp-7.4.1.1.zip` |
| 7.4.0 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/7.4.0.1/ingest-opennlp-7.4.0.1.zip` |
| 7.3.2 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/7.3.2.1/ingest-opennlp-7.3.2.1.zip` |
| 7.3.1 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/7.3.1.1/ingest-opennlp-7.3.1.1.zip` |
| 7.3.0 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/7.3.0.1/ingest-opennlp-7.3.0.1.zip` |
| 7.2.1 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/7.2.1.1/ingest-opennlp-7.2.1.1.zip` |
| 7.2.0 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/7.2.0.1/ingest-opennlp-7.2.0.1.zip` |
| 7.1.1 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/7.1.1.1/ingest-opennlp-7.1.1.1.zip` |
| 7.1.0 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/7.1.0.1/ingest-opennlp-7.1.0.1.zip` |
| 7.0.1 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/7.0.1.1/ingest-opennlp-7.0.1.1.zip` |
| 7.0.0 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/7.0.0.1/ingest-opennlp-7.0.0.1.zip` |
| 6.8.5 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/6.8.5.1/ingest-opennlp-6.8.5.1.zip` |
| 6.8.4 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/6.8.4.1/ingest-opennlp-6.8.4.1.zip` |
| 6.8.3 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/6.8.3.1/ingest-opennlp-6.8.3.1.zip` |
| 6.8.2 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/6.8.2.1/ingest-opennlp-6.8.2.1.zip` |
| 6.8.1 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/6.8.1.1/ingest-opennlp-6.8.1.1.zip` |
| 6.8.0 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/6.8.0.1/ingest-opennlp-6.8.0.1.zip` |
| 6.7.2 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/6.7.2.1/ingest-opennlp-6.7.2.1.zip` |
| 6.7.1 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/6.7.1.1/ingest-opennlp-6.7.1.1.zip` |
| 6.7.0 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/6.7.0.1/ingest-opennlp-6.7.0.1.zip` |
| 6.6.2 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/6.6.2.1/ingest-opennlp-6.6.2.1.zip` |
| 6.6.1 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/6.6.1.1/ingest-opennlp-6.6.1.1.zip` |
| 6.6.0 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/6.6.0.1/ingest-opennlp-6.6.0.1.zip` |
| 6.5.4 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/6.5.4.1/ingest-opennlp-6.5.4.1.zip` |
| 6.5.3 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/6.5.3.1/ingest-opennlp-6.5.3.1.zip` |
| 6.5.2 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/6.5.2.1/ingest-opennlp-6.5.2.1.zip` |
| 6.5.1 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/6.5.1.1/ingest-opennlp-6.5.1.1.zip` |
| 6.5.0 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/6.5.0.1/ingest-opennlp-6.5.0.1.zip` |
| 6.4.3 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/6.4.3.1/ingest-opennlp-6.4.3.1.zip` |
| 6.4.2 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/6.4.2.1/ingest-opennlp-6.4.2.1.zip` |
| 6.4.1 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/6.4.1.1/ingest-opennlp-6.4.1.1.zip` |
| 6.4.0 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/6.4.0.1/ingest-opennlp-6.4.0.1.zip` |
| 6.3.2 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/6.3.2.1/ingest-opennlp-6.3.2.1.zip` |
| 6.3.1 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/6.3.1.1/ingest-opennlp-6.3.1.1.zip` |
| 6.3.0 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/6.3.0.1/ingest-opennlp-6.3.0.1.zip` |
| 6.2.4 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/6.2.4.1/ingest-opennlp-6.2.4.1.zip` |
| 6.2.3 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/6.2.3.1/ingest-opennlp-6.2.3.1.zip` |
| 6.2.2 | `bin/elasticsearch-plugin install https://github.com/spinscale/elasticsearch-ingest-opennlp/releases/download/6.2.2.1/ingest-opennlp-6.2.2.1.zip` |
| 5.2.0 | `bin/elasticsearch-plugin install https://oss.sonatype.org/content/repositories/releases/de/spinscale/elasticsearch/plugin/ingest/ingest-opennlp/5.2.0.1/ingest-opennlp-5.2.0.1.zip` |
| 5.1.2 | `bin/elasticsearch-plugin install https://oss.sonatype.org/content/repositories/releases/de/spinscale/elasticsearch/plugin/ingest/ingest-opennlp/5.1.2.1/ingest-opennlp-5.1.2.1.zip` |
| 5.1.1 | `bin/elasticsearch-plugin install https://oss.sonatype.org/content/repositories/releases/de/spinscale/elasticsearch/plugin/ingest/ingest-opennlp/5.1.1.1/ingest-opennlp-5.1.1.1.zip` |

**IMPORTANT**: If you are running this plugin with Elasticsearch 6.5.2 or
newer, you need to download the NER models from sourceforge after
installation.

To download the models, run the following under Linux and osx (this is in
the `bin` directory of your Elasticsearch installation)

```
bin/ingest-opennlp/download-models
```

If you are using windows, please use the following command

```
bin\ingest-opennlp\download-models.bat
```


## Usage

This is how you configure a pipeline with support for opennlp

You can add the following lines to the `config/elasticsearch.yml` (as those models are shipped by default, they are easy to enable). The models are looked up in the `config/ingest-opennlp/` directory.

```
ingest.opennlp.model.file.persons: en-ner-persons.bin
ingest.opennlp.model.file.dates: en-ner-dates.bin
ingest.opennlp.model.file.locations: en-ner-locations.bin
```

Now fire up Elasticsearch and configure a pipeline

```
PUT _ingest/pipeline/opennlp-pipeline
{
  "description": "A pipeline to do named entity extraction",
  "processors": [
    {
      "opennlp" : {
        "field" : "my_field"
      }
    }
  ]
}

PUT /my-index/my-type/1?pipeline=opennlp-pipeline
{
  "my_field" : "Kobe Bryant was one of the best basketball players of all times. Not even Michael Jordan has ever scored 81 points in one game. Munich is really an awesome city, but New York is as well. Yesterday has been the hottest day of the year."
}

GET /my-index/my-type/1
{
  "my_field" : "Kobe Bryant was one of the best basketball players of all times. Not even Michael Jordan has ever scored 81 points in one game. Munich is really an awesome city, but New York is as well. Yesterday has been the hottest day of the year.",
  "entities" : {
    "locations" : [ "Munich", "New York" ],
    "dates" : [ "Yesterday" ],
    "names" : [ "Kobe Bryant", "Michael Jordan" ]
  }
}
```

You can also specify only certain named entities in the processor, i.e. if you only want to extract names


```
PUT _ingest/pipeline/opennlp-pipeline
{
  "description": "A pipeline to do named entity extraction",
  "processors": [
    {
      "opennlp" : {
        "field" : "my_field"
        "fields" : [ "names" ]
      }
    }
  ]
}
```

You can also emit text in the format used by the [annotated text plugin](https://www.elastic.co/guide/en/elasticsearch/plugins/current/mapper-annotated-text.html).

```
PUT _ingest/pipeline/opennlp-pipeline
{
  "description": "A pipeline to do named entity extraction",
  "processors": [
    {
      "opennlp" : {
        "field" : "my_field",
        "annotated_text_field" : "my_annotated_text_field"
      }
    }
  ]
}
```

**Note: The creation of annotated text field syntax is only supported when running on Elasticsearch 7.0.1 onwards**


## Configuration

You can configure own models per field, the setting for this is prefixed `ingest.opennlp.model.file.`. So you can configure any model with any field name, by specifying a name and a path to file, like the three examples below:

| Parameter | Use |
| --- | --- |
| ingest.opennlp.model.file.name     | Configure the file for named entity recognition for the field name        |
| ingest.opennlp.model.file.date     | Configure the file for date entity recognition for the field date         |
| ingest.opennlp.model.file.person   | Configure the file for person entity recognition for the field person     |
| ingest.opennlp.model.file.WHATEVER | Configure the file for WHATEVER entity recognition for the field WHATEVER |

## Development setup & running tests

In order to install this plugin, you need to create a zip distribution first by running

```bash
./gradlew clean check
```

This will produce a zip file in `build/distributions`. As part of the build, the models are packaged into the zip file, but need to be downloaded before. There is a special task in the `build.gradle` which is downloading the models, in case they dont exist.

After building the zip file, you can install it like this

```bash
bin/plugin install file:///path/to/elasticsearch-ingest-opennlp/build/distribution/ingest-opennlp-X.Y.Z-SNAPSHOT.zip
```

Ensure that you have the models downloaded, before testing.

## Bugs & TODO

* A couple of groovy build mechanisms from core are disabled. See the `build.gradle` for further explanations
* Only the most basic NLP functions are exposed, please fork and add your own code to this!

