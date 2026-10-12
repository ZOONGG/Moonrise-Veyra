package dev.veyra.weave.mixins;

import net.weavemc.api.Hook;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** Early hook avoids resolving Minecraft classes during mixin preparation. */
public final class ExperimentalTimerFreezeMixin extends Hook {
    private static java.lang.instrument.Instrumentation runtimeInstrumentation;
    private static String connectionResult;
    private static boolean correctionConnected;

    public static void connectCorrectionClass(Class<?> actual, Class<?> timer) {
        if (correctionConnected || runtimeInstrumentation == null) return;
        correctionConnected = true;
        try {
            runtimeInstrumentation.addTransformer(new java.lang.instrument.ClassFileTransformer() {
                @Override public byte[] transform(ClassLoader loader, String name, Class<?> redefining,
                        java.security.ProtectionDomain domain, byte[] bytes) {
                    if (redefining != actual) return null;
                    ClassNode node = new ClassNode();
                    new ClassReader(bytes).accept(node, 0);
                    MethodNode handler = node.methods.stream()
                            .filter(method -> method.name.equals("handlePlayerPosLook"))
                            .findFirst().orElseThrow(() -> new IllegalStateException("Position correction handler missing"));
                    handler.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC,
                            timer.getName().replace('.', '/'), "serverCorrection", "()V", false));
                    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                    node.accept(writer);
                    return writer.toByteArray();
                }
            }, true);
            runtimeInstrumentation.retransformClasses(actual);
        } catch (Exception error) {
            System.err.println("Freeze correction hook: " + error);
        }
    }

    public static String connectActualClass(Class<?> actual, Class<?> timer) {
        if (connectionResult != null) return connectionResult;
        try {
            if (runtimeInstrumentation == null) {
                Class<?> loader = Class.forName("net.weavemc.loader.impl.WeaveLoader");
                Object instance = loader.getMethod("getInstance").invoke(null);
                java.lang.reflect.Field field = loader.getDeclaredField("instrumentation");
                field.setAccessible(true);
                runtimeInstrumentation = (java.lang.instrument.Instrumentation) field.get(instance);
            }
            if (!runtimeInstrumentation.isRetransformClassesSupported()
                    || !runtimeInstrumentation.isModifiableClass(actual)) {
                return connectionResult = "Cannot retransform " + actual.getName();
            }
            runtimeInstrumentation.addTransformer(new java.lang.instrument.ClassFileTransformer() {
                @Override
                public byte[] transform(ClassLoader loader, String name, Class<?> redefining,
                        java.security.ProtectionDomain domain, byte[] bytes) {
                    if (redefining != actual) return null;
                    try {
                        ClassNode node = new ClassNode();
                        new ClassReader(bytes).accept(node, ClassReader.EXPAND_FRAMES);
                        new ExperimentalTimerFreezeMixin().transform(node, () -> {});
                        // Use the actual loaded Timer owner, without guessing a namespace.
                        for (MethodNode method : node.methods) for (AbstractInsnNode insn : method.instructions) {
                            if (insn instanceof MethodInsnNode call && call.name.equals("freezeMovement")) {
                                call.owner = timer.getName().replace('.', '/');
                                call.desc = "(Ljava/lang/Object;)Z";
                            }
                        }
                        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                        node.accept(writer);
                        connectionResult = "Attached to " + actual.getName();
                        return writer.toByteArray();
                    } catch (Throwable error) {
                        connectionResult = "Attach failed: " + error;
                        return null;
                    }
                }
            }, true);
            runtimeInstrumentation.retransformClasses(actual);
            if (connectionResult == null) connectionResult = "Transformer not called: " + actual.getName();
        } catch (Throwable error) {
            connectionResult = "Attach failed: " + error;
        }
        return connectionResult;
    }
    public ExperimentalTimerFreezeMixin() {
        super("net/minecraft/entity/Entity");
    }
    public static void installNamedFallback(java.lang.instrument.Instrumentation instrumentation) {
        runtimeInstrumentation = instrumentation;
        boolean capable = instrumentation.isRetransformClassesSupported();
        instrumentation.addTransformer(new java.lang.instrument.ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String name, Class<?> redefining,
                    java.security.ProtectionDomain domain, byte[] bytes) {
                if (!"net/minecraft/entity/Entity".equals(name)) return null;
                try {
                    ClassNode node = new ClassNode();
                    new ClassReader(bytes).accept(node, ClassReader.EXPAND_FRAMES);
                    new ExperimentalTimerFreezeMixin().transform(node, () -> {});
                    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
                        @Override
                        protected String getCommonSuperClass(String left, String right) {
                            try {
                                Class<?> a = Class.forName(left.replace('/', '.'), false, loader);
                                Class<?> b = Class.forName(right.replace('/', '.'), false, loader);
                                if (a.isAssignableFrom(b)) return left;
                                if (b.isAssignableFrom(a)) return right;
                                if (a.isInterface() || b.isInterface()) return "java/lang/Object";
                                do { a = a.getSuperclass(); } while (!a.isAssignableFrom(b));
                                return a.getName().replace('.', '/');
                            } catch (ClassNotFoundException error) {
                                throw new IllegalStateException(error);
                            }
                        }
                    };
                    node.accept(writer);
                    return writer.toByteArray();
                } catch (Throwable error) {
                    return null;
                }
            }
        }, capable);
        for (Class<?> loaded : instrumentation.getAllLoadedClasses()) {
            if (!loaded.getName().equals("net.minecraft.entity.Entity")) continue;
            if (capable && instrumentation.isModifiableClass(loaded)) {
                try { instrumentation.retransformClasses(loaded); }
                catch (Exception error) { System.err.println("Freeze hook: " + error); }
            }
        }
    }
    @Override
    public void transform(ClassNode node, AssemblerConfig config) {
        MethodNode movement = node.methods.stream()
                .filter(method -> method.name.equals("moveEntity") && method.desc.equals("(DDD)V"))
                .findFirst().orElseThrow(() -> new IllegalStateException("Freeze: moveEntity not found"));
        for (AbstractInsnNode instruction : movement.instructions) {
            if (instruction instanceof MethodInsnNode call && call.name.equals("freezeMovement")
                    && call.owner.equals("dev/veyra/client/module/modules/movement/Timer")) return;
        }
        LabelNode normal = new LabelNode();
        InsnList guard = new InsnList();
        guard.add(new VarInsnNode(Opcodes.ALOAD, 0));
        guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                "dev/veyra/client/module/modules/movement/Timer", "freezeMovement",
                "(Ljava/lang/Object;)Z", false));
        guard.add(new JumpInsnNode(Opcodes.IFEQ, normal));
        guard.add(new InsnNode(Opcodes.RETURN));
        guard.add(normal);
        guard.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        movement.instructions.insert(guard);
        config.computeFrames();
    }
}
