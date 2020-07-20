package co.navdeep.kafkaer;

import co.navdeep.kafkaer.model.Broker;
import co.navdeep.kafkaer.model.Config;
import co.navdeep.kafkaer.model.Topic;
import co.navdeep.kafkaer.utils.Utils;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import lombok.Data;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Data
public class Configurator {
    private Configuration properties;
    private Config config;
    private AdminClient adminClient;
    private SchemaRegistryClient schemaRegistryClient;

    private static Logger logger = LoggerFactory.getLogger(Configurator.class);

    public Configurator(String propertiesLocation, String configLocation) throws ConfigurationException, IOException {
        properties = Utils.readProperties(propertiesLocation);
        config = Utils.readConfig(configLocation, Utils.propertiesToMap(properties));
        adminClient = AdminClient.create(Utils.getClientConfig(properties));
        initializeSchemaRegistryClient();
    }

    public Configurator(Configuration p, Config c){
        properties = p;
        config = c;
        adminClient = AdminClient.create(Utils.getClientConfig(properties));
        initializeSchemaRegistryClient();
    }

    private void initializeSchemaRegistryClient(){
        String url = Utils.getSchemaRegistryUrl(properties);
        if(StringUtils.isNotBlank(url)){
            schemaRegistryClient = new CachedSchemaRegistryClient(url, 12384, Utils.getSchemaRegistryConfigs(properties));
        }
    }

    @Deprecated
    public void wipeTopics() throws ExecutionException, InterruptedException {
        wipeTopics(false, false);
    }

    public void wipeTopics(boolean confirmDelete, boolean wipeSchema) throws ExecutionException, InterruptedException {
        logger.debug("Deleting topics");
        DeleteTopicsResult result = adminClient.deleteTopics(config.getAllTopicNames());
        for(String topic : result.values().keySet()){
            try {
                logger.debug("Deleting topic: {}", topic);
                result.values().get(topic).get();
                if(confirmDelete) waitForDelete(topic);
            } catch(ExecutionException e){
                if(e.getCause() instanceof UnknownTopicOrPartitionException){
                    logger.debug("Unable to delete topic {} because it does not exist.", topic);
                } else {
                    throw new ExecutionException(e);
                }
            } finally {
                if(wipeSchema) {
                    try {
                        wipeSchema(topic);
                    } catch (IOException | RestClientException e) {
                        logger.error("Error deleting schema for [{}]", topic);
                        throw new ExecutionException(e);
                    }
                }
            }

        }
    }

    public void wipeSchema(String topicName) throws IOException, RestClientException {
        if(schemaRegistryClient == null){
            logger.warn("No schema registry configured. Set property [{}]", Utils.SCHEMA_REGISTRY_URL_CONFIG);
            return;
        }

        Collection<String> currentSubjects = schemaRegistryClient.getAllSubjects();
        for(String subject : currentSubjects){
            if(StringUtils.contains(subject, topicName)){
                logger.debug("Deleting subject [{}] from schema registry", subject);
                schemaRegistryClient.deleteSubject(subject);
            }
        }
    }
    private void waitForDelete(String topicName) throws ExecutionException, InterruptedException {
        int maxWaitTime = Utils.getMaxDeleteConfirmWaitTime(properties);
        int maxTries = Math.floorDiv(maxWaitTime, 5);
        int tries = 0;
        logger.debug("Confirming topic [{}] was deleted from all brokers. Will wait for max [{}]s", topicName, maxWaitTime);
        while(tries < maxTries){
            DescribeTopicsResult result = adminClient.describeTopics(Collections.singletonList(topicName));
            try{
                result.values().get(topicName).get();
            } catch(Exception e){
                if(e.getCause() instanceof UnknownTopicOrPartitionException){
                    logger.debug("Confirmed: topic [{}] was deleted.", topicName);
                    return;
                }
                throw e;
            }
            Thread.sleep(5000);
            tries++;
        }
    }
    public void applyConfig() throws ExecutionException, InterruptedException {
        configureTopics();
        configureBrokers();
        configureAcls();
    }

    public void configureAcls() throws ExecutionException, InterruptedException {
        logger.debug("Configuring ACLs");
        List<AclBinding> bindings = config.getAclBindings();
        if(bindings.isEmpty()){
            logger.debug("No ACLs defined in config. Nothing done.");
            return;
        }

        CreateAclsResult result = adminClient.createAcls(bindings);
        for(AclBinding binding : result.values().keySet()){
            logger.debug("Creating ACL {}", binding);
            result.values().get(binding).get();
        }
    }

    public void configureBrokers() throws ExecutionException, InterruptedException {
        logger.debug("Configuring brokers");
        if(!config.hasBrokerConfig()){
            logger.debug("No broker configs defined. Nothing done.");
            return;
        }

        Map<ConfigResource, org.apache.kafka.clients.admin.Config> updateConfig = new HashMap<>();
        for(Broker broker : config.getBrokers()){
            logger.debug("Applying broker config {}", broker);
            ConfigResource configResource = new ConfigResource(ConfigResource.Type.BROKER, broker.getId());
            updateConfig.put(configResource, broker.configsAsKafkaConfig());
        }

        AlterConfigsResult result = adminClient.alterConfigs(updateConfig);
        result.all().get();

    }

    public void configureTopics() throws ExecutionException, InterruptedException {
        logger.debug("Configuring topics");
        Map<String, KafkaFuture<TopicDescription>> topicResults = adminClient.describeTopics(config.getAllTopicNames()).values();
        for(Topic topic : config.getTopics()){
            logger.debug("Topic config: {}", topic);
            try {
                TopicDescription td = topicResults.get(topic.getName()).get();
                logger.debug("Updating existing topic {}", topic.getName());
                handleTopicPartitionsUpdate(td, topic);
                handleTopicConfigUpdate(topic);
            } catch(ExecutionException e){
                if(e.getCause() instanceof UnknownTopicOrPartitionException) {
                    logger.debug("Creating new topic {}", topic.getName());
                    CreateTopicsResult result = adminClient.createTopics(Collections.singleton(topic.toNewTopic()));
                    result.all().get();
                } else {
                    throw(e);
                }
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
                logger.debug("Updating partition count for topic {} from [{}] to [{}]", topic.getName(), current.partitions().size(), topic.getPartitions());
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
