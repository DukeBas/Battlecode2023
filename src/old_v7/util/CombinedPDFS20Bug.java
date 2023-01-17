package old_v7.util;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class CombinedPDFS20Bug implements Pathfinding {
    private static int MAX_TURNS_NOT_CLOSER = 20;

    RobotController rc;
    int mapWidth;
    int mapHeight;
    // Variables to detect if we are getting stuck
    MapLocation lastTarget;
    MapLocation closestLocation;
    int turnStartedSeekingThisTarget = 0;

    public CombinedPDFS20Bug(RobotController rc){
        this.rc = rc;
        this.mapWidth = rc.getMapWidth();
        this.mapHeight = rc.getMapWidth();
        this.pseudoDFS20 = new PseudoDFS20(rc);
        this.bug = new BugPathing(rc);
        this.lastTarget = new MapLocation(-1,-1);
        this.closestLocation = new MapLocation(-1,-1);
    }

    PseudoDFS20 pseudoDFS20;
    BugPathing bug;
    @Override
    public Direction getDirection(MapLocation target) throws GameActionException {
        if (lastTarget.equals(target)) {
            // If we did not get closer for 20 turns, we are likely stuck :(
            int dist_saved_to_target = closestLocation.distanceSquaredTo(target);
            int dist_current_to_target = rc.getLocation().distanceSquaredTo(target);

            if (dist_current_to_target < dist_saved_to_target) {
                // We are closer, reset counter and save new position
                turnStartedSeekingThisTarget = rc.getRoundNum();
                closestLocation = rc.getLocation();
            } else {
                // Not closer :(
                if (rc.getRoundNum() - turnStartedSeekingThisTarget >= MAX_TURNS_NOT_CLOSER){
                    // Reset
                    turnStartedSeekingThisTarget = rc.getRoundNum();
                    // Change bug pathing mode
                    bug.flipDirection();
                }
            }

        } else {
            // New target, reset!
            lastTarget = target;
            closestLocation = rc.getLocation();
            turnStartedSeekingThisTarget = rc.getRoundNum();
        }


        Direction dir = pseudoDFS20.getDirection(target);

        return dir == Direction.CENTER ? bug.getDirection(target) : dir;
    }
}
