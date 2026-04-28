package weaponinventorymod.patcher;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;
import jdk.internal.org.objectweb.asm.tree.AbstractInsnNode;
import jdk.internal.org.objectweb.asm.tree.ClassNode;
import jdk.internal.org.objectweb.asm.tree.FieldInsnNode;
import jdk.internal.org.objectweb.asm.tree.InsnList;
import jdk.internal.org.objectweb.asm.tree.InsnNode;
import jdk.internal.org.objectweb.asm.tree.JumpInsnNode;
import jdk.internal.org.objectweb.asm.tree.LabelNode;
import jdk.internal.org.objectweb.asm.tree.LdcInsnNode;
import jdk.internal.org.objectweb.asm.tree.LineNumberNode;
import jdk.internal.org.objectweb.asm.tree.MethodInsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodNode;
import jdk.internal.org.objectweb.asm.tree.TypeInsnNode;
import jdk.internal.org.objectweb.asm.tree.VarInsnNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class CargoStackViewPatcher {
    private static final String TARGET_CLASS = "com/fs/starfarer/campaign/ui/trade/CargoStackView";
    private static final String TARGET_CLASS_ENTRY = TARGET_CLASS + ".class";
    private static final String TARGET_METHOD = "renderAtCenter";
    private static final String TARGET_DESC = "(FFF)V";

    private static final String CARGO_ITEM_TYPE_OWNER = "com/fs/starfarer/api/campaign/CargoAPI$CargoItemType";
    private static final String CARGO_ITEM_TYPE_WEAPONS = "WEAPONS";

    private static final String POSITION_OWNER = "com/fs/starfarer/ui/OO0O";
    private static final String POSITION_GET_WIDTH = "getWidth";
    private static final String POSITION_GET_HEIGHT = "getHeight";
    private static final String POSITION_GET_DIM_DESC = "()F";

    private static final String OLD_HOOK_OWNER = "weaponinventorymod/internal/CargoWeaponMarkerHook";
    private static final String OLD_HOOK_METHOD = "render";
    private static final String OLD_HOOK_DESC_1 = "(F)V";
    private static final String OLD_HOOK_DESC_3 = "(FFF)V";

    private static final String SPRITE_OWNER = "com/fs/graphics/Sprite";
    private static final String SPRITE_INIT = "<init>";
    private static final String SPRITE_INIT_DESC = "(Ljava/lang/String;)V";
    private static final String SPRITE_SET_NORMAL_BLEND = "setNormalBlend";
    private static final String SPRITE_SET_NORMAL_BLEND_DESC = "()V";
    private static final String SPRITE_SET_ALPHA = "setAlphaMult";
    private static final String SPRITE_SET_ALPHA_DESC = "(F)V";
    private static final String SPRITE_GET_HEIGHT = "getHeight";
    private static final String SPRITE_GET_HEIGHT_DESC = "()F";
    private static final String SPRITE_RENDER = "render";
    private static final String SPRITE_RENDER_DESC = "(FF)V";

    private static final String MARKER_SPRITE_PATH = "graphics/ui/weapon_inventory_test_marker.png";
    private static final float MARKER_PADDING = 5f;

    private static final String DRAW_DESC = "(FFFF)V";
    private static final float DIAG_X_OFFSET = 18f;
    private static final int DIAG_Y_LOCAL = 10;
    private static final int DIAG_ANGLE_LOCAL = 13;
    private static final int DIAG_ALPHA_LOCAL = 3;

    private static final class SlotDimensionLocals {
        final int widthLocal;
        final int heightLocal;

        SlotDimensionLocals(int widthLocal, int heightLocal) {
            this.widthLocal = widthLocal;
            this.heightLocal = heightLocal;
        }
    }

    private static final class WeaponRenderInfo {
        final String weaponFieldName;
        final String weaponFieldDesc;
        final String weaponTypeOwner;
        final String drawMethodName;

        WeaponRenderInfo(String weaponFieldName, String weaponFieldDesc, String weaponTypeOwner, String drawMethodName) {
            this.weaponFieldName = weaponFieldName;
            this.weaponFieldDesc = weaponFieldDesc;
            this.weaponTypeOwner = weaponTypeOwner;
            this.drawMethodName = drawMethodName;
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("Usage: CargoStackViewPatcher <patch|restore> <obfJarPath> <backupJarPath>");
        }

        String mode = args[0].toLowerCase();
        Path jarPath = Path.of(args[1]);
        Path backupPath = Path.of(args[2]);

        if ("patch".equals(mode)) {
            patch(jarPath, backupPath);
            return;
        }
        if ("restore".equals(mode)) {
            restore(jarPath, backupPath);
            return;
        }

        throw new IllegalArgumentException("Unknown mode: " + args[0]);
    }

    private static void patch(Path jarPath, Path backupPath) throws Exception {
        if (!Files.exists(jarPath)) {
            throw new IllegalStateException("Target jar does not exist: " + jarPath);
        }

        byte[] classBytes = readClassBytes(jarPath, TARGET_CLASS_ENTRY);
        if (classBytes == null) {
            throw new IllegalStateException("Target class not found in jar: " + TARGET_CLASS_ENTRY);
        }

        ClassNode classNode = readClassNode(classBytes);
        MethodNode method = findTargetMethod(classNode);
        if (method == null) {
            throw new IllegalStateException("Patch refused: method not found: " + TARGET_METHOD + TARGET_DESC);
        }

        JumpInsnNode weaponTypeGuard = findWeaponTypeGuard(method);
        if (weaponTypeGuard == null) {
            throw new IllegalStateException("Patch refused: deterministic WEAPONS branch guard not found.");
        }
        LabelNode nonWeaponLabel = weaponTypeGuard.label;

        WeaponRenderInfo renderInfo = findWeaponRenderInfo(classNode, method, weaponTypeGuard, nonWeaponLabel);

        boolean oldHookCall = hasLegacyHookCall(method);
        boolean duplicateDraw = hasDiagnosticDuplicateDraw(method, classNode.name, renderInfo);
        boolean markerInWeaponsBranch = hasMarkerDrawInWeaponsBranch(method, weaponTypeGuard, nonWeaponLabel);
        boolean markerOutsideWeaponsBranch = hasMarkerDrawOutsideWeaponsBranch(method, weaponTypeGuard, nonWeaponLabel);
        if (oldHookCall || duplicateDraw || markerInWeaponsBranch || markerOutsideWeaponsBranch) {
            String reason = oldHookCall
                    ? "legacy hook call already present"
                    : (duplicateDraw
                    ? "diagnostic duplicate draw already present"
                    : (markerInWeaponsBranch ? "weapon marker patch already present" : "stale marker patch outside WEAPONS branch already present"));
            throw new IllegalStateException("Patch refused: " + reason + ".");
        }

        SlotDimensionLocals slotLocals = findSlotDimensionLocals(method);

        if (!Files.exists(backupPath)) {
            Files.copy(jarPath, backupPath, StandardCopyOption.COPY_ATTRIBUTES);
            System.out.println("Created backup: " + backupPath);
        } else {
            System.out.println("Backup already exists: " + backupPath);
        }

        int markerSpriteLocal = method.maxLocals;
        int markerXLocal = markerSpriteLocal + 1;
        int markerYLocal = markerSpriteLocal + 2;
        method.maxLocals += 3;

        InsnList inject = new InsnList();
        inject.add(new TypeInsnNode(Opcodes.NEW, SPRITE_OWNER));
        inject.add(new InsnNode(Opcodes.DUP));
        inject.add(new LdcInsnNode(MARKER_SPRITE_PATH));
        inject.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, SPRITE_OWNER, SPRITE_INIT, SPRITE_INIT_DESC, false));
        inject.add(new VarInsnNode(Opcodes.ASTORE, markerSpriteLocal));

        inject.add(new VarInsnNode(Opcodes.FLOAD, slotLocals.widthLocal));
        inject.add(new InsnNode(Opcodes.FNEG));
        inject.add(new InsnNode(Opcodes.FCONST_2));
        inject.add(new InsnNode(Opcodes.FDIV));
        inject.add(new LdcInsnNode(MARKER_PADDING));
        inject.add(new InsnNode(Opcodes.FADD));
        inject.add(new VarInsnNode(Opcodes.FSTORE, markerXLocal));

        inject.add(new VarInsnNode(Opcodes.FLOAD, slotLocals.heightLocal));
        inject.add(new InsnNode(Opcodes.FCONST_2));
        inject.add(new InsnNode(Opcodes.FDIV));
        inject.add(new VarInsnNode(Opcodes.ALOAD, markerSpriteLocal));
        inject.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, SPRITE_OWNER, SPRITE_GET_HEIGHT, SPRITE_GET_HEIGHT_DESC, false));
        inject.add(new InsnNode(Opcodes.FSUB));
        inject.add(new LdcInsnNode(MARKER_PADDING));
        inject.add(new InsnNode(Opcodes.FSUB));
        inject.add(new VarInsnNode(Opcodes.FSTORE, markerYLocal));

        inject.add(new VarInsnNode(Opcodes.ALOAD, markerSpriteLocal));
        inject.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, SPRITE_OWNER, SPRITE_SET_NORMAL_BLEND, SPRITE_SET_NORMAL_BLEND_DESC, false));
        inject.add(new VarInsnNode(Opcodes.ALOAD, markerSpriteLocal));
        inject.add(new VarInsnNode(Opcodes.FLOAD, DIAG_ALPHA_LOCAL));
        inject.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, SPRITE_OWNER, SPRITE_SET_ALPHA, SPRITE_SET_ALPHA_DESC, false));
        inject.add(new VarInsnNode(Opcodes.ALOAD, markerSpriteLocal));
        inject.add(new VarInsnNode(Opcodes.FLOAD, markerXLocal));
        inject.add(new VarInsnNode(Opcodes.FLOAD, markerYLocal));
        inject.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, SPRITE_OWNER, SPRITE_RENDER, SPRITE_RENDER_DESC, false));
        method.instructions.insertBefore(nonWeaponLabel, inject);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        byte[] patchedBytes = writer.toByteArray();

        verifyPatchedState(patchedBytes);
        writePatchedJar(jarPath, TARGET_CLASS_ENTRY, patchedBytes);

        System.out.println("Patched class: " + TARGET_CLASS_ENTRY);
        System.out.println("Injected slot-anchored marker draw at end of WEAPONS branch using Sprite.render(FF).");
        System.out.println("Patched jar: " + jarPath);
    }

    private static void restore(Path jarPath, Path backupPath) throws IOException {
        if (!Files.exists(backupPath)) {
            throw new IllegalStateException("Restore refused: backup not found at " + backupPath);
        }
        Files.copy(backupPath, jarPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        System.out.println("Restored jar from backup: " + backupPath);
    }

    private static MethodNode findTargetMethod(ClassNode classNode) {
        MethodNode found = null;
        for (MethodNode method : classNode.methods) {
            if (!TARGET_METHOD.equals(method.name) || !TARGET_DESC.equals(method.desc)) {
                continue;
            }
            if (found != null) {
                throw new IllegalStateException("Patch refused: multiple target methods matched " + TARGET_METHOD + TARGET_DESC);
            }
            found = method;
        }
        return found;
    }

    private static JumpInsnNode findWeaponTypeGuard(MethodNode method) {
        JumpInsnNode found = null;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() != Opcodes.IF_ACMPNE) {
                continue;
            }
            AbstractInsnNode prev = previousRealInsn(insn.getPrevious());
            if (!(prev instanceof FieldInsnNode)) {
                continue;
            }
            FieldInsnNode field = (FieldInsnNode) prev;
            if (field.getOpcode() != Opcodes.GETSTATIC) {
                continue;
            }
            if (!CARGO_ITEM_TYPE_OWNER.equals(field.owner) || !CARGO_ITEM_TYPE_WEAPONS.equals(field.name)) {
                continue;
            }
            if (found == null) {
                found = (JumpInsnNode) insn;
            }
        }
        return found;
    }

    private static WeaponRenderInfo findWeaponRenderInfo(ClassNode classNode, MethodNode method, JumpInsnNode guard, LabelNode nonWeaponLabel) {
        FieldInsnNode weaponField = null;
        MethodInsnNode drawMethod = null;

        for (AbstractInsnNode insn = guard.getNext(); insn != null && insn != nonWeaponLabel; insn = insn.getNext()) {
            if (weaponField == null && insn instanceof FieldInsnNode) {
                FieldInsnNode field = (FieldInsnNode) insn;
                if (field.getOpcode() == Opcodes.GETFIELD && classNode.name.equals(field.owner) && "weaponIcon".equals(field.name)) {
                    weaponField = field;
                }
            }
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode methodInsn = (MethodInsnNode) insn;
                if (methodInsn.getOpcode() == Opcodes.INVOKEVIRTUAL && DRAW_DESC.equals(methodInsn.desc)) {
                    String owner = methodInsn.owner;
                    if (owner.startsWith("com/fs/starfarer/ui/B/")) {
                        if (drawMethod == null) {
                            drawMethod = methodInsn;
                        } else if (!drawMethod.owner.equals(methodInsn.owner) || !drawMethod.name.equals(methodInsn.name)) {
                            throw new IllegalStateException("Patch refused: multiple weapon draw method signatures detected.");
                        }
                    }
                }
            }
        }

        if (weaponField == null) {
            throw new IllegalStateException("Patch refused: weaponIcon field access not found in WEAPONS branch.");
        }
        if (drawMethod == null) {
            throw new IllegalStateException("Patch refused: weapon draw method not found in WEAPONS branch.");
        }
        String weaponTypeOwner = Type.getType(weaponField.desc).getInternalName();
        if (!weaponTypeOwner.equals(drawMethod.owner)) {
            throw new IllegalStateException("Patch refused: weaponIcon type and draw method owner mismatch.");
        }
        return new WeaponRenderInfo(weaponField.name, weaponField.desc, weaponTypeOwner, drawMethod.name);
    }

    private static SlotDimensionLocals findSlotDimensionLocals(MethodNode method) {
        Integer widthLocal = null;
        Integer heightLocal = null;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode)) {
                continue;
            }
            MethodInsnNode methodInsn = (MethodInsnNode) insn;
            if (methodInsn.getOpcode() != Opcodes.INVOKEVIRTUAL || !POSITION_OWNER.equals(methodInsn.owner) || !POSITION_GET_DIM_DESC.equals(methodInsn.desc)) {
                continue;
            }

            AbstractInsnNode store = nextRealInsn(insn.getNext());
            if (!(store instanceof VarInsnNode) || store.getOpcode() != Opcodes.FSTORE) {
                continue;
            }
            int local = ((VarInsnNode) store).var;
            if (POSITION_GET_WIDTH.equals(methodInsn.name) && widthLocal == null) {
                widthLocal = local;
            } else if (POSITION_GET_HEIGHT.equals(methodInsn.name) && heightLocal == null) {
                heightLocal = local;
            }

            if (widthLocal != null && heightLocal != null) {
                return new SlotDimensionLocals(widthLocal, heightLocal);
            }
        }
        throw new IllegalStateException("Patch refused: slot width/height locals not found from getPosition().getWidth()/getHeight().");
    }

    private static boolean isAload0(AbstractInsnNode insn) {
        if (!(insn instanceof VarInsnNode)) {
            return false;
        }
        VarInsnNode varInsn = (VarInsnNode) insn;
        return varInsn.var == 0;
    }

    private static boolean hasLegacyHookCall(MethodNode method) {
        return containsMethodCall(method, OLD_HOOK_OWNER, OLD_HOOK_METHOD, OLD_HOOK_DESC_1)
                || containsMethodCall(method, OLD_HOOK_OWNER, OLD_HOOK_METHOD, OLD_HOOK_DESC_3);
    }

    private static boolean containsMethodCall(MethodNode method, String owner, String name, String desc) {
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode)) {
                continue;
            }
            MethodInsnNode methodInsn = (MethodInsnNode) insn;
            if (owner.equals(methodInsn.owner) && name.equals(methodInsn.name) && desc.equals(methodInsn.desc)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasDiagnosticDuplicateDraw(MethodNode method, String classOwner, WeaponRenderInfo info) {
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode)) {
                continue;
            }
            MethodInsnNode call = (MethodInsnNode) insn;
            if (call.getOpcode() != Opcodes.INVOKEVIRTUAL
                    || !info.weaponTypeOwner.equals(call.owner)
                    || !info.drawMethodName.equals(call.name)
                    || !DRAW_DESC.equals(call.desc)) {
                continue;
            }
            if (matchesDiagnosticPattern(call, classOwner, info)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasMarkerDrawInWeaponsBranch(MethodNode method, JumpInsnNode guard, LabelNode nonWeaponLabel) {
        boolean sawMarkerPath = false;
        boolean sawMarkerRender = false;
        for (AbstractInsnNode insn = guard.getNext(); insn != null && insn != nonWeaponLabel; insn = insn.getNext()) {
            if (insn instanceof LdcInsnNode) {
                LdcInsnNode ldc = (LdcInsnNode) insn;
                if (MARKER_SPRITE_PATH.equals(ldc.cst)) {
                    sawMarkerPath = true;
                }
            }
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode methodInsn = (MethodInsnNode) insn;
                if (methodInsn.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && SPRITE_OWNER.equals(methodInsn.owner)
                        && SPRITE_RENDER.equals(methodInsn.name)
                        && SPRITE_RENDER_DESC.equals(methodInsn.desc)) {
                    sawMarkerRender = true;
                }
            }
        }
        return sawMarkerPath && sawMarkerRender;
    }

    private static boolean hasMarkerDrawOutsideWeaponsBranch(MethodNode method, JumpInsnNode guard, LabelNode nonWeaponLabel) {
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof LdcInsnNode)) {
                continue;
            }
            LdcInsnNode ldc = (LdcInsnNode) insn;
            if (!MARKER_SPRITE_PATH.equals(ldc.cst)) {
                continue;
            }
            if (isInWeaponsBranch(ldc, guard, nonWeaponLabel)) {
                continue;
            }
            if (hasNearbySpriteRender(ldc, 60)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasNearbySpriteRender(AbstractInsnNode anchor, int maxForwardRealInsns) {
        int seen = 0;
        for (AbstractInsnNode next = nextRealInsn(anchor.getNext()); next != null && seen <= maxForwardRealInsns; next = nextRealInsn(next.getNext())) {
            seen++;
            if (!(next instanceof MethodInsnNode)) {
                continue;
            }
            MethodInsnNode methodInsn = (MethodInsnNode) next;
            if (methodInsn.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && SPRITE_OWNER.equals(methodInsn.owner)
                    && SPRITE_RENDER.equals(methodInsn.name)
                    && SPRITE_RENDER_DESC.equals(methodInsn.desc)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInWeaponsBranch(AbstractInsnNode target, JumpInsnNode guard, LabelNode nonWeaponLabel) {
        for (AbstractInsnNode insn = guard.getNext(); insn != null && insn != nonWeaponLabel; insn = insn.getNext()) {
            if (insn == target) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesDiagnosticPattern(MethodInsnNode call, String classOwner, WeaponRenderInfo info) {
        AbstractInsnNode a7 = previousRealInsn(call.getPrevious()); // alpha
        AbstractInsnNode a6 = previousRealInsn(a7 == null ? null : a7.getPrevious()); // angle
        AbstractInsnNode a5 = previousRealInsn(a6 == null ? null : a6.getPrevious()); // fneg
        AbstractInsnNode a4 = previousRealInsn(a5 == null ? null : a5.getPrevious()); // y
        AbstractInsnNode a3 = previousRealInsn(a4 == null ? null : a4.getPrevious()); // x offset
        AbstractInsnNode a2 = previousRealInsn(a3 == null ? null : a3.getPrevious()); // getfield
        AbstractInsnNode a1 = previousRealInsn(a2 == null ? null : a2.getPrevious()); // aload_0

        if (!(a7 instanceof VarInsnNode) || ((VarInsnNode) a7).getOpcode() != Opcodes.FLOAD || ((VarInsnNode) a7).var != DIAG_ALPHA_LOCAL) {
            return false;
        }
        if (!(a6 instanceof VarInsnNode) || ((VarInsnNode) a6).getOpcode() != Opcodes.FLOAD || ((VarInsnNode) a6).var != DIAG_ANGLE_LOCAL) {
            return false;
        }
        if (a5 == null || a5.getOpcode() != Opcodes.FNEG) {
            return false;
        }
        if (!(a4 instanceof VarInsnNode) || ((VarInsnNode) a4).getOpcode() != Opcodes.FLOAD || ((VarInsnNode) a4).var != DIAG_Y_LOCAL) {
            return false;
        }
        if (!(a3 instanceof LdcInsnNode) || !isFloatConstant((LdcInsnNode) a3, DIAG_X_OFFSET)) {
            return false;
        }
        if (!(a2 instanceof FieldInsnNode)) {
            return false;
        }
        FieldInsnNode field = (FieldInsnNode) a2;
        if (field.getOpcode() != Opcodes.GETFIELD
                || !classOwner.equals(field.owner)
                || !info.weaponFieldName.equals(field.name)
                || !info.weaponFieldDesc.equals(field.desc)) {
            return false;
        }
        return isAload0(a1);
    }

    private static boolean isFloatConstant(LdcInsnNode ldc, float expected) {
        if (!(ldc.cst instanceof Float)) {
            return false;
        }
        return Float.compare((Float) ldc.cst, expected) == 0;
    }

    private static void verifyPatchedState(byte[] patchedClassBytes) {
        ClassNode classNode = readClassNode(patchedClassBytes);
        MethodNode method = findTargetMethod(classNode);
        if (method == null) {
            throw new IllegalStateException("Patch verification failed: target method missing.");
        }
        JumpInsnNode weaponTypeGuard = findWeaponTypeGuard(method);
        if (weaponTypeGuard == null) {
            throw new IllegalStateException("Patch verification failed: WEAPONS branch guard missing.");
        }
        LabelNode nonWeaponLabel = weaponTypeGuard.label;
        WeaponRenderInfo info = findWeaponRenderInfo(classNode, method, weaponTypeGuard, nonWeaponLabel);
        if (hasLegacyHookCall(method)) {
            throw new IllegalStateException("Patch verification failed: legacy hook call still present.");
        }
        if (hasDiagnosticDuplicateDraw(method, classNode.name, info)) {
            throw new IllegalStateException("Patch verification failed: duplicate diagnostic weapon draw still present.");
        }
        if (!hasMarkerDrawInWeaponsBranch(method, weaponTypeGuard, nonWeaponLabel)) {
            throw new IllegalStateException("Patch verification failed: weapon-branch marker patch not found.");
        }
        if (hasMarkerDrawOutsideWeaponsBranch(method, weaponTypeGuard, nonWeaponLabel)) {
            throw new IllegalStateException("Patch verification failed: stale marker patch outside WEAPONS branch still present.");
        }
    }

    private static ClassNode readClassNode(byte[] classBytes) {
        ClassNode classNode = new ClassNode();
        ClassReader reader = new ClassReader(classBytes);
        reader.accept(classNode, 0);
        return classNode;
    }

    private static AbstractInsnNode previousRealInsn(AbstractInsnNode node) {
        AbstractInsnNode current = node;
        while (current != null && isStructuralNode(current)) {
            current = current.getPrevious();
        }
        return current;
    }

    private static AbstractInsnNode nextRealInsn(AbstractInsnNode node) {
        AbstractInsnNode current = node;
        while (current != null && isStructuralNode(current)) {
            current = current.getNext();
        }
        return current;
    }

    private static boolean isStructuralNode(AbstractInsnNode node) {
        return node instanceof LabelNode || node instanceof LineNumberNode || node.getType() == AbstractInsnNode.FRAME;
    }

    private static byte[] readClassBytes(Path jarPath, String entryName) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            JarEntry entry = jarFile.getJarEntry(entryName);
            if (entry == null) {
                return null;
            }
            try (InputStream in = jarFile.getInputStream(entry)) {
                return readAll(in);
            }
        }
    }

    private static void writePatchedJar(Path jarPath, String entryName, byte[] replacementBytes) throws IOException {
        Path tempPath = jarPath.resolveSibling(jarPath.getFileName().toString() + ".wim_tmp");

        try (JarFile inputJar = new JarFile(jarPath.toFile())) {
            Manifest manifest = inputJar.getManifest();
            try (JarOutputStream output = manifest == null
                    ? new JarOutputStream(Files.newOutputStream(tempPath))
                    : new JarOutputStream(Files.newOutputStream(tempPath), manifest)) {
                Enumeration<JarEntry> entries = inputJar.entries();
                boolean replaced = false;
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (JarFile.MANIFEST_NAME.equalsIgnoreCase(entry.getName())) {
                        continue;
                    }

                    JarEntry outEntry = new JarEntry(entry.getName());
                    outEntry.setTime(entry.getTime());
                    output.putNextEntry(outEntry);

                    if (entryName.equals(entry.getName())) {
                        output.write(replacementBytes);
                        replaced = true;
                    } else {
                        try (InputStream in = inputJar.getInputStream(entry)) {
                            copy(in, output);
                        }
                    }

                    output.closeEntry();
                }

                if (!replaced) {
                    throw new IllegalStateException("Patch refused: target class entry not replaced: " + entryName);
                }
            }
        }

        Files.move(tempPath, jarPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(in, out);
        return out.toByteArray();
    }

    private static void copy(InputStream in, java.io.OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
    }
}
