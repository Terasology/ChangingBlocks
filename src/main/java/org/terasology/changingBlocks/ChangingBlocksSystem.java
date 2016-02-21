/*
 * Copyright 2015 MovingBlocks
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
import org.terasology.math.geom.Vector3i;
import org.terasology.registry.In;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RegisterSystem(RegisterMode.AUTHORITY)
public class ChangingBlocksSystem extends BaseComponentSystem implements UpdateSubscriberSystem {
    private static final int CHECK_INTERVAL = 1000;

    @In
    private BlockManager blockManager;
    @In
    private EntityManager entityManager;
    @In
    private Time timer;
    @In
    private WorldProvider worldprovider;

    private long lastCheckTime;

    @Override
    public void initialise() {
    }

    @ReceiveEvent(components = {ChangingBlocksComponent.class, LocationComponent.class, BlockComponent.class})
    public void onSpawn(OnAddedComponent event, EntityRef entity) {
        long initTime = timer.getGameTimeInMs();
        ChangingBlocksComponent changingBlocks = entity.getComponent(ChangingBlocksComponent.class);
        LocationComponent locComponent = entity.getComponent(LocationComponent.class);
        Block currentBlock = worldprovider.getBlock(locComponent.getWorldPosition());
        String currentBlockFamilyStage = currentBlock.getURI().toString();
        changingBlocks.timeInGameMsToNextStage = changingBlocks.blockFamilyStages.get(currentBlockFamilyStage);
        changingBlocks.lastGameTimeCheck = initTime;
        entity.saveComponent(changingBlocks);
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void update(float delta) {
        // System last time check is to try to improve performance
        long gameTimeInMs = timer.getGameTimeInMs();
        if (lastCheckTime + CHECK_INTERVAL < gameTimeInMs) {
            for (EntityRef changingBlocks : entityManager.getEntitiesWith(ChangingBlocksComponent.class, BlockComponent.class, LocationComponent.class)) {
                ChangingBlocksComponent blockAnimation = changingBlocks.getComponent(ChangingBlocksComponent.class);
                if (blockAnimation.stopped) {
                    return;
                }
                if (blockAnimation.lastGameTimeCheck == -1) {
                    blockAnimation.lastGameTimeCheck = gameTimeInMs;
                    changingBlocks.saveComponent(blockAnimation);
                    return;
                }
                if (gameTimeInMs - blockAnimation.lastGameTimeCheck > blockAnimation.timeInGameMsToNextStage) {
                    blockAnimation.lastGameTimeCheck = timer.getGameTimeInMs();
                    LocationComponent locComponent = changingBlocks.getComponent(LocationComponent.class);
                    Block currentBlock = worldprovider.getBlock(locComponent.getWorldPosition());
                    String currentBlockFamilyStage = currentBlock.getURI().toString();
                    Set<String> keySet = blockAnimation.blockFamilyStages.keySet();
                    List<String> keyList = new ArrayList<>(keySet);
                    int currentstageIndex = keyList.indexOf(currentBlockFamilyStage);
                    int lastStageIndex = blockAnimation.blockFamilyStages.size() - 1;
                    if (lastStageIndex > currentstageIndex) {
                        currentstageIndex++;
                        if (currentstageIndex == lastStageIndex) {
                            if (blockAnimation.loops) {
                                currentstageIndex = 0;
                            } else {
                                blockAnimation.stopped = true;
                                changingBlocks.send(new OnBlockSequenceComplete());
                            }
                        }
                        String newBlockUri = keyList.get(currentstageIndex);
                        Block newBlock = blockManager.getBlock(newBlockUri);
                        if (newBlockUri.equals(newBlock.getURI().toString())) {
                            worldprovider.setBlock(new Vector3i(locComponent.getWorldPosition()), newBlock);
                            blockAnimation.timeInGameMsToNextStage = blockAnimation.blockFamilyStages.get(currentBlockFamilyStage);
                        }
                    }
                    changingBlocks.saveComponent(blockAnimation);
                }
            }
            lastCheckTime = gameTimeInMs;
        }
    }
}
