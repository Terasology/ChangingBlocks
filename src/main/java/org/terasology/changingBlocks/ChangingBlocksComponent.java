// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.changingBlocks;

import org.terasology.engine.entitySystem.Component;
import org.terasology.engine.world.block.ForceBlockActive;

import java.util.Map;

@ForceBlockActive
public final class ChangingBlocksComponent implements Component {

    // determines if animation loops back to first block after last block is reached
    public boolean loops;

    // disable animation check.  Automatically set on last animation if not looping
    public boolean stopped;

    // List of block names to cycle through
    public Map<String, Long> blockFamilyStages;

    // internal: used to determine time to next block change
    public long timeInGameMsToNextStage;

    // internal: used to track the last time we checked to see if we needed to change a block
    public long lastGameTimeCheck = -1;

}
