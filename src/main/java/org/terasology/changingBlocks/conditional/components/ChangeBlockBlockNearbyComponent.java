// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.changingBlocks.conditional.components;

import com.google.common.collect.Lists;
import org.terasology.changingBlocks.conditional.BlockCondition;

import java.util.List;

public class ChangeBlockBlockNearbyComponent implements ConditionalBlockChangeComponent<ChangeBlockBlockNearbyComponent> {
    public List<BlockCondition.BlockNearby> changes;

    @Override
    public void copy(ChangeBlockBlockNearbyComponent other) {
        this.changes = Lists.newArrayList(other.changes);
    }
}
