package main.bots;

import battlecode.common.*;
import main.util.Constants;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;

import static first_bot.util.Constants.directions;

public class HQ extends Robot {
    MapLocation ownLocation;

    public HQ(RobotController rc) {
        super(rc);
        ownLocation = rc.getLocation();
    }

    boolean[][] test;

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
        switch (turnCount) {
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
                int turnCountStart = turnCount;

                extracted();

                int after = Clock.getBytecodesLeft();
                System.out.println("USED " + (before - after) + " BYTECODE" + (turnCountStart != turnCount ? ", WENT OVER LIMIT!!!" : ""));
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
            rc.setIndicatorString("Building anchor! " + rc.getAnchor());
        }
    }

    private void extracted() throws GameActionException {
        MapLocation testLoc = ownLocation
                .add(Direction.NORTHEAST)
                .add(Direction.NORTHEAST)
                .add(Direction.EAST);

        test = new boolean[61][61];

//        LinkedList<MapLocation> queue = new LinkedList<>(); // todo custom linked list
//        queue.add(testLoc);
        // TODO check if having an initial capacity is smart
        // todo custom hashset for locations
//        HashSet<MapLocation> seen = new HashSet<>(70); // MapLocations can be compared easily
//        seen.add(ownLocation);
        test[ownLocation.x][ownLocation.y] = true;

        // Start from own location and try two DFS', one right inclined and one left
        int max_depth = 8;
        MapLocation head = ownLocation;

        for (int i = max_depth; --i > 0; ) {
            Direction dirToTarget = head.directionTo(testLoc);
            for (int j = 7; --j >= 0; ) {
                dirToTarget = dirToTarget.rotateRight();
                MapLocation next_possible_head = head.add(dirToTarget);

                if (locationOnMap(next_possible_head) && rc.sensePassability(next_possible_head) && !rc.isLocationOccupied(next_possible_head)) {
                    head = next_possible_head;
                    break; // CHANGE
                }
            }
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
