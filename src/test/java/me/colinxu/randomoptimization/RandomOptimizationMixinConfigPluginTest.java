package me.colinxu.randomoptimization;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomOptimizationMixinConfigPluginTest {
    @Test
    void suppressesModernFixOnlyWhenReplacementIsActive() {
        assertTrue(RandomOptimizationMixinConfigPlugin
                .shouldDisableModernFixPackOptimization(true, true));
        assertFalse(RandomOptimizationMixinConfigPlugin
                .shouldDisableModernFixPackOptimization(false, true));
        assertFalse(RandomOptimizationMixinConfigPlugin
                .shouldDisableModernFixPackOptimization(true, false));
        assertFalse(RandomOptimizationMixinConfigPlugin
                .shouldDisableModernFixPackOptimization(false, false));
    }

    @Test
    void suppressesQuickPackOnlyWhenReplacementIsActive() {
        assertTrue(RandomOptimizationMixinConfigPlugin
                .shouldDisableQuickPackOptimization(true, true));
        assertFalse(RandomOptimizationMixinConfigPlugin
                .shouldDisableQuickPackOptimization(false, true));
        assertFalse(RandomOptimizationMixinConfigPlugin
                .shouldDisableQuickPackOptimization(true, false));
        assertFalse(RandomOptimizationMixinConfigPlugin
                .shouldDisableQuickPackOptimization(false, false));
    }

    @Test
    void replacesQuickPackHandlerWithIdentityFunction() {
        ClassNode target = new ClassNode();
        MethodNode handler = new MethodNode(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                "modifyReturnValue$zzi000$useFastFilePackResources",
                "(Ljava/lang/Object;Ljava/lang/String;Ljava/io/File;Z)Ljava/lang/Object;",
                null,
                null);
        handler.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        handler.instructions.add(new InsnNode(Opcodes.ARETURN));
        target.methods.add(handler);

        assertTrue(RandomOptimizationMixinConfigPlugin.neutralizeQuickPackHandler(target));
        assertEquals(2, handler.instructions.size());
        assertEquals(Opcodes.ALOAD, handler.instructions.getFirst().getOpcode());
        assertEquals(Opcodes.ARETURN, handler.instructions.getLast().getOpcode());
        assertEquals(0, ((org.objectweb.asm.tree.VarInsnNode) handler.instructions.getFirst()).var);
    }
}
