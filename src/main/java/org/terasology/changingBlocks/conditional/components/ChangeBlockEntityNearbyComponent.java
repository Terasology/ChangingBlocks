// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.changingBlocks.conditional.components;

import com.google.common.collect.Lists;
import org.terasology.changingBlocks.conditional.BlockCondition;

import java.util.List;

public class ChangeBlockEntityNearbyComponent implements ConditionalBlockChangeComponent<ChangeBlockEntityNearbyComponent> {
    public List<BlockCondition.EntityNearby> changes;

    @Override
    public void copy(ChangeBlockEntityNearbyComponent other) {
        this.changes = Lists.newArrayList(other.changes);
    }
}
