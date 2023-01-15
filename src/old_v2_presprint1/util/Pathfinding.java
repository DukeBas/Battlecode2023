package old_v2_presprint1.util;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

/**
 * Interface defines getDirection function for pathfinding.
 */
@FunctionalInterface
public interface Pathfinding {
    /**
     * Returns direction robot should take to find a path from source to target.
     *
     * @param target location
     * @return direction to take
     */
    Direction getDirection(final MapLocation target) throws GameActionException;
}
