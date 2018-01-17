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
import org.terasology.registry.In;
import org.terasology.signalling.componentSystem.SignalSystem;
import org.terasology.signalling.components.SignalLeafComponent;
import org.terasology.signalling.components.gates.AndGateComponent;
import org.terasology.signalling.components.gates.NotGateComponent;
import org.terasology.signalling.components.gates.OrGateComponent;
import org.terasology.signalling.components.gates.XorGateComponent;
import org.terasology.signalling.event.LeafNodeSignalChange;
import org.terasology.world.block.BlockComponent;

@RegisterSystem(RegisterMode.AUTHORITY)
public class GateAction extends BaseComponentSystem {

    @In
    private SignalSystem signalSystem;

    @ReceiveEvent(components = {BlockComponent.class, XorGateComponent.class, SignalLeafComponent.class})
    public void signalXorChange(LeafNodeSignalChange event, EntityRef entity,SignalLeafComponent signalLeafComponent, BlockComponent blockComponent, XorGateComponent xorGateComponent) {
        if (event.getInputs().size() % 2 == 1) {
            // An odd number of inputs are active: Output HIGH.
            for (Side side : signalLeafComponent.outputs) {
                signalSystem.setLeafOutput(entity, side, xorGateComponent.strength, xorGateComponent.delay);
            }
        } else {
            // An even number of inputs are active: Output LOW. Note that this includes 0 active inputs.
            for (Side side : signalLeafComponent.outputs) {
                signalSystem.setLeafOutput(entity, side, (byte) 0, xorGateComponent.delay);
            }
        }
    }

    @ReceiveEvent(components = {BlockComponent.class, AndGateComponent.class, SignalLeafComponent.class})
    public void signalAndChange(LeafNodeSignalChange event, EntityRef entity,SignalLeafComponent signalLeafComponent, BlockComponent blockComponent, AndGateComponent andGateComponent) {


        if (event.getInputs().size() == signalSystem.getActiveSides(entity,signalLeafComponent.inputs).size() && event.getInputs().size() != 0) {
            // More than one input is connected, and all of them are HIGH: Output HIGH.
            for (Side side : signalLeafComponent.outputs) {
                signalSystem.setLeafOutput(entity, side, andGateComponent.strength, andGateComponent.delay);
            }
        } else {
            // No input is connected, or one of the connected inputs is LOW: Output LOW.
            for (Side side : signalLeafComponent.outputs) {
                signalSystem.setLeafOutput(entity, side, (byte) 0, andGateComponent.delay);
            }
        }
    }

    @ReceiveEvent(components = {BlockComponent.class, OrGateComponent.class, SignalLeafComponent.class})
    public void signalOrChange(LeafNodeSignalChange event, EntityRef entity,SignalLeafComponent signalLeafComponent, BlockComponent blockComponent, OrGateComponent orGateComponent) {
        if (event.getInputs().size() > 0) {
            // At least one input is HIGH: Output HIGH.
            for (Side side : signalLeafComponent.outputs) {
                signalSystem.setLeafOutput(entity, side, orGateComponent.strength, orGateComponent.delay);
            }
        } else {
            // No input is connected, or all inputs are LOW: Output LOW.
            for (Side side : signalLeafComponent.outputs) {
                signalSystem.setLeafOutput(entity, side, (byte) 0, orGateComponent.delay);
            }
        }
    }

    @ReceiveEvent(components = {BlockComponent.class, NotGateComponent.class, SignalLeafComponent.class})
    public void signalNotChange(LeafNodeSignalChange event, EntityRef entity,SignalLeafComponent signalLeafComponent, BlockComponent blockComponent, NotGateComponent notGateComponent) {
        if (event.getInputs().size() > 0) {
            // Either a HIGH input, or no input at all: Output LOW.
            for (Side side : signalLeafComponent.outputs) {
                signalSystem.setLeafOutput(entity, side, (byte) 0, notGateComponent.delay);
            }
        } else {
            // One LOW input: output HIGH.
            for (Side side : signalLeafComponent.outputs) {
                signalSystem.setLeafOutput(entity, side, notGateComponent.strength, notGateComponent.delay);
            }
        }
    }
}
