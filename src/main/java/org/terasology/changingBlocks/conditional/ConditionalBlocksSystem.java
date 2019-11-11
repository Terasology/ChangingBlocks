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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.changingBlocks.conditional.components.ChangeBlockBlockDirectedComponent;
import org.terasology.changingBlocks.conditional.components.ChangeBlockBlockNearbyComponent;
import org.terasology.changingBlocks.conditional.components.ChangeBlockEntityDirectedComponent;
import org.terasology.changingBlocks.conditional.components.ChangeBlockEntityNearbyComponent;
import org.terasology.changingBlocks.conditional.components.ConditionalBlockChangeComponent;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.BeforeRemoveComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnAddedComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnChangedComponent;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.characters.CharacterComponent;
import org.terasology.logic.characters.CharacterMovementComponent;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.logic.players.PlayerCharacterComponent;
import org.terasology.math.Direction;
import org.terasology.math.Side;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.physics.Physics;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.registry.In;
import org.terasology.utilities.random.FastRandom;
import org.terasology.utilities.random.Random;
import org.terasology.world.OnChangedBlock;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RegisterSystem(RegisterMode.AUTHORITY)
public class ConditionalBlocksSystem extends BaseComponentSystem {

    @In
    private BlockManager blockManager;
    @In
    private EntityManager entityManager;
    @In
    private WorldProvider worldprovider;
    @In
    private Physics physics;
    @In
    private Random random;

    private static Logger log = LoggerFactory.getLogger(ConditionalBlocksSystem.class);

    /**
     * Maps block IDs and entity categories to lists of block entities that have changes triggered by such blocks or entities.
     */
    private Map<String, List<EntityRef>> triggerCollections = new HashMap<>();

    @Override
    public void initialise() {
        random = new FastRandom(worldprovider.getSeed().hashCode());
    }

    /**
     * Registers a particular block entity with a particular triggering string.
     * This method is public so that other modules may register their own custom entity-trigger types.
     * @param trigger The name of the trigger, such as a block ID or entity category.
     * @param triggerable The entity that may be changed by the trigger.
     * @param isBlock Whether this trigger is a block or a free-moving entity like a player or NPC.
     */
    public void registerTrigger(String trigger, EntityRef triggerable, Boolean isBlock)
    {
        triggerCollections.compute(
                trigger,
                (k, v) -> {
                    if (v == null) v = new ArrayList<>();
                    v.add(triggerable);
                    return v;
                }
        );
    }

