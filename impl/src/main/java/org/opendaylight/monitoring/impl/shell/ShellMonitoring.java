package org.opendaylight.monitoring.impl.shell;

import java.util.Date;
import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.monitoring.impl.util.FrameworkHelper;
import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;

import com.google.common.collect.Lists;

@Command(scope = "monitoring", name = "date", description="Prints date")
public class ShellMonitoring extends OsgiCommandSupport {
    //    private OvsdbInventoryService ovsdbInventory;

    @Argument(index=0, name="nodeName", description="Node Name", required=true, multiValued=false)
    String nodeName = null;

    @Override
    protected Object doExecute() throws Exception {
        System.out.println("NodeName: " + nodeName);
        System.out.println("Date: " + new Date().toString());
        OvsdbConnectionService connectionService = (OvsdbConnectionService)FrameworkHelper.getGlobalInstance(OvsdbConnectionService.class, this);

        List<Node> nodes = connectionService.getNodes();
        System.out.println("X3:Nodes: " + nodes);
        if (nodes == null) {
            return null;
        }

        List<String> nodeIds = Lists.newArrayList();
        for (Node node : nodes) {
            nodeIds.add(node.getId().getValue());
        }
        System.out.println("NodeIds: " + nodeIds);
        //        NodeId nodeId = new NodeId(nodeName);
        //        NodeKey nodeKey = new NodeKey(nodeId);
        //        Node node = new NodeBuilder()
        //        .setId(nodeId)
        //        .setKey(nodeKey)
        //        .build();
        //        ovsdbInventory.printCache(node);
        return null;
    }

    //    public void setOvsdbInventory(OvsdbInventoryService inventoryService){
    //        this.ovsdbInventory = inventoryService;
    //    }

}
