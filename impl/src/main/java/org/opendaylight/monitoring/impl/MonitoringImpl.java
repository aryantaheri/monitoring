package org.opendaylight.monitoring.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.opendaylight.monitoring.impl.util.FrameworkHelper;
import org.opendaylight.monitoring.impl.util.TunnelUtil;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;
import org.opendaylight.ovsdb.plugin.api.Status;
import org.opendaylight.ovsdb.plugin.api.StatusCode;
import org.opendaylight.ovsdb.plugin.api.StatusWithUuid;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Mirror;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.monitoring.rev150105.MonWorldInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.monitoring.rev150105.MonWorldOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.monitoring.rev150105.MonWorldOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.monitoring.rev150105.MonitoringService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

public class MonitoringImpl implements MonitoringService {
    private static final Logger LOG = LoggerFactory.getLogger(MonitoringImpl.class);

    public MonitoringImpl() {

    }

    @Override
    public Future<RpcResult<MonWorldOutput>> monWorld(MonWorldInput input) {
        MonWorldOutputBuilder monBuilder = new MonWorldOutputBuilder();
        monBuilder.setGreating("Hello " + input.getName());
        return RpcResultBuilder.success(monBuilder.build()).buildFuture();
    }

    public Status addTunnel(String node1Name, String node2Name,
            String node1IpString, String node2IpString,
            String br1Name, String br2Name,
            String tunnelType) {
        OvsdbConfigurationService configurationService = (OvsdbConfigurationService) FrameworkHelper
                .getGlobalInstance(OvsdbConfigurationService.class, this);
        OvsdbConnectionService connectionService = (OvsdbConnectionService) FrameworkHelper
                .getGlobalInstance(OvsdbConnectionService.class, this);

        if (configurationService == null || connectionService == null) {
            LOG.error("addMirror: Configuration={} or Connection={} services are not available", configurationService, connectionService);
            return new StatusWithUuid(StatusCode.NOSERVICE);
        }
        TunnelUtil tunnelUtil = new TunnelUtil(connectionService, configurationService);
        return tunnelUtil.addTunnel(node1Name, node2Name, node1IpString, node2IpString, br1Name, br2Name, tunnelType);
    }

    public StatusWithUuid addMirror(String nodeId, String bridgeName, String mirrorName, String outputPortName, Set<String> selectSrcPortNames, Set<String> selectDstPortNames) {
        OvsdbConfigurationService configurationService = (OvsdbConfigurationService) FrameworkHelper
                .getGlobalInstance(OvsdbConfigurationService.class, this);
        OvsdbConnectionService connectionService = (OvsdbConnectionService) FrameworkHelper
                .getGlobalInstance(OvsdbConnectionService.class, this);

        if (configurationService == null || connectionService == null) {
            LOG.error("addMirror: Configuration={} or Connection={} services are not available", configurationService, connectionService);
            return new StatusWithUuid(StatusCode.NOSERVICE);
        }

        Node node = connectionService.getNode(nodeId);
        OvsdbClient client = connectionService.getConnection(node).getClient();
        String mirrorUuid = getMirrorUuid(node, mirrorName);

        Mirror mirror = configurationService.createTypedRow(node, Mirror.class);

        // Set output port
        UUID outputPortUuid = getPortUuid(node, outputPortName);
        mirror.setOutputPort(ImmutableSet.of(outputPortUuid));

        // Set select port
        Set<UUID> selectSrcPortUuids = getPortUuids(node, selectSrcPortNames);
        Set<UUID> selectDstPortUuids = getPortUuids(node, selectDstPortNames);
        mirror.setSelectSrcPort(selectSrcPortUuids);
        mirror.setSelectDstPort(selectDstPortUuids);

        String bridgeUuid = getBridgeUuid(node, bridgeName);

        if (mirrorUuid == null) {
            mirror.setName(ImmutableSet.of(mirrorName));
            StatusWithUuid statusWithUuid = configurationService.insertRow(node, mirror.getSchema().getName(), bridgeUuid, mirror.getRow());

            if (!statusWithUuid.isSuccess()) {
                LOG.error("addMirror: FAILED adding mirror on node={} to bridge={} "
                        + "mirrorName={} srcPort={} dstPort={} outputPort={} status={}",
                        node, bridgeName, mirrorName,
                        selectSrcPortNames, selectDstPortNames,
                        outputPortName, statusWithUuid);
            }
            return statusWithUuid;
        } else {
            return new StatusWithUuid(StatusCode.NOTIMPLEMENTED);
        }
    }

