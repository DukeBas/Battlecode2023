package main.bots;

import battlecode.common.*;
import main.util.PseudoDFS20;

import static first_bot.util.Constants.directions;

public class HQ extends Robot {
    MapLocation ownLocation;

    public HQ(RobotController rc) {
        super(rc);
        ownLocation = rc.getLocation();
        pathfinding = new PseudoDFS20(rc);
    }

    int mostBytecodeExtracted = -1;

    /**
     * This code is run once per turn (assuming we do not go over bytecode limits.)
     *
     * @throws GameActionException if an illegal game action is performed.
     */
    @Override
    void run() throws GameActionException {
       /*
            HQ-specific Communication
        */
        switch (rc.getRoundNum()) {
            case 1:
                // Save our location to shared array
                // Find an empty spot, save it there
                for (int i = START_INDEX_FRIENDLY_HQS; i < START_INDEX_FRIENDLY_HQS + MAX_HQS; i++) {
                    if (rc.readSharedArray(i) == 0) {
                        rc.writeSharedArray(i, encode_HQ_location(ownLocation));
                        break;
                    }
                }
                break;
            case 2:
                // Try to figure out the symmetry of the map
                // Reflection lines/middle of the map gives away info
                break;
            default:
                // Uncomment to try out how much bytecode something costs
                int before = Clock.getBytecodesLeft();

                MapLocation testLoc = ownLocation
                        .add(Direction.NORTHEAST)
                        .add(Direction.NORTHEAST)
                        .add(Direction.EAST);
                pathfinding.getDirection(testLoc);

                int after = Clock.getBytecodesLeft();
                int diff = before - after;
//                System.out.println("USED " + diff + " BYTECODE" + (turnCountStart != turnCount ? ", WENT OVER LIMIT!!!" : ""));
                if (diff > mostBytecodeExtracted) {
                    mostBytecodeExtracted = diff;
                    System.out.println("new bytecode record :(  " + diff);
                }
                break;
        }

        // Uncomment below to see shared array for every turn!
//        int[] arr = new int[64];
//        for (int i = 0; i < 64; i++) {
//            arr[i] = rc.readSharedArray(i);
//        }
//        System.out.println(Arrays.toString(arr));



        /*
            Unit building
         */
        if (rc.senseNearbyRobots(RobotType.HEADQUARTERS.visionRadiusSquared, friendly).length < 35) {
            // Let's try to build a carrier.
            tryToBuild(RobotType.CARRIER);
            // Let's try to build a launcher.
            tryToBuild(RobotType.LAUNCHER);
        } else if (rc.canBuildAnchor(Anchor.STANDARD)) {
            // If we can build an anchor do it!
            rc.buildAnchor(Anchor.STANDARD);
            // TODO use acc. anchors
            rc.setIndicatorString("Building anchor! Currently have" + rc.getNumAnchors(Anchor.STANDARD));
        }
    }


    public void tryToBuild(RobotType type) throws GameActionException {
        // Try all directions to find one to build in
        Direction dir = null;
        for (Direction d : directions) {
            MapLocation loc = ownLocation.add(d);
            if (!rc.isLocationOccupied(loc) && rc.sensePassability(loc)) {
                // we can build on this spot!
                dir = d;
                break;
            }
        }

        // TODO: base build direction on RobotType and situation

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
