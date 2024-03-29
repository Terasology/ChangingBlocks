// Copyright 2022 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.changingBlocks.conditional;

import org.terasology.engine.math.Side;

public class BlockCondition {
    /**
     * The chance that the change will be executed if this condition is reached, where 0.0 = 0% and 1.0 = 100%
     */
    public float chance = 1.0F;

    /**
     * The block ID of the block which will replace this block.
     */
    public String targetBlockID;

    /**
     * This base condition should be used if another block is part of the condition.
     */
    public static class BlockTrigger extends BlockCondition {
        /**
         * The block ID of the block which will trigger this action
         */
        public String triggerBlockID;
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

        public boolean throughWalls = false;
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

        public boolean throughWalls = false;
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

        public boolean throughWalls = false;
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

        public boolean throughWalls = false;
    }
}