    private void addMirrorToBridge(Node node, String bridgeName, UUID uuid) {
        OvsdbConfigurationService configurationService = (OvsdbConfigurationService) FrameworkHelper
                .getGlobalInstance(OvsdbConfigurationService.class, this);
        OvsdbConnectionService connectionService = (OvsdbConnectionService) FrameworkHelper
                .getGlobalInstance(OvsdbConnectionService.class, this);
        Preconditions.checkNotNull(configurationService, "Configuration Service Not Available");
        Preconditions.checkNotNull(connectionService, "Connection Service Not Available");

    }

    public String getBridgeUuid(Node node, String bridgeName) {
        OvsdbConfigurationService configurationService = (OvsdbConfigurationService) FrameworkHelper
                .getGlobalInstance(OvsdbConfigurationService.class, this);
        Preconditions.checkNotNull(configurationService, "Configuration Service Not Available");
        try {
            Map<String, Row> bridgeTable = configurationService.getRows(node, configurationService.getTableName(node, Bridge.class));
            if (bridgeTable == null) {
                return null;
            }
            for (String key : bridgeTable.keySet()) {
                Bridge bridge = configurationService.getTypedRow(node, Bridge.class, bridgeTable.get(key));
                if (bridge.getName().equals(bridgeName)) {
                    return key;
                }
            }
        } catch (Exception e) {
            LOG.error("Error getting Bridge Identifier in Node={} / bridgeName={}", node, bridgeName, e);
        }
        return null;
    }

    public String getMirrorUuid(Node node, String mirrorName) {
        OvsdbConfigurationService configurationService = (OvsdbConfigurationService) FrameworkHelper
                .getGlobalInstance(OvsdbConfigurationService.class, this);
        Preconditions.checkNotNull(configurationService, "Configuration Service Not Available");
        try {
            Map<String, Row> mirrorTable = configurationService.getRows(node, configurationService.getTableName(node, Mirror.class));
            if (mirrorTable == null) {
                return null;
            }
            for (String key : mirrorTable.keySet()) {
                Mirror mirror = configurationService.getTypedRow(node, Mirror.class, mirrorTable.get(key));
                if (mirror.getName().equals(mirrorName)) {
                    return key;
                }
            }
        } catch (Exception e) {
            LOG.error("Error getting Mirror Identifier in Node={} / mirrorName={}", node, mirrorName, e);
        }
        return null;
    }

    public UUID getPortUuid(Node node, String portName) {
        OvsdbConfigurationService configurationService = (OvsdbConfigurationService) FrameworkHelper
                .getGlobalInstance(OvsdbConfigurationService.class, this);
        Preconditions.checkNotNull(configurationService, "Configuration Service Not Available");
        try {
            Map<String, Row> portTable = configurationService.getRows(node, configurationService.getTableName(node, Port.class));
            if (portTable == null) {
                return null;
            }
            for (String key : portTable.keySet()) {
                Port port = configurationService.getTypedRow(node, Port.class, portTable.get(key));
                if (port.getName().equals(portName)) {
                    return new UUID(key);
                }
            }
        } catch (Exception e) {
            LOG.error("Error getting Port Identifier in Node={} / portName={}", node, portName, e);
        }
        return null;
    }

    public Set<UUID> getPortUuids(Node node, Set<String> portNames) {
        OvsdbConfigurationService configurationService = (OvsdbConfigurationService) FrameworkHelper
                .getGlobalInstance(OvsdbConfigurationService.class, this);
        Preconditions.checkNotNull(configurationService, "Configuration Service Not Available");
        Set<UUID> uuids = new HashSet<UUID>();
        try {
            Map<String, Row> portTable = configurationService.getRows(node, configurationService.getTableName(node, Port.class));
            if (portTable == null) {
                return null;
            }
            for (String key : portTable.keySet()) {
                Port port = configurationService.getTypedRow(node, Port.class, portTable.get(key));
                for (String portName : portNames) {
                    if (port.getName().equals(portName)) {
                        uuids.add(new UUID(key));
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Error getting Port Identifier in Node={} / portNames={}", node, portNames, e);
            return null;
        }
        if (uuids.size() != portNames.size()) {
            LOG.error("getPortUuids: all ports are not found portNames[{}]={}, uuids[{}]={}", portNames.size(), portNames, uuids.size(), uuids);
        }
        return uuids;
    }

    private String getBackwardCompatibleTableName(OvsdbClient client, String databaseName, String tableName) {
        DatabaseSchema dbSchema = client.getDatabaseSchema(databaseName);
        if (dbSchema == null || tableName == null) return tableName;
        for (String dbTableName : dbSchema.getTables()) {
            if (dbTableName.equalsIgnoreCase(tableName)) return dbTableName;
        }
        return tableName;
    }
}
