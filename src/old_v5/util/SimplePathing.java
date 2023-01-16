package old_v5.util;

import battlecode.common.*;

/**
 * Simple pathfinding going straight to target, ignoring terrain and other robots.
 */
public class SimplePathing implements Pathfinding {
    RobotController rc;
    int mapWidth;
    int mapHeight;

    public SimplePathing(RobotController rc){
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

        if (rc.canSenseLocation(toMoveTo) && rc.sensePassability(toMoveTo) && !rc.isLocationOccupied(toMoveTo)){
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
            if (rc.canSenseLocation(loc) && rc.sensePassability(loc) && !rc.isLocationOccupied(loc)){
                return ownLocation.directionTo(loc);
            }
        }

        // We cannot move closer so just wait?
        rc.setIndicatorString("simple pathfinding done booboo, idk what do for target " + target);
        return Direction.CENTER;
    }

    public Direction tryDirection(Direction targetDir) throws GameActionException {
        MapLocation ownLocation = rc.getLocation();

        // Check if we can actually move to that spot
        MapLocation toMoveTo = ownLocation.add(targetDir);

        if (rc.canSenseLocation(toMoveTo) && rc.sensePassability(toMoveTo) && !rc.isLocationOccupied(toMoveTo)){
            return targetDir;
        }

        // Location was not possible... try out other options...
        MapLocation[] options = new MapLocation[]{
                ownLocation.add(targetDir.rotateLeft()),
                ownLocation.add(targetDir.rotateRight()),
                ownLocation.add(targetDir.rotateLeft().rotateLeft()),
                ownLocation.add(targetDir.rotateRight().rotateRight()),
        };

        // Try to move to one of the alternative options
        for (MapLocation loc : options) {
            if (rc.canSenseLocation(loc) && rc.sensePassability(loc) && !rc.isLocationOccupied(loc)){
                return ownLocation.directionTo(loc);
            }
        }

        // We cannot move closer so just wait?
        rc.setIndicatorString("simple pathfinding done booboo, idk what do using this direction " + targetDir + "test");
        return Direction.CENTER;
    }
}
