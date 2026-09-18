package dev.veyra.client.clickgui;

import java.util.ArrayDeque;
import java.util.Deque;

/** Nested scissor model that always restores the exact parent clip. */
public final class UiClipStack {
    private final Deque<Region> regions = new ArrayDeque<>();

    public Region push(Region requested) {
        Region actual = regions.isEmpty() ? requested.normalized()
                : regions.peek().intersection(requested);
        regions.push(actual);
        return actual;
    }

    public Region pop() {
        if (!regions.isEmpty()) regions.pop();
        return regions.peek();
    }

    public boolean isEmpty() {
        return regions.isEmpty();
    }

    public void clear() {
        regions.clear();
    }

    public record Region(int x, int y, int width, int height) {
        Region normalized() {
            return new Region(x, y, Math.max(0, width), Math.max(0, height));
        }

        Region intersection(Region other) {
            int left = Math.max(x, other.x);
            int top = Math.max(y, other.y);
            int right = Math.min(x + width, other.x + Math.max(0, other.width));
            int bottom = Math.min(y + height, other.y + Math.max(0, other.height));
            if (right <= left || bottom <= top) {
                return new Region(x + width, y + height, 0, 0);
            }
            return new Region(left, top, right - left, bottom - top);
        }
    }
}
