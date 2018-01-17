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
package org.terasology.signalling.componentSystem;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.config.ModuleConfigManager;
import org.terasology.math.Rotation;
import org.terasology.math.Side;
import org.terasology.math.SideBitFlag;
import org.terasology.math.geom.Vector3i;
import org.terasology.registry.In;
import org.terasology.registry.Share;
import org.terasology.signalling.components.CableComponent;
import org.terasology.signalling.components.SignalLeafComponent;
import org.terasology.signalling.components.SignalStateComponent;
import org.terasology.signalling.event.LeafNodeSignalChange;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.family.MultiConnectFamily;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;


@RegisterSystem(value = RegisterMode.AUTHORITY)
@Share(value = SignalSystem.class)
public class SignalSystem extends BaseComponentSystem implements UpdateSubscriberSystem {
    private static final Logger logger = LoggerFactory.getLogger(SignalSystem.class);
    private PriorityQueue<SignalDelayHandler> delays = new PriorityQueue<>(new SignalDelayComparitor());

    @In
    private Time time;
    @In
    private WorldProvider worldProvider;
    @In
    private BlockEntityRegistry blockEntityRegistry;
    @In
    private ModuleConfigManager moduleConfigManager;
    

    public Side getTransformedSide(EntityRef entityRef,Side side) {
        BlockComponent blockComponent = entityRef.getComponent(BlockComponent.class);
        if (blockComponent == null)
            return null;
        return blockComponent.getBlock().getRotation().rotate(side);

    }

    public EnumSet<Side> getActiveSides(EntityRef entityRef,Set<Side> sides) {
        BlockComponent blockComponent = entityRef.getComponent(BlockComponent.class);
        if (blockComponent == null)
            return EnumSet.noneOf(Side.class);

        EnumSet<Side> result = EnumSet.noneOf(Side.class);
        for (Side side : sides) {

            EntityRef neighborEntity = blockEntityRegistry.getBlockEntityAt(new Vector3i(new Vector3i(blockComponent.getPosition())).add(getTransformedSide(entityRef, side).getVector3i()));
            if (neighborEntity.hasComponent(CableComponent.class) || neighborEntity.hasComponent(SignalLeafComponent.class)) {
                result.add(side);
            }
        }
        return result;
    }

    public int getLeafOutput(EntityRef entityRef, Side side) {
        SignalStateComponent signalStateComponent = entityRef.getComponent(SignalStateComponent.class);
        if (signalStateComponent == null)
            return 0;
        return signalStateComponent.outputs[SignalStateComponent.OUTPUT_SIDES.indexOf(getTransformedSide(entityRef,side))];
    }

    public boolean setLeafOutput(EntityRef entityRef, Side side, byte strength) {
        SignalLeafComponent signalLeafComponent = entityRef.getComponent(SignalLeafComponent.class);
        if (signalLeafComponent == null)
            return false;

        if (signalLeafComponent.outputs.contains(side)) {

            int sideIndex = SignalStateComponent.OUTPUT_SIDES.indexOf(getTransformedSide(entityRef,side));
            SignalStateComponent signalStateComponent = entityRef.getComponent(SignalStateComponent.class);
            if (signalStateComponent == null)
                signalStateComponent = new SignalStateComponent();
            if (signalStateComponent.outputs[sideIndex] == strength)
                return true;

            int previousValue = signalStateComponent.outputs[sideIndex];
            signalStateComponent.outputs[sideIndex] = strength;
            entityRef.addOrSaveComponent(signalStateComponent);
            signalAllLeafsFromSide(entityRef, side, Math.max(strength, previousValue));
            return true;
        }
        return false;
    }

    public boolean setLeafOutput(EntityRef entityRef, Side side, byte strength, long delay) {
        SignalLeafComponent signalLeafComponent = entityRef.getComponent(SignalLeafComponent.class);
        if (signalLeafComponent == null)
            return false;

        if (signalLeafComponent.outputs.contains(side)) {
            SignalStateComponent signalStateComponent = entityRef.getComponent(SignalStateComponent.class);
            if (signalStateComponent == null)
                signalStateComponent = new SignalStateComponent();
            if (signalStateComponent.outputs[SignalStateComponent.OUTPUT_SIDES.indexOf(getTransformedSide(entityRef,side))] == strength)
                return true;

            delays.add(new SignalDelayHandler(delay, time.getGameTimeInMs(), entityRef, strength, side));
            return true;

        }
        return false;
    }


    public int getLeafInput(EntityRef entityRef, Side side) {

        BlockComponent blockComponent = entityRef.getComponent(BlockComponent.class);

        AtomicInteger strength = new AtomicInteger();
        findDistanceToLeaf(blockComponent.getPosition(), getTransformedSide(entityRef,side), (targetSide, distance, target) -> {
            SignalStateComponent signalStateComponent = target.getComponent(SignalStateComponent.class);
            int outputStrength = 0;
            if (signalStateComponent != null)
                outputStrength = signalStateComponent.outputs[SignalStateComponent.OUTPUT_SIDES.indexOf(targetSide)];
            if (outputStrength == -1) {
                strength.set(-1);
                return false;
            }
            int delta = outputStrength - distance;
            if (delta > 0 && strength.get() < delta) {
                strength.set(delta);
            }
            return true;
        }, Integer.MAX_VALUE);
        return strength.get();
    }

