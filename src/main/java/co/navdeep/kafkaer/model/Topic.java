package co.navdeep.kafkaer.model;

import co.navdeep.kafkaer.utils.Utils;
import lombok.Data;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.NewTopic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class Topic {
    private String name;
    private int partitions;
    private short replicationFactor;
    private Map<String, String> configs;

    public NewTopic toNewTopic(){
        NewTopic newTopic = new NewTopic(name, partitions, replicationFactor);
        if(configs != null)
            newTopic.configs(configs);
        return newTopic;
    }

    public Config configsAsKafkaConfig(){
        return Utils.configsAsKafkaConfig(configs);
    }
}
