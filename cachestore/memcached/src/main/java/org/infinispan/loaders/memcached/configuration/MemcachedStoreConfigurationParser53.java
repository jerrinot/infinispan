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

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.parsing.*;
import org.infinispan.util.StringPropertyReplacer;

import javax.xml.stream.XMLStreamException;

@Namespaces({
        @Namespace(uri = "urn:infinispan:config:memcached:5.3", root = "memcachedStore"),
        @Namespace(root = "memcached")
})
public class MemcachedStoreConfigurationParser53 implements ConfigurationParser {

    public MemcachedStoreConfigurationParser53() {
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, ConfigurationBuilderHolder holder) throws XMLStreamException {
        ConfigurationBuilder builder = holder.getCurrentConfigurationBuilder();
        Element element = Element.forName(reader.getLocalName());
        switch (element) {
            case MEMCACHED_STORE: {
                parseMemcachedCacheStore(
                        reader,
                        builder.loaders().addLoader(
                                MemcachedStoreConfigurationBuilder.class));
                break;
            }
            default: {
                throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    private void parseMemcachedCacheStore(XMLExtendedStreamReader reader, MemcachedStoreConfigurationBuilder builder) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            String value = StringPropertyReplacer.replaceProperties(reader
                    .getAttributeValue(i));
            Attribute attribute = Attribute.forName(reader
                    .getAttributeLocalName(i));

            switch (attribute) {
                case HOSTNAME: {
                    builder.hostname(value);
                    break;
                }
                case PORT: {
                    builder.port(Integer.valueOf(value));
                    break;
                }
                default: {
                    Parser53.parseCommonStoreAttributes(reader, i, builder);
                }
            }
        }
}
}
