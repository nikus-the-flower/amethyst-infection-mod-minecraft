package com.amethystmod.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/** Reads a piece of the world's story on an old yellowed page, a few lines per page. */
public class LoreScreen extends Screen {
	private static final int W = 230;
	private static final int H = 200;
	private static final int LINES = 15;
	private final String id;
	private List<List<OrderedText>> pages = List.of();
	private int page;
	private int left;
	private int top;
	private ButtonWidget prev;
	private ButtonWidget next;

	public LoreScreen(String id) {
		super(Text.translatable("lore.amethystmod." + id + ".title"));
		this.id = id;
	}

	public static void open(String id) {
		net.minecraft.client.MinecraftClient.getInstance().setScreen(new LoreScreen(id));
	}

	@Override
	protected void init() {
		left = (width - W) / 2;
		top = (height - H) / 2;
		// the text, wrapped and cut into pages; "\n\n\n" starts a new page on purpose
		pages = new ArrayList<>();
		String raw = Text.translatable("lore.amethystmod." + id + ".text").getString();
		for (String part : raw.split("\n\n\n")) {
			List<OrderedText> lines = new ArrayList<>();
			for (String para : part.split("\n")) {
				lines.addAll(textRenderer.wrapLines(Text.literal(para), W - 30));
				lines.add(OrderedText.EMPTY);
			}
			for (int i = 0; i < lines.size(); i += LINES) {
				pages.add(lines.subList(i, Math.min(lines.size(), i + LINES)));
			}
		}
		prev = addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> turn(-1)).dimensions(left + 10, top + H - 26, 20, 20).build());
		next = addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> turn(1)).dimensions(left + W - 30, top + H - 26, 20, 20).build());
		turn(0);
	}

	private void turn(int by) {
		page = Math.max(0, Math.min(pages.size() - 1, page + by));
		prev.visible = page > 0;
		next.visible = page < pages.size() - 1;
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		super.renderBackground(context, mouseX, mouseY, delta);
		// an old page: dark edge, yellowed paper, a darker band for the title
		context.fill(left - 3, top - 3, left + W + 3, top + H + 3, 0xFF3B2A17);
		context.fill(left, top, left + W, top + H, 0xFFE8D6AE);
		context.fill(left + 4, top + 4, left + W - 4, top + H - 4, 0xFFF1E2BE);
		context.fill(left + 10, top + 24, left + W - 10, top + 25, 0xFF9C7A4A);
		context.drawCenteredTextWithShadow(textRenderer, title.copy().formatted(Formatting.BOLD), left + W / 2, top + 10, 0xFF5A2E0E);
		if (!pages.isEmpty()) {
			int y = top + 32;
			for (OrderedText line : pages.get(page)) {
				context.drawText(textRenderer, line, left + 15, y, 0xFF3A2410, false);
				y += 10;
			}
			if (pages.size() > 1) {
				context.drawCenteredTextWithShadow(textRenderer, Text.literal((page + 1) + " / " + pages.size()), left + W / 2, top + H - 20, 0xFF8A6A40);
			}
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
