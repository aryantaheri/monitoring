package org.opendaylight.monitoring.impl.util;

import java.util.Map;
import java.util.Set;

import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class OvsdbUtil {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbUtil.class);



    public static String getBridgeUuid(OvsdbConfigurationService configurationService, Node node, String bridgeName) {
        Preconditions.checkNotNull(configurationService, "Configuration Service Not Specified");
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

    public UUID getPortUuid(OvsdbConfigurationService configurationService, Node node, String portName) {
        Preconditions.checkNotNull(configurationService, "Configuration Service Not Specified");
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

    public static String getInterfaceUuid(OvsdbConfigurationService configurationService, Node node, String portUuid, int timeout) {
        String interfaceUuid = null;
        while ((interfaceUuid == null) && (timeout > 0)) {
            Row portRow = configurationService.getRow(node, configurationService.getTableName(node, Port.class), portUuid);
            Port port = configurationService.getTypedRow(node, Port.class, portRow);
            Set<UUID> interfaces = port.getInterfacesColumn().getData();
            if (interfaces == null || interfaces.size() == 0) {
                // Wait for the OVSDB update to sync up the Local cache.
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    LOG.error("getInterfaceUuid", e);
                }
                timeout--;
                continue;
            }
            interfaceUuid = interfaces.toArray()[0].toString();
            Row intfRow = configurationService.getRow(node, configurationService.getTableName(node, Interface.class), interfaceUuid);
            Interface intf = configurationService.getTypedRow(node, Interface.class, intfRow);
            if (intf == null) interfaceUuid = null;
        }
        return interfaceUuid;
    }
}
