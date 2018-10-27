/*
 * Copyright (C) 2017-2018 Dremio Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dremio.exec.store.hive;

import java.util.List;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.orc.OrcConf;

import com.dremio.common.VM;
import com.dremio.exec.catalog.conf.ConnectionConf;
import com.dremio.exec.catalog.conf.DisplayMetadata;
import com.dremio.exec.catalog.conf.Property;
import com.dremio.exec.store.StoragePlugin;
import com.google.common.base.Preconditions;

import io.protostuff.Tag;

/**
 * Base configuration for the Hive storage plugin
 */
public abstract class BaseHiveStoragePluginConfig<T extends ConnectionConf<T, P>, P extends StoragePlugin> extends ConnectionConf<T, P>{
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BaseHiveStoragePluginConfig.class);

  /*
   * Hostname where Hive metastore server is running
   */
  @Tag(1)
  @DisplayMetadata(label = "Hive Metastore Host")
  public String hostname;

  /*
   * Listening port of Hive metastore server
   */
  @Tag(2)
  @DisplayMetadata(label = "Port")
  public int port = 9083;

  /*
   * Is kerberos authentication enabled on metastore services?
   */
  @Tag(3)
  @DisplayMetadata(label = "Enable SASL")
  public boolean enableSasl = false;

  /*
   * Kerberos principal name of metastore servers if kerberos authentication is enabled
   */
  @Tag(4)
  @DisplayMetadata(label = "Hive Kerberos Principal")
  public String kerberosPrincipal;

  /*
   * List of configuration properties.
   */
  @Tag(5)
  public List<Property> propertyList;

  public BaseHiveStoragePluginConfig() {
  }

  protected static HiveConf createHiveConf(BaseHiveStoragePluginConfig<?,?> config) {
    final HiveConf hiveConf = new HiveConf();

    final String metastoreURI = String.format("thrift://%s:%d", Preconditions.checkNotNull(config.hostname, "Hive hostname must be provided."), config.port);
    hiveConf.set(HiveConf.ConfVars.METASTOREURIS.varname, metastoreURI);

    if (config.enableSasl) {
      hiveConf.set(HiveConf.ConfVars.METASTORE_USE_THRIFT_SASL.varname, "true");
      if (config.kerberosPrincipal != null) {
        hiveConf.set(HiveConf.ConfVars.METASTORE_KERBEROS_PRINCIPAL.varname, config.kerberosPrincipal);
      }
    }

    // Check if zero-copy has been set by user
    boolean useZeroCopyNotSet = true;
    if(config.propertyList != null) {
      for(Property prop : config.propertyList) {
        useZeroCopyNotSet = useZeroCopyNotSet && !OrcConf.USE_ZEROCOPY.getAttribute().equals(prop.name)
            && !HiveConf.ConfVars.HIVE_ORC_ZEROCOPY.varname.equals(prop.name);
        hiveConf.set(prop.name, prop.value);
        if(logger.isTraceEnabled()){
          logger.trace("HiveConfig Override {}={}", prop.name, prop.value);
        }
      }
    }

    if (useZeroCopyNotSet) {
      if (VM.isWindowsHost() || VM.isMacOSHost()) {
        logger.debug("MacOS or Windows host detected. Not automatically enabling ORC zero-copy feature");
      } else {
        String fs = hiveConf.get(FileSystem.FS_DEFAULT_NAME_KEY);
        // Equivalent to a case-insensitive startsWith...
        if (fs.regionMatches(true, 0, "maprfs", 0, 6)) {
          // DX-12672: do not enable ORC zero-copy on MapRFS
          logger.debug("MapRFS detected. Not automatically enabling ORC zero-copy feature");
        } else {
          logger.debug("Linux host detected. Enabling ORC zero-copy feature");
          hiveConf.setBoolean(HiveConf.ConfVars.HIVE_ORC_ZEROCOPY.varname, true);
        }
      }
    } else {
      boolean useZeroCopy = OrcConf.USE_ZEROCOPY.getBoolean(hiveConf);
      if (useZeroCopy) {
        logger.warn("ORC zero-copy feature has been manually enabled. This is not recommended.");
      } else {
        logger.error("ORC zero-copy feature has been manually disabled. This is not recommended and might cause memory issues");
      }
    }
    // Configure zero-copy for ORC reader

    return hiveConf;
  }
}
