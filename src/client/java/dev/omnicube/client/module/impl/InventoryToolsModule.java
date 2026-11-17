package dev.omnicube.client.module.impl;

import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;

public final class InventoryToolsModule extends Module {
	public InventoryToolsModule() {
		super("inventory_tools", "Inventory Tools", "Search your inventory and quick-move the selected stack with vanilla shift-click behavior.", ModuleCategory.MISC, false);
	}
}
