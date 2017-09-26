# Elasticsearch OpenNLP Ingest Processors

This is a fork from the [Elasticsearch OpenNLP Ingest Processor](https://github.com/spinscale/elasticsearch-ingest-opennlp) written by spinscale.

In addition to the original processor doing name/date/location/'whatever you have a model for' entity recognition, it adds a new processor for counting the different part of speech tags in the ingested document. Both processors save the output in the JSON before it is being stored.

<!---
## Installation

| ES    | Command |
| ----- | ------- |
| 5.2.0 | `bin/elasticsearch-plugin install https://oss.sonatype.org/content/repositories/releases/de/spinscale/elasticsearch/plugin/ingest/ingest-opennlp/5.2.0.1/ingest-opennlp-5.2.0.1.zip` |
| 5.1.2 | `bin/elasticsearch-plugin install https://oss.sonatype.org/content/repositories/releases/de/spinscale/elasticsearch/plugin/ingest/ingest-opennlp/5.1.2.1/ingest-opennlp-5.1.2.1.zip` |
| 5.1.1 | `bin/elasticsearch-plugin install https://oss.sonatype.org/content/repositories/releases/de/spinscale/elasticsearch/plugin/ingest/ingest-opennlp/5.1.1.1/ingest-opennlp-5.1.1.1.zip` |
-->
## Usage

The plugin provides two processors, opennlp_ner for doing named entity recognition and opennlp_pos for counting the part of speech tags.

### opennlp_ner

This is how you configure a pipeline with support for opennlp_ner. You can add the following lines to the `config/elasticsearch.yml` (as those models are shipped by default, they are easy to enable). The models are looked up in the `config/ingest-opennlp/` directory.

```
ingest.opennlp.model.file.ner.names: en-ner-persons.bin
ingest.opennlp.model.file.ner.dates: en-ner-dates.bin
ingest.opennlp.model.file.ner.locations: en-ner-locations.bin
```

Now fire up Elasticsearch and configure a pipeline

```
PUT _ingest/pipeline/opennlp-ner-pipeline
{
  "description": "A pipeline to do named entity extraction",
  "processors": [
    {
      "opennlp_ner" : {
        "field" : "my_field"
      }
    }
  ]
}

PUT /my-index/my-type/1?pipeline=opennlp-ner-pipeline
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
PUT _ingest/pipeline/opennlp-ner-pipeline
{
  "description": "A pipeline to do named entity extraction",
  "processors": [
    {
      "opennlp_ner" : {
        "field" : "my_field"
        "fields" : [ "names" ]
      }
    }
  ]
}
```

### opennlp_pos

You can add the following lines to the `config/elasticsearch.yml` (as those models are shipped by default, they are easy to enable). The models are looked up in the `config/ingest-opennlp/` directory.

```
ingest.opennlp.model.file.pos: en-pos-maxent.bin
```

Configure a pipeline

```
PUT _ingest/pipeline/opennlp-pos-pipeline
{
  "description": "A pipeline to count part of speech tags",
  "processors": [
    {
      "opennlp_pos" : {
        "field" : "my_field"
      }
    }
  ]
}

PUT /my-index/my-type/1?pipeline=opennlp-pos-pipeline
{
  "my_field" : "Kobe Bryant was one of the best basketball players of all times. Not even Michael Jordan has ever scored 81 points in one game. Munich is really an awesome city, but New York is as well. Yesterday has been the hottest day of the year."
}

GET /my-index/my-type/1
{
  "my_field" : "Kobe Bryant was one of the best basketball players of all times. Not even Michael Jordan has ever scored 81 points in one game. Munich is really an awesome city, but New York is as well. Yesterday has been the hottest day of the year.",
  "tags": {
    "CC": 1,
    "CD": 3,
    "DT": 5,
    "IN": 4,
    "JJ": 1,
    "JJS": 2,
    "NN": 6,
    "NNP": 7,
    "NNS": 3,
    "PUNCT": 5,
    "RB": 6,
    "VBD": 1,
    "VBN": 2,
    "VBZ": 4
  }
}
```

You can specify only certain tags in the processor, i.e. if you only want to count proper nouns


```
PUT _ingest/pipeline/opennlp-pos-pipeline
{
  "description": "A pipeline to do named entity extraction",
  "processors": [
    {
      "opennlp_pos" : {
        "field" : "my_field",
        "tags" : [ "NNP" ]
      }
    }
  ]
}
```

You can also count the proportion of each tags (as a number between 0 and 1)

```
PUT _ingest/pipeline/opennlp-pos-pipeline
{
  "description": "A pipeline to do named entity extraction",
  "processors": [
    {
      "opennlp_pos" : {
        "field" : "my_field",
        "normalize" : true
      }
    }
  ]
}

GET /my-index/my-type/1
{
  "my_field" : "Kobe Bryant was one of the best basketball players of all times. Not even Michael Jordan has ever scored 81 points in one game. Munich is really an awesome city, but New York is as well. Yesterday has been the hottest day of the year.",
  "tags": {
    "CC": 0.02,
    "CD": 0.06,
    "DT": 0.1,
    "IN": 0.08,
    "JJ": 0.02,
    "JJS": 0.04,
    "NN": 0.12,
    "NNP": 0.14,
    "NNS": 0.06,
    "PUNCT": 0.1,
    "RB": 0.12,
    "VBD": 0.02,
    "VBN": 0.04,
    "VBZ": 0.08
  }
}
```

## Configuration

### opennlp_ner

You can configure own models per field, the setting for this is prefixed `ingest.opennlp.model.file.ner`. So you can configure any model with any field name, by specifying a name and a path to file, like the three examples below:

| Parameter | Use |
| --- | --- |
| ingest.opennlp.model.file.ner.name     | Configure the file for named entity recognition for the field name    |
| ingest.opennlp.model.file.ner.date     | Configure the file for date entity recognition for the field date     |
| ingest.opennlp.model.file.ner.person   | Configure the file for person entity recognition for the field date     |
| ingest.opennlp.model.file.ner.WHATEVER | Configure the file for WHATEVER entity recognition for the field date     |

### opennlp_pos

| Parameter | Use |
| --- | --- |
| ingest.opennlp.model.file.pos | Configure the file for POS tagging |

## Setup

In order to install this plugin, you need to create a zip distribution first by running

```bash
gradle clean check
```

This will produce a zip file in `build/distributions`. As part of the build, the models are packaged into the zip file, but need to be downloaded before. There is a special task in the `build.gradle` which is downloading the models, in case they dont exist.

After building the zip file, you can install it like this

```bash
bin/elasticsearch-plugin install file:///path/to/elasticsearch-ingest-opennlp/build/distribution/ingest-opennlp-5.6.1.1-SNAPSHOT.zip
```

There is no need to configure anything, as the models art part of the zip file.

## Bugs & TODO

* A couple of groovy build mechanisms from core are disabled. See the `build.gradle` for further explanations
* Only the most basic NLP functions are exposed, please fork and add your own code to this!

