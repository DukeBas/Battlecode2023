package first_bot.util;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
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
    public Direction getDirection(final MapLocation target) throws GameActionException {
        MapLocation ownLocation = rc.getLocation();

        // Best direct location to target
        Direction dirToTarget = ownLocation.directionTo(target);

        // Check if we can actually move to that spot
        MapLocation toMoveTo = ownLocation.add(dirToTarget);

        if (rc.sensePassability(toMoveTo) && !rc.isLocationOccupied(toMoveTo)){
            return dirToTarget;
        }

        // Location was not possible... try out other options...
        MapLocation[] options = new MapLocation[]{
                ownLocation.add(dirToTarget.rotateLeft()),
                ownLocation.add(dirToTarget.rotateRight()),
                ownLocation.add(dirToTarget.rotateLeft().rotateLeft()),
                ownLocation.add(dirToTarget.rotateRight().rotateRight()),
        };

        // Try to move to one of the alternative options
        for (MapLocation loc : options) {
            if (rc.sensePassability(loc) && !rc.isLocationOccupied(loc)){
                return ownLocation.directionTo(loc);
            }
        }

        // We cannot move closer so just wait?
        return Direction.CENTER;
    }
}
