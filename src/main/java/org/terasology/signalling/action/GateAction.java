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
import org.terasology.math.Side;
import org.terasology.math.SideBitFlag;
import org.terasology.registry.In;
import org.terasology.signalling.componentSystem.SignalSystem;
import org.terasology.signalling.components.SignalLeafComponent;
import org.terasology.signalling.components.gates.AndGateComponent;
import org.terasology.signalling.components.gates.OrGateComponent;
import org.terasology.signalling.components.gates.XorGateComponent;
import org.terasology.signalling.event.LeafNodeSignalChange;
import org.terasology.world.block.BlockComponent;

@RegisterSystem(RegisterMode.AUTHORITY)
public class GateAction extends BaseComponentSystem {

    @In
    private SignalSystem signalSystem;

    @ReceiveEvent(components = {BlockComponent.class, XorGateComponent.class, SignalLeafComponent.class})
    public void signalXorChange(LeafNodeSignalChange event, EntityRef entity, BlockComponent blockComponent) {
        if (event.getInputs().size() == 1) {
            for (Side side : SideBitFlag.getSides(signalSystem.getConnectedOutputs(entity))) {
                signalSystem.setLeafOutput(entity, side, -1);
            }
        } else {
            for (Side side : SideBitFlag.getSides(signalSystem.getConnectedOutputs(entity))) {
                signalSystem.setLeafOutput(entity, side, 0);
            }
        }
    }

    @ReceiveEvent(components = {BlockComponent.class, AndGateComponent.class, SignalLeafComponent.class})
    public void signalAndChange(LeafNodeSignalChange event, EntityRef entity, BlockComponent blockComponent) {

        if (event.getInputs().size() == SideBitFlag.getSides(signalSystem.getConnectedInputs(entity)).size()) {
            for (Side side : SideBitFlag.getSides(signalSystem.getConnectedOutputs(entity))) {
                signalSystem.setLeafOutput(entity, side, -1);
            }
        } else {
            for (Side side : SideBitFlag.getSides(signalSystem.getConnectedOutputs(entity))) {
                signalSystem.setLeafOutput(entity, side, 0);
            }
        }
    }

    @ReceiveEvent(components = {BlockComponent.class, OrGateComponent.class, SignalLeafComponent.class})
    public void signalOrChange(LeafNodeSignalChange event, EntityRef entity, BlockComponent blockComponent) {
        if (event.getInputs().size() > 0) {
            for (Side side : SideBitFlag.getSides(signalSystem.getConnectedOutputs(entity))) {
                signalSystem.setLeafOutput(entity, side, -1);
            }
        } else {
            for (Side side : SideBitFlag.getSides(signalSystem.getConnectedOutputs(entity))) {
                signalSystem.setLeafOutput(entity, side, 0);
            }
        }
    }
}
