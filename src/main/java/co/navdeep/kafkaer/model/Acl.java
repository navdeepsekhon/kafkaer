package co.navdeep.kafkaer.model;

import lombok.Data;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;

@Data
public class Acl {
    private String resourceType;
    private String resourceName;
    private String principal;
    private String patternType;
    private String host;
    private String operation;
    private String permissionType;

    public Acl(){
        super();
    }

    public Acl(String s){
        //principal,resourceType,patternType,resourceName,operation,permissionType,host
        String[] splits = s.split(",");
        if(splits.length < 7) throw new RuntimeException("Invalid ACL:" + s);
        principal = splits[0];
        resourceType = splits[1];
        patternType = splits[2];
        resourceName = splits[3];
        operation = splits[4];
        permissionType = splits[5];
        host = splits[6];
    }

    public AclBinding toKafkaAclBinding(){
        return new AclBinding(getKafkaResourcePattern(), getKafkaAccessControlEntry());
    }
    public ResourcePattern getKafkaResourcePattern(){
        return new ResourcePattern(ResourceType.fromString(resourceType.toUpperCase()), resourceName, PatternType.fromString(patternType.toUpperCase()));
    }

    public AccessControlEntry getKafkaAccessControlEntry(){
        return new AccessControlEntry(principal, host, AclOperation.fromString(operation.toUpperCase()), AclPermissionType.fromString(permissionType.toUpperCase()));
    }
}
