package dev.veyra.client.utils.asm;

import net.weavemc.api.event.EventBus;
import net.weavemc.api.event.MouseEvent;
import org.lwjgl.input.Mouse;

import java.nio.ByteBuffer;

/**
 * @author sassan
 * 23.11.2023, 2023
 */
public class HookUtils {
    public static void setMouseButtonState(int mouseButton, boolean held) {
        MouseEvent m = new MouseEvent();
        ReflectionUtils.setPrivateValue(MouseEvent.class, m, mouseButton, "button");
        ReflectionUtils.setPrivateValue(MouseEvent.class, m, held, "buttonState");
        EventBus.postEvent(m);

        ByteBuffer buttons = (ByteBuffer) ReflectionUtils.getPrivateValue(Mouse.class, null, "buttons");
        if (buttons == null) {
            System.out.println("buttons is null, something is wrong");
            return;
        }
        buttons.put(mouseButton, (byte) (held ? 1 : 0));
        ReflectionUtils.setPrivateValue(Mouse.class, null, buttons, "buttons");
    }
}
