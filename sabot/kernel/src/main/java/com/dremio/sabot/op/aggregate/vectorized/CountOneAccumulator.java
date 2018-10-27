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
package com.dremio.sabot.op.aggregate.vectorized;

import com.dremio.sabot.op.common.ht2.LBlockHashTableNoSpill;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.FieldVector;

import io.netty.util.internal.PlatformDependent;

import static com.dremio.sabot.op.aggregate.vectorized.VectorizedHashAggOperator.HTORDINAL_OFFSET;
import static com.dremio.sabot.op.aggregate.vectorized.VectorizedHashAggOperator.PARTITIONINDEX_HTORDINAL_WIDTH;

public class CountOneAccumulator extends BaseSingleAccumulator {
  private final static int ACCUMULATOR_WIDTH = 8;

  public CountOneAccumulator(FieldVector input, FieldVector output,
                             FieldVector transferVector, int maxValuesPerBatch,
                             BufferAllocator computationVectorAllocator) {
    super(input, output, transferVector, AccumulatorBuilder.AccumulatorType.COUNT1, maxValuesPerBatch,
          computationVectorAllocator);
  }

  public void accumulate(final long memoryAddr, final int count){
    final long maxAddr = memoryAddr + count * PARTITIONINDEX_HTORDINAL_WIDTH;
    final long[] valueAddresses = this.valueAddresses;
    final int maxValuesPerBatch = super.maxValuesPerBatch;
    for (long partitionAndOrdinalAddr = memoryAddr; partitionAndOrdinalAddr < maxAddr; partitionAndOrdinalAddr += PARTITIONINDEX_HTORDINAL_WIDTH) {
      /* get the hash table ordinal */
      final int tableIndex = PlatformDependent.getInt(partitionAndOrdinalAddr + HTORDINAL_OFFSET);
      /* get the target addresses of accumulation vector */
      final int chunkIndex = getChunkIndexForOrdinal(tableIndex, maxValuesPerBatch);
      final int chunkOffset = getOffsetInChunkForOrdinal(tableIndex, maxValuesPerBatch);
      final long countAddr = valueAddresses[chunkIndex] + chunkOffset * ACCUMULATOR_WIDTH;
      /* store the accumulated values(count) at the target location of accumulation vector */
      PlatformDependent.putLong(countAddr, PlatformDependent.getLong(countAddr) + 1);
    }
  }

  public void accumulateNoSpill(final long offsetAddr, final int count){
    final long maxAddr = offsetAddr + count * 4;
    final long[] valueAddresses = this.valueAddresses;
    for(long ordinalAddr = offsetAddr; ordinalAddr < maxAddr; ordinalAddr += 4){
      final int tableIndex = PlatformDependent.getInt(ordinalAddr);
      final int chunkIndex = tableIndex >>> LBlockHashTableNoSpill.BITS_IN_CHUNK;
      final int chunkOffset = tableIndex & LBlockHashTableNoSpill.CHUNK_OFFSET_MASK;
      final long countAddr = valueAddresses[chunkIndex] + (chunkOffset) * ACCUMULATOR_WIDTH;
      PlatformDependent.putLong(countAddr, PlatformDependent.getLong(countAddr) + 1);
    }
  }
}
