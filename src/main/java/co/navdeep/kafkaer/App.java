package co.navdeep.kafkaer;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

public class App {
    public static void main(String[] a) throws Exception {
        Args args = new Args();
        CmdLineParser parser = new CmdLineParser(args);
        try {
            parser.parseArgument(a);
        } catch(CmdLineException e){
            throw new Exception("Invalid command line arguments", e);
        }
        if(args.isHelp()){
            parser.printUsage(System.out);
            return;
        }
        if(args.getProperties() == null || args.getConfig() == null) {
            throw new RuntimeException("Missing required arguments - propertiesLocation, configLocation");
        }

        Configurator configurator = new Configurator(args.getProperties(), args.getConfig());
        if(args.isWipe())
            configurator.wipeTopics();
        else
            configurator.applyConfig();
    }
}
