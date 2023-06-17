package com.railwayteam.railways.content.switches;

import com.railwayteam.railways.mixin_interfaces.ISwitchDisabledEdge;
import com.railwayteam.railways.registry.CREdgePointTypes;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.graph.*;
import com.simibubi.create.content.trains.signal.SignalPropagator;
import com.simibubi.create.content.trains.signal.SingleBlockEntityEdgePoint;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.NBTHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/*
done: actually disable edges that aren't the active target
in Navigation.java we need to hook into this:
```java
if (!validTypes.contains(target.getValue().getTrackMaterial().trackType))
    continue;
```

and in Train.java we need to hook into this:
```java
blocked |= carriage.blocked || carriage.isOnIncompatibleTrack();
```
actually hooked into Carriage#isOnIncompatibleTrack


done: don't actually block, just remove it from the list of things we can go toward
to do this:
hook into {@link TrackEdge#canTravelTo }
 */
public class TrackSwitch extends SingleBlockEntityEdgePoint {
    private TrackNodeLocation switchPoint;
    private final List<TrackNodeLocation> exits = new ArrayList<>();

    public TrackSwitch() {
    }

    private @Nullable TrackNodeLocation straightExit;
    private @Nullable TrackNodeLocation leftExit;
    private @Nullable TrackNodeLocation rightExit;

    private @NotNull TrackSwitchBlock.SwitchState switchState = TrackSwitchBlock.SwitchState.NORMAL;
    private boolean automatic;

    public boolean isAutomatic() {
        return automatic;
    }

    @Override
    public EdgePointType<?> getType() {
        return CREdgePointTypes.SWITCH;
    }

    @Override
    public boolean canCoexistWith(EdgePointType<?> otherType, boolean front) {
        return otherType == EdgePointType.SIGNAL;
    }

    @Override
    public void blockEntityAdded(BlockEntity tile, boolean front) {
        super.blockEntityAdded(tile, front);

        if (tile instanceof TrackSwitchTileEntity te) {
            te.calculateExits(this);
            automatic = te.isAutomatic();
        }

        notifyTrains(tile.getLevel());
    }

    @Override
    public void onRemoved(TrackGraph graph) {
        exits.clear();
        sortExits();
        removeFromAllGraphs();
    }

    private void notifyTrains(Level level) {
        TrackGraph graph = Create.RAILWAYS.sided(level).getGraph(level, edgeLocation.getFirst());
        if (graph == null)
            return;
        TrackEdge edge = graph.getConnection(edgeLocation.map(graph::locateNode));
        if (edge == null)
            return;
        SignalPropagator.notifyTrains(graph, edge);
    }

    void updateExits(TrackNodeLocation switchPoint, Collection<TrackNodeLocation> newExits) {
        this.switchPoint = switchPoint;

        if (edgeLocation == null) {
            exits.clear();
            return;
        }
        Vec3 forward = edgeLocation.getFirst().getLocation().vectorTo(edgeLocation.getSecond().getLocation());
        exits.clear();
        exits.addAll(newExits.stream()
                // Exit should be in the same direction switch is facing
                .filter(e -> forward.dot(switchPoint.getLocation().vectorTo(e.getLocation())) > 0)
                .toList());
        sortExits();
        ensureValidState();
    }

    private void sortExits() {
        Vec3 forward = edgeLocation.getFirst().getLocation().vectorTo(edgeLocation.getSecond().getLocation()).normalize();
        exits.sort(Comparator.comparing(e -> {
            // Relative exit directions -- 0 is straight on, negative for left, positive for right
            Vec3 exitDir = switchPoint.getLocation().vectorTo(e.getLocation());
            return forward.x * exitDir.z - forward.z * exitDir.x;
        }));

        if (exits.size() == 1) {
            leftExit = null;
            straightExit = exits.get(0);
            rightExit = null;
        } else if (exits.size() == 2) {
            Vec3 firstExitDir = switchPoint.getLocation().vectorTo(exits.get(0).getLocation()).normalize();
            Vec3 secondExitDir = switchPoint.getLocation().vectorTo(exits.get(1).getLocation()).normalize();

            double firstExitRelativeDir = forward.x * firstExitDir.z - forward.z * firstExitDir.x;
            double secondExitRelativeDir = forward.x * secondExitDir.z - forward.z * secondExitDir.x;

            double cutoff = 0.2;

            // Determine which exit is left/right/straight based on relative exit directions
            // 0.2 *should* be straight enough, maybe
            if (firstExitRelativeDir < 0 && secondExitRelativeDir <= cutoff) {
                //    / /         /
                // --+-'   or  --+---  = left, straight
                leftExit = exits.get(0);
                straightExit = exits.get(1);
                rightExit = null;
            } else if (firstExitRelativeDir >= -cutoff && secondExitRelativeDir > 0) {
                // --+-.       --+---
                //    \ \  or     \    = right, straight
                leftExit = null;
                straightExit = exits.get(0);
                rightExit = exits.get(1);
            } else {
                //    /
                // --<   = left, right
                //    \
                leftExit = exits.get(0);
                straightExit = null;
                rightExit = exits.get(1);
            }
        } else if (exits.size() == 3) {
            leftExit = exits.get(0);
            straightExit = exits.get(1);
            rightExit = exits.get(2);
        } else {
            leftExit = null;
            straightExit = null;
            rightExit = null;
        }
    }

    @Nullable TrackNodeLocation getSwitchPoint() {
        return switchPoint;
    }

