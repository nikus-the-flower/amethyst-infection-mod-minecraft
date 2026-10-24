package com.amethystmod.client;

import com.amethystmod.entity.AmethystMob;
import com.amethystmod.entity.HeartEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * A model built from a {@link GeoDef}. Part names decide how it animates: legs, arms, head,
 * tail, wings, tentacles and spinning/pulsing parts are posed procedurally for the model's
 * animation family ("quadruped", "biped", "raptor", "spider", "flyer", "heart").
 */
public class GeoModel<T extends MobEntity> extends SinglePartEntityModel<T> {
	private final ModelPart root;
	private final String anim;
	private final Map<String, ModelPart> parts = new HashMap<>();

	public GeoModel(ModelPart root, GeoDef def) {
		this.root = root;
		this.anim = def.anim;
		for (GeoDef.Part part : def.parts) {
			java.util.ArrayDeque<String> chain = new java.util.ArrayDeque<>();
			for (GeoDef.Part cur = part; cur != null; cur = cur.parent() == null ? null : def.byName.get(cur.parent())) {
				chain.addFirst(cur.name());
			}
			ModelPart found = root;
			for (String name : chain) {
				found = found.getChild(name);
			}
			parts.put(part.name(), found);
		}
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	private ModelPart p(String name) {
		return parts.get(name);
	}

	private void show(String name, boolean visible) {
		ModelPart part = parts.get(name);
		if (part != null) {
			part.visible = visible;
		}
	}

	private void rot(String name, float pitch, float yaw, float roll) {
		ModelPart part = parts.get(name);
		if (part != null) {
			part.pitch += pitch;
			part.yaw += yaw;
			part.roll += roll;
		}
	}

	/** Ability poses of humanoids (Amitariel, walkers, colossi); {@code left} = ticks remaining. */
	private void pose(int anim, int left, float age) {
		float shake = MathHelper.sin(age * 1.4f) * 0.06f;
		ModelPart torso = p("torso");
		switch (anim) {
			case AmethystMob.ANIM_THRUST -> {
				// spear drawn back, then driven forward
				boolean strike = left < 10;
				rot("arm_r", strike ? -1.7f : 0.6f, strike ? -0.2f : 0.3f, 0);
				rot("arm_r_low", strike ? 0.0f : -1.2f, 0, 0);
				rot("arm_l", strike ? 0.6f : -0.8f, 0, -0.3f);
				rot("torso", strike ? 0.35f : -0.15f, strike ? -0.3f : 0.25f, 0);
			}
			case AmethystMob.ANIM_CAST -> {
				rot("arm_r", -1.5f + shake, -0.15f, 0);
				rot("arm_r_low", -0.1f, 0, 0);
				rot("arm_l", -0.6f, 0, -0.5f);
				rot("head", -0.1f, 0, 0);
			}
			case AmethystMob.ANIM_SLAM -> {
				// arms raised high, then brought down onto the floor
				boolean down = left < 8;
				rot("arm_l", down ? -0.4f : -2.8f + shake, 0, 0.15f);
				rot("arm_r", down ? -0.4f : -2.8f - shake, 0, -0.15f);
				rot("torso", down ? 0.45f : -0.2f, 0, 0);
				rot("head", down ? 0.3f : -0.35f, 0, 0);
			}
			case AmethystMob.ANIM_SPREAD -> {
				// arms spread wide, rising into the air
				rot("arm_l", -0.3f, 0, -1.3f + shake);
				rot("arm_r", -0.3f, 0, 1.3f - shake);
				rot("head", -0.4f, 0, 0);
				if (torso != null) {
					torso.pivotY -= 4 + MathHelper.sin(age * 0.15f) * 1.5f;
				}
				rot("leg_l", 0.25f, 0, -0.1f);
				rot("leg_r", 0.15f, 0, 0.1f);
			}
			case AmethystMob.ANIM_CROUCH -> {
				if (torso != null) {
					torso.pivotY += 4;
				}
				rot("torso", 0.5f, 0, 0);
				rot("leg_l", -0.9f, 0, 0);
				rot("leg_r", -0.9f, 0, 0);
				rot("arm_r", -0.9f, 0, 0.4f);
			}
			case AmethystMob.ANIM_SPIN -> {
				// the spear held out to the side while he whirls (the whole body turns on the server)
				rot("arm_r", -0.2f, 0, 1.35f);
				rot("arm_r_low", -0.2f, 0, 0);
				rot("arm_l", -0.2f, 0, -1.2f);
				rot("torso", 0.12f, 0, 0);
				rot("head", -0.15f, 0, 0);
			}
			case AmethystMob.ANIM_KNEEL -> {
				if (torso != null) {
					torso.pivotY += 9;
				}
				rot("leg_l", -1.5f, 0, -0.1f);
				rot("leg_r", 0.2f, 0, 0.1f);
				rot("torso", 0.45f, 0, 0);
				rot("head", 0.7f, 0, 0);
				rot("arm_l", 0.2f, 0, -0.2f);
				rot("arm_r", -0.5f, 0, 0.1f);
			}
			default -> {
				rot("arm_l", -2.4f + shake, 0, 0.2f);
				rot("arm_r", -2.4f - shake, 0, -0.2f);
				rot("head", -0.25f, 0, 0);
			}
		}
	}

	/** 0 closed .. 1 open, eased from the world time the Heart opened or closed. */
	private static float openness(HeartEntity heart, float age) {
		int t = heart.getOpenTime();
		float now = (float) (heart.getWorld().getTime() % 1_000_000_000L) + (age - (int) age);
		if (t > 0) {
			return MathHelper.clamp((now - t) / 12.0f, 0, 1);
		}
		return 1.0f - MathHelper.clamp((now + t) / 8.0f, 0, 1);
	}

	@Override
	public void setAngles(T entity, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
		root.traverse().forEach(ModelPart::resetTransform);
		float yaw = headYaw * 0.017453292f;
		float pitch = headPitch * 0.017453292f;
		float swing = MathHelper.cos(limbAngle * 0.6662f) * limbDistance;
		float action = entity instanceof AmethystMob mob && mob.getAction() > 0 ? 1.0f : 0.0f;
		float attack = entity.handSwingProgress;
		float chop = -MathHelper.sin(MathHelper.sqrt(attack) * (float) Math.PI);

		switch (anim) {
			case "quadruped" -> {
				rot("leg_fl", 1.2f * swing, 0, 0);
				rot("leg_br", 1.2f * swing, 0, 0);
				rot("leg_fr", -1.2f * swing, 0, 0);
				rot("leg_bl", -1.2f * swing, 0, 0);
				rot("head", pitch + (entity.isAttacking() ? 0.35f : 0) + chop * 0.6f, yaw * 0.8f, 0);
				rot("tail", 0, MathHelper.sin(age * 0.1f) * 0.12f + swing * 0.3f, 0);
				rot("body", chop * -0.15f, 0, 0);
			}
			case "biped", "raptor" -> {
				float legs = anim.equals("raptor") ? 1.6f : 1.3f;
				rot("leg_l", -legs * swing, 0, 0);
				rot("leg_r", legs * swing, 0, 0);
				rot("head", pitch, yaw * 0.8f, 0);
				float armSwing = anim.equals("raptor") ? 0.6f : 1.0f;
				rot("arm_l", armSwing * swing, 0, -0.05f);
				rot("arm_r", -armSwing * swing, 0, 0.05f);
				rot("arm_l_low", -0.2f - Math.abs(swing) * 0.4f, 0, 0);
				rot("arm_r_low", -0.2f - Math.abs(swing) * 0.4f, 0, 0);
				// melee swing
				rot("arm_r", chop * 1.7f, 0, 0);
				rot("arm_l", chop * 1.1f, 0, 0);
				rot("arm_r_low", chop * 0.9f, 0, 0);
				if (action > 0 && entity instanceof AmethystMob mob) {
					pose(mob.getAnim(), mob.getAction(), age);
				}
				rot("tail", 0, MathHelper.sin(age * 0.12f) * 0.15f + swing * 0.3f, 0);
				rot("tail_b", 0, MathHelper.sin(age * 0.12f - 1.0f) * 0.25f, 0);
				rot("torso", anim.equals("raptor") ? MathHelper.abs(swing) * -0.08f : 0, 0, 0);
			}
			case "spider" -> {
				rot("head", pitch * 0.5f, yaw * 0.6f, 0);
				float[] offsets = {0.0f, (float) Math.PI, (float) Math.PI / 2, 3.0f * (float) Math.PI / 2};
				for (int i = 0; i < 4; i++) {
					float swayYaw = -(MathHelper.cos(limbAngle * 1.3324f + offsets[i]) * 0.4f) * limbDistance;
					float lift = Math.abs(MathHelper.sin(limbAngle * 0.6662f + offsets[i])) * 0.4f * limbDistance;
					rot("leg_l" + i, 0, swayYaw, lift);
					rot("leg_r" + i, 0, -swayYaw, -lift);
					rot("leg_l" + i + "_b", 0, 0, lift * 0.5f);
					rot("leg_r" + i + "_b", 0, 0, -lift * 0.5f);
				}
				rot("abdomen", MathHelper.sin(age * 0.1f) * 0.04f - 0.04f, 0, 0);
			}
			case "flyer" -> {
				ModelPart body = p("body");
				float bob = MathHelper.sin(age * 0.12f) * 2.0f;
				if (body != null) {
					body.pivotY += bob;
					body.pitch += 0.15f + (action > 0 ? -0.35f : 0);
				}
				rot("head", pitch * 0.6f - 0.15f, yaw * 0.6f, 0);
				for (int i = 0; i < 6; i++) {
					float flap = MathHelper.sin(age * 0.45f - i * 0.18f) * (action > 0 ? 0.25f : 0.5f) - (action > 0 ? 0.6f : 0f);
					rot("wing_l_" + i, 0, 0, -flap);
					rot("wing_r_" + i, 0, 0, flap);
				}
				for (int i = 0; i < 7; i++) {
					float sway = MathHelper.sin(age * 0.1f + i * 1.3f) * 0.22f;
					rot("tentacle_" + i, sway, 0, MathHelper.cos(age * 0.09f + i) * 0.18f);
					rot("tentacle_" + i + "_b", sway * 1.4f, 0, MathHelper.cos(age * 0.11f + i) * 0.22f);
				}
			}
			case "heart" -> {
				ModelPart core = p("core");
				HeartEntity heart = entity instanceof HeartEntity h ? h : null;
				int heartAnim = heart == null ? 0 : heart.getAnim();
				int left = heart == null ? 0 : heart.getAction();
				float open = heart == null ? 0 : openness(heart, age);
				if (core != null) {
					float beat = 1.0f + 0.035f * MathHelper.sin(age * 0.16f);
					float lift = MathHelper.sin(age * 0.05f) * 1.5f;
					float shiver = 0;
					if (heart != null) {
						int move = heartAnim - HeartEntity.AN_MOVE;
						if (heartAnim == HeartEntity.AN_AWAKEN) {
							// rises from a slump, beat slowly gathering strength
							float p = 1.0f - left / 140.0f;
							lift += -14.0f * (1.0f - MathHelper.clamp(p * 1.4f, 0, 1));
							beat = 0.85f + 0.15f * MathHelper.clamp(p * 1.4f, 0, 1) + 0.06f * MathHelper.sin(age * 0.4f) * p;
						} else if (heartAnim == HeartEntity.AN_RAGE) {
							shiver = 1.2f;
							beat += 0.08f * MathHelper.sin(age * 0.9f);
						} else if (heartAnim == HeartEntity.AN_DYING) {
							float p = 1.0f - left / 120.0f;
							shiver = 0.6f + p * 2.5f;
							beat = 1.0f - p * 0.25f + 0.05f * MathHelper.sin(age * 1.3f);
							lift -= p * 6.0f;
						} else if (move == HeartEntity.Move.ERUPTION.ordinal()) {
							// sinks, gathering, then slams down as the crystals burst
							lift += left > 8 ? -3.0f * (1 - left / 44.0f) : 2.5f;
						} else if (move == HeartEntity.Move.PENTAGON.ordinal()) {
							// rears up, then slams down as the big crystals burst
							lift += left > 50 ? 3.5f * (90 - left) / 40.0f : left > 40 ? -2.0f : 0.0f;
						} else if (move == HeartEntity.Move.CHASE.ordinal() || (move >= 0 && move < HeartEntity.Move.values().length
								&& HeartEntity.Move.values()[move].combo())) {
							beat += 0.07f * MathHelper.sin(age * 1.6f);
							shiver = 0.4f;
						} else if (move == HeartEntity.Move.SPIRAL.ordinal() || move == HeartEntity.Move.BIG_SPIRAL.ordinal()
								|| move == HeartEntity.Move.DOUBLE_SPIRAL.ordinal()) {
							beat += 0.06f * MathHelper.sin(age * 0.8f);
							open = Math.max(open, 0.3f);
						} else if (move == HeartEntity.Move.SHOCKWAVE.ordinal()) {
							beat = left > 65 ? 1.0f - 0.12f * (96 - left) / 31.0f : 1.12f - 0.12f * MathHelper.clamp((65 - left) / 10.0f, 0, 1);
						} else if (move == HeartEntity.Move.PULL_NOVA.ordinal()) {
							beat = left > 8 ? 1.0f + 0.18f * (48 - left) / 40.0f : 0.9f;
						} else if (move == HeartEntity.Move.FLASH.ordinal()) {
							beat = left > 26 ? 1.0f + 0.15f * (56 - left) / 30.0f : 1.0f;
						} else if (move == HeartEntity.Move.VOLLEY.ordinal() || move == HeartEntity.Move.RAIN.ordinal()
								|| move == HeartEntity.Move.GROW.ordinal()) {
							beat += 0.05f * MathHelper.sin(age * 1.2f);
						} else if (move == HeartEntity.Move.RING_BEAMS.ordinal() || move == HeartEntity.Move.DEATH_BEAM.ordinal()
								|| move == HeartEntity.Move.LASERS.ordinal()) {
							open = Math.max(open, 0.45f);
						}
						if (move == HeartEntity.Move.FLASH.ordinal()) {
							open = Math.max(open, 0.9f);
						}
						if (heartAnim == HeartEntity.AN_DYING) {
							open = 1.0f + (1.0f - left / 120.0f) * 1.5f;
						}
					}
					core.xScale = beat;
					core.yScale = beat;
					core.zScale = beat;
					core.pivotY += lift;
					if (shiver > 0) {
						core.pivotX += MathHelper.sin(age * 3.1f) * shiver;
						core.pivotZ += MathHelper.cos(age * 2.7f) * shiver;
					}
				}
					// later phases: the lower plates fell off (phase 2), then all of them (phase 3+)
				int stage = heart == null ? 0 : heart.getStage();
				for (int i = 0; i < 8; i++) {
					boolean gone = stage >= 3 || (stage == 2 && (i == 0 || i == 5));
					show("plate_f_" + i, !gone);
					show("plate_b_" + i, !gone);
				}
				for (int i = 0; i < 4; i++) {
					show("node_" + i, stage <= 1);
					show("node_shell_" + i, stage <= 1);
					show("weak2_" + i, stage == 2);
					show("weak2_cap_" + i, stage == 2);
					show("weak3_" + i, stage >= 3);
					show("weak3_cap_" + i, stage == 3);
				}
				// phase 4: nothing left to hide, the Heart stays open
				if (stage >= 4) {
					open = Math.max(open, 1.0f);
				}
				// the caps over the weak points of phases 2-3 slide away while the Heart is open
				for (int i = 0; i < 4; i++) {
					for (String cap : new String[]{"weak2_cap_" + i, "weak3_cap_" + i}) {
						ModelPart part = p(cap);
						if (part != null) {
							float o = Math.min(1.0f, open);
							part.xScale = 1.0f - o * 0.8f;
							part.yScale = 1.0f - o * 0.8f;
							part.zScale = 1.0f - o * 0.8f;
						}
					}
					for (String weak : new String[]{"weak2_" + i, "weak3_" + i}) {
						ModelPart part = p(weak);
						if (part != null) {
							float s = 0.8f + 0.2f * Math.min(1.0f, open) + 0.1f * MathHelper.sin(age * 0.3f + i) * Math.min(1.0f, open);
							part.xScale = s;
							part.yScale = s;
							part.zScale = s;
						}
					}
				}
				// armour plates slide out and tilt when the Heart opens
				for (int i = 0; i < 8; i++) {
					ModelPart front = p("plate_f_" + i);
					ModelPart back = p("plate_b_" + i);
					if (front != null) {
						front.pivotZ -= open * 3.0f;
						front.pitch -= open * 0.25f * ((i % 2) * 2 - 1);
					}
					if (back != null) {
						back.pivotZ += open * 3.0f;
						back.pitch += open * 0.25f * ((i % 2) * 2 - 1);
					}
				}
				// node shells lift away to expose the glowing nodes
				for (int i = 0; i < 4; i++) {
					ModelPart shell = p("node_shell_" + i);
					if (shell != null) {
						shell.pivotY -= Math.min(1.0f, open) * 9.0f;
						float s = 1.0f - Math.min(1.0f, open) * 0.4f;
						shell.xScale = s;
						shell.zScale = s;
					}
				}
				rot("spin_ring", 0, age * (action > 0 ? 0.08f : 0.025f), 0);
				rot("spin_ring2", 0, -age * (action > 0 ? 0.06f : 0.018f), 0);
				for (int i = 0; i < 7; i++) {
					rot("ring2_shard_" + i, 0, age * 0.05f, 0);
				}
				for (int i = 0; i < 4; i++) {
					ModelPart rune = p("rune_" + i);
					if (rune != null) {
						rune.pivotY += MathHelper.sin(age * 0.06f + i * 1.6f) * 3.0f;
					}
				}
				for (int i = 0; i < 3; i++) {
					float pulse = 1.0f + 0.06f * MathHelper.sin(age * 0.16f - i * 0.8f);
					ModelPart aorta = p("aorta_" + i);
					if (aorta != null) {
						aorta.xScale = pulse;
						aorta.zScale = pulse;
					}
				}
				for (int i = 0; i < 10; i++) {
					rot("ring_shard_" + i, age * 0.04f, 0, age * 0.03f);
				}
				for (int i = 0; i < 4; i++) {
					ModelPart node = p("node_" + i);
					if (node != null) {
						float s = 0.7f + 0.3f * Math.min(1.0f, open) + 0.12f * MathHelper.sin(age * 0.3f + i) * Math.min(1.0f, open);
						node.xScale = s;
						node.yScale = s;
						node.zScale = s;
					}
				}
				for (int i = 0; i < 6; i++) {
					float sway = MathHelper.sin(age * 0.08f + i * 1.7f) * 0.06f;
					rot("sway_vein_" + i, sway, 0, MathHelper.cos(age * 0.07f + i) * 0.18f);
					rot("sway_vein_" + i + "_b", sway * 1.3f, 0, 0);
				}
			}
			case "core" -> {
				// one model holds all three phases; only the current one is shown
				int phase = entity instanceof com.amethystmod.entity.InfectionCoreEntity core ? core.getPhase() : 1;
				for (int i = 1; i <= 3; i++) {
					show("p" + i, i == phase);
				}
				ModelPart heart = p("p" + phase + "_heart");
				if (heart != null) {
					heart.pivotY += MathHelper.sin(age * 0.07f) * (0.4f + phase * 0.3f);
					heart.yaw += age * 0.012f;
				}
				rot("p" + phase + "_ring", 0, age * (action > 0 ? 0.12f : 0.035f), 0);
			}
			case "crystal" -> {
				ModelPart bob = p("bob");
				if (bob != null) {
					bob.pivotY += MathHelper.sin(age * 0.08f) * 1.5f;
					bob.yaw += age * 0.01f;
				}
				rot("spin_ring", 0, age * (action > 0 ? 0.15f : 0.05f), 0);
			}
			default -> {
			}
		}
	}
}
