// Copyright 2022 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.changingBlocks;

import org.terasology.engine.core.SimpleUri;
import org.terasology.engine.world.block.ForceBlockActive;
import org.terasology.gestalt.entitysystem.component.Component;

import java.util.Map;

@ForceBlockActive
public final class ChangingBlocksComponent implements Component<ChangingBlocksComponent> {

    // determines if animation loops back to first block after last block is reached
    public boolean loops;

    // disable animation check.  Automatically set on last animation if not looping
    public boolean stopped;

    // List of block names to cycle through
    public Map<SimpleUri, Long> blockFamilyStages;

    // internal: used to determine time to next block change
    public long timeInGameMsToNextStage;

    // internal: used to track the last time we checked to see if we needed to change a block
    public long lastGameTimeCheck = -1;

    @Override
    public void copyFrom(ChangingBlocksComponent other) {
        this.loops = other.loops;
        this.stopped = other.stopped;
        this.blockFamilyStages.clear();
        other.blockFamilyStages.forEach((k, v) -> this.blockFamilyStages.put(k, v));
        this.timeInGameMsToNextStage = other.timeInGameMsToNextStage;
        this.lastGameTimeCheck = other.lastGameTimeCheck;
    }
}
