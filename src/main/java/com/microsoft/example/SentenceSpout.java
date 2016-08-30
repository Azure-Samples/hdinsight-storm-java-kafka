package com.microsoft.example;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;

import java.util.Map;
import java.util.Random;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

// Randomly emit sentences
public class SentenceSpout extends BaseRichSpout {
    // Collector used to emit
    private SpoutOutputCollector _collector;
    // Used to generate a random number
    private Random _rand;
    // Sentences that will be emitted
    private static String[] _sentences = {
            "the cow jumped over the moon",
            "an apple a day keeps the doctor away",
            "four score and seven years ago",
            "snow white and the seven dwarfs",
            "i am at two with nature"
    };

    private static final Logger logger = LogManager.getLogger(SentenceSpout.class);

    @Override
    public void open(Map config, TopologyContext context, SpoutOutputCollector collector) {
        //Store the collector for this instance
        _collector = collector;
        //Initialize random
        _rand = new Random();
    }

    // Emit data to the stream
    @Override
    public void nextTuple() {
        //Sleep for a few seconds
        Utils.sleep(100);
        //Pick a sentence
        Integer index = _rand.nextInt(_sentences.length);
        String sentence = _sentences[index];
        logger.info("Sending " + String.valueOf(index) + ": " + sentence);
        //Emit the sentence. In this case,
        // we are using index as the 'key', and sentence as the 'value'
        _collector.emit(new Values(String.valueOf(index),sentence));
    }

    //Ack is not implemented since this is a basic example
    @Override
    public void ack(Object id) {
    }

    //Fail is not implemented since this is a basic example
    @Override
    public void fail(Object id) {
    }

    //Declare the output fields.
    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {

        // The KafkaBolt expects a key & message to write to Kafka.
        // In this case, we use the FieldNameBasedTupleToKafkaMapper,
        // which actually expects the names to be 'key' and 'message'
        declarer.declare(new Fields("key", "message"));
    }
}