    Collection<TrackNodeLocation> getExits() {
        return exits.stream().toList();
    }

    private boolean isStateValid(TrackSwitchBlock.SwitchState state) {
        return switch (state) {
            case NORMAL -> hasStraightExit();
            case REVERSE_RIGHT -> hasRightExit();
            case REVERSE_LEFT -> hasLeftExit();
        };
    }

    private TrackSwitchBlock.SwitchState getValidSwitchState() {
        for (TrackSwitchBlock.SwitchState state : TrackSwitchBlock.SwitchState.values()) {
            if (isStateValid(state))
                return state;
        }
        return TrackSwitchBlock.SwitchState.NORMAL;
    }

    void ensureValidState() {
        if (!isStateValid(switchState)) {
            switchState = getValidSwitchState();
        }
    }

    public void setSwitchState(@NotNull TrackSwitchBlock.SwitchState state) {
        if (isStateValid(state) && switchState != state) {
            switchState = state;
            ticks = 10000; // force a tick
        }
    }

    public @NotNull TrackSwitchBlock.SwitchState getSwitchState() {
        return switchState;
    }

    public @Nullable TrackNodeLocation getSwitchTarget() {
        //ensureValidState();
        return switch (switchState) {
            case NORMAL -> straightExit;
            case REVERSE_RIGHT -> rightExit;
            case REVERSE_LEFT -> leftExit;
        };
    }

    public boolean hasStraightExit() {
        return straightExit != null;
    }

    public boolean hasLeftExit() {
        return leftExit != null;
    }

    public boolean hasRightExit() {
        return rightExit != null;
    }

    @Override
    public void write(CompoundTag nbt, DimensionPalette dimensions) {
        super.write(nbt, dimensions);
        nbt.put("SwitchPoint", switchPoint.write(dimensions));
        nbt.put("Exits", NBTHelper.writeCompoundList(exits, e -> e.write(dimensions)));
        nbt.putString("SwitchState", switchState.getSerializedName());
        nbt.putBoolean("Automatic", automatic);
    }

    @Override
    public void write(FriendlyByteBuf buffer, DimensionPalette dimensions) {
        super.write(buffer, dimensions);
        buffer.writeInt(switchState.ordinal());
        buffer.writeBoolean(automatic);
        switchPoint.send(buffer, dimensions);
        buffer.writeCollection(exits, (buf, e) -> e.send(buf, dimensions));
    }

    @Override
    public void read(CompoundTag nbt, boolean migration, DimensionPalette dimensions) {
        super.read(nbt, migration, dimensions);
        String exit = nbt.getString("SwitchState");
        try {
            switchState = TrackSwitchBlock.SwitchState.valueOf(exit.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            switchState = getValidSwitchState();
        }
        automatic = nbt.getBoolean("Automatic");
        updateExits(
                TrackNodeLocation.read(nbt.getCompound("SwitchPoint"), dimensions),
                nbt.getList("Exits", Tag.TAG_COMPOUND)
                        .stream()
                        .map(t -> TrackNodeLocation.read((CompoundTag) t, dimensions))
                        .toList()
        );
    }

    @Override
    public void read(FriendlyByteBuf buffer, DimensionPalette dimensions) {
        super.read(buffer, dimensions);
        switchState = TrackSwitchBlock.SwitchState.values()[buffer.readInt()];
        automatic = buffer.readBoolean();
        updateExits(
                TrackNodeLocation.receive(buffer, dimensions),
                buffer.readList(buf -> TrackNodeLocation.receive(buf, dimensions))
        );
    }

    private int ticks = 0;

    @Override
    public void tick(TrackGraph graph, boolean preTrains) {
        super.tick(graph, preTrains);
        if (preTrains) {
            ticks++;
            if (ticks < 10) {
                return;
            }
            ticks = 0;
            updateEdges(graph);
        }
    }

    public void updateEdges(TrackGraph graph) {
        updateEdges(graph, false);
    }

    public void setEdgesActive(TrackGraph graph) {
        updateEdges(graph, true);
    }

    private void updateEdges(TrackGraph graph, boolean forceActive) {
        TrackNodeLocation from = switchPoint;
        for (TrackNodeLocation to : exits) {
            if (to != null) {
                TrackNode toNode = graph.locateNode(to);
                Map<TrackNode, TrackEdge> connections = graph.getConnectionsFrom(toNode);
                if (connections == null)
                    continue;
                TrackNode closestFromNode = null;
                TrackEdge closestEdge = null;
                double closestDistance = Double.MAX_VALUE;
                for (Map.Entry<TrackNode, TrackEdge> otherEnd : connections.entrySet()) {
                    double distance = otherEnd.getKey().getLocation().distSqr(from);
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closestEdge = otherEnd.getValue();
                        closestFromNode = otherEnd.getKey();
                    }
                }
                if (closestEdge != null) {
                    ((ISwitchDisabledEdge) closestEdge.getEdgeData()).setEnabled(forceActive || getSwitchTarget() == to);
                }
                if (closestFromNode != null) {
                    TrackEdge reverseEdge = graph.getConnection(Couple.create(closestFromNode, toNode));
                    if (reverseEdge != null)
                        ((ISwitchDisabledEdge) reverseEdge.getEdgeData()).setEnabled(forceActive || getSwitchTarget() == to);
                }
                // not sure if this enabled state will be synchronized to client... hmm...
            }
        }
    }
}
