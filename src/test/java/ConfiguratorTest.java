import co.navdeep.kafkaer.Configurator;
import co.navdeep.kafkaer.model.Config;
import co.navdeep.kafkaer.model.Topic;
import co.navdeep.kafkaer.utils.Utils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.config.ConfigResource;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class ConfiguratorTest {

    final static String PROPERTIES_LOCATION="src/test/resources/test.properties";
    final static String CONFIG_LOCATION="src/test/resources/kafka-config.json";
    static AdminClient adminClient;

    @BeforeClass
    public static void setup() throws ConfigurationException {
        Configuration properties = Utils.readProperties(PROPERTIES_LOCATION);
        adminClient = AdminClient.create(Utils.getClientConfig(properties));
    }

    @Test
    public void testReadConfig() throws IOException, ConfigurationException {
        Configurator configurator = new Configurator(PROPERTIES_LOCATION, CONFIG_LOCATION);
        Config config = configurator.getConfig();
        Assert.assertFalse(config.getTopics().isEmpty());
        Assert.assertEquals(config.getTopics().get(0).getName(), "withSuffix-iamasuffix");
        Assert.assertEquals(config.getTopics().get(0).getPartitions(), 1);
        Assert.assertEquals(config.getTopics().get(0).getReplicationFactor(), 1);
        Assert.assertEquals(config.getTopics().get(0).getConfigs().get("compression.type"), "gzip");
    }

    @Test
    public void testTopicCreation() throws ExecutionException, InterruptedException, ConfigurationException {
        Config config = new Config();
        String topicName = UUID.randomUUID().toString();
        Topic topic = new Topic(topicName, 1, (short)1);
        config.getTopics().add(topic);

        Configurator configurator = new Configurator(Utils.readProperties(PROPERTIES_LOCATION), config);
        configurator.applyConfig();

        compareWithKafkaTopic(topic);
    }

    @Test
    public void testMultipleTopicCreation() throws ExecutionException, InterruptedException, ConfigurationException {
        Config config = new Config();
        String topicName = UUID.randomUUID().toString();
        String topicName2 = UUID.randomUUID().toString();
        Topic topic = new Topic(topicName, 1, (short)1);
        Topic topic2 = new Topic(topicName2, 2, (short)1);
        config.getTopics().add(topic);
        config.getTopics().add(topic2);

        Configurator configurator = new Configurator(Utils.readProperties(PROPERTIES_LOCATION), config);
        configurator.applyConfig();

        compareWithKafkaTopic(topic);
        compareWithKafkaTopic(topic2);
    }

    @Test
    public void testTopicCreationWithConfigs() throws ExecutionException, InterruptedException, ConfigurationException {
        Config config = new Config();
        Topic topic = new Topic(UUID.randomUUID().toString(), 1, (short)1);
        topic.setConfigs(Collections.singletonMap("delete.retention.ms", "123"));
        config.getTopics().add(topic);

        Configurator configurator = new Configurator(Utils.readProperties(PROPERTIES_LOCATION), config);
        configurator.applyConfig();

        ConfigResource configResource = new ConfigResource(ConfigResource.Type.TOPIC, topic.getName());
        DescribeConfigsResult result = adminClient.describeConfigs(Collections.singletonList(configResource));

        org.apache.kafka.clients.admin.Config topicConfig = result.all().get().get(configResource);

        Assert.assertEquals(topicConfig.get("delete.retention.ms").value(), "123");
    }

    private void compareWithKafkaTopic(Topic topic) throws ExecutionException, InterruptedException {
        DescribeTopicsResult result = adminClient.describeTopics(Collections.singletonList(topic.getName()));
        TopicDescription kafkaTopic = result.all().get().get(topic.getName());
        Assert.assertNotNull(kafkaTopic);
        Assert.assertEquals(kafkaTopic.partitions().size(), topic.getPartitions());
        Assert.assertEquals(kafkaTopic.partitions().get(0).replicas().size(), topic.getReplicationFactor());
    }

}
