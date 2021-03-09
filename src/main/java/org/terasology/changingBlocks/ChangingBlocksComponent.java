/*
 * Copyright 2013 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
