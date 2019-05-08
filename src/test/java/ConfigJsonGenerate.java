import co.navdeep.kafkaer.model.Broker;
import co.navdeep.kafkaer.model.Config;
import co.navdeep.kafkaer.model.Topic;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

public class ConfigJsonGenerate {
    @Test
    public void generateConfigJson() throws JsonProcessingException {
        Config config = new Config();
        Topic topic = new Topic();
        topic.name("test");
        topic.configs(new HashMap<>());
        topic.configs().put("cleanup.policy", "compact");
        topic.configs().put("compression.type", "gzip");
        config.topics(new ArrayList<>());
        config.topics().add(topic);

        Broker broker = new Broker();
        broker.id("1");
        broker.config(new HashMap<>());
        broker.config().put("sasl.login.refresh.window.jitter", "0.05");
        config.brokers(new ArrayList<>());
        config.brokers().add(broker);
        ObjectMapper mapper = new ObjectMapper();
        System.out.println(mapper.writeValueAsString(config));
    }
}
