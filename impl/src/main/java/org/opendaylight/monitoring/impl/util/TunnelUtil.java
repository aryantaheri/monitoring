package org.opendaylight.monitoring.impl.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;
import org.opendaylight.ovsdb.plugin.api.Status;
import org.opendaylight.ovsdb.plugin.api.StatusCode;
import org.opendaylight.ovsdb.plugin.api.StatusWithUuid;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

public class TunnelUtil {
    private static final Logger LOG = LoggerFactory.getLogger(TunnelUtil.class);

    OvsdbConfigurationService configurationService;
    OvsdbConnectionService connectionService;

    public TunnelUtil(OvsdbConnectionService connectionService, OvsdbConfigurationService configurationService) {
        this.configurationService = configurationService;
        this.connectionService = connectionService;
    }

    public Status addTunnel(String node1Name, String node2Name,
            String node1IpString, String node2IpString,
            String br1Name, String br2Name,
            String tunnelType
            ) {
        Preconditions.checkNotNull(configurationService, "Configuration Service Not Available");
        Preconditions.checkNotNull(connectionService, "Connection Service Not Available");

        LOG.info("addTunnel: n1={}, n2={}, n1IP={}, n2IP={}, br1={}, br2={}, type={}",
                node1Name, node2Name, node1IpString, node2IpString, br1Name, br2Name, tunnelType);

        Node node1 = connectionService.getNode(node1Name);
        Node node2 = connectionService.getNode(node2Name);

        String br1Uuid = OvsdbUtil.getBridgeUuid(configurationService, node1, br1Name);
        String br2Uuid = OvsdbUtil.getBridgeUuid(configurationService, node2, br2Name);

        InetAddress node1Ip = null;
        InetAddress node2Ip = null;
        try {
            node1Ip = InetAddress.getByName(node1IpString);
            node2Ip = InetAddress.getByName(node2IpString);
        } catch (UnknownHostException e) {
            LOG.error("addTunnel", e);
        }

        Status status = addTunnelPort(node1, br1Uuid, tunnelType, node1Ip, node2Ip);
        if (!status.isSuccess()){
            LOG.error("addTunnel: Failed to addTunnelPort node={} brUuid={} tunnelType={} srcIp={} dstIp={}",
                    node1, br1Uuid, tunnelType, node1Ip, node2Ip);
            return status;
        } else {
            LOG.info("addTunnel: Successfully added TunnelPort node={} brUuid={} tunnelType={} srcIp={} dstIp={}",
                    node1, br1Uuid, tunnelType, node1Ip, node2Ip);

        }

        status = addTunnelPort(node2, br2Uuid, tunnelType, node2Ip, node1Ip);
        if (!status.isSuccess()){
            LOG.error("addTunnel: Failed to addTunnelPort node={} brUuid={} tunnelType={} srcIp={} dstIp={}",
                    node2, br2Uuid, tunnelType, node2Ip, node1Ip);
            return status;
        } else {
            LOG.info("addTunnel: Successfully added TunnelPort node={} brUuid={} tunnelType={} srcIp={} dstIp={}",
                    node2, br2Uuid, tunnelType, node2Ip, node1Ip);

        }

        return status;
    }


    private Status addTunnelPort (Node node, String bridgeUuid, String tunnelType, InetAddress src, InetAddress dst) {
        Preconditions.checkNotNull(configurationService);
        try {
            String portName = getTunnelName(tunnelType, dst);

            Port tunnelPort = configurationService.createTypedRow(node, Port.class);
            tunnelPort.setName(portName);

            StatusWithUuid statusWithUuid = configurationService.insertRow(node, tunnelPort.getSchema().getName(), bridgeUuid, tunnelPort.getRow());
            if (!statusWithUuid.isSuccess()) {
                LOG.error("Failed to insert Tunnel port {} in br={} on Node={}", portName, bridgeUuid, node);
                return statusWithUuid;
            }

            String tunnelPortUuid = statusWithUuid.getUuid().toString();
            int timeout = 6;
            String interfaceUuid = null;
            interfaceUuid = OvsdbUtil.getInterfaceUuid(configurationService, node, tunnelPortUuid, timeout);

            if (interfaceUuid == null) {
                LOG.error("Cannot identify Tunnel Interface for port {}/{}", portName, tunnelPortUuid);
                return new Status(StatusCode.INTERNALERROR);
            }

            Interface tunInterface = configurationService.createTypedRow(node, Interface.class);
            tunInterface.setType(tunnelType);
            Map<String, String> options = Maps.newHashMap();
            options.put("key", "flow");
            options.put("local_ip", src.getHostAddress());
            options.put("remote_ip", dst.getHostAddress());
            tunInterface.setOptions(options);
            Status status = configurationService.updateRow(node, configurationService.getTableName(node, Interface.class),
                    tunnelPortUuid, interfaceUuid, tunInterface.getRow());
            LOG.debug("Tunnel {} add status : {}", tunInterface, status);
            return status;
        } catch (Exception e) {
            LOG.error("Exception in addTunnelPort", e);
            return new Status(StatusCode.INTERNALERROR);
        }
    }

    private String getTunnelName(String tunnelType, InetAddress dst) {
        return tunnelType+"-"+dst.getHostAddress();
    }
}
