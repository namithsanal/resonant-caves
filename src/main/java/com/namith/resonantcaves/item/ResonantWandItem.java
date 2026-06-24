package com.namith.resonantcaves.item;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * A hitscan ranged weapon fueled by nearby Station energy — see {@link WandAttacks} for the
 * shared attack logic also used by wand-holding skeletons and snow golems. Right-click fires
 * instantly at whatever the wielder is looking at; right-clicking a Snow Golem equips it instead.
 */
public class ResonantWandItem extends Item {
	public ResonantWandItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		if (!world.isClient) {
			WandAttacks.AttackResult result = WandAttacks.fire(user, false, WandAttacks.EXTENDED_STATION_SEARCH_RADIUS, true);
			if (!result.fired()) {
				user.sendMessage(Text.translatable("item.resonantcaves.resonant_wand.no_power"), true);
			}
		}
		return TypedActionResult.success(user.getStackInHand(hand), world.isClient);
	}

	@Override
	public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
		if (!(entity instanceof SnowGolemEntity golem)) {
			return ActionResult.PASS;
		}
		if (!user.getWorld().isClient) {
			golem.equipStack(EquipmentSlot.MAINHAND, stack.copyWithCount(1));
			if (!user.getAbilities().creativeMode) {
				stack.decrement(1);
			}
		}
		return ActionResult.success(user.getWorld().isClient);
	}
}
