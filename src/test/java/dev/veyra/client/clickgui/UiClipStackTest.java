package dev.veyra.client.clickgui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiClipStackTest {
    @Test
    void nestedClipIsIntersectedAndParentIsRestored() {
        UiClipStack stack = new UiClipStack();
        UiClipStack.Region parent = stack.push(new UiClipStack.Region(10, 10, 100, 80));
        UiClipStack.Region nested = stack.push(new UiClipStack.Region(0, 30, 40, 100));

        assertEquals(new UiClipStack.Region(10, 30, 30, 60), nested);
        assertEquals(parent, stack.pop());
        assertFalse(stack.isEmpty());
        stack.pop();
        assertTrue(stack.isEmpty());
    }

    @Test
    void disjointNestedClipBecomesEmptyWithoutLosingParent() {
        UiClipStack stack = new UiClipStack();
        UiClipStack.Region parent = stack.push(new UiClipStack.Region(20, 20, 30, 30));

        assertEquals(new UiClipStack.Region(50, 50, 0, 0),
                stack.push(new UiClipStack.Region(80, 80, 10, 10)));
        assertEquals(parent, stack.pop());
    }
}
