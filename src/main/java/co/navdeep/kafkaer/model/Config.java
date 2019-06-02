package co.navdeep.kafkaer.model;

import lombok.Data;
import org.apache.kafka.common.acl.AclBinding;

import java.util.ArrayList;
import java.util.List;

@Data
public class Config {
    private List<Topic> topics;
    private List<Broker> brokers;
    private List<Acl> acls;
    private List<String> aclStrings;

    public Config(){
        topics = new ArrayList<>();
        brokers = new ArrayList<>();
        acls = new ArrayList<>();
        aclStrings = new ArrayList<>();
    }

    public List<AclBinding> getAclBindings(){
        List<AclBinding> bindings = new ArrayList<>();
        for(Acl acl : acls){
            bindings.add(acl.toKafkaAclBinding());
        }

        for(String acl : aclStrings){
            bindings.add(new Acl(acl).toKafkaAclBinding());
        }

        return getAclBindings();
    }
    public List<String> getAllTopicNames(){
        List<String> names = new ArrayList<>();
        for(Topic t : topics){
            names.add(t.getName());
        }
        return names;
    }

    public boolean hasBrokerConfig(){
        return brokers != null && !brokers.isEmpty();
    }

}
