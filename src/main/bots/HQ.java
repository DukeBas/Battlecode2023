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

        LinkedList<MapLocation> queue = new LinkedList<>(); // todo custom linked list
        queue.add(testLoc);
        // TODO check if having an initial capacity is smart
        // todo custom hashset for locations
        HashSet<MapLocation> seen = new HashSet<>(70); // MapLocations can be compared easily
        seen.add(ownLocation);

        // Start from target and work towards own location
        while (!queue.isEmpty()){
            MapLocation current = queue.removeFirst();
            System.out.println("Current " + current.toString());

            // Check all neighbors TODO unroll, optimize
            MapLocation toCheck;
            for (Direction d : Constants.directions) {
                toCheck = current.add(d);

                if (toCheck.distanceSquaredTo(ownLocation) > 20) continue;
                // Location within range!

                if (!rc.onTheMap(toCheck)) continue;
                // Location exists! // TODO account for clouds

                if (!rc.sensePassability(toCheck)) continue;
                // Location is passable!

                int bef = Clock.getBytecodesLeft();
                if (!seen.contains(toCheck)){
                    System.out.println();
                }
                System.out.println("Single lookup costs: " + (bef - Clock.getBytecodesLeft()));

                // Add location to queue if it is new
                if (!seen.contains(toCheck)){
                    queue.add(toCheck);
                } else {
                    continue;
                }
                // Mark location as seen
                seen.add(toCheck);

                rc.setIndicatorDot(toCheck, 255, 100, 50);

                // Check if it is the goal, if so return best direction
                if (toCheck.equals(ownLocation)) {
                    Direction dir = ownLocation.directionTo(toCheck);
                    System.out.println("Found goal! Best direction is " + dir.toString());
                    // TODO
                }
            }
        }


        MapInfo[] infos = rc.senseNearbyMapInfos(20);
        for (int i = 0; i < infos.length; i++){
            rc.sensePassability(infos[i].getMapLocation());
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
