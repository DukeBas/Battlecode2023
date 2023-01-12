package main.bots;

import battlecode.common.*;

import static first_bot.util.Constants.directions;

public class HQ extends Robot {
    MapLocation ownLocation;

    public HQ(RobotController rc) {
        super(rc);
        ownLocation = rc.getLocation();
    }

    boolean[] test;
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
            rc.setIndicatorString("Building anchor! " + rc.getAnchor());
        }
    }

    private void extracted() throws GameActionException {
        // todo: optimize for map size instead of always just using 60 (max)
        MapLocation testLoc = ownLocation
                .add(Direction.NORTHEAST)
                .add(Direction.NORTHEAST)
                .add(Direction.EAST);
        rc.setIndicatorDot(testLoc, 255, 255, 255);

        test = new boolean[3601];
//        test = new boolean[61][61];

        test[60 * ownLocation.x + ownLocation.y] = true;

        // Start from own location and try two DFS', one right inclined and one left
        int max_depth = 8;
        MapLocation head = ownLocation;

        // right inclined
        for (int i = max_depth; --i > 0; ) {
            Direction dirToTarget = head.directionTo(testLoc);

            // Check if we found the goal
            if (head.equals(testLoc)) {
//                System.out.println("Goal found in " + (max_depth - i) + " steps");
                break;
            }

            boolean head_same = true;

            for (int j = 7; --j >= 0; ) {
                MapLocation next_possible_head = head.add(dirToTarget);

                if (!locationOnMap(next_possible_head)) continue; // location out of bounds

                if (!test[60 * next_possible_head.x + next_possible_head.y]) {
//                    System.out.println("Trying " + next_possible_head.toString());
                    // set as seen so we don't revisit
                    test[60 * next_possible_head.x + next_possible_head.y] = true;

                    if (rc.canSenseLocation(next_possible_head) && rc.sensePassability(next_possible_head) && !rc.isLocationOccupied(next_possible_head)) {
//                        System.out.println("OLD HEAD IS " + head.toString() + " and next HEAD is " + next_possible_head.toString());
                        head_same = false;
                        rc.setIndicatorDot(head, 100, 150, 23);
                        break;
                    }
                }

                dirToTarget = dirToTarget.rotateRight();
            }

            // Check if we are stuck, just quit if we are
            if (head_same) {
//                System.out.println("stuck right");
                break;
            }
        }

        //todo check bytecode used/left

        // reset seen around start
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a != 0 || b != 0) {
                    int new_x = ownLocation.x + a;
                    int new_y = ownLocation.y + b;
                    if (new_x >= 0 && new_x < rc.getMapWidth() && new_y >= 0 && new_y < rc.getMapHeight()) {
                        test[60 * new_x + new_y] = false;
                    }
                }
            }
        }


        // left inclined
        head = ownLocation;
        for (int i = max_depth; --i > 0; ) {
            Direction dirToTarget = head.directionTo(testLoc);

            // Check if we found the goal
            if (head.equals(testLoc)) {
//                System.out.println("Goal found in " + (max_depth - i) + " steps");
                break;
            }

            boolean head_same = true;

            for (int j = 7; --j >= 0; ) {
                MapLocation next_possible_head = head.add(dirToTarget);

                if (!locationOnMap(next_possible_head)) continue; // location out of bounds

                if (!test[60 * next_possible_head.x + next_possible_head.y]) {
//                    System.out.println("Trying " + next_possible_head.toString());
                    // set as seen so we don't revisit
                    test[60 * next_possible_head.x + next_possible_head.y] = true;

                    if (rc.canSenseLocation(next_possible_head) && rc.sensePassability(next_possible_head) && !rc.isLocationOccupied(next_possible_head)) {
//                        System.out.println("OLD HEAD IS " + head.toString() + " and next HEAD is " + next_possible_head.toString());
                        head = next_possible_head;
                        head_same = false;
                        rc.setIndicatorDot(head, 200, 50, 230);
                        break;
                    }
                }

                dirToTarget = dirToTarget.rotateLeft();
            }

            // Check if we are stuck, just quit if we are
            if (head_same) {
//                System.out.println("stuck left");
                return;
            }
        }
    }

    // TODO REMOVE
    public boolean locationOnMap(MapLocation loc) {
        int x = loc.x;
        int y = loc.y;
        return x >= 0 && x < rc.getMapWidth() && y >= 0 && y < rc.getMapHeight();
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
