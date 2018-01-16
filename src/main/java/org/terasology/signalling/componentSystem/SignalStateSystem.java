/*
 * Copyright 2017 MovingBlocks
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
package org.terasology.signalling.componentSystem;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.delay.DelayedActionComponent;
import org.terasology.logic.delay.DelayedActionTriggeredEvent;
import org.terasology.logic.health.BeforeDestroyEvent;
import org.terasology.logic.health.DoDestroyEvent;
import org.terasology.math.Side;
import org.terasology.math.SideBitFlag;
import org.terasology.registry.In;
import org.terasology.signalling.components.CableComponent;
import org.terasology.signalling.components.SignalLeafComponent;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.items.OnBlockItemPlaced;

@RegisterSystem(value = RegisterMode.AUTHORITY)
public class SignalStateSystem extends BaseComponentSystem {

    @In
    private SignalSystem signalSystem;
    @In
    private WorldProvider worldProvider;
    @In
    private BlockManager blockManager;
    /**
     * Adds the placed block to the correct list
     *
     * @param event     The event triggered by block placement
     * @param entityRef The entity information of the placed block
     */
    @ReceiveEvent()
    public void onBlockPlaced(OnBlockItemPlaced event, EntityRef entityRef) {
        signalSystem.signalAllSidesAroundLocation(event.getPosition(),Integer.MAX_VALUE);
    }

    @ReceiveEvent(components = {SignalLeafComponent.class})
    public void onLeafRemoved(DoDestroyEvent event, EntityRef block) {
        BlockComponent blockComponent = block.getComponent(BlockComponent.class);
        worldProvider.setBlock(blockComponent.getPosition(), blockManager.getBlock(BlockManager.AIR_ID));
        signalSystem.signalAllSidesAroundLocation(blockComponent.getPosition(),Integer.MAX_VALUE);
    }

    @ReceiveEvent(components = {CableComponent.class})
    public void onCableRemoved(DoDestroyEvent event, EntityRef block) {
        BlockComponent blockComponent = block.getComponent(BlockComponent.class);
        worldProvider.setBlock(blockComponent.getPosition(), blockManager.getBlock(BlockManager.AIR_ID));
        signalSystem.signalAllSidesAroundLocation(blockComponent.getPosition(),Integer.MAX_VALUE);
    }

}
