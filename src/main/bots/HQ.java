package main.bots;

import battlecode.common.*;
import main.util.Constants;
import main.util.Map_helper;
import main.util.PseudoDFS20;

import static first_bot.util.Constants.directions;

public class HQ extends Robot {
    int HQ_id = -1;

    MapLocation ownLocation;
    Map_helper map_helper;

    public HQ(RobotController rc) {
        super(rc);
        ownLocation = rc.getLocation();
        pathfinding = new PseudoDFS20(rc);
        map_helper = new Map_helper(rc);
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
                        HQ_id = i - START_INDEX_FRIENDLY_HQS;
                        break;
                    }
                }
                break;
            case 2:
                // Own HQ locations & reflection lines/middle of the map gives away info
                MapLocation[] friendly_HQs = getFriendlyHQLocations();
                int width = rc.getMapWidth();
                int height = rc.getMapHeight();

                /*
                 Test for Rotational symmetry (180 deg around middle)
                 */
                // -> HQs
                boolean rotational_possible = true;
                for (MapLocation hq : friendly_HQs) {
                    // If we can see the rotational symmetric version of a location, check if it has an HQ
                    MapLocation sym = map_helper.rotationalSymmetricLocation(hq);
                    if (rc.canSenseLocation(sym)) {
                        // It is in range!
                        if (rc.canSenseRobotAtLocation(sym)){
                            // There's a robot there ?!
                            // Is it an enemy HQ?
                            RobotInfo info = rc.senseRobotAtLocation(sym);
                            if (info.team != enemy || info.type != RobotType.HEADQUARTERS){
                                // Different robot type, so not possible
                                rotational_possible = false;
                                break;
                            }
                        } else {
                            // No robot found there... so this symmetry is not possible
                            rotational_possible = false;
                            break;
                        }
                    }
                }

                // -> Map details TODO
//                if (rotational_possible){}

                if (!rotational_possible){
                    // We have disproven possibility of rotational symmetry here
                    System.out.println("DISPROVEN ROTATIONAL");
                    commSaveBool(Constants.Communication_bools.SYM_ROTATIONAL, false);
                }

                /*
                 Test for vertical symmetry (reflect in middle vertical line)
                 */
                // -> HQs
                boolean vertical_possible = true;
                for (MapLocation hq : friendly_HQs) {
                    // If we can see the symmetric version of a location, check if it has an HQ
                    MapLocation sym = map_helper.verticalSymmetricLocation(hq);
//                    rc.setIndicatorDot(sym, 100, 123, 0);
                    if (rc.canSenseLocation(sym)) {
                        // It is in range!
                        if (rc.canSenseRobotAtLocation(sym)){
                            // There's a robot there ?!
                            // Is it an enemy HQ?
                            RobotInfo info = rc.senseRobotAtLocation(sym);
                            if (info.team != enemy || info.type != RobotType.HEADQUARTERS){
                                // Different robot type, so not possible
                                vertical_possible = false;
                                break;
                            }
                        } else {
                            // No robot found there... this symmetry is not possible
                            vertical_possible = false;
                            break;
                        }
                    }
                }

                // -> Map details
                if (vertical_possible){
                    int lower_y = height/2 - 1;
                    int upper_y = lower_y+1 + (height % 2 != 0 ? 1 : 0); // Account for odd height

                    for (int j = width; --j >= 0; ){
                        MapLocation lower = new MapLocation(j, lower_y);
                        MapLocation upper = new MapLocation(j, upper_y);
                        rc.setIndicatorDot(lower, 0,0,0);
                        rc.setIndicatorDot(upper, 110,110,110);

                        if (rc.canSenseLocation(lower) && rc.canSenseLocation(upper)){
                            // We can check the symmetry!!
                            MapInfo info_upper = rc.senseMapInfo(upper);
                            MapInfo info_lower = rc.senseMapInfo(lower);

                            if (info_upper.hasCloud() != info_lower.hasCloud() ||
                                    info_upper.isPassable() != info_lower.isPassable() ||
                                    info_upper.getCurrentDirection() != info_upper.getCurrentDirection()
                            ){
                                // Tiles are different! This symmetry is not possible!
                                vertical_possible = false;
                                break;
                            }
                        }
                    }

                }

                if (!vertical_possible){
                    // We have disproven possibility of vertical symmetry here
                    System.out.println("DISPROVEN VERTICAL");
                    commSaveBool(Constants.Communication_bools.SYM_VERTICAL, false);
                }



                /*
                 Test for horizontal symmetry (reflect in middle horizontal line)
                 */
                // -> HQs
                boolean horizontal_possible = true;
                for (MapLocation hq : friendly_HQs) {
                    // If we can see the symmetric version of a location, check if it has an HQ
                    MapLocation sym = map_helper.horizontalSymmetricLocation(hq);
//                    rc.setIndicatorDot(sym, 120, 12, 200);
                    if (rc.canSenseLocation(sym)) {
                        // It is in range!
                        if (rc.canSenseRobotAtLocation(sym)){
                            // There's a robot there ?!
                            // Is it an enemy HQ?
                            RobotInfo info = rc.senseRobotAtLocation(sym);
                            if (info.team != enemy || info.type != RobotType.HEADQUARTERS){
                                // Different robot type, so not possible
                                horizontal_possible = false;
                                break;
                            }
                        } else {
                            // No robot found there... this symmetry is not possible
                            horizontal_possible = false;
                            break;
                        }
                    }
                }

                // -> Map details TODO
//                if (vertical_possible){}

                if (!horizontal_possible){
                    // We have disproven possibility of horizontal symmetry here
                    System.out.println("DISPROVEN VERTICAL");
                    commSaveBool(Constants.Communication_bools.SYM_VERTICAL, false);
                }

                break;
            default:
                // Uncomment to try out how much bytecode something costs
//                int before = Clock.getBytecodesLeft();
//
//
//                int after = Clock.getBytecodesLeft();
//                int diff = before - after;
////                System.out.println("USED " + diff + " BYTECODE" + (turnCountStart != turnCount ? ", WENT OVER LIMIT!!!" : ""));
//                if (diff > mostBytecodeExtracted) {
//                    mostBytecodeExtracted = diff;
//                    System.out.println("new bytecode record :(  " + diff);
//                }
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
            // tryToBuild(RobotType.CARRIER);
            if (rng.nextBoolean()) {
                build_carrier(ResourceType.MANA);
            } else {
                build_carrier(ResourceType.ADAMANTIUM);
            }
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
            if (rc.canSenseLocation(loc) && !rc.isLocationOccupied(loc) && rc.sensePassability(loc)) {
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

    // Build carrier of certain resource type
    void build_carrier(ResourceType type) throws GameActionException{
        tryToBuild(RobotType.CARRIER);
        assign_carrier(type, HQ_id);
    }
}