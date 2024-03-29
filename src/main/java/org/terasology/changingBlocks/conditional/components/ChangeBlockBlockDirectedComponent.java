// Copyright 2022 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.changingBlocks.conditional.components;

import com.google.common.collect.Lists;
import org.terasology.changingBlocks.conditional.BlockCondition;

import java.util.List;

public class ChangeBlockBlockDirectedComponent implements ConditionalBlockChangeComponent<ChangeBlockBlockDirectedComponent> {
    public List<BlockCondition.BlockDirected> changes;

    @Override
    public void copyFrom(ChangeBlockBlockDirectedComponent other) {
        this.changes = Lists.newArrayList(other.changes);
    }
}
