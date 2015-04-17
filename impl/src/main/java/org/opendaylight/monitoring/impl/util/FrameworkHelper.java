/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.monitoring.impl.util;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrameworkHelper {

    private static final Logger LOG = LoggerFactory.getLogger(FrameworkHelper.class);

    public static Object getGlobalInstance(Class<?> clazz, Object bundle) {
        return getGlobalInstance(clazz, bundle, null);
    }

    public static Object getGlobalInstance(Class<?> clazz, Object bundle,
            String serviceFilter) {
        Object[] instances = getGlobalInstances(clazz, bundle, serviceFilter);
        if (instances != null) {
            return instances[0];
        }
        return null;
    }

    public static Object[] getGlobalInstances(Class<?> clazz, Object bundle,
            String serviceFilter) {
        Object instances[] = null;
        try {
            BundleContext bCtx = FrameworkUtil.getBundle(bundle.getClass())
                    .getBundleContext();

            ServiceReference<?>[] services = bCtx.getServiceReferences(clazz
                    .getName(), serviceFilter);

            if (services != null) {
                instances = new Object[services.length];
                for (int i = 0; i < services.length; i++) {
                    instances[i] = bCtx.getService(services[i]);
                }
            }
        } catch (Exception e) {
            LOG.error("Instance reference is NULL");
        }
        return instances;
    }
}
