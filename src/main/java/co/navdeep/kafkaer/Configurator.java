package co.navdeep.kafkaer;

import co.navdeep.kafkaer.model.Config;
import co.navdeep.kafkaer.model.Topic;
import co.navdeep.kafkaer.utils.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.config.ConfigResource;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static java.nio.charset.StandardCharsets.UTF_8;

@Data
public class Configurator {
    private Configuration properties;
    private Config config;
    private AdminClient adminClient;

    public Configurator(String propertiesLocation, String configLocation) throws ConfigurationException, IOException {
        properties = Utils.readProperties(propertiesLocation);
        config = readConfig(configLocation, Utils.propertiesToMap(properties));
        adminClient = AdminClient.create(Utils.getClientConfig(properties));
    }

    private Config readConfig(String location, Map<String, String> valueMap) throws IOException {
        String configString = FileUtils.readFileToString(new File(location), UTF_8);
        StringSubstitutor substitutor = new StringSubstitutor(valueMap);
        configString = substitutor.replace(configString);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(configString, Config.class);
    }

    public void applyConfig() throws ExecutionException, InterruptedException {
        configureTopics();
        configureBrokers();
    }

    public void configureBrokers() throws ExecutionException, InterruptedException {
        if(!config.hasBrokerConfig()) return;
        DescribeClusterResult describeClusterResult = adminClient.describeCluster();
        List<Node> nodes = new ArrayList<>(describeClusterResult.nodes().get());
        if(!nodes.isEmpty()){
            ConfigResource resource = new ConfigResource(ConfigResource.Type.BROKER, String.valueOf(nodes.get(0).id()));
            Map<ConfigResource, org.apache.kafka.clients.admin.Config> updateConfig = new HashMap<>();
            updateConfig.put(resource, config.getBrokers().get(0).configsAsKafkaConfig());
        }
    }
    public void configureTopics() throws ExecutionException, InterruptedException {
        Map<String, KafkaFuture<TopicDescription>> topicResults = adminClient.describeTopics(config.getAllTopicNames()).values();
        for(Topic topic : config.getTopics()){
            try {
                TopicDescription td = topicResults.get(topic.getName()).get();
                handleTopicPartitionsUpdate(td, topic);
                handleTopicConfigUpdate(topic);
            } catch(ExecutionException e){
                CreateTopicsResult result = adminClient.createTopics(Collections.singleton(topic.toNewTopic()));
                result.all().get();
            }
        }
    }

    private void handleTopicConfigUpdate(Topic topic) throws InterruptedException {
        ConfigResource configResource = new ConfigResource(ConfigResource.Type.TOPIC, topic.getName());
        Map<ConfigResource, org.apache.kafka.clients.admin.Config> updateConfig = new HashMap<>();
        updateConfig.put(configResource, topic.configsAsKafkaConfig());
        AlterConfigsResult alterConfigsResult = adminClient.alterConfigs(updateConfig);
        try {
            alterConfigsResult.all().get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
    private void handleTopicPartitionsUpdate(TopicDescription current, Topic topic) throws InterruptedException {
        try {
            if(current.partitions().size() < topic.getPartitions()){
                CreatePartitionsResult result = adminClient.createPartitions(Collections.singletonMap(topic.getName(), NewPartitions.increaseTo(topic.getPartitions())));
                result.all().get();
            } else if(current.partitions().size() > topic.getPartitions()){
                throw new RuntimeException("Can not reduce number of partitions for topic [" + topic.getName() + "] from current:" + current.partitions().size() + " to " + topic.getPartitions());
            }
        } catch(ExecutionException e){
            throw new RuntimeException(e);
        }
    }
}
