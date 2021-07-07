import co.navdeep.kafkaer.Configurator;
import co.navdeep.kafkaer.model.Acl;
import co.navdeep.kafkaer.model.Broker;
import co.navdeep.kafkaer.model.Topic;
import co.navdeep.kafkaer.utils.Utils;
import co.navdeep.kafkaer.model.Config;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.acl.AccessControlEntryFilter;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.resource.ResourcePatternFilter;
import org.junit.*;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ConfiguratorTest {

    final static String PROPERTIES_LOCATION="src/test/resources/test.properties";
    final static String CONFIG_LOCATION="src/test/resources/kafka-config.json";
    static AdminClient adminClient;

    @BeforeClass
    public static void setup() throws ConfigurationException {
        Configuration properties = Utils.readProperties(PROPERTIES_LOCATION);
        adminClient = AdminClient.create(Utils.getClientConfig(properties));
    }

    @Before
    @After
    public void cleanup() throws ExecutionException, InterruptedException {
        deleteAllAcls();
    }

    @Test
    public void testReadConfig() throws IOException, ConfigurationException {
        Configurator configurator = new Configurator(PROPERTIES_LOCATION, CONFIG_LOCATION);
        Config config = configurator.getConfig();
        Assert.assertFalse(config.getTopics().isEmpty());
        Assert.assertEquals(config.getTopics().size(), 2);
        Assert.assertEquals(config.getTopics().get(0).getName(), "withSuffix-iamasuffix");
        Assert.assertEquals(config.getTopics().get(0).getPartitions(), 1);
        Assert.assertEquals(config.getTopics().get(0).getReplicationFactor(), 1);
        Assert.assertEquals(config.getTopics().get(0).getConfigs().get("compression.type"), "gzip");

        Assert.assertEquals(config.getBrokers().size(), 1);
        Assert.assertEquals(config.getBrokers().get(0).getConfig().get("sasl.login.refresh.window.jitter"), "0.05");


        Assert.assertEquals(config.getAcls().size(), 1);
        Assert.assertEquals(config.getAcls().get(0).getPrincipal(), "User:joe");
        Assert.assertEquals(config.getAcls().get(0).getResourceType(), "Topic");
        Assert.assertEquals(config.getAcls().get(0).getPatternType(), "LITERAL");
        Assert.assertEquals(config.getAcls().get(0).getResourceName(), "test");
        Assert.assertEquals(config.getAcls().get(0).getOperation(), "Read");
        Assert.assertEquals(config.getAcls().get(0).getPermissionType(), "Allow");
        Assert.assertEquals(config.getAcls().get(0).getHost(), "*");

        Assert.assertEquals(config.getAclStrings().size(), 2);
        Assert.assertTrue(config.getAclStrings().containsAll(Arrays.asList("User:joe,Topic,LITERAL,test,Read,Allow,*", "User:jon,Cluster,LITERAL,kafka-cluster,Create,Allow,*")));
    }

    @Test
    public void testTopicCreation() throws ExecutionException, InterruptedException, ConfigurationException {
        Config config = new Config();
        String topicName = UUID.randomUUID().toString();
        Topic topic = new Topic(topicName, 1, (short)1);
        config.getTopics().add(topic);

        Configurator configurator = new Configurator(Utils.readProperties(PROPERTIES_LOCATION), config);
        configurator.applyConfig();

        sleep();
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

        sleep();
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

        sleep();
        ConfigResource configResource = new ConfigResource(ConfigResource.Type.TOPIC, topic.getName());
        DescribeConfigsResult result = adminClient.describeConfigs(Collections.singletonList(configResource));

        org.apache.kafka.clients.admin.Config topicConfig = result.all().get().get(configResource);

        Assert.assertEquals(topicConfig.get("delete.retention.ms").value(), "123");
    }

    @Test
    public void testIncreasePartitions() throws ExecutionException, InterruptedException, ConfigurationException {
        Config config = new Config();
        String topicName = UUID.randomUUID().toString();
        Topic topic = new Topic(topicName, 1, (short)1);
        config.getTopics().add(topic);

        Configurator configurator = new Configurator(Utils.readProperties(PROPERTIES_LOCATION), config);
        configurator.applyConfig();

        sleep();
        compareWithKafkaTopic(topic);

        topic.setPartitions(2);
        configurator.applyConfig();

        sleep();
        compareWithKafkaTopic(topic);
    }

    @Test
    public void testPreservePartitions() throws ExecutionException, InterruptedException, ConfigurationException {
        Config config = new Config();
        String topicName = UUID.randomUUID().toString();
        Topic topic = new Topic(topicName, 1, (short)1);
        config.getTopics().add(topic);

        Configurator configurator = new Configurator(Utils.readProperties(PROPERTIES_LOCATION), config);
        configurator.setPreservePartitionCount(true);
        configurator.applyConfig();

        sleep();
        compareWithKafkaTopic(topic);

        //Increase the partitions and apply config
        topic.setPartitions(2);
        configurator.applyConfig();

        //Still expect 1 partition
        topic.setPartitions(1);
        compareWithKafkaTopic(topic);
    }

    @Test
    public void testUpdateExistingTopicConfig() throws ConfigurationException, ExecutionException, InterruptedException {
        Config config = new Config();
        Topic topic = new Topic(UUID.randomUUID().toString(), 1, (short)1);
        topic.setConfigs(Collections.singletonMap("delete.retention.ms", "123"));
        config.getTopics().add(topic);

        Configurator configurator = new Configurator(Utils.readProperties(PROPERTIES_LOCATION), config);
        configurator.applyConfig();

        sleep();
        ConfigResource configResource = new ConfigResource(ConfigResource.Type.TOPIC, topic.getName());
        DescribeConfigsResult result = adminClient.describeConfigs(Collections.singletonList(configResource));

        org.apache.kafka.clients.admin.Config topicConfig = result.all().get().get(configResource);

        Assert.assertEquals(topicConfig.get("delete.retention.ms").value(), "123");

        //update the same topic
        topic.setConfigs(Collections.singletonMap("delete.retention.ms", "321"));
        configurator.applyConfig();
        result = adminClient.describeConfigs(Collections.singletonList(configResource));
        topicConfig = result.all().get().get(configResource);
        Assert.assertEquals(topicConfig.get("delete.retention.ms").value(), "321");
    }


    @Test
    public void testSpecificBrokerConfigUpdate() throws ExecutionException, InterruptedException, ConfigurationException {
        Node brokerNode = new ArrayList<>(adminClient.describeCluster().nodes().get()).get(0);
        Config config = new Config();
        Broker broker = new Broker();
        broker.setId(String.valueOf(brokerNode.id()));
        broker.setConfig(Collections.singletonMap("sasl.kerberos.min.time.before.relogin", "60001"));
        config.getBrokers().add(broker);

        Configurator configurator = new Configurator(Utils.readProperties(PROPERTIES_LOCATION), config);
        configurator.applyConfig();

        sleep();
        ConfigResource configResource = new ConfigResource(ConfigResource.Type.BROKER, String.valueOf(brokerNode.id()));
        DescribeConfigsResult result = adminClient.describeConfigs(Collections.singletonList(configResource));
        org.apache.kafka.clients.admin.Config brokerConfig = result.all().get().get(configResource);

        //Default is 60000
        Assert.assertEquals(brokerConfig.get("sasl.kerberos.min.time.before.relogin").value(), "60001");
    }

    @Test
    public void testClusterwideConfigUpdate() throws ExecutionException, InterruptedException, ConfigurationException {
        List<Node> nodes = new ArrayList<>(adminClient.describeCluster().nodes().get());
        Broker broker = new Broker();
        Config config = new Config();
        //Default is 2147483647
        broker.setConfig(Collections.singletonMap("max.connections.per.ip", "10000"));
        config.getBrokers().add(broker);
        Configurator configurator = new Configurator(Utils.readProperties(PROPERTIES_LOCATION), config);
        configurator.applyConfig();

        sleep();
        for(Node node : nodes){
            ConfigResource configResource = new ConfigResource(ConfigResource.Type.BROKER, String.valueOf(node.id()));
            DescribeConfigsResult result = adminClient.describeConfigs(Collections.singletonList(configResource));
            org.apache.kafka.clients.admin.Config brokerConfig = result.all().get().get(configResource);
            Assert.assertEquals(brokerConfig.get("max.connections.per.ip").value(), "10000");
        }
    }

    @Test
    public void testCreateAclsStructured() throws ConfigurationException, ExecutionException, InterruptedException {
        Config config = new Config();
        config.getAcls().add(new Acl("User:joe,Topic,LITERAL,test,Read,Allow,*"));
        config.getAcls().add(new Acl("User:jon,Cluster,LITERAL,kafka-cluster,Create,Allow,*"));

        Configurator configurator = new Configurator(Utils.readProperties(PROPERTIES_LOCATION), config);
        configurator.applyConfig();

        sleep();
        DescribeAclsResult describeAclsResult = adminClient.describeAcls(new AclBindingFilter(ResourcePatternFilter.ANY, AccessControlEntryFilter.ANY));

        Assert.assertEquals(describeAclsResult.values().get().size(), 2);
        Assert.assertTrue(describeAclsResult.values().get().containsAll(config.getAclBindings()));
    }

    @Test
    public void testCreateAclsFromStrings() throws ExecutionException, InterruptedException, ConfigurationException {
        Config config = new Config();
        config.getAclStrings().add("User:joe,Topic,LITERAL,test,Read,Allow,*");
        config.getAclStrings().add("User:jon,Cluster,LITERAL,kafka-cluster,Create,Allow,*");

        Configurator configurator = new Configurator(Utils.readProperties(PROPERTIES_LOCATION), config);
        configurator.applyConfig();

        sleep();
        DescribeAclsResult describeAclsResult = adminClient.describeAcls(new AclBindingFilter(ResourcePatternFilter.ANY, AccessControlEntryFilter.ANY));

        Assert.assertEquals(describeAclsResult.values().get().size(), 2);
        Assert.assertTrue(describeAclsResult.values().get().containsAll(config.getAclBindings()));
    }


    @Test
    public void testCreateAclsMix() throws ConfigurationException, ExecutionException, InterruptedException {
        Config config = new Config();
        config.getAclStrings().add("User:joe,Topic,LITERAL,test,Read,Allow,*");
        config.getAcls().add(new Acl("User:jon,Cluster,LITERAL,kafka-cluster,Create,Allow,*"));

        Configurator configurator = new Configurator(Utils.readProperties(PROPERTIES_LOCATION), config);
        configurator.applyConfig();

        sleep();
        DescribeAclsResult describeAclsResult = adminClient.describeAcls(new AclBindingFilter(ResourcePatternFilter.ANY, AccessControlEntryFilter.ANY));

        Assert.assertEquals(describeAclsResult.values().get().size(), 2);
        Assert.assertTrue(describeAclsResult.values().get().containsAll(config.getAclBindings()));
    }

    @Test
    public void testWipe() throws ConfigurationException, ExecutionException, InterruptedException {
        Config config = new Config();
        String topicName = UUID.randomUUID().toString();
        Topic topic = new Topic(topicName, 1, (short)1);
        config.getTopics().add(topic);

        Configurator configurator = new Configurator(Utils.readProperties(PROPERTIES_LOCATION), config);
        configurator.applyConfig();
        sleep();
        compareWithKafkaTopic(topic);

        configurator.wipeTopics(true, false);

        Assert.assertFalse(adminClient.listTopics().names().get().contains(topic.getName()));
    }

    @Test
    public void testWipeWithSchemaWipe() throws ConfigurationException, ExecutionException, InterruptedException, IOException, RestClientException {
        Config config = new Config();
        String topicName = UUID.randomUUID().toString();
        Topic topic = new Topic(topicName, 1, (short)1);
        config.getTopics().add(topic);

        Configurator configurator = new Configurator(Utils.readProperties(PROPERTIES_LOCATION), config);
        configurator.applyConfig();
        sleep();
        compareWithKafkaTopic(topic);

        SchemaRegistryClient mock = Mockito.mock(SchemaRegistryClient.class);
        configurator.setSchemaRegistryClient(mock);

        String subjectName = topicName + "-value";
        Mockito.when(mock.getAllSubjects()).thenReturn(Collections.singletonList(subjectName));
        Mockito.when(mock.deleteSubject(subjectName)).thenReturn(Collections.singletonList(1));

        configurator.wipeTopics(true, true);

        Mockito.verify(mock).getAllSubjects();
        Mockito.verify(mock).deleteSubject(ArgumentMatchers.eq(subjectName));

        Assert.assertFalse(adminClient.listTopics().names().get().contains(topic.getName()));
    }

    @Test
    public void testWipeWithSchemaWipeTopicDoesNotExist() throws ConfigurationException, ExecutionException, InterruptedException, IOException, RestClientException {
        Config config = new Config();
        String topicName = UUID.randomUUID().toString();
        Topic topic = new Topic(topicName, 1, (short)1);
        config.getTopics().add(topic);

        Configurator configurator = new Configurator(Utils.readProperties(PROPERTIES_LOCATION), config);

        SchemaRegistryClient mock = Mockito.mock(SchemaRegistryClient.class);
        configurator.setSchemaRegistryClient(mock);

        String subjectName = topicName + "-value";
        Mockito.when(mock.getAllSubjects()).thenReturn(Collections.singletonList(subjectName));
        Mockito.when(mock.deleteSubject(subjectName)).thenReturn(Collections.singletonList(1));


        configurator.wipeTopics(true, true);

        //Topic did not exist, it should still delete schema
        Mockito.verify(mock).getAllSubjects();
        Mockito.verify(mock).deleteSubject(ArgumentMatchers.eq(subjectName));

        Assert.assertFalse(adminClient.listTopics().names().get().contains(topic.getName()));
    }

    @Test
    public void testNonExistingTopicWipeNoException() throws ConfigurationException {
        Config config = new Config();
        String topicName = UUID.randomUUID().toString();
        Topic topic = new Topic(topicName, 1, (short)1);
        config.getTopics().add(topic);

        Configurator configurator = new Configurator(Utils.readProperties(PROPERTIES_LOCATION), config);

        try {
            configurator.wipeTopics(true, false);
        } catch(Exception e){
            Assert.fail();
            e.printStackTrace();
        }
    }

    @Test
    public void testWipeSchema() throws ConfigurationException, IOException, RestClientException {
        Configurator configurator = new Configurator(Utils.readProperties(PROPERTIES_LOCATION), new Config());
        SchemaRegistryClient mock = Mockito.mock(SchemaRegistryClient.class);
        configurator.setSchemaRegistryClient(mock);

        Mockito.when(mock.getAllSubjects()).thenReturn(Collections.singletonList("x-value"));
        Mockito.when(mock.deleteSubject("x-value")).thenReturn(Collections.singletonList(1));

        configurator.wipeSchema("x");

        Mockito.verify(mock).getAllSubjects();
        Mockito.verify(mock).deleteSubject(ArgumentMatchers.eq("x-value"));
    }

    @Test(expected = RuntimeException.class)
    public void testDuplicateTopicValidation() throws ExecutionException, InterruptedException, ConfigurationException {
        Config config = new Config();
        String topicName = UUID.randomUUID().toString();
        Topic topic = new Topic(topicName, 1, (short)1);
        Topic topic2 = new Topic(topicName, 2, (short)1);
        config.getTopics().add(topic);
        config.getTopics().add(topic2);

        Configurator configurator = new Configurator(Utils.readProperties(PROPERTIES_LOCATION), config);
        configurator.applyConfig();

    }

    private void compareWithKafkaTopic(Topic topic) throws ExecutionException, InterruptedException {
        DescribeTopicsResult result = adminClient.describeTopics(Collections.singletonList(topic.getName()));
        TopicDescription kafkaTopic = result.all().get().get(topic.getName());
        Assert.assertNotNull(kafkaTopic);
        Assert.assertEquals(kafkaTopic.partitions().size(), topic.getPartitions());
        Assert.assertEquals(kafkaTopic.partitions().get(0).replicas().size(), topic.getReplicationFactor());
    }

    private void deleteAllAcls() throws ExecutionException, InterruptedException {
        AclBindingFilter all = new AclBindingFilter(ResourcePatternFilter.ANY, AccessControlEntryFilter.ANY);
        DeleteAclsResult result = adminClient.deleteAcls(Collections.singleton(all));
        result.all().get();
        TimeUnit.SECONDS.sleep(1);
    }

    private void sleep() throws InterruptedException {
        TimeUnit.SECONDS.sleep(3);
    }

}
