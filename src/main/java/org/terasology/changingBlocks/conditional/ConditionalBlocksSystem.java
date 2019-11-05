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

import org.terasology.changingBlocks.ChangingBlocksComponent;
import org.terasology.changingBlocks.OnBlockSequenceComplete;
import org.terasology.changingBlocks.conditional.components.ChangeBlockBlockDirectedComponent;
import org.terasology.changingBlocks.conditional.components.ChangeBlockBlockNearbyComponent;
import org.terasology.changingBlocks.conditional.components.ChangeBlockEntityDirectedComponent;
import org.terasology.changingBlocks.conditional.components.ChangeBlockEntityNearbyComponent;
import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.OnAddedComponent;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.Direction;
import org.terasology.math.Side;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.physics.Physics;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.physics.bullet.BulletPhysics;
import org.terasology.registry.In;
import org.terasology.utilities.random.Random;
import org.terasology.world.OnChangedBlock;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RegisterSystem(RegisterMode.AUTHORITY)
public class ConditionalBlocksSystem extends BaseComponentSystem implements UpdateSubscriberSystem {

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

    private List<String> blockTriggers = new ArrayList<>();
    private List<String> entityTriggers = new ArrayList<>();

    @Override
    public void initialise() {
    }

    @ReceiveEvent(components = {LocationComponent.class, BlockComponent.class})
    public void onUpdate(OnChangedBlock event, EntityRef entity) {
        //if this block is a registered trigger
        if (blockTriggers.contains(event.getNewType().getURI().toString())) {
            //get every non-directional changer
            for (EntityRef blockChange : entityManager.getEntitiesWith(ChangeBlockBlockNearbyComponent.class)) {
                ChangeBlockBlockNearbyComponent bn = blockChange.getComponent(ChangeBlockBlockNearbyComponent.class);
                //check the possible changes for this block
                for (BlockCondition.BlockNearby change : bn.changes) {
                    //if the trigger matches
                    if (change.triggerBlock.equals(event.getNewType().getURI().toString())) {
                        LocationComponent blockLocation = blockChange.getComponent(LocationComponent.class);
                        Vector3f changeSpot = blockLocation.getWorldPosition();
                        float distance = changeSpot.distance(event.getBlockPosition().toVector3f());
                        //if it is adjacent
                        if (change.adjacent && distance <= 1)
                        {
                            worldprovider.setBlock(new Vector3i(changeSpot.x, changeSpot.y, changeSpot.z), blockManager.getBlock(change.blockToBecome));
                        } else {
                            //if it is within range
                            if (distance >= change.minDistance && distance <= change.maxDistance) {
                                Vector3f direction = event.getBlockPosition().toVector3f().sub(changeSpot);
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

            //get every directional changer
            for (EntityRef blockChange : entityManager.getEntitiesWith(ChangeBlockBlockDirectedComponent.class)) {
                ChangeBlockBlockDirectedComponent bd = blockChange.getComponent(ChangeBlockBlockDirectedComponent.class);
                //check the possible changes for this block
                for (BlockCondition.BlockDirected change : bd.changes) {
                    //if the trigger matches
                    if (change.triggerBlock.equals(event.getNewType().getURI().toString())) {
                        LocationComponent blockLocation = blockChange.getComponent(LocationComponent.class);
                        Vector3f changeSpot = blockLocation.getWorldPosition();
                        float distance = changeSpot.distance(event.getBlockPosition().toVector3f());
                        //if it is within range
                        if (distance >= change.minDistance && distance <= change.maxDistance) {
                            Side global = change.blockSide.getRelativeSide(Direction.inDirection(blockLocation.getLocalDirection()));
                            Vector3f direction = event.getBlockPosition().toVector3f().sub(changeSpot);
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

    @ReceiveEvent(components = {ChangeBlockBlockDirectedComponent.class, LocationComponent.class, BlockComponent.class})
    public void onSpawnBlockDirected(OnAddedComponent event, EntityRef entity) {
        ChangeBlockBlockDirectedComponent changingBlocks = entity.getComponent(ChangeBlockBlockDirectedComponent.class);
        List<BlockCondition.BlockDirected> listOfChanges = changingBlocks.changes;
        for (BlockCondition.BlockDirected bc : listOfChanges) {
            String trigger = bc.triggerBlock;
            if (!blockTriggers.contains(trigger)) {
                blockTriggers.add(trigger);
            }
        }
    }

    @ReceiveEvent(components = {ChangeBlockBlockNearbyComponent.class, LocationComponent.class, BlockComponent.class})
    public void onSpawnBlockNearby(OnAddedComponent event, EntityRef entity) {
        ChangeBlockBlockNearbyComponent changingBlocks = entity.getComponent(ChangeBlockBlockNearbyComponent.class);
        List<BlockCondition.BlockNearby> listOfChanges = changingBlocks.changes;
        for (BlockCondition.BlockNearby bc : listOfChanges) {
            String trigger = bc.triggerBlock;
            if (!blockTriggers.contains(trigger)) {
                blockTriggers.add(trigger);
            }
        }
    }

    @ReceiveEvent(components = {ChangeBlockEntityDirectedComponent.class, LocationComponent.class, BlockComponent.class})
    public void onSpawnEntityDirected(OnAddedComponent event, EntityRef entity) {
        ChangeBlockEntityDirectedComponent changingBlocks = entity.getComponent(ChangeBlockEntityDirectedComponent.class);
        List<BlockCondition.EntityDirected> listOfChanges = changingBlocks.changes;
        for (BlockCondition.EntityDirected ec : listOfChanges) {
            String trigger = ec.triggerEntity;
            if (!entityTriggers.contains(trigger)) {
                entityTriggers.add(trigger);
            }
        }
    }

    @ReceiveEvent(components = {ChangeBlockEntityNearbyComponent.class, LocationComponent.class, BlockComponent.class})
    public void onSpawnEntityNearby(OnAddedComponent event, EntityRef entity) {
        ChangeBlockEntityNearbyComponent changingBlocks = entity.getComponent(ChangeBlockEntityNearbyComponent.class);
        List<BlockCondition.EntityNearby> listOfChanges = changingBlocks.changes;
        for (BlockCondition.EntityNearby ec : listOfChanges) {
            String trigger = ec.triggerEntity;
            if (!entityTriggers.contains(trigger)) {
                entityTriggers.add(trigger);
            }
        }
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void update(float delta) {

    }
}
