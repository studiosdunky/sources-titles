package com.github.neveshardd.titles;

import io.netty.buffer.Unpooled;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.List;

final class TitleStack {

    private final Player owner;
    private final Slime slime;
    private final ArmorStand stand;
    private final List<SynchedEntityData.DataValue<?>> legacySlimeData;
    private final List<SynchedEntityData.DataValue<?>> modernSlimeData;
    private final ClientboundUpdateAttributesPacket modernSlimeScale;

    TitleStack(Player owner, Component text, int legacySize, int modernSize, double modernScale) {
        this.owner = owner;
        ServerLevel level = ((CraftWorld) owner.getWorld()).getHandle();

        this.slime = new Slime(EntityTypes.SLIME, level);
        slime.setInvisible(true);
        slime.setSilent(true);
        slime.setNoAi(true);
        slime.setNoGravity(true);
        slime.setSize(legacySize, false);
        this.legacySlimeData = slime.getEntityData().packAll();
        slime.setSize(modernSize, false);
        this.modernSlimeData = slime.getEntityData().packAll();
        AttributeInstance scale = slime.getAttribute(Attributes.SCALE);
        scale.setBaseValue(modernScale);
        this.modernSlimeScale = new ClientboundUpdateAttributesPacket(slime.getId(), List.of(scale));

        this.stand = new ArmorStand(EntityTypes.ARMOR_STAND, level);
        stand.setInvisible(true);
        stand.setSilent(true);
        stand.setNoGravity(true);
        stand.setMarker(true);
        stand.setCustomNameVisible(true);
        stand.setCustomName(PaperAdventure.asVanilla(text));
    }

    Player owner() {
        return owner;
    }

    boolean owns(int entityId) {
        return entityId == slime.getId() || entityId == stand.getId();
    }

    void show(Player viewer, boolean legacy) {
        Location at = owner.getLocation();
        send(viewer, spawn(slime, at));
        send(viewer, spawn(stand, at));
        send(viewer, new ClientboundSetEntityDataPacket(slime.getId(), legacy ? legacySlimeData : modernSlimeData));
        send(viewer, new ClientboundSetEntityDataPacket(stand.getId(), stand.getEntityData().packAll()));
        if (!legacy) {
            send(viewer, modernSlimeScale);
        }
        send(viewer, passengers(owner.getEntityId(), slime.getId()));
        send(viewer, passengers(slime.getId(), stand.getId()));
    }

    void hide(Player viewer) {
        send(viewer, new ClientboundRemoveEntitiesPacket(slime.getId(), stand.getId()));
    }

    void rename(Component text, Iterable<? extends Player> viewers) {
        stand.setCustomName(PaperAdventure.asVanilla(text));
        ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(stand.getId(), stand.getEntityData().packAll());
        for (Player viewer : viewers) {
            send(viewer, packet);
        }
    }

    private static ClientboundAddEntityPacket spawn(Entity entity, Location at) {
        return new ClientboundAddEntityPacket(entity.getId(), entity.getUUID(), at.getX(), at.getY(), at.getZ(),
                0F, 0F, entity.getType(), 0, Vec3.ZERO, 0D);
    }

    private static ClientboundSetPassengersPacket passengers(int vehicle, int... riders) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buf.writeVarInt(vehicle);
            buf.writeVarIntArray(riders);
            return ClientboundSetPassengersPacket.STREAM_CODEC.decode(buf);
        } finally {
            buf.release();
        }
    }

    private static void send(Player viewer, Packet<?> packet) {
        ((CraftPlayer) viewer).getHandle().connection.send(packet);
    }
}
