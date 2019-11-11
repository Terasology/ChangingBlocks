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

    private Logger log;

    private Map<String, List<EntityRef>> triggerCollections = new HashMap<>();

    @Override
    public void initialise() {
        random = new FastRandom();
        log = LoggerFactory.getLogger(this.getClass());
        log.info("Logger created");
    }

    public void checkLocational(EntityRef entity, Vector3f triggerPosition, String triggername, Boolean isBlock)
    {
        for (EntityRef blockChange : triggerCollections.get(triggername)) {
            if (isBlock) {
                ChangeBlockBlockNearbyComponent bn = blockChange.getComponent(ChangeBlockBlockNearbyComponent.class);
                if (bn != null) {
                    //check the possible changes for this block
                    for (BlockCondition.BlockNearby change : bn.changes) {
                        //if the trigger matches
                        if (change.triggerBlock.equals(triggername)) {
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
                        if (change.triggerBlock.equals(triggername)) {
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
                        if (change.triggerEntity.equals(triggername)) {
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
                        if (change.triggerEntity.equals(triggername)) {
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

    @ReceiveEvent(components = {LocationComponent.class, ItemComponent.class})
    public void onItemUpdate(OnChangedComponent event, EntityRef entity)
    {
        String trigger = "item";
        if (triggerCollections.containsKey(trigger)) {
            LocationComponent lc = entity.getComponent(LocationComponent.class);
            checkLocational(entity, lc.getWorldPosition(), trigger, false);
        }
    }

    @ReceiveEvent(components = {LocationComponent.class, CharacterComponent.class})
    public void onCharacterUpdate(OnChangedComponent event, EntityRef entity)
    {
        String trigger = "npc";
        if (triggerCollections.containsKey(trigger)) {
            LocationComponent lc = entity.getComponent(LocationComponent.class);
            checkLocational(entity, lc.getWorldPosition(), trigger, false);
        }
    }

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

    @ReceiveEvent(components = {BlockComponent.class, LocationComponent.class})
    public void onUpdate(OnChangedBlock event, EntityRef entity) {
        String trigger = event.getNewType().getURI().toString();
        if (triggerCollections.containsKey(trigger)) {
            checkLocational(entity, event.getBlockPosition().toVector3f(), trigger, true);
        }
    }

    public void registerTrigger(String trigger, EntityRef triggerable, Boolean isBlock)
    {
        List<EntityRef> triggerTaggers = triggerCollections.computeIfAbsent(trigger, k -> new ArrayList<>());
        triggerTaggers.add(triggerable);
    }

    @ReceiveEvent(components = {ChangeBlockBlockDirectedComponent.class, LocationComponent.class, BlockComponent.class})
    public void onSpawnBlockDirected(OnAddedComponent event, EntityRef entity) {
        ChangeBlockBlockDirectedComponent changingBlocks = entity.getComponent(ChangeBlockBlockDirectedComponent.class);
        List<BlockCondition.BlockDirected> listOfChanges = changingBlocks.changes;
        for (BlockCondition.BlockDirected bc : listOfChanges) {
            registerTrigger(bc.triggerBlock, entity, true);
        }
    }

    @ReceiveEvent(components = {ChangeBlockBlockNearbyComponent.class, LocationComponent.class, BlockComponent.class})
    public void onSpawnBlockNearby(OnAddedComponent event, EntityRef entity) {
        ChangeBlockBlockNearbyComponent changingBlocks = entity.getComponent(ChangeBlockBlockNearbyComponent.class);
        List<BlockCondition.BlockNearby> listOfChanges = changingBlocks.changes;
        for (BlockCondition.BlockNearby bc : listOfChanges) {
            registerTrigger(bc.triggerBlock, entity, true);
        }
    }

    @ReceiveEvent(components = {ChangeBlockEntityDirectedComponent.class, LocationComponent.class, BlockComponent.class})
    public void onSpawnEntityDirected(OnAddedComponent event, EntityRef entity) {
        ChangeBlockEntityDirectedComponent changingBlocks = entity.getComponent(ChangeBlockEntityDirectedComponent.class);
        List<BlockCondition.EntityDirected> listOfChanges = changingBlocks.changes;
        for (BlockCondition.EntityDirected ec : listOfChanges) {
            registerTrigger(ec.triggerEntity, entity, false);
        }
    }

    @ReceiveEvent(components = {ChangeBlockEntityNearbyComponent.class, LocationComponent.class, BlockComponent.class})
    public void onSpawnEntityNearby(OnAddedComponent event, EntityRef entity) {
        ChangeBlockEntityNearbyComponent changingBlocks = entity.getComponent(ChangeBlockEntityNearbyComponent.class);
        List<BlockCondition.EntityNearby> listOfChanges = changingBlocks.changes;
        for (BlockCondition.EntityNearby ec : listOfChanges) {
            registerTrigger(ec.triggerEntity, entity, false);
        }
    }

    @ReceiveEvent(components = {ConditionalBlockChangeComponent.class, LocationComponent.class, BlockComponent.class})
    public void onRemoving(BeforeRemoveComponent event, EntityRef entity) {
        for (List<EntityRef> refs : triggerCollections.values())
        {
            refs.remove(entity);
        }
    }

    @Override
    public void shutdown() {
        for (String trigger : triggerCollections.keySet())
        {
            log.info("Clearing list of " + triggerCollections.get(trigger).size() + " entities triggered by " + trigger);
        }
        triggerCollections.clear();
    }

}
