package co.navdeep.kafkaer;

import co.navdeep.kafkaer.model.Broker;
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
import org.apache.kafka.common.acl.AclBinding;
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
        config = Utils.readConfig(configLocation, Utils.propertiesToMap(properties));
        adminClient = AdminClient.create(Utils.getClientConfig(properties));
    }

    public Configurator(Configuration p, Config c){
        properties = p;
        config = c;
        adminClient = AdminClient.create(Utils.getClientConfig(properties));
    }
    public void applyConfig() throws ExecutionException, InterruptedException {
        configureTopics();
        configureBrokers();
        configureAcls();
    }

    public void configureAcls() throws ExecutionException, InterruptedException {
        List<AclBinding> bindings = config.getAclBindings();
        if(bindings.isEmpty()) return;

        CreateAclsResult result = adminClient.createAcls(bindings);
        result.all().get();
    }

    public void configureBrokers() throws ExecutionException, InterruptedException {
        if(!config.hasBrokerConfig()) return;

        Map<ConfigResource, org.apache.kafka.clients.admin.Config> updateConfig = new HashMap<>();
        for(Broker broker : config.getBrokers()){
            ConfigResource configResource = new ConfigResource(ConfigResource.Type.BROKER, broker.getId());
            updateConfig.put(configResource, broker.configsAsKafkaConfig());
        }

        AlterConfigsResult result = adminClient.alterConfigs(updateConfig);
        result.all().get();

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
        if(!topic.hasConfigs()) return;
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
