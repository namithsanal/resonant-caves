package com.namith.resonantcaves.block.entity;

import com.namith.resonantcaves.network.payload.OpenVillageCoreScreenPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

/** Stores the last-generated village depth so the GUI resumes from where the player left off. */
public class CreativeVillageCoreBlockEntity extends BlockEntity {
	private int depth = 0;

	public CreativeVillageCoreBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CREATIVE_VILLAGE_CORE, pos, state);
	}

	public int getDepth() {
		return this.depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
		this.markDirty();
	}

	public void openScreen(ServerPlayerEntity player) {
		if (this.world == null || this.world.isClient) {
			return;
		}
		ServerPlayNetworking.send(player, new OpenVillageCoreScreenPayload(this.pos, this.depth));
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.writeNbt(nbt, registryLookup);
		nbt.putInt("Depth", this.depth);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.readNbt(nbt, registryLookup);
		this.depth = nbt.contains("Depth") ? nbt.getInt("Depth") : 0;
	}
}
