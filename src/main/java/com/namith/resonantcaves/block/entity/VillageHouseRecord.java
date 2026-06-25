package com.namith.resonantcaves.block.entity;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;

/**
 * One structure a {@code VillageCoreBlockEntity} has placed — a house (counts toward upkeep and
 * can have residents) or a decoration (zero draw, never gets a starter villager, but still
 * tracked so later site searches don't overlap it).
 */
final class VillageHouseRecord {
	final BlockPos origin;
	final Identifier templateId;
	final BlockRotation rotation;
	final int sizeX;
	final int sizeY;
	final int sizeZ;
	final long baseDraw;
	final long perVillagerDraw;
	final boolean decoration;

	VillageHouseRecord(BlockPos origin, Identifier templateId, BlockRotation rotation,
			int sizeX, int sizeY, int sizeZ, long baseDraw, long perVillagerDraw, boolean decoration) {
		this.origin = origin;
		this.templateId = templateId;
		this.rotation = rotation;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.sizeZ = sizeZ;
		this.baseDraw = baseDraw;
		this.perVillagerDraw = perVillagerDraw;
		this.decoration = decoration;
	}

	BlockBox footprint() {
		return new BlockBox(
				this.origin.getX(), this.origin.getY(), this.origin.getZ(),
				this.origin.getX() + this.sizeX - 1, this.origin.getY() + this.sizeY - 1, this.origin.getZ() + this.sizeZ - 1);
	}

	/** {@code footprint()} expanded by a walking-space margin, for overlap checks against other sites. */
	BlockBox expandedFootprint(int margin) {
		return this.footprint().expand(margin);
	}

	NbtCompound writeNbt() {
		NbtCompound nbt = new NbtCompound();
		nbt.putInt("X", this.origin.getX());
		nbt.putInt("Y", this.origin.getY());
		nbt.putInt("Z", this.origin.getZ());
		nbt.putString("Template", this.templateId.toString());
		nbt.putString("Rotation", this.rotation.name());
		nbt.putInt("SizeX", this.sizeX);
		nbt.putInt("SizeY", this.sizeY);
		nbt.putInt("SizeZ", this.sizeZ);
		nbt.putLong("BaseDraw", this.baseDraw);
		nbt.putLong("PerVillagerDraw", this.perVillagerDraw);
		nbt.putBoolean("Decoration", this.decoration);
		return nbt;
	}

	static VillageHouseRecord readNbt(NbtCompound nbt) {
		BlockPos origin = new BlockPos(nbt.getInt("X"), nbt.getInt("Y"), nbt.getInt("Z"));
		Identifier templateId = Identifier.of(nbt.getString("Template"));
		BlockRotation rotation = BlockRotation.valueOf(nbt.getString("Rotation"));
		return new VillageHouseRecord(origin, templateId, rotation,
				nbt.getInt("SizeX"), nbt.getInt("SizeY"), nbt.getInt("SizeZ"),
				nbt.getLong("BaseDraw"), nbt.getLong("PerVillagerDraw"), nbt.getBoolean("Decoration"));
	}
}
