package co.navdeep.kafkaer.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class Config {
    private List<Topic> topics;
    private List<Broker> brokers;

    public List<String> getAllTopicNames(){
        List<String> names = new ArrayList<>();
        for(Topic t : topics){
            names.add(t.getName());
        }
        return names;
    }

    public boolean hasBrokerConfig(){
        return brokers != null && !brokers.isEmpty();
    }
}
