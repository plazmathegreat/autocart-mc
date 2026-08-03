package com.shadowslice.autotntcart.network;

import com.shadowslice.autotntcart.AutoTntCartMod;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public record AutoTntCartPayload(BlockPos pos, Direction direction) implements CustomPayload {

    public static final CustomPayload.Id<AutoTntCartPayload> ID =
            new CustomPayload.Id<>(AutoTntCartMod.id("trigger"));

    public static final PacketCodec<net.minecraft.network.RegistryByteBuf, AutoTntCartPayload> CODEC =
            PacketCodec.tuple(
                    BlockPos.PACKET_CODEC, AutoTntCartPayload::pos,
                    Direction.PACKET_CODEC, AutoTntCartPayload::direction,
                    AutoTntCartPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
