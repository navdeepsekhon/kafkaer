import co.navdeep.kafkaer.utils.Utils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

public class UtilsTest {
    @Test
    public void getClientConfigsTest() throws ConfigurationException {
        Configuration properties = Utils.readProperties("src/test/resources/test.properties");
        Properties config = Utils.getClientConfig(properties);
        Assert.assertEquals(config.size(), 2);
        Assert.assertEquals(config.getProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG), "localhost:29092");
        Assert.assertEquals(config.getProperty(AdminClientConfig.CLIENT_ID_CONFIG), "kafkaer");
    }
}
