package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShapes;

public class Scaffold extends ToggleModule {
    private final SettingGroup sg = settings.getDefaultGroup();

    private final Setting<Boolean> safeWalk = sg.add(new BoolSetting.Builder()
            .name("safe-walk")
            .description("Safe walk.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> fastTower = sg.add(new BoolSetting.Builder()
            .name("fast-tower")
            .description("To the sky.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> radius = sg.add(new IntSetting.Builder()
            .name("radius")
            .description("Radius.")
            .defaultValue(1)
            .min(1)
            .sliderMin(1)
            .sliderMax(7)
            .build()
    );

    private final Setting<Boolean> swingHand = sg.add(new BoolSetting.Builder()
            .name("swing-hand")
            .description("Only client side.")
            .defaultValue(false)
            .build()
    );

    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();
    private BlockState blockState, slotBlockState;
    private int slot, prevSelectedSlot;

    public Scaffold() {
        super(Category.Movement, "scaffold", "Places blocks under you.");
    }

    @EventHandler
    private final Listener<TickEvent> onTick = new Listener<>(event -> {
        if (fastTower.get() && !mc.world.getBlockState(setPos(0, -1, 0)).getMaterial().isReplaceable() && mc.options.keyJump.isPressed() && findSlot(mc.world.getBlockState(setPos(0, -1, 0))) != -1 && mc.player.sidewaysSpeed == 0 &&mc.player.forwardSpeed == 0) mc.player.jump();
        blockState = mc.world.getBlockState(setPos(0, -1, 0));
        if (!blockState.getMaterial().isReplaceable()) return;

        // Search for block in hotbar
        slot = findSlot(blockState);
        if (slot == -1) return;

        // Change slot
        prevSelectedSlot = mc.player.inventory.selectedSlot;
        mc.player.inventory.selectedSlot = slot;

        // Check if has solid horizontal neighbour
        boolean hasNeighbour = true;
        BlockState neighbourUnder = mc.world.getBlockState(setPos(0, -1 - 1, 0));
        BlockState neighbourRight = mc.world.getBlockState(setPos(1, -1, 0));
        BlockState neighbourLeft = mc.world.getBlockState(setPos(-1, -1, 0));
        BlockState neighbourTop = mc.world.getBlockState(setPos(0, -1, 1));
        BlockState neighbourBottom = mc.world.getBlockState(setPos(0, -1, -1));
        if (!isSolid(neighbourUnder) && !isSolid(neighbourRight) && !isSolid(neighbourLeft) && !isSolid(neighbourTop) && !isSolid(neighbourBottom)) hasNeighbour = false;

        // No neighbour so player is going diagonally
        if (!hasNeighbour) {
            // Place extra block
            boolean placed = Utils.place(slotBlockState, setPos(1, -1, 0), swingHand.get(), false, true);
            if (!placed) placed = Utils.place(slotBlockState, setPos(-1, -1, 0), swingHand.get(), false, true);
            if (!placed) placed = Utils.place(slotBlockState, setPos(0, -1, 1), swingHand.get(), false, true);
            if (!placed) placed = Utils.place(slotBlockState, setPos(0, -1, -1), swingHand.get(), false, true);
            if (!placed) placed = Utils.place(slotBlockState, setPos(0, -1 - 1, 0), swingHand.get(), false, true);
            if (!placed) {
                System.err.println("[Meteor]: Scaffold: Failed to place extra block when going diagonally. This shouldn't happen.");
                return;
            }

            // Search for extra block is needed
            if (!findBlock()) return;
        }

        // Place block
        Utils.place(slotBlockState, setPos(0, -1, 0), swingHand.get(), false, false);

        // Place blocks around if radius is bigger than 1
        for (int i = 1; i < radius.get(); i++) {
            int count = 1 + (i - 1) * 2;
            int countHalf = count / 2;

            // Forward
            for (int j = 0; j < count; j++) {
                if (!findBlock()) return;
                Utils.place(slotBlockState, setPos(j - countHalf, -1, i), swingHand.get(), false, false);
            }
            // Backward
            for (int j = 0; j < count; j++) {
                if (!findBlock()) return;
                Utils.place(slotBlockState, setPos(j - countHalf, -1, -i), swingHand.get(), false, false);
            }
            // Right
            for (int j = 0; j < count; j++) {
                if (!findBlock()) return;
                Utils.place(slotBlockState, setPos(i, -1, j - countHalf), swingHand.get(), false, false);
            }
            // Left
            for (int j = 0; j < count; j++) {
                if (!findBlock()) return;
                Utils.place(slotBlockState, setPos(-i, -1, j - countHalf), swingHand.get(), false, false);
            }

            // Diagonals
            if (!findBlock()) return;
            Utils.place(slotBlockState, setPos(-i, -1, i), swingHand.get(), false, false);
            if (!findBlock()) return;
            Utils.place(slotBlockState, setPos(i, -1, i), swingHand.get(), false, false);
            if (!findBlock()) return;
            Utils.place(slotBlockState, setPos(-i, -1, -i), swingHand.get(), false, false);
            if (!findBlock()) return;
            Utils.place(slotBlockState, setPos(i, -1, -i), swingHand.get(), false, false);
        }

        // Change back to previous slot
        mc.player.inventory.selectedSlot = prevSelectedSlot;
    });

    private boolean findBlock() {
        if (mc.player.inventory.getInvStack(slot).isEmpty()) {
            slot = findSlot(blockState);
            if (slot == -1) {
                mc.player.inventory.selectedSlot = prevSelectedSlot;
                return false;
            }
        }

        return true;
    }

    private boolean isSolid(BlockState state) {
        return state.getOutlineShape(mc.world, setPos(0, -1, 0)) != VoxelShapes.empty();
    }

    private BlockPos setPos(int x, int y, int z) {
        blockPos.set(mc.player);
        if (x != 0) blockPos.setX(blockPos.getX() + x);
        if (y != 0) blockPos.setY(blockPos.getY() + y);
        if (z != 0) blockPos.setZ(blockPos.getZ() + z);
        return blockPos;
    }

    private int findSlot(BlockState blockState) {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getInvStack(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) continue;

            // Filter out non solid blocks
            Block block = ((BlockItem) stack.getItem()).getBlock();
            slotBlockState = block.getDefaultState();
            if (!Block.isShapeFullCube(slotBlockState.getCollisionShape(mc.world, setPos(0, -1, 0)))) continue;

            // Filter out blocks that would fall
            if (block instanceof FallingBlock && FallingBlock.canFallThrough(blockState)) continue;

            slot = i;
            break;
        }

        return slot;
    }

    public boolean hasSafeWalk() {
        return safeWalk.get();
    }
}