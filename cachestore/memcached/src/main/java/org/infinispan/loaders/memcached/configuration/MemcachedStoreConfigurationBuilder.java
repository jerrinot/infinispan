/*
* C2B2, The Leading Independent Middleware Experts.
* Copyright 2013, C2B2 Consulting Limited.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.infinispan.loaders.memcached.configuration;

import org.infinispan.configuration.Builder;
import org.infinispan.configuration.cache.AbstractStoreConfigurationBuilder;
import org.infinispan.configuration.cache.LoadersConfigurationBuilder;
import org.infinispan.util.TypedProperties;

public class MemcachedStoreConfigurationBuilder extends AbstractStoreConfigurationBuilder<MemcachedStoreConfiguration, MemcachedStoreConfigurationBuilder> {

    private String hostname = "localhost";
    private int port = 11211;

    public MemcachedStoreConfigurationBuilder(LoadersConfigurationBuilder builder) {
        super(builder);
    }

    public MemcachedStoreConfigurationBuilder hostname(String hostname) {
        this.hostname = hostname;
        return self();
    }

    public MemcachedStoreConfigurationBuilder port(int port) {
        this.port = port;
        return self();
    }


    @Override
    public MemcachedStoreConfiguration create() {
        return new MemcachedStoreConfiguration(hostname, port, purgeOnStartup, purgeSynchronously, purgerThreads,
                fetchPersistentState, ignoreModifications, TypedProperties.toTypedProperties(properties), async.create(), singletonStore.create());
    }

    @Override
    public Builder<?> read(MemcachedStoreConfiguration template) {
        hostname = template.hostname();
        port = template.port();

        // AbstractStore-specific configuration
        fetchPersistentState = template.fetchPersistentState();
        ignoreModifications = template.ignoreModifications();
        properties = template.properties();
        purgeOnStartup = template.purgeOnStartup();
        purgeSynchronously = template.purgeSynchronously();
        async.read(template.async());
        singletonStore.read(template.singletonStore());

        return this;
    }

    @Override
    public MemcachedStoreConfigurationBuilder self() {
        return this;
    }
}
