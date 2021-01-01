// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.changingBlocks.conditional;

import org.joml.RoundingMode;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector3i;
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
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.characters.CharacterComponent;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.logic.location.LocationChangedEvent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.logic.players.PlayerCharacterComponent;
import org.terasology.math.Direction;
import org.terasology.math.Side;
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
    private static Logger log = LoggerFactory.getLogger(ConditionalBlocksSystem.class);

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

    /**
     * Maps block IDs and entity categories to lists of block entities that have changes triggered by such blocks or
     * entities.
     */
    private Map<String, List<EntityRef>> triggerCollections = new HashMap<>();

    @Override
    public void initialise() {
        random = new FastRandom(worldprovider.getSeed().hashCode());
    }

    /**
     * Registers a particular block entity with a particular triggering string. This method is public so that other
     * modules may register their own custom entity-trigger types.
     *
     * @param trigger The name of the trigger, such as a block ID or entity category.
     * @param triggerable The entity that may be changed by the trigger.
     * @param isBlock Whether this trigger is a block or a free-moving entity like a player or NPC.
     */
    public void registerTrigger(String trigger, EntityRef triggerable, Boolean isBlock) {
        triggerCollections.compute(
            trigger.toLowerCase(),
            (k, v) -> {
                if (v == null) {
                    v = new ArrayList<>();
                }
                v.add(triggerable);
                return v;
            }
        );
    }

    private void checkBlockNearby(EntityRef entity, Vector3fc triggerPosition, String triggerName, EntityRef blockChange) {
        ChangeBlockBlockNearbyComponent bn = blockChange.getComponent(ChangeBlockBlockNearbyComponent.class);
        if (bn != null) {
            //check the possible changes for this block
            for (BlockCondition.BlockNearby change : bn.changes) {
                //if the trigger matches
                if (change.triggerBlockID.equals(triggerName)) {
                    LocationComponent blockLocation = blockChange.getComponent(LocationComponent.class);
                    Vector3f changeSpot = blockLocation.getWorldPosition(new Vector3f());
                    float distance = changeSpot.distance(triggerPosition);
                    //if it is adjacent
                    if (change.adjacent && distance < 2 && change.chance >= random.nextFloat()) {
                        worldprovider.setBlock(new Vector3i(changeSpot, RoundingMode.FLOOR), blockManager.getBlock(change.targetBlockID));
                    } else {
                        //if it is within range
                        if (distance >= change.minDistance && distance <= change.maxDistance) {
                            Vector3f direction = triggerPosition.sub(changeSpot, new Vector3f());
                            //if the change can occur when obstructed, or otherwise if there is no obstruction
                            if (change.throughWalls || physics.rayTrace(changeSpot, direction, change.maxDistance, StandardCollisionGroup.WORLD).getEntity() == entity) {
                                //if the random odds are in our favor
                                if (change.chance >= random.nextFloat()) {
                                    worldprovider.setBlock(new Vector3i(changeSpot, RoundingMode.FLOOR), blockManager.getBlock(change.targetBlockID));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void checkBlockDirected(EntityRef entity, Vector3fc triggerPosition, String triggerName, EntityRef blockChange) {
        ChangeBlockBlockDirectedComponent bd = blockChange.getComponent(ChangeBlockBlockDirectedComponent.class);
        if (bd != null) {
            //check the possible changes for this block
            for (BlockCondition.BlockDirected change : bd.changes) {
                //if the trigger matches
                if (change.triggerBlockID.equals(triggerName)) {
                    LocationComponent blockLocation = blockChange.getComponent(LocationComponent.class);
                    Vector3f changeSpot = blockLocation.getWorldPosition(new Vector3f());
                    float distance = changeSpot.distance(triggerPosition);
                    //if it is within range
                    if (distance >= change.minDistance && distance <= change.maxDistance) {
                        Side global = change.blockSide.getRelativeSide(Direction.inDirection(blockLocation.getLocalDirection(new Vector3f())));
                        Vector3f direction = triggerPosition.sub(changeSpot, new Vector3f());
                        //if it's on the correct side of the block
                        if (Side.inDirection(direction) == global) {
                            //if the angle is within the change's field of view limit
                            if (direction.angle(new Vector3f(global.direction())) <= change.fieldOfView) {
                                //if the change can occur when obstructed, or otherwise if there is no obstruction
                                if (change.throughWalls || physics.rayTrace(changeSpot, direction, change.maxDistance, StandardCollisionGroup.WORLD).getEntity() == entity) {
                                    //if the random odds are in our favor
                                    if (change.chance >= random.nextFloat()) {
                                        worldprovider.setBlock(new Vector3i(changeSpot, RoundingMode.FLOOR), blockManager.getBlock(change.targetBlockID));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void checkEntityNearby(EntityRef entity, Vector3fc triggerPosition, String triggerName, EntityRef blockChange) {
        ChangeBlockEntityNearbyComponent en = blockChange.getComponent(ChangeBlockEntityNearbyComponent.class);
        if (en != null) {
            //check the possible changes for this block
            for (BlockCondition.EntityNearby change : en.changes) {
                //if the trigger matches
                if (change.triggerEntity.equals(triggerName)) {
                    LocationComponent blockLocation = blockChange.getComponent(LocationComponent.class);
                    Vector3f changeSpot = blockLocation.getWorldPosition(new Vector3f());
                    float distance = changeSpot.distance(triggerPosition);
                    //if it is within range
                    if (distance >= change.minDistance && distance <= change.maxDistance) {
                        Vector3f direction = triggerPosition.sub(changeSpot, new Vector3f());
                        //if the change can occur when obstructed, or otherwise if there is no obstruction
                        if (change.throughWalls || physics.rayTrace(changeSpot, direction, change.maxDistance, StandardCollisionGroup.WORLD).getEntity() == entity) {
                            //if the random odds are in our favor
                            if (change.chance >= random.nextFloat()) {
                                worldprovider.setBlock(new Vector3i(changeSpot, RoundingMode.FLOOR), blockManager.getBlock(change.targetBlockID));
                            }
                        }
                    }
                }
            }
        }
    }

    private void checkEntityDirected(EntityRef entity, Vector3fc triggerPosition, String triggerName, EntityRef blockChange) {
        ChangeBlockEntityDirectedComponent ed = blockChange.getComponent(ChangeBlockEntityDirectedComponent.class);
        if (ed != null) {
            //check the possible changes for this block
            for (BlockCondition.EntityDirected change : ed.changes) {
                //if the trigger matches
                if (change.triggerEntity.equals(triggerName)) {
                    LocationComponent blockLocation = blockChange.getComponent(LocationComponent.class);
                    Vector3f changeSpot = blockLocation.getWorldPosition(new Vector3f());
                    float distance = changeSpot.distance(triggerPosition);
                    //if it is within range
                    if (distance >= change.minDistance && distance <= change.maxDistance) {
                        Side global = change.blockSide.getRelativeSide(Direction.inDirection(blockLocation.getLocalDirection(new Vector3f())));
                        Vector3f direction = triggerPosition.sub(changeSpot, new Vector3f());
                        //if it's on the correct side of the block
                        if (Side.inDirection(direction) == global) {
                            //if the angle is within the change's field of view limit
                            if (direction.angle(new Vector3f(global.direction())) <= change.fieldOfView) {
                                //if the change can occur when obstructed, or otherwise if there is no obstruction
                                if (change.throughWalls || physics.rayTrace(changeSpot, direction, change.maxDistance, StandardCollisionGroup.WORLD).getEntity() == entity) {
                                    //if the random odds are in our favor
                                    if (change.chance >= random.nextFloat()) {
                                        worldprovider.setBlock(new Vector3i(changeSpot, RoundingMode.FLOOR), blockManager.getBlock(change.targetBlockID));
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
     * Executes any conditional changes that may be started by the given entity (including block entities) at the given
     * position. This method is public so that other modules may register their own custom event triggers.
     *
     * @param entity The entity that may cause the trigger.
     * @param triggerPosition The location at which the entity is found.
     * @param triggerName The string representing this entity's type.
     * @param isBlock Whether this entity is a block entity or another type.
     */
    public void checkLocational(EntityRef entity, Vector3fc triggerPosition, String triggerName, Boolean isBlock) {
        for (EntityRef blockChange : triggerCollections.get(triggerName)) {
            if (isBlock) {
                checkBlockNearby(entity, triggerPosition, triggerName, blockChange);
                checkBlockDirected(entity, triggerPosition, triggerName, blockChange);
            } else {
                checkEntityNearby(entity, triggerPosition, triggerName, blockChange);
                checkEntityDirected(entity, triggerPosition, triggerName, blockChange);
            }
        }
    }

    /**
     * Triggers any conditional changes that should be caused by a dropped Item.
     *
     * @param event The event triggered by the location or item change.
     * @param entity The item.
     */
    @ReceiveEvent(components = {LocationComponent.class, ItemComponent.class})
    public void onItemUpdate(LocationChangedEvent event, EntityRef entity) {
        String trigger = "item";
        if (triggerCollections.containsKey(trigger)) {
            LocationComponent lc = entity.getComponent(LocationComponent.class);
            checkLocational(entity, lc.getWorldPosition(new Vector3f()), trigger, false);
        }
    }

    /**
     * Triggers any conditional changes that should be caused by a non-player character.
     *
     * @param event The event triggered by the location or character change.
     * @param entity The NPC.
     */
    @ReceiveEvent(components = {LocationComponent.class, CharacterComponent.class})
    public void onCharacterUpdate(LocationChangedEvent event, EntityRef entity) {
        String trigger = "npc";
        if (triggerCollections.containsKey(trigger)) {
            LocationComponent lc = entity.getComponent(LocationComponent.class);
            checkLocational(entity, lc.getWorldPosition(new Vector3f()), trigger, false);
        }
    }

    /**
     * Triggers any conditional changes that should be caused by a player.
     *
     * @param event The event triggered by the location or player change.
     * @param entity The player.
     */
    @ReceiveEvent(components = {LocationComponent.class, PlayerCharacterComponent.class})
    public void onPlayerUpdate(LocationChangedEvent event, EntityRef entity) {
        String trigger = "player";
        if (triggerCollections.containsKey(trigger)) {
            LocationComponent lc = entity.getComponent(LocationComponent.class);
            checkLocational(entity, lc.getWorldPosition(new Vector3f()), trigger, false);
        }
    }

    /**
     * Triggers any conditional changes that should be caused by a block being added or removed.
     *
     * @param event The information about the block change.
     * @param entity The new block entity.
     */
    @ReceiveEvent(components = {BlockComponent.class, LocationComponent.class})
    public void onUpdate(OnChangedBlock event, EntityRef entity) {
        String trigger = event.getNewType().getURI().toString().toLowerCase();
        if (triggerCollections.containsKey(trigger)) {
            checkLocational(entity, new Vector3f(event.getBlockPosition()), trigger, true);
        }
    }

    /**
     * Registers every block that may be triggered by a directed change.
     *
     * @param event The event caused by the block's creation.
     * @param entity The block entity to register.
     */
    @ReceiveEvent(components = {ChangeBlockBlockDirectedComponent.class, LocationComponent.class, BlockComponent.class})
    public void onSpawnBlockDirected(OnAddedComponent event, EntityRef entity) {
        ChangeBlockBlockDirectedComponent changingBlocks = entity.getComponent(ChangeBlockBlockDirectedComponent.class);
        List<BlockCondition.BlockDirected> listOfChanges = changingBlocks.changes;
        for (BlockCondition.BlockDirected bc : listOfChanges) {
            registerTrigger(bc.triggerBlockID, entity, true);
        }
    }

    /**
     * Registers every block that may be triggered by a non-directional change.
     *
     * @param event The event caused by the block's creation.
     * @param entity The block entity to register.
     */
    @ReceiveEvent(components = {ChangeBlockBlockNearbyComponent.class, LocationComponent.class, BlockComponent.class})
    public void onSpawnBlockNearby(OnAddedComponent event, EntityRef entity) {
        ChangeBlockBlockNearbyComponent changingBlocks = entity.getComponent(ChangeBlockBlockNearbyComponent.class);
        List<BlockCondition.BlockNearby> listOfChanges = changingBlocks.changes;
        for (BlockCondition.BlockNearby bc : listOfChanges) {
            registerTrigger(bc.triggerBlockID, entity, true);
        }
    }

    /**
     * Registers every block that may be triggered by an entity in a certain direction.
     *
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
     *
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
     *
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
        for (String trigger : triggerCollections.keySet()) {
            log.info("Clearing list of " + triggerCollections.get(trigger).size() + " entities triggered by " + trigger);
        }
        triggerCollections.clear();
    }

}
