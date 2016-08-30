package com.microsoft.example;

import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseBasicBolt;
import backtype.storm.tuple.Tuple;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Created by larryfr on 8/17/2016.
 */
public class PrinterBolt extends BaseBasicBolt {
    //Create logger for this class

    private static final Logger logger = LogManager.getLogger(PrinterBolt.class);

    public  void execute(Tuple input, BasicOutputCollector collector) {
        String data = input.getString(0);

//        System.out.println("Received: " + data);
        logger.info("Received data: " + data);
    }
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        // we're not emitting anything
    }
}
