package dev.omnicube.client.mixin;

import dev.omnicube.client.OmniCubeClient;
import dev.omnicube.client.module.impl.ChatCollapseModule;
import dev.omnicube.client.perf.PerfMonitor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Vanilla has no public API for replacing the last chat line, so this feature
 * needs a tiny injection. It is HEAD-only and cancels the add after editing the
 * existing GuiMessage; it never overwrites a vanilla method.
 */
@Mixin(value = ChatComponent.class, priority = 1500)
public abstract class ChatComponentMixin {
	@Shadow
	private List<GuiMessage> allMessages;

	@Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
			at = @At("HEAD"), cancellable = true, require = 0)
	private void omnicube$collapseRepeatedMessage(Component message, MessageSignature signature, GuiMessageTag tag,
			CallbackInfo callback) {
		long started = PerfMonitor.start();
		try {
			ChatCollapseModule module = OmniCubeClient.chatCollapse();
			if (module == null || message == null) {
				return;
			}
			int count = module.collapseCount(message);
			if (count <= 1 || allMessages == null || allMessages.isEmpty()) {
				return;
			}
			// Vanilla stores newest first: allMessages.addFirst(...).
			GuiMessage previous = allMessages.getFirst();
			Component content = message.copy().append(" (" + count + ")");
			allMessages.set(0,
					new GuiMessage(previous.addedTime(), content, previous.signature(), previous.tag()));
			((ChatComponentAccessor) (Object) this).omnicube$refreshTrimmedMessages();
			callback.cancel();
		} catch (Throwable throwable) {
			ChatCollapseModule module = OmniCubeClient.chatCollapse();
			if (module != null) {
				module.fail("replace last line", throwable);
			}
		} finally {
			PerfMonitor.record("chat.collapse", started);
		}
	}
}
