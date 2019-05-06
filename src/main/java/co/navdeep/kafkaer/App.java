package co.navdeep.kafkaer;

import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class App {
    public static void main(String[] args) throws IOException, ConfigurationException, ExecutionException, InterruptedException {
        if(args.length != 2) {
            System.out.println("Missing required arguments - propertiesLocation, configLocation");
            return;
        }

        Configurator configurator = new Configurator(args[0], args[1]);
        configurator.applyConfig();
    }
}
