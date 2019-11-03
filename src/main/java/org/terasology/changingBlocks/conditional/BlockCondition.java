/*
 * Copyright 2019 MovingBlocks
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
package org.terasology.changingBlocks.conditional;

import org.terasology.math.Side;

public class BlockCondition {
    /**
     * The chance that this condition will be executed if it is reached, where 0.0 = 0% and 1.0 = 100%
     */
    public float chance = 1.0F;

    /**
     * The block ID of the block which will replace this block.
     */
    public String blockToBecome;

    /**
     * This base condition should be used if another block is part of the condition.
     */
    public static class BlockTrigger extends BlockCondition {
        /**
         * The block ID of the block which will trigger this action
         */
        public String triggerBlock;
    }

    /**
     * This condition should be used if the presence of a nearby or adjacent block should trigger the condition.
     */
    public static class BlockNearby extends BlockTrigger {
        /**
         * The minimum distance away the triggering block must be located. Default 0.
         */
        public float minDistance = 0;
        /**
         * The maximum distance away the triggering block must be located. Default 1.5F.
         */
        public float maxDistance = 1.5F;
        /**
         * If true, ignore distances and only check if it touches this block.
         */
        public boolean adjacent = false;
    }

    /**
     * This condition should be used if the presence of a nearby or adjacent block in a specific direction should trigger the condition.
     */
    public static class BlockDirected extends BlockTrigger {
        /**
         * Which side of this block must be facing the trigger
         */
        public Side blockSide;
        /**
         * The minimum distance away the triggering block must be located. Default 0.
         */
        public float minDistance = 0;
        /**
         * The maximum distance away the triggering block must be located. Default 1.
         */
        public float maxDistance = 1F;
        /**
         * The field of view to check. 0 = direct line from faces only; 1 = any block on that side of this block.
         */
        public float fieldOfView = 0;
    }

    /**
     * This base condition should be used if an entity is part of the condition.
     */
    public static class EntityTrigger extends BlockCondition {
        /**
         * The ID of the entity which will trigger this action
         */
        public String triggerEntity;
    }

    /**
     * This condition should be used if the presence of a nearby entity triggers the condition.
     */
    public static class EntityNearby extends EntityTrigger {
        /**
         * The minimum distance away the triggering entity must be located. Default 0.
         */
        public float minDistance = 0;
        /**
         * The maximum distance away the triggering entity must be located. Default 1.5F.
         */
        public float maxDistance = 1.5F;
    }

    /**
     * This condition should be used if the presence of a nearby entity in a certain direction triggers the condition.
     */
    public static class EntityDirected extends EntityTrigger {
        /**
         * Which side of this block must be facing the trigger
         */
        public Side blockSide;
        /**
         * The minimum distance away the triggering entity must be located. Default 0.
         */
        public float minDistance = 0;
        /**
         * The maximum distance away the triggering entity must be located. Default 1.
         */
        public float maxDistance = 1F;
        /**
         * The field of view to check. 0 = direct line from faces only; 1 = any block on that side of this block.
         */
        public float fieldOfView = 0;
    }
}
