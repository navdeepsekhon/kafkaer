package co.navdeep.kafkaer.model;

import co.navdeep.kafkaer.utils.Utils;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.NewTopic;

import java.util.Map;

@Data
@RequiredArgsConstructor
@NoArgsConstructor
public class Topic {
    @NonNull private String name;
    @NonNull private int partitions;
    @NonNull private short replicationFactor;
    private Map<String, String> configs;

    public NewTopic toNewTopic(){
        NewTopic newTopic = new NewTopic(name, partitions, replicationFactor);
        if(configs != null)
            newTopic.configs(configs);
        return newTopic;
    }

    public boolean hasConfigs(){
        return configs != null && !configs.isEmpty();
    }
    public Config configsAsKafkaConfig(){
        return Utils.configsAsKafkaConfig(configs);
    }
}