    /**
     * Executes any conditional changes that may be started by the given entity (including block entities) at the given position.
     * This method is public so that other modules may register their own custom event triggers.
     * @param entity The entity that may cause the trigger.
     * @param triggerPosition The location at which the entity is found.
     * @param triggerName The string representing this entity's type.
     * @param isBlock Whether this entity is a block entity or another type.
     */
    public void checkLocational(EntityRef entity, Vector3f triggerPosition, String triggerName, Boolean isBlock)
    {
        for (EntityRef blockChange : triggerCollections.get(triggerName)) {
            if (isBlock) {
                ChangeBlockBlockNearbyComponent bn = blockChange.getComponent(ChangeBlockBlockNearbyComponent.class);
                if (bn != null) {
                    //check the possible changes for this block
                    for (BlockCondition.BlockNearby change : bn.changes) {
                        //if the trigger matches
                        if (change.triggerBlock.equals(triggerName)) {
                            LocationComponent blockLocation = blockChange.getComponent(LocationComponent.class);
                            Vector3f changeSpot = blockLocation.getWorldPosition();
                            float distance = changeSpot.distance(triggerPosition);
                            //if it is adjacent
                            if (change.adjacent && distance < 2 && change.chance >= random.nextFloat()) {
                                worldprovider.setBlock(new Vector3i(changeSpot.x, changeSpot.y, changeSpot.z), blockManager.getBlock(change.blockToBecome));
                            } else {
                                //if it is within range
                                if (distance >= change.minDistance && distance <= change.maxDistance) {
                                    Vector3f direction = triggerPosition.sub(changeSpot);
                                    //if the change can occur when obstructed, or otherwise if there is no obstruction
                                    if (change.throughWalls || physics.rayTrace(changeSpot, direction, change.maxDistance, StandardCollisionGroup.WORLD).getEntity() == entity) {
                                        //if the random odds are in our favor
                                        if (change.chance >= random.nextFloat()) {
                                            worldprovider.setBlock(new Vector3i(changeSpot.x, changeSpot.y, changeSpot.z), blockManager.getBlock(change.blockToBecome));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                ChangeBlockBlockDirectedComponent bd = blockChange.getComponent(ChangeBlockBlockDirectedComponent.class);
                if (bd != null) {
                    //check the possible changes for this block
                    for (BlockCondition.BlockDirected change : bd.changes) {
                        //if the trigger matches
                        if (change.triggerBlock.equals(triggerName)) {
                            LocationComponent blockLocation = blockChange.getComponent(LocationComponent.class);
                            Vector3f changeSpot = blockLocation.getWorldPosition();
                            float distance = changeSpot.distance(triggerPosition);
                            //if it is within range
                            if (distance >= change.minDistance && distance <= change.maxDistance) {
                                Side global = change.blockSide.getRelativeSide(Direction.inDirection(blockLocation.getLocalDirection()));
                                Vector3f direction = triggerPosition.sub(changeSpot);
                                //if it's on the correct side of the block
                                if (Side.inDirection(direction) == global) {
                                    //if the angle is within the change's field of view limit
                                    if (direction.angle(global.getVector3i().toVector3f()) <= change.fieldOfView) {
                                        //if the change can occur when obstructed, or otherwise if there is no obstruction
                                        if (change.throughWalls || physics.rayTrace(changeSpot, direction, change.maxDistance, StandardCollisionGroup.WORLD).getEntity() == entity) {
                                            //if the random odds are in our favor
                                            if (change.chance >= random.nextFloat()) {
                                                worldprovider.setBlock(new Vector3i(changeSpot.x, changeSpot.y, changeSpot.z), blockManager.getBlock(change.blockToBecome));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                ChangeBlockEntityNearbyComponent en = blockChange.getComponent(ChangeBlockEntityNearbyComponent.class);
                if (en != null) {
                    //check the possible changes for this block
                    for (BlockCondition.EntityNearby change : en.changes) {
                        //if the trigger matches
                        if (change.triggerEntity.equals(triggerName)) {
                            LocationComponent blockLocation = blockChange.getComponent(LocationComponent.class);
                            Vector3f changeSpot = blockLocation.getWorldPosition();
                            float distance = changeSpot.distance(triggerPosition);
                            //if it is within range
                            if (distance >= change.minDistance && distance <= change.maxDistance) {
                                Vector3f direction = triggerPosition.sub(changeSpot);
                                //if the change can occur when obstructed, or otherwise if there is no obstruction
                                if (change.throughWalls || physics.rayTrace(changeSpot, direction, change.maxDistance, StandardCollisionGroup.WORLD).getEntity() == entity) {
                                    //if the random odds are in our favor
                                    if (change.chance >= random.nextFloat()) {
                                        worldprovider.setBlock(new Vector3i(changeSpot.x, changeSpot.y, changeSpot.z), blockManager.getBlock(change.blockToBecome));
                                    }
                                }
                            }
                        }
                    }
                }

                ChangeBlockEntityDirectedComponent ed = blockChange.getComponent(ChangeBlockEntityDirectedComponent.class);
                if (ed != null) {
                    //check the possible changes for this block
                    for (BlockCondition.EntityDirected change : ed.changes) {
                        //if the trigger matches
                        if (change.triggerEntity.equals(triggerName)) {
                            LocationComponent blockLocation = blockChange.getComponent(LocationComponent.class);
                            Vector3f changeSpot = blockLocation.getWorldPosition();
                            float distance = changeSpot.distance(triggerPosition);
                            //if it is within range
                            if (distance >= change.minDistance && distance <= change.maxDistance) {
                                Side global = change.blockSide.getRelativeSide(Direction.inDirection(blockLocation.getLocalDirection()));
                                Vector3f direction = triggerPosition.sub(changeSpot);
                                //if it's on the correct side of the block
                                if (Side.inDirection(direction) == global) {
                                    //if the angle is within the change's field of view limit
                                    if (direction.angle(global.getVector3i().toVector3f()) <= change.fieldOfView) {
                                        //if the change can occur when obstructed, or otherwise if there is no obstruction
                                        if (change.throughWalls || physics.rayTrace(changeSpot, direction, change.maxDistance, StandardCollisionGroup.WORLD).getEntity() == entity) {
                                            //if the random odds are in our favor
                                            if (change.chance >= random.nextFloat()) {
                                                worldprovider.setBlock(new Vector3i(changeSpot.x, changeSpot.y, changeSpot.z), blockManager.getBlock(change.blockToBecome));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Triggers any conditional changes that should be caused by a dropped Item.
     * @param event The event triggered by the location or item change.
     * @param entity The item.
     */
    @ReceiveEvent(components = {LocationComponent.class, ItemComponent.class})
    public void onItemUpdate(OnChangedComponent event, EntityRef entity)
    {
        String trigger = "item";
        if (triggerCollections.containsKey(trigger)) {
            LocationComponent lc = entity.getComponent(LocationComponent.class);
            checkLocational(entity, lc.getWorldPosition(), trigger, false);
        }
    }

    /**
     * Triggers any conditional changes that should be caused by a non-player character.
     * @param event The event triggered by the location or character change.
     * @param entity The NPC.
     */
    @ReceiveEvent(components = {LocationComponent.class, CharacterComponent.class})
    public void onCharacterUpdate(OnChangedComponent event, EntityRef entity)
    {
        String trigger = "npc";
        if (triggerCollections.containsKey(trigger)) {
            LocationComponent lc = entity.getComponent(LocationComponent.class);
            checkLocational(entity, lc.getWorldPosition(), trigger, false);
        }
    }

    /**
     * Triggers any conditional changes that should be caused by a player.
     * @param event The event triggered by the location or player change.
     * @param entity The player.
     */
    @ReceiveEvent(components = {LocationComponent.class, PlayerCharacterComponent.class})
    public void onPlayerUpdate(OnChangedComponent event, EntityRef entity)
    {
        String trigger = "player";
        log.info("Recieved movement for " + entity.toString());
        if (triggerCollections.containsKey(trigger)) {
            LocationComponent lc = entity.getComponent(LocationComponent.class);
            checkLocational(entity, lc.getWorldPosition(), trigger, false);
        }
    }

    /**
     * Triggers any conditional changes that should be caused by a block being added or removed.
     * @param event The information about the block change.
     * @param entity The new block entity.
     */
    @ReceiveEvent(components = {BlockComponent.class, LocationComponent.class})
    public void onUpdate(OnChangedBlock event, EntityRef entity) {
        String trigger = event.getNewType().getURI().toString();
        if (triggerCollections.containsKey(trigger)) {
            checkLocational(entity, event.getBlockPosition().toVector3f(), trigger, true);
        }
    }

    /**
     * Registers every block that may be triggered by a directed change.
     * @param event The event caused by the block's creation.
     * @param entity The block entity to register.
     */
    @ReceiveEvent(components = {ChangeBlockBlockDirectedComponent.class, LocationComponent.class, BlockComponent.class})
    public void onSpawnBlockDirected(OnAddedComponent event, EntityRef entity) {
        ChangeBlockBlockDirectedComponent changingBlocks = entity.getComponent(ChangeBlockBlockDirectedComponent.class);
        List<BlockCondition.BlockDirected> listOfChanges = changingBlocks.changes;
        for (BlockCondition.BlockDirected bc : listOfChanges) {
            registerTrigger(bc.triggerBlock, entity, true);
        }
    }

    /**
     * Registers every block that may be triggered by a non-directional change.
     * @param event The event caused by the block's creation.
     * @param entity The block entity to register.
     */
    @ReceiveEvent(components = {ChangeBlockBlockNearbyComponent.class, LocationComponent.class, BlockComponent.class})
    public void onSpawnBlockNearby(OnAddedComponent event, EntityRef entity) {
        ChangeBlockBlockNearbyComponent changingBlocks = entity.getComponent(ChangeBlockBlockNearbyComponent.class);
        List<BlockCondition.BlockNearby> listOfChanges = changingBlocks.changes;
        for (BlockCondition.BlockNearby bc : listOfChanges) {
            registerTrigger(bc.triggerBlock, entity, true);
        }
    }

    /**
     * Registers every block that may be triggered by an entity in a certain direction.
     * @param event The event caused by the block's creation.
     * @param entity The block entity to register.
     */
    @ReceiveEvent(components = {ChangeBlockEntityDirectedComponent.class, LocationComponent.class, BlockComponent.class})
    public void onSpawnEntityDirected(OnAddedComponent event, EntityRef entity) {
        ChangeBlockEntityDirectedComponent changingBlocks = entity.getComponent(ChangeBlockEntityDirectedComponent.class);
        List<BlockCondition.EntityDirected> listOfChanges = changingBlocks.changes;
        for (BlockCondition.EntityDirected ec : listOfChanges) {
            registerTrigger(ec.triggerEntity, entity, false);
        }
    }

    /**
     * Registers every block that may be triggered by a nearby entity regardless of direction.
     * @param event The event caused by the block's creation.
     * @param entity The block entity to register.
     */
    @ReceiveEvent(components = {ChangeBlockEntityNearbyComponent.class, LocationComponent.class, BlockComponent.class})
    public void onSpawnEntityNearby(OnAddedComponent event, EntityRef entity) {
        ChangeBlockEntityNearbyComponent changingBlocks = entity.getComponent(ChangeBlockEntityNearbyComponent.class);
        List<BlockCondition.EntityNearby> listOfChanges = changingBlocks.changes;
        for (BlockCondition.EntityNearby ec : listOfChanges) {
            registerTrigger(ec.triggerEntity, entity, false);
        }
    }

    /**
     * Removes a block entity from the list of triggerable blocks whenever the block is removed.
     * @param event The event caused by the block's removal.
     * @param entity The block entity to deregister.
     */
    @ReceiveEvent(components = {ConditionalBlockChangeComponent.class, LocationComponent.class, BlockComponent.class})
    public void onRemoving(BeforeRemoveComponent event, EntityRef entity) {
        triggerCollections.replaceAll(
                (trigger, refs) -> {
                    refs.remove(entity);
                    return refs;
                }
        );
    }

    /**
     * Just to be safe, manually clears the list of triggerable blocks when the world ends.
     */
    @Override
    public void shutdown() {
        for (String trigger : triggerCollections.keySet())
        {
            log.info("Clearing list of " + triggerCollections.get(trigger).size() + " entities triggered by " + trigger);
        }
        triggerCollections.clear();
    }

}
