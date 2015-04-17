/*
 * UiS and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.monitoring.impl;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.monitoring.rev150105.MonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MonitoringProvider implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(MonitoringProvider.class);
    private RpcRegistration<MonitoringService> monitoringService;

    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("MonitoringProvider Session Initiated");
        MonitoringImpl impl = new MonitoringImpl();
        monitoringService = session.addRpcImplementation(MonitoringService.class, impl);
    }

    @Override
    public void close() throws Exception {
        LOG.info("MonitoringProvider Closed");
        if (monitoringService != null) {
            monitoringService.close();
        }
    }

}
