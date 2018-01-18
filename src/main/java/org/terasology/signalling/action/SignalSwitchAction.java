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
import org.terasology.logic.common.ActivateEvent;
import org.terasology.math.Side;
import org.terasology.math.SideBitFlag;
import org.terasology.registry.In;
import org.terasology.signalling.componentSystem.SignalSystem;
import org.terasology.signalling.components.SignalLeafComponent;
import org.terasology.signalling.components.ToggleSwitchComponent;
import org.terasology.world.block.BlockComponent;

@RegisterSystem(RegisterMode.AUTHORITY)
public class SignalSwitchAction extends BaseComponentSystem {

    @In
    private SignalSystem signalSystem;

    @ReceiveEvent(components = {BlockComponent.class, ToggleSwitchComponent.class, SignalLeafComponent.class})
    public void signalActivated(ActivateEvent event, EntityRef entity, ToggleSwitchComponent signalSwitchComponent, SignalLeafComponent leafNodeComponent) {
        signalSwitchComponent.isActive = !signalSwitchComponent.isActive;
        if (signalSwitchComponent.isActive) {
            for (Side side : leafNodeComponent.outputs) {
                signalSystem.setLeafOutput(entity, side, signalSwitchComponent.strength);
            }
        } else {
            for (Side side : leafNodeComponent.outputs) {
                signalSystem.setLeafOutput(entity, side, (byte) 0);
            }
        }

    }
}
