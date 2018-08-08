/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.bot.pathing.movement.movements;

import baritone.bot.InputOverrideHandler;
import baritone.bot.pathing.movement.CalculationContext;
import baritone.bot.pathing.movement.Movement;
import baritone.bot.pathing.movement.MovementHelper;
import baritone.bot.pathing.movement.MovementState;
import baritone.bot.pathing.movement.MovementState.MovementStatus;
import baritone.bot.utils.BlockStateInterface;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockVine;
import net.minecraft.util.math.BlockPos;

public class MovementDescend extends Movement {

    public MovementDescend(BlockPos start, BlockPos end) {
        super(start, end, new BlockPos[] { end.up(2), end.up(), end }, new BlockPos[] { end.down() });
    }

    @Override
    protected double calculateCost(CalculationContext context) {
        if (!MovementHelper.canWalkOn(positionsToPlace[0])) {
            return COST_INF;
        }
        Block tmp1 = BlockStateInterface.get(dest).getBlock();
        if (tmp1 instanceof BlockLadder || tmp1 instanceof BlockVine) {
            return COST_INF;
        }
        // we walk half the block plus 0.3 to get to the edge, then we walk the other 0.2 while simultaneously falling (math.max because of how it's in parallel)
        return WALK_ONE_BLOCK_COST * 0.8 + Math.max(FALL_N_BLOCKS_COST[1], WALK_ONE_BLOCK_COST * 0.2) + getTotalHardnessOfBlocksToBreak(context.getToolSet());
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        switch (state.getStatus()) {
            case PREPPING:
            case UNREACHABLE:
            case FAILED:
                return state;
            case WAITING:
                state.setStatus(MovementStatus.RUNNING);
            case RUNNING:
                BlockPos playerFeet = playerFeet();

                if (playerFeet.equals(dest) && player().posY - playerFeet.getY() < 0.01) {
                    // Wait until we're actually on the ground before saying we're done because sometimes we continue to fall if the next action starts immediately
                    state.setStatus(MovementStatus.SUCCESS);
                    return state;
                }
                double diffX = player().posX - (dest.getX() + 0.5);
                double diffZ = player().posZ - (dest.getZ() + 0.5);
                double ab = Math.sqrt(diffX * diffX + diffZ * diffZ);
                if (!playerFeet.equals(dest) || ab > 0.3) {
                    BlockPos fakeDest = new BlockPos(dest.getX() * 2 - src.getX(), dest.getY(), dest.getZ() * 2 - src.getZ());
                    double diffX2 = player().posX - (fakeDest.getX() + 0.5);
                    double diffZ2 = player().posZ - (fakeDest.getZ() + 0.5);
                    double d = Math.sqrt(diffX2 * diffX2 + diffZ2 * diffZ2);
                    if (d > ab)
                        moveTowards(fakeDest);
                    else {
                        state.setInput(InputOverrideHandler.Input.MOVE_FORWARD, false);
                        state.setInput(InputOverrideHandler.Input.MOVE_BACK, true);
                    }
                }
                return state;
            default:
                return state;
        }
    }

}