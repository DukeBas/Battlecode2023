package first_bot.util;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

/**
 * Simple pathfinding going straight to target, ignoring terrain and other robots.
 */
public class SimplePathing implements Pathfinding {
    RobotController rc;

    SimplePathing(RobotController rc){
        this.rc = rc;
    }

    @Override
    public Direction getDirection(final MapLocation target) {
        MapLocation ownLocation = rc.getLocation();
        Direction dirToTarget = ownLocation.directionTo(target);
        return dirToTarget;
    }
}
