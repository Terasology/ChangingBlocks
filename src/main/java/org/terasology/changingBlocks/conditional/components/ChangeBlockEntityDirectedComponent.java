// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.changingBlocks.conditional.components;

import com.google.common.collect.Lists;
import org.terasology.changingBlocks.conditional.BlockCondition;

import java.util.List;

public class ChangeBlockEntityDirectedComponent implements ConditionalBlockChangeComponent<ChangeBlockEntityDirectedComponent> {
    public List<BlockCondition.EntityDirected> changes;

    @Override
    public void copyFrom(ChangeBlockEntityDirectedComponent other) {
        this.changes = Lists.newArrayList(other.changes);
    }
}
