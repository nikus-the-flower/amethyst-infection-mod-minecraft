package com.amethystmod.client;

import com.amethystmod.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Talking to Ricardo: his portrait, his words appearing letter by letter, and the answers to choose
 * below. The talk is a small tree (texts "dialog.amethystmod.ricardo.&lt;node&gt;"); "trade" and "gift"
 * are sent to the server.
 */
public class RicardoScreen extends Screen {
	private static final int W = 340;
	private static final int H = 150;

	private final int entityId;
	private final boolean altarAwake;
	private final boolean gifted;
	private String node = "greet";
	private List<OrderedText> lines = List.of();
	private int shown;
	private float typed;
	private int left;
	private int top;

	public RicardoScreen(int entityId, boolean altarAwake, boolean gifted) {
		super(Text.translatable("entity.amethystmod.ricardo"));
		this.entityId = entityId;
		this.altarAwake = altarAwake;
		this.gifted = gifted;
	}

	@Override
	protected void init() {
		left = (width - W) / 2;
		top = height - H - 20;
		show(node);
	}

	/** Answers available from a node: (text key suffix, next node or "!action"). */
	private List<String[]> options(String at) {
		List<String[]> o = new ArrayList<>();
		switch (at) {
			case "greet", "back" -> {
				o.add(new String[]{"who", "who"});
				o.add(new String[]{"place", "place"});
				o.add(new String[]{"altar", altarAwake ? (gifted ? "altar_thanks" : "altar_done") : "altar_quest"});
				o.add(new String[]{"trade", "!trade"});
				o.add(new String[]{"bye", "!bye"});
			}
			case "altar_done" -> {
				o.add(new String[]{"accept", "!gift"});
			}
			case "gift" -> o.add(new String[]{"thanks", "back"});
			case "who" -> {
				o.add(new String[]{"who_more", "who_more"});
				o.add(new String[]{"back", "back"});
			}
			default -> o.add(new String[]{"back", "back"});
		}
		return o;
	}

	private void show(String next) {
		node = next;
		clearChildren();
		lines = textRenderer.wrapLines(Text.translatable("dialog.amethystmod.ricardo." + next), W - 110);
		shown = 0;
		typed = 0;
		List<String[]> answers = options(next);
		int bw = (W - 110) / 2 - 4;
		for (int i = 0; i < answers.size(); i++) {
			String[] a = answers.get(i);
			int bx = left + 100 + (i % 2) * (bw + 6);
			int by = top + H - 52 + (i / 2) * 22;
			addDrawableChild(ButtonWidget.builder(Text.translatable("dialog.amethystmod.ricardo.opt." + a[0]), b -> choose(a[1]))
					.dimensions(bx, by, bw, 20).build());
		}
	}

	private void choose(String target) {
		if (target.equals("!bye")) {
			close();
		} else if (target.equals("!trade")) {
			ClientPlayNetworking.send(new ModNetworking.RicardoAction(entityId, "trade"));
			close();
		} else if (target.equals("!gift")) {
			ClientPlayNetworking.send(new ModNetworking.RicardoAction(entityId, "gift"));
			show("gift");
		} else {
			show(target);
		}
	}

	@Override
	public void tick() {
		// the words appear a few letters at a time
		typed += 2.2f;
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		context.fillGradient(0, 0, width, height, 0x60000000, 0xA0100820);
		context.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFFB57BFF);
		context.fill(left, top, left + W, top + H, 0xF0160B2B);
		context.fill(left + 4, top + 4, left + 92, top + H - 4, 0xFF0C0618);
		Entity entity = client.world == null ? null : client.world.getEntityById(entityId);
		if (entity instanceof LivingEntity living) {
			InventoryScreen.drawEntity(context, left + 4, top + 4, left + 92, top + H - 4, 60, 0.0625f, mouseX, mouseY, living);
		}
		context.drawText(textRenderer, title.copy().formatted(Formatting.GOLD, Formatting.BOLD), left + 100, top + 8, 0xFFFFFF, true);
		// the text, typed out
		int budget = (int) typed;
		int y = top + 22;
		for (OrderedText line : lines) {
			StringBuilder sb = new StringBuilder();
			line.accept((index, style, codePoint) -> {
				sb.appendCodePoint(codePoint);
				return true;
			});
			String s = sb.toString();
			if (budget <= 0) {
				break;
			}
			String part = s.substring(0, Math.min(s.length(), budget));
			budget -= s.length();
			context.drawText(textRenderer, Text.literal(part), left + 100, y, 0xE8D8FF, false);
			y += 10;
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		// a click on the text shows it all at once
		if (mouseY < top + H - 54 && mouseX > left + 96) {
			typed = 10000;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
