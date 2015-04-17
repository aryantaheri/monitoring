package org.opendaylight.monitoring.impl.internal;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.opendaylight.monitoring.impl.MonitoringImpl;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;
import org.opendaylight.ovsdb.plugin.api.OvsdbInventoryService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.monitoring.rev150105.MonitoringService;
import org.osgi.framework.BundleContext;

public class Activator extends DependencyActivatorBase {

    @Override
    public void destroy(BundleContext arg0, DependencyManager arg1)
            throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void init(BundleContext context, DependencyManager manager)
            throws Exception {
        manager.add(createComponent()
                .setInterface(MonitoringService.class.getName(), null)
                .setImplementation(MonitoringImpl.class)
                .add(createServiceDependency()
                        .setService(OvsdbConfigurationService.class)
                        .setRequired(true))
                        .add(createServiceDependency()
                                .setService(OvsdbConnectionService.class)
                                .setRequired(true))
                                .add(createServiceDependency().setService(
                                        OvsdbInventoryService.class).setRequired(true)));
    }

}
