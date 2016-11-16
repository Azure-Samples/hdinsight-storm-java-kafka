package com.microsoft.example;

import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.tuple.Tuple;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;


public class LoggerBolt extends BaseBasicBolt {
    //Create logger for this class

    private static final Logger logger = LogManager.getLogger(LoggerBolt.class);

    @Override
    public  void execute(Tuple input, BasicOutputCollector collector) {
        String data = input.getString(0);

        logger.info("Received data: " + data);
    }

    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        // we're not emitting anything
    }
}
