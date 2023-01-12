package main.util;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

/**
 * Bug pathfinding going straight to target, if most direct way is blocked, keep going to the right until we make it
 */
public class BugPathing implements Pathfinding {
    RobotController rc;
    int mapWidth;
    int mapHeight;

    public BugPathing(RobotController rc) {
        this.rc = rc;
        this.mapWidth = rc.getMapWidth();
        this.mapHeight = rc.getMapWidth();
    }

    @Override
    public Direction getDirection(final MapLocation target) throws GameActionException {
        MapLocation ownLocation = rc.getLocation();

        // Best direct location to target
        Direction dirToTarget = ownLocation.directionTo(target);

        // Check if we can actually move to that spot
        MapLocation toMoveTo = ownLocation.add(dirToTarget);

        if (locationOnMap(toMoveTo) && rc.sensePassability(toMoveTo) && !rc.isLocationOccupied(toMoveTo)) {
            return dirToTarget;
        }

        // Location was not possible... try more and more to the right
        MapLocation next;
        for (int i = 7; --i >= 0; ) {
            dirToTarget = dirToTarget.rotateRight();
            next = ownLocation.add(dirToTarget);

            if (locationOnMap(next) && rc.sensePassability(next) && !rc.isLocationOccupied(next)) {
                return ownLocation.directionTo(next);
            }
        }

        // We cannot move closer so just wait?
        rc.setIndicatorString("pathfinding done booboo, idk what do");
        return Direction.CENTER;
    }

    public boolean locationOnMap(MapLocation loc) {
        int x = loc.x;
        int y = loc.y;
        return x >= 0 && x < mapWidth && y >= 0 && y < mapHeight;
    }
}
