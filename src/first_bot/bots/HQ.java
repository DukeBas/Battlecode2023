package first_bot.bots;

import battlecode.common.*;
import static first_bot.util.Constants.directions;

public class HQ extends Robot{
    MapLocation ownLocation;

    public HQ(RobotController rc) {
        super(rc);
        ownLocation = rc.getLocation();
    }

    /**
     * This code is run once per turn (assuming we do not go over bytecode limits.)
     *
     * @throws GameActionException if an illegal game action is performed.
     */
    @Override
    void run() throws GameActionException {
//        if (rc.canBuildAnchor(Anchor.STANDARD)) {
//            // If we can build an anchor do it!
//            rc.buildAnchor(Anchor.STANDARD);
//            rc.setIndicatorString("Building anchor! " + rc.getAnchor());
//        }


        // Let's try to build a carrier.
        tryToBuild(RobotType.CARRIER);
        // Let's try to build a launcher.
        tryToBuild(RobotType.LAUNCHER);
    }

    public void tryToBuild(RobotType type) throws GameActionException {
        // Try all directions to find one to build in
        Direction dir = null;
        for (Direction d : directions) {
            MapLocation loc = ownLocation.add(d);
            if (!rc.isLocationOccupied(loc) && rc.sensePassability(loc)){
                // we can build on this spot!
                dir = d;
                break;
            }
        }

        // Check if we have a valid spot to build in
        if (dir == null) {
            return;
        }
        MapLocation buildLocation = rc.getLocation().add(dir);

        rc.setIndicatorString("Trying to build a " + type.toString());
        if (rc.canBuildRobot(type, buildLocation)) {
            rc.buildRobot(type, buildLocation);
        }
    }
}
