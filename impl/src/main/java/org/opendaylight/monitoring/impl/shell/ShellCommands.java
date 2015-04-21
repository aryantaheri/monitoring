package org.opendaylight.monitoring.impl.shell;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.monitoring.impl.MonitoringImpl;
import org.opendaylight.monitoring.impl.util.FrameworkHelper;
import org.opendaylight.ovsdb.plugin.api.StatusWithUuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.monitoring.rev150105.MonitoringService;

@Command(scope = "monitoring", name = "cmd", description="A lot of stuff")
public class ShellCommands extends OsgiCommandSupport {

    private static final String ADD_MIRROR = "addMirror";
    private static final String ADD_TUNNEL = "addTunnel";

    @Argument(index=0, name="cmd", description="Monitoring Command", required=true, multiValued=false)
    String cmd = null;

    @Argument(index=1, name="params", description="CMD params", required=true, multiValued=true)
    String[] params = null;

    @Override
    protected Object doExecute() throws Exception {
        System.out.println("Date: " + new Date().toString());
        MonitoringImpl monitoringImpl  = (MonitoringImpl)FrameworkHelper.getGlobalInstance(MonitoringService.class, this);
        System.out.println("monitoringImpl: " + monitoringImpl);
        System.out.println("cmd: " + cmd);
        System.out.println("params[" + params.length +"]: " + Arrays.toString(params));
        if (cmd.equalsIgnoreCase(ADD_MIRROR)){
            addMirror(monitoringImpl, params);
        } else if (cmd.equalsIgnoreCase(ADD_TUNNEL)){
            addTunnel(monitoringImpl, params);
        }

        return null;
    }

    private void addTunnel(MonitoringImpl monitoringImpl, String[] params) {
        int requireParams = 7;
        if (params.length != requireParams) {
            System.err.println("addMirror: Wrong #params[" + params.length + "]: " + Arrays.toString(params));
            System.err.println("addMirror: Required params[" + requireParams + "]:"
                    + " node1Name, node2Name, node1IpString, node2IpString, br1Name, br2Name, tunnelType");
            return;
        }
        String node1Name        = params[0];
        String node2Name        = params[1];
        String node1IpString    = params[2];
        String node2IpString    = params[3];
        String br1Name          = params[4];
        String br2Name          = params[5];
        String tunnelType       = params[6];

        monitoringImpl.addTunnel(node1Name, node2Name, node1IpString, node2IpString, br1Name, br2Name, tunnelType);

    }

    private void addMirror(MonitoringImpl impl, String[] params) {

        if (params.length != 6) {
            System.err.println("addMirror: Not enough params[" + params.length + "]: " + Arrays.toString(params));
            System.err.println("addMirror: Required params[" + 6 + "]: nodeId, bridgeName, mirrorName, outputPortName, selectSrcPortNames, selectDstPortNames");
            return;
        }

        String nodeId = params[0];
        String bridgeName = params[1];
        String mirrorName = params[2];
        String outputPortName = params[3];
        Set<String> selectSrcPortNames = new HashSet<String>(Arrays.asList(params[4].split(",")));
        Set<String> selectDstPortNames = new HashSet<String>(Arrays.asList(params[5].split(",")));
        StatusWithUuid status = impl.addMirror(nodeId, bridgeName, mirrorName, outputPortName, selectSrcPortNames, selectDstPortNames);
        System.out.println("Status: " + status);

    }
}
