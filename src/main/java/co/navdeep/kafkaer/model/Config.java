package co.navdeep.kafkaer.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true, fluent = true)
public class Config {
    private List<Topic> topics;
    private List<Broker> brokers;

    public Config(){
        topics = new ArrayList<>();
        brokers = new ArrayList<>();
    }

    public List<String> getAllTopicNames(){
        List<String> names = new ArrayList<>();
        for(Topic t : topics){
            names.add(t.name());
        }
        return names;
    }

    public boolean hasBrokerConfig(){
        return brokers != null && !brokers.isEmpty();
    }
}
