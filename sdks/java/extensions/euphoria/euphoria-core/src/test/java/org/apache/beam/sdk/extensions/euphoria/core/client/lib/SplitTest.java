/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.beam.sdk.extensions.euphoria.core.client.lib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import org.apache.beam.sdk.extensions.euphoria.core.client.dataset.Dataset;
import org.apache.beam.sdk.extensions.euphoria.core.client.flow.Flow;
import org.apache.beam.sdk.extensions.euphoria.core.client.functional.UnaryPredicate;
import org.apache.beam.sdk.extensions.euphoria.core.client.operator.Filter;
import org.apache.beam.sdk.extensions.euphoria.core.client.operator.Util;
import org.apache.beam.sdk.extensions.euphoria.core.client.operator.base.Operator;
import org.junit.Test;

/** Split unit testing. */
public class SplitTest {

  @Test
  public void testBuild() {
    String opName = "split";
    Flow flow = Flow.create("split-test");
    Dataset<String> dataset = Util.createMockDataset(flow, 1);

    Split.Output<String> split =
        Split.named(opName).of(dataset).using((UnaryPredicate<String>) what -> true).output();

    assertEquals(2, flow.size());
    Filter positive = (Filter) getOperator(flow, opName + Split.POSITIVE_FILTER_SUFFIX);
    assertSame(flow, positive.getFlow());
    assertNotNull(positive.getPredicate());
    assertSame(positive.output(), split.positive());
    Filter negative = (Filter) getOperator(flow, opName + Split.NEGATIVE_FILTER_SUFFIX);
    assertSame(flow, negative.getFlow());
    assertNotNull(negative.getPredicate());
    assertSame(negative.output(), split.negative());
  }

  @Test
  public void testBuild_ImplicitName() {
    Flow flow = Flow.create("split-test");
    Dataset<String> dataset = Util.createMockDataset(flow, 1);

    Split.of(dataset).using((UnaryPredicate<String>) what -> true).output();

    assertNotNull(getOperator(flow, Split.DEFAULT_NAME + Split.POSITIVE_FILTER_SUFFIX));
    assertNotNull(getOperator(flow, Split.DEFAULT_NAME + Split.NEGATIVE_FILTER_SUFFIX));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testBuild_NegatedPredicate() {
    Flow flow = Flow.create("split-test");
    Dataset<Integer> dataset = Util.createMockDataset(flow, 1);

    Split.of(dataset).using((UnaryPredicate<Integer>) what -> what % 2 == 0).output();

    Filter<Integer> oddNumbers =
        (Filter<Integer>) getOperator(flow, Split.DEFAULT_NAME + Split.NEGATIVE_FILTER_SUFFIX);
    assertFalse(oddNumbers.getPredicate().apply(0));
    assertFalse(oddNumbers.getPredicate().apply(2));
    assertFalse(oddNumbers.getPredicate().apply(4));
    assertTrue(oddNumbers.getPredicate().apply(1));
    assertTrue(oddNumbers.getPredicate().apply(3));
    assertTrue(oddNumbers.getPredicate().apply(5));
  }

  private Operator<?, ?> getOperator(Flow flow, String name) {
    Optional<Operator<?, ?>> op =
        flow.operators().stream().filter(o -> o.getName().equals(name)).findFirst();
    return op.isPresent() ? op.get() : null;
  }
}
