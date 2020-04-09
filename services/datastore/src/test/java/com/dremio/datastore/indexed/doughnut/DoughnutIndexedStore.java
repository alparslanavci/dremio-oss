/*
 * Copyright (C) 2017-2019 Dremio Corporation
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
package com.dremio.datastore.indexed.doughnut;

import com.dremio.datastore.api.IndexedStore;
import com.dremio.datastore.api.StoreBuildingFactory;
import com.dremio.datastore.api.StoreCreationFunction;
import com.dremio.datastore.format.Format;

/**
 * DoughnutStore for IndexedStore tests.
 */
public class DoughnutIndexedStore implements StoreCreationFunction<IndexedStore<String, Doughnut>> {
  @Override
  public IndexedStore<String, Doughnut> build(StoreBuildingFactory factory) {
    return factory.<String, Doughnut>newStore()
      .name("test-doughnut-indexed-store")
      .keyFormat(Format.ofString())
      .valueFormat(Format.wrapped(Doughnut.class, new DoughnutConverter(), Format.ofBytes()))
      .buildIndexed(DoughnutDocumentConverter.class);
  }
}