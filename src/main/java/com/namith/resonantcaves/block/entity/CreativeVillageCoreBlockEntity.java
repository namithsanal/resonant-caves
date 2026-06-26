package com.namith.resonantcaves.block.entity;

import com.namith.resonantcaves.network.payload.OpenVillageCoreScreenPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

/** Minimal block entity — just opens the GUI on right-click; no state to persist. */
public class CreativeVillageCoreBlockEntity extends BlockEntity {
	public CreativeVillageCoreBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CREATIVE_VILLAGE_CORE, pos, state);
	}

	public void openScreen(ServerPlayerEntity player) {
		if (this.world == null || this.world.isClient) {
			return;
		}
		ServerPlayNetworking.send(player, new OpenVillageCoreScreenPayload(this.pos));
	}
}
