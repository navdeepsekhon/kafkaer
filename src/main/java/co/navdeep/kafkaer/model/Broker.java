package co.navdeep.kafkaer.model;

import co.navdeep.kafkaer.utils.Utils;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.kafka.clients.admin.Config;

import java.util.Map;

@Data
@Accessors(chain = true, fluent = true)
public class Broker {
    private String id;
    private Map<String, String> config;

    public Config configsAsKafkaConfig(){
        return Utils.configsAsKafkaConfig(config);
    }

    public boolean hasId(){
        return id != null && !"".equals(id);
    }
}
