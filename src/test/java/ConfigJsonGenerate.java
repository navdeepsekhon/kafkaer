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
        topic.setName("test");
        topic.setConfigs(new HashMap<>());
        topic.getConfigs().put("cleanup.policy", "compact");
        topic.getConfigs().put("compression.type", "gzip");
        config.setTopics(new ArrayList<>());
        config.getTopics().add(topic);

        Broker broker = new Broker();
        broker.setId("1");
        broker.setConfig(new HashMap<>());
        broker.getConfig().put("sasl.login.refresh.window.jitter", "0.05");
        config.setBrokers(new ArrayList<>());
        config.getBrokers().add(broker);
        ObjectMapper mapper = new ObjectMapper();
        System.out.println(mapper.writeValueAsString(config));
    }
}
