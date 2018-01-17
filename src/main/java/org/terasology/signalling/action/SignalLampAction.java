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
package org.terasology.signalling.action;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.registry.In;
import org.terasology.signalling.componentSystem.SignalSystem;
import org.terasology.signalling.components.SignalLeafComponent;
import org.terasology.signalling.components.SignalLampComponent;
import org.terasology.signalling.event.LeafNodeSignalChange;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;


@RegisterSystem(RegisterMode.AUTHORITY)
public class SignalLampAction extends BaseComponentSystem {
    @In
    private BlockManager blockManager;

    @In
    private WorldProvider worldProvider;

    @In
    private BlockEntityRegistry blockEntityRegistry;

    private Block lampTurnedOff;
    private Block lampTurnedOn;

    @Override
    public void initialise() {
        lampTurnedOff = blockManager.getBlock("ProtoSignals:SignalLampOff");
        lampTurnedOn = blockManager.getBlock("ProtoSignals:SignalLampOn");
    }

    @ReceiveEvent(components = {BlockComponent.class, SignalLampComponent.class, SignalLeafComponent.class})
    public void signalChange(LeafNodeSignalChange event, EntityRef entity, BlockComponent blockComponent) {

        if (event.getInputs().size() > 0) {
            worldProvider.setBlock(blockComponent.getPosition(), lampTurnedOn);
        } else {
            worldProvider.setBlock(blockComponent.getPosition(), lampTurnedOff);
        }
    }
}