    private EnumSet<Side> getConnections(EntityRef entityRef) {
        if (entityRef.hasComponent(SignalLeafComponent.class)) {
            SignalLeafComponent signalLeafComponent = entityRef.getComponent(SignalLeafComponent.class);
            EnumSet<Side> sides = EnumSet.noneOf(Side.class);
            for (Side side : signalLeafComponent.inputs) {
                sides.add(getTransformedSide(entityRef,side));
            }

            for (Side side : signalLeafComponent.outputs) {
                sides.add(getTransformedSide(entityRef,side));
            }
            return sides;
        } else if (entityRef.hasComponent(CableComponent.class)) {
            BlockComponent blockComponent = entityRef.getComponent(BlockComponent.class);
            Block block = blockComponent.getBlock();
            if (block.getBlockFamily() instanceof MultiConnectFamily) {
                return SideBitFlag.getSides(((MultiConnectFamily) block.getBlockFamily()).getConnections(block.getURI()));
            }
        }
        return EnumSet.noneOf(Side.class);
    }


    public void signalAllSidesAroundLocation(Vector3i location, int distanceCap) {

        for (Side side : Side.values()) {
            this.findDistanceToLeaf(location, side, (targetSide, distance, target) -> {
                signalLeafChange(target);
                return true;
            }, distanceCap);
        }

    }

    public void signalAllLeafsFromSide(EntityRef entityRef, Side side, int distanceCap) {
        if (entityRef.hasComponent(SignalLeafComponent.class)) {
            BlockComponent blockComponent = entityRef.getComponent(BlockComponent.class);
            this.findDistanceToLeaf(blockComponent.getPosition(), getTransformedSide(entityRef,side), (targetSide, distance, target) -> {
                signalLeafChange(target);
                return true;
            }, distanceCap);
        }
    }


    public void signalLeafChange(EntityRef entityRef) {
        SignalLeafComponent signalLeafComponent = entityRef.getComponent(SignalLeafComponent.class);

        Map<Side, Integer> inputs = Maps.newHashMap();
        for (Side side : signalLeafComponent.inputs) {
            if (getLeafInput(entityRef, side) != 0) {
                inputs.put(side, getLeafInput(entityRef, side));
            }
        }
        entityRef.send(new LeafNodeSignalChange(inputs));
    }

    public void findDistanceToLeaf(Vector3i location, Side side, SignalResponse handler, int distanceCap) {
        Vector3i startingSide = new Vector3i(location).add(side.getVector3i());
        EntityRef entityRef = blockEntityRegistry.getBlockEntityAt(startingSide);
        if (entityRef.hasComponent(SignalLeafComponent.class)) {
            handler.response(side.reverse(), 0, entityRef);
            return;
        }

        TreeMap<Integer, Set<Vector3i>> toVisit = new TreeMap<>();
        toVisit.put(1, Sets.newHashSet(startingSide));
        Set<Vector3i> visited = Sets.newHashSet(location);
        do {
            int minimum = toVisit.firstKey();
            if (minimum > distanceCap)
                break;
            Set<Vector3i> entries = toVisit.get(minimum);
            for (Vector3i entry : entries) {
                EntityRef ref = blockEntityRegistry.getBlockEntityAt(entry);
                BlockComponent blockComponent = ref.getComponent(BlockComponent.class);
                for (Side s : getConnections(ref)) {
                    Vector3i loc = new Vector3i(blockComponent.getPosition()).add(s.getVector3i());
                    EntityRef nextEntityRef = blockEntityRegistry.getBlockEntityAt(loc);
                    if (!visited.contains(loc)) {
                        if (nextEntityRef.hasComponent(SignalLeafComponent.class)) {
                            if (!handler.response(s.reverse(), minimum, nextEntityRef))
                                return;
                        } else if (nextEntityRef.hasComponent(CableComponent.class)) {
                            toVisit.putIfAbsent(minimum + 1, Sets.newHashSet());
                            Set<Vector3i> entrySet = toVisit.get(minimum + 1);
                            entrySet.add(loc);
                        }
                        visited.add(loc);
                    }
                }
            }
            toVisit.remove(minimum);
        }
        while (toVisit.size() > 0);

    }

    @Override
    public void update(float delta) {
        while (delays.peek() != null && delays.peek().getTime() < time.getGameTimeInMs()) {
            SignalDelayHandler signalDelayHandler = delays.poll();
            this.setLeafOutput(signalDelayHandler.entityRef, signalDelayHandler.side, signalDelayHandler.strength);
        }
    }

    public interface SignalResponse {
        boolean response(Side targetSide, int distance, EntityRef target);
    }

    public static class SignalDelayHandler {
        public final long delta;
        public final long currentTime;
        public final EntityRef entityRef;
        public final byte strength;
        public final Side side;

        SignalDelayHandler(long delta, long currentTime, EntityRef entityRef, byte strength, Side side) {
            this.delta = delta;
            this.currentTime = currentTime;

            this.entityRef = entityRef;
            this.strength = strength;
            this.side = side;
        }

        public long getTime() {
            return currentTime + delta;
        }

    }

    public static class SignalDelayComparitor implements Comparator<SignalDelayHandler> {

        @Override
        public int compare(SignalDelayHandler t1, SignalDelayHandler t2) {
            return (int) (t1.getTime() - t2.getTime());
        }
    }

}

