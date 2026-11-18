package dev.omnicube.client.module.impl;

import dev.omnicube.client.module.Module;
import dev.omnicube.client.module.ModuleCategory;
import dev.omnicube.client.setting.Setting;

/**
 * Watches FPS and disables the optional heavy features once per world. It does
 * not change video settings itself; the Performance module owns those.
 */
public final class FpsSafetyModule extends Module {
	private final Setting.Num floor = add(new Setting.Num("fps_floor", "FPS floor", 30.0D, 10.0D, 60.0D, 1.0D));

	public FpsSafetyModule() {
		super("fps_safety", "FPS Safety", "If FPS stays below the floor for five seconds, disables optional heavy features and says so.", ModuleCategory.PERFORMANCE, true);
	}

	public int floor() {
		return floor.intValue();
	}
}
