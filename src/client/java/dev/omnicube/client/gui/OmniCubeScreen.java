package dev.omnicube.client.gui;

import dev.omnicube.client.OmniCubeClient;
import dev.omnicube.client.compat.ClientCompat;
import dev.omnicube.client.diag.Diagnostics;
import dev.omnicube.client.feature.Feature;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.util.OmniCubeText;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class OmniCubeScreen extends Screen {
	private static final int SIDEBAR_WIDTH = 108;
	private static final int ROW_HEIGHT = 24;
	private static final int ROW_GAP = 2;
	private static final int HEADER_HEIGHT = 46;
	private static final int TAB_FEATURES = 0;
	private static final int TAB_DIAGNOSTICS = 1;

	/** Read once from the loaded mod metadata; never a hard-coded release number. */
	private static final String MOD_VERSION = readModVersion();

	private final Screen parent;
	private String query = "";
	private ModuleCategory category;
	private int scroll;
	private int tab = TAB_FEATURES;
	private EditBox search;
	private List<Feature> visible = new ArrayList<>();
	private List<Diagnostics.FeatureRecord> diagnostics = new ArrayList<>();
	private int keybindConflicts;

	private Button openLogButton;
	private Button copyReportButton;
	private Button refreshDiagnosticsButton;
	/** Feature-tab footer buttons, hidden while the Diagnostics tab is open. */
	private final List<Button> featureOnlyButtons = new ArrayList<>();

	public OmniCubeScreen(Screen parent) {
		super(Component.literal("OmniCube"));
		this.parent = parent;
	}

	private static String readModVersion() {
		try {
			return FabricLoader.getInstance().getModContainer("omnicube")
					.map(container -> container.getMetadata().getVersion().getFriendlyString())
					.orElse("dev");
		} catch (Throwable throwable) {
			// A missing container must not stop the screen from opening.
			return "dev";
		}
	}

	@Override
	protected void init() {
		int listX = listX();
		int listW = listWidth();

		search = new EditBox(this.font, listX, 24, listW, 18, Component.literal("Search"));
		search.setValue(query);
		search.setResponder(value -> {
			query = value == null ? "" : value;
			scroll = 0;
			refresh();
		});
		addRenderableWidget(search);

		int footerY = this.height - 27;
		int doneWidth = Math.min(60, Math.max(30, this.width / 5));
		int doneX = this.width - doneWidth - 10;
		int buttonWidth = Math.max(24, Math.min(96, (doneX - 18) / 5 - 4));
		int buttonX = 10;

		featureOnlyButtons.add(addRenderableWidget(Button.builder(Component.literal("HUD"), button -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(new HudEditorScreen(this));
			}
		}).bounds(buttonX, footerY, buttonWidth, 20).build()));
		buttonX += buttonWidth + 4;

		featureOnlyButtons.add(addRenderableWidget(Button.builder(Component.literal("Macros"), button -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(new MacroScreen(this));
			}
		}).bounds(buttonX, footerY, buttonWidth, 20).build()));
		buttonX += buttonWidth + 4;

		featureOnlyButtons.add(addRenderableWidget(Button.builder(Component.literal("Compat"), button -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(new CompatibilityScreen(this));
			}
		}).bounds(buttonX, footerY, buttonWidth, 20).build()));
		buttonX += buttonWidth + 4;

		Button profilesButton = addRenderableWidget(Button.builder(Component.literal("Profiles"), button -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(new ProfilesScreen(this));
			}
		}).bounds(buttonX, footerY, buttonWidth, 20).build());
		Feature profilesFeature = OmniCubeClient.featureById("settings_profiles");
		profilesButton.active = profilesFeature == null || (profilesFeature.isAvailable() && profilesFeature.isEnabled());
		featureOnlyButtons.add(profilesButton);
		buttonX += buttonWidth + 4;

		featureOnlyButtons.add(addRenderableWidget(Button.builder(Component.literal("Reload"), button -> {
			OmniCubeClient.reloadConfig();
			refresh();
			refreshDiagnostics();
		}).bounds(buttonX, footerY, buttonWidth, 20).build()));

		addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
				.bounds(doneX, footerY, doneWidth, 20).build());

		// Diagnostics-only actions. They share the footer space with the feature actions
		// and are shown only on the Diagnostics tab.
		int logWidth = Math.max(60, Math.min(110, (doneX - 20) / 3 - 4));
		openLogButton = addRenderableWidget(Button.builder(Component.literal("Open log folder"), button -> {
			try {
				ClientCompat.openFile(Diagnostics.logDirectory().toFile());
			} catch (Throwable throwable) {
				Diagnostics.exception("gui.open_log", throwable);
				OmniCubeText.sendError("Could not open the log folder.");
			}
		}).bounds(10, footerY, logWidth, 20).build());
		copyReportButton = addRenderableWidget(Button.builder(Component.literal("Copy report"), button -> {
			OmniCubeText.copyToClipboard(Diagnostics.report());
			OmniCubeText.send("Diagnostics report copied.");
		}).bounds(14 + logWidth, footerY, logWidth, 20).build());
		refreshDiagnosticsButton = addRenderableWidget(Button.builder(Component.literal("Refresh"), button -> {
			refreshDiagnostics();
			refresh();
		}).bounds(18 + logWidth * 2, footerY, Math.max(40, Math.min(80, doneX - (22 + logWidth * 2))), 20).build());

		refresh();
		refreshDiagnostics();
		updateTabVisibility();
		keybindConflicts = dev.omnicube.client.keybind.KeybindConflicts.find().size();
	}

	private void updateTabVisibility() {
		boolean features = tab == TAB_FEATURES;
		if (search != null) {
			search.visible = features;
			search.setFocused(features && search.isFocused());
		}
		for (Button button : featureOnlyButtons) {
			if (button != null) {
				button.visible = features;
			}
		}
		if (openLogButton != null) {
			openLogButton.visible = !features;
		}
		if (copyReportButton != null) {
			copyReportButton.visible = !features;
		}
		if (refreshDiagnosticsButton != null) {
			refreshDiagnosticsButton.visible = !features;
		}
	}

	private void refresh() {
		List<Feature> all = OmniCubeClient.features();
		String needle = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
		List<Feature> filtered = new ArrayList<>();
		for (Feature feature : all) {
			if (category != null && feature.category() != category) {
				continue;
			}
			if (!needle.isEmpty()) {
				String haystack = (feature.name() + " " + feature.id() + " " + feature.description()).toLowerCase(Locale.ROOT);
				if (!haystack.contains(needle)) {
					continue;
				}
			}
			filtered.add(feature);
		}
		visible = filtered;
		scroll = clampScroll(scroll);
	}

	private void refreshDiagnostics() {
		diagnostics = new ArrayList<>(Diagnostics.features());
		scroll = clampScroll(scroll);
	}

	private int sidebarWidth() {
		return Math.max(84, Math.min(SIDEBAR_WIDTH, this.width / 4));
	}

	private int listX() {
		return sidebarWidth() + 10;
	}

	private int listWidth() {
		return Math.max(40, this.width - listX() - 10);
	}

	private int listTop() {
		return HEADER_HEIGHT;
	}

	private int listBottom() {
		return this.height - 34;
	}

	private int listHeight() {
		return Math.max(1, listBottom() - listTop());
	}

	private int contentHeight() {
		if (tab == TAB_DIAGNOSTICS) {
			return diagnostics.size() * 22 + 26 + Diagnostics.notes().size() * 11;
		}
		return visible.size() * (ROW_HEIGHT + ROW_GAP);
	}

	private int clampScroll(int value) {
		return Math.max(0, Math.min(Math.max(0, contentHeight() - listHeight()), value));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int accent = OmniCubeClient.config().accentArgb();
		int sidebar = sidebarWidth();
		graphics.fill(0, 0, sidebar, this.height, 0xCC0B0F0B);
		graphics.fill(sidebar, 0, sidebar + 1, this.height, 0x557ED957);
		graphics.fill(sidebar + 1, HEADER_HEIGHT - 1, this.width, HEADER_HEIGHT, 0x33FFFFFF);

		graphics.drawString(this.font, "OMNICUBE", 12, 13, accent, true);
		graphics.drawString(this.font, "Minecraft 1.21.11", 12, 24, 0xFF8A8A8A, false);
		graphics.drawString(this.font, "v" + MOD_VERSION, 12, 34, 0xFF6F6F6F, false);

		drawCategory(graphics, "All", null, 54, mouseX, mouseY);
		int y = 54 + 19;
		for (ModuleCategory value : ModuleCategory.values()) {
			drawCategory(graphics, value.label(), value, y, mouseX, mouseY);
			y += 19;
		}

		drawTabs(graphics, mouseX, mouseY, accent);

		int listX = listX();
		int listW = listWidth();
		int top = listTop();
		int bottom = listBottom();
		graphics.fill(listX - 2, top - 2, listX + listW + 2, bottom + 2, 0x88101010);

		graphics.enableScissor(listX, top, listX + listW, bottom);
		try {
			if (tab == TAB_FEATURES) {
				renderFeatureRows(graphics, listX, listW, top, bottom, mouseX, mouseY, accent);
			} else {
				renderDiagnosticsRows(graphics, listX, listW, top, bottom);
			}
		} catch (Throwable throwable) {
			// A single malformed row must not leave the scissor enabled and blank the rest
			// of the screen, which is what made the list look empty before.
			Diagnostics.exception("gui.list_render", throwable);
		} finally {
			graphics.disableScissor();
		}

		if (contentHeight() > listHeight()) {
			int trackHeight = listHeight();
			int thumbHeight = Math.max(16, (int) ((long) trackHeight * trackHeight / Math.max(1, contentHeight())));
			int thumbY = top + (int) ((long) (trackHeight - thumbHeight) * scroll / Math.max(1, contentHeight() - listHeight()));
			graphics.fill(listX + listW - 3, top, listX + listW, bottom, 0x33FFFFFF);
			graphics.fill(listX + listW - 3, thumbY, listX + listW, thumbY + thumbHeight, accent);
		}

		super.render(graphics, mouseX, mouseY, partialTick);

		if (tab == TAB_FEATURES) {
			Feature hovered = featureAt(mouseX, mouseY);
			if (hovered != null) {
				String tooltip = hovered.description();
				String reason = hovered.stateReason();
				// Show the description first. Only a real problem (blocked, failed or
				// auto-disabled) replaces it; the ordinary "Enabled (default)" reason used
				// to hide every description in the list.
				if (!hovered.isAvailable()) {
					tooltip = hovered.unavailableReason();
				} else if (reason != null && (reason.startsWith("Failed") || reason.startsWith("Hidden"))) {
					tooltip = reason;
				}
				graphics.setTooltipForNextFrame(this.font, Component.literal(tooltip), mouseX, mouseY);
			}
		}

		if (keybindConflicts > 0) {
			graphics.drawString(this.font, "! " + keybindConflicts + " keybind conflict(s) - see Compatibility",
					10, this.height - 40, 0xFFFFAA55, false);
		}
	}

	private void renderFeatureRows(GuiGraphics graphics, int listX, int listW, int top, int bottom,
			int mouseX, int mouseY, int accent) {
		int rowY = top - scroll;
		for (Feature feature : visible) {
			if (rowY + ROW_HEIGHT >= top - ROW_HEIGHT && rowY <= bottom + ROW_HEIGHT) {
				try {
					drawFeatureRow(graphics, feature, listX, rowY, listW, mouseX, mouseY, accent);
				} catch (Throwable throwable) {
					Diagnostics.exception("gui.feature_row." + feature.id(), throwable);
				}
			}
			rowY += ROW_HEIGHT + ROW_GAP;
		}
		if (visible.isEmpty()) {
			graphics.drawString(this.font, "No feature matches this filter.", listX + 8, top + 8, 0xFFAAAAAA, false);
		}
	}

	private void renderDiagnosticsRows(GuiGraphics graphics, int listX, int listW, int top, int bottom) {
		int y = top - scroll;
		graphics.drawString(this.font, "Diagnostics: " + diagnostics.size() + " features, "
				+ Diagnostics.exceptionCount() + " exception(s)", listX + 4, y, 0xFFFFFFFF, true);
		graphics.drawString(this.font, "Log: " + Diagnostics.logPath(), listX + 4, y + 11, 0xFF888888, false);
		y += 22;
		// Named status lines (Fullbright mode and hook state, for example).
		for (String note : Diagnostics.notes()) {
			if (y + 10 >= top && y <= bottom) {
				graphics.drawString(this.font, this.font.plainSubstrByWidth(note, listW - 8), listX + 4, y, 0xFF7ED957,
						false);
			}
			y += 11;
		}
		y += 4;
		if (diagnostics.isEmpty()) {
			graphics.drawString(this.font, "No feature status recorded.", listX + 4, y, 0xFFAAAAAA, false);
			return;
		}
		for (Diagnostics.FeatureRecord record : diagnostics) {
			if (y + 20 >= top && y <= bottom) {
				int color = switch (record.state()) {
					case "ENABLED" -> 0xFF7ED957;
					case "BLOCKED" -> 0xFFFFAA55;
					case "FAILED" -> 0xFFFF6666;
					default -> 0xFF9A9A9A;
				};
				String line = record.name() + "  [" + record.state() + "]";
				graphics.drawString(this.font, this.font.plainSubstrByWidth(line, listW - 8), listX + 4, y, color, false);
				String reason = record.reason().isBlank() ? "-" : record.reason();
				graphics.drawString(this.font, this.font.plainSubstrByWidth(reason, listW - 8), listX + 8, y + 9,
						0xFFB0B0B0, false);
			}
			y += 22;
		}
	}

	private void drawTabs(GuiGraphics graphics, int mouseX, int mouseY, int accent) {
		int x = listX();
		int featuresWidth = 62;
		int diagWidth = 78;
		boolean featureHover = mouseX >= x && mouseX < x + featuresWidth && mouseY >= 6 && mouseY <= 20;
		boolean diagHover = mouseX >= x + featuresWidth + 4 && mouseX < x + featuresWidth + 4 + diagWidth
				&& mouseY >= 6 && mouseY <= 20;
		graphics.fill(x, 6, x + featuresWidth, 20,
				tab == TAB_FEATURES ? 0x447ED957 : featureHover ? 0x22FFFFFF : 0x00000000);
		graphics.fill(x + featuresWidth + 4, 6, x + featuresWidth + 4 + diagWidth, 20,
				tab == TAB_DIAGNOSTICS ? 0x447ED957 : diagHover ? 0x22FFFFFF : 0x00000000);
		graphics.drawString(this.font, "Features", x + 6, 9, tab == TAB_FEATURES ? 0xFFFFFFFF : 0xFFB0B0B0, false);
		graphics.drawString(this.font, "Diagnostics", x + featuresWidth + 10, 9,
				tab == TAB_DIAGNOSTICS ? 0xFFFFFFFF : 0xFFB0B0B0, false);

		String count = tab == TAB_FEATURES
				? visible.size() + " features"
				: Diagnostics.failedFeatures() + " failed";
		graphics.drawString(this.font, count, this.width - 10 - this.font.width(count), 9,
				tab == TAB_FEATURES ? 0xFFEDEDED : 0xFFFFAA55, false);
	}

	private void drawCategory(GuiGraphics graphics, String label, ModuleCategory value, int y, int mouseX, int mouseY) {
		boolean selected = tab == TAB_FEATURES && category == value;
		int sidebar = sidebarWidth();
		boolean hovered = mouseX >= 4 && mouseX <= sidebar - 4 && mouseY >= y && mouseY <= y + 17;
		int background = selected ? 0x447ED957 : hovered ? 0x22FFFFFF : 0x00000000;
		if (background != 0) {
			graphics.fill(4, y, sidebar - 4, y + 17, background);
		}
		if (selected) {
			graphics.fill(4, y, 6, y + 17, OmniCubeClient.config().accentArgb());
		}
		graphics.drawString(this.font, label, 10, y + 5, selected ? 0xFFFFFFFF : 0xFFB0B0B0, false);
	}

	private void drawFeatureRow(GuiGraphics graphics, Feature feature, int x, int y, int width, int mouseX, int mouseY, int accent) {
		boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + ROW_HEIGHT;
		boolean enabled = feature.isEnabled();
		boolean available = feature.isAvailable();
		graphics.fill(x, y, x + width, y + ROW_HEIGHT, hovered ? 0x33FFFFFF : 0x22101010);
		if (enabled && available) {
			graphics.fill(x, y, x + 3, y + ROW_HEIGHT, accent);
		}

		int textWidth = Math.max(20, width - (feature.settings().isEmpty() || width < 180 ? 48 : 128));
		String name = this.font.plainSubstrByWidth(feature.name(), textWidth);
		int nameColor = !available ? 0xFF666666 : enabled ? 0xFFF2F2F2 : 0xFF9A9A9A;
		graphics.drawString(this.font, name, x + 9, y + 4, nameColor, false);
		String subtitle = this.font.plainSubstrByWidth(feature.category().label() + " • " + feature.id(), textWidth);
		graphics.drawString(this.font, subtitle, x + 9, y + 14, available ? 0xFF777777 : 0xFF555555, false);

		int stateX = x + width - 28;
		String state = !available ? "N/A" : enabled ? "ON" : "OFF";
		graphics.drawString(this.font, state, stateX, y + 8,
				!available ? 0xFF777777 : enabled ? accent : 0xFF777777, true);

		if (!feature.settings().isEmpty() && width >= 180 && available) {
			graphics.drawString(this.font, "SETTINGS", x + width - 118, y + 8, hovered ? 0xFFFFFFFF : 0xFF8A8A8A, false);
		}
	}

	private boolean settingsHit(double mouseX, double mouseY, int x, int y, int width) {
		return width >= 180 && mouseX >= x + width - 120 && mouseX <= x + width - 54 && mouseY >= y && mouseY < y + ROW_HEIGHT;
	}

	private Feature featureAt(double mouseX, double mouseY) {
		if (tab != TAB_FEATURES) {
			return null;
		}
		int listX = listX();
		int listW = listWidth();
		if (mouseX < listX || mouseX >= listX + listW || mouseY < listTop() || mouseY >= listBottom()) {
			return null;
		}
		int index = (int) ((mouseY - listTop() + scroll) / (ROW_HEIGHT + ROW_GAP));
		if (index < 0 || index >= visible.size()) {
			return null;
		}
		int rowY = listTop() - scroll + index * (ROW_HEIGHT + ROW_GAP);
		if (mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
			return visible.get(index);
		}
		return null;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		if (event.button() != 0) {
			return false;
		}

		double mouseX = event.x();
		double mouseY = event.y();

		if (mouseY >= 6 && mouseY <= 20) {
			int x = listX();
			if (mouseX >= x && mouseX < x + 62) {
				setTab(TAB_FEATURES);
				return true;
			}
			if (mouseX >= x + 66 && mouseX < x + 66 + 78) {
				setTab(TAB_DIAGNOSTICS);
				return true;
			}
		}

		if (mouseX >= 4 && mouseX <= sidebarWidth() - 4) {
			if (mouseY >= 54 && mouseY < 71) {
				category = null;
				scroll = 0;
				refresh();
				return true;
			}
			int y = 73;
			for (ModuleCategory value : ModuleCategory.values()) {
				if (mouseY >= y && mouseY < y + 17) {
					category = value;
					scroll = 0;
					refresh();
					return true;
				}
				y += 19;
			}
		}

		Feature feature = featureAt(mouseX, mouseY);
		if (feature == null) {
			return false;
		}

		int listX = listX();
		int listW = listWidth();
		int index = visible.indexOf(feature);
		int rowY = listTop() - scroll + index * (ROW_HEIGHT + ROW_GAP);

		if (!feature.isAvailable()) {
			OmniCubeText.sendError(feature.unavailableReason());
			return true;
		}

		if (!feature.settings().isEmpty() && settingsHit(mouseX, mouseY, listX, rowY, listW)) {
			if (this.minecraft != null) {
				this.minecraft.setScreen(new ModuleSettingsScreen(this, feature));
			}
			return true;
		}

		feature.setEnabled(!feature.isEnabled());
		OmniCubeClient.saveConfig();
		return true;
	}

	private void setTab(int newTab) {
		if (tab == newTab) {
			return;
		}
		tab = newTab;
		scroll = 0;
		if (newTab == TAB_DIAGNOSTICS) {
			refreshDiagnostics();
		}
		updateTabVisibility();
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
			return true;
		}
		if (mouseX >= listX() && mouseY >= listTop() && mouseY <= listBottom()) {
			scroll = clampScroll(scroll - (int) Math.round(verticalAmount * 18.0D));
			return true;
		}
		return false;
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) {
			this.minecraft.setScreen(parent);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
