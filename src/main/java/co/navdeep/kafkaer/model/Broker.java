package co.navdeep.kafkaer.model;

import co.navdeep.kafkaer.utils.Utils;
import org.apache.kafka.clients.admin.Config;

import java.util.Map;

public class Broker {
    Map<String, String> config;

    public Config configsAsKafkaConfig(){
        return Utils.configsAsKafkaConfig(config);
    }
}
