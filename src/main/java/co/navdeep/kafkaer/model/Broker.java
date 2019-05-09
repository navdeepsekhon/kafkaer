package co.navdeep.kafkaer.model;

import co.navdeep.kafkaer.utils.Utils;
import lombok.Data;
import org.apache.kafka.clients.admin.Config;

import java.util.Map;

@Data
public class Broker {
    private String id;
    private Map<String, String> config;

    public Config configsAsKafkaConfig(){
        return Utils.configsAsKafkaConfig(config);
    }

    public String getId(){
        return id == null ? "" : id;
    }
}
