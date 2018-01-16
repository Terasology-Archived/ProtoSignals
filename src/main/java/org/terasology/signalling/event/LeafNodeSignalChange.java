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
package org.terasology.signalling.event;

import org.terasology.entitySystem.event.AbstractConsumableEvent;
import org.terasology.math.Side;
import org.terasology.math.geom.Vector3i;

import java.util.Map;

public class LeafNodeSignalChange extends AbstractConsumableEvent {

    private Map<Side,Integer> inputs;

    public LeafNodeSignalChange(Map<Side,Integer> inputs){
        this.inputs = inputs;
    }

    public Map<Side, Integer> getInputs() {
        return inputs;
    }
}
