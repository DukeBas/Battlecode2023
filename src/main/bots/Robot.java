package main.bots;

import battlecode.common.*;
import main.util.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public abstract class Robot {
    /**
     * Generic variables
     */
    RobotController rc;
    RobotType ownType;
    Team friendly;
    Team enemy;
    MapLocation built_by;
    Pathfinding pathfinding;


    /*
        COMMUNICATION VARIABLES
     */
    // Sets that hold messages that we could not send before
    HashSet<Integer> hq_messages;
    HashSet<Integer> well_messages;
    static int MAX_WELLS = 16;
    static int MAX_HQS = 4; // accounts only for one team, so max HQ 4 means 8 HQs in total on the map

    // Indices for in the shared array
    static int RESERVED_SPOT_BOOLS = 0;
    static int START_INDEX_ROLE_ASSIGNMENT = RESERVED_SPOT_BOOLS + 1;
    static int START_INDEX_FRIENDLY_HQS = START_INDEX_ROLE_ASSIGNMENT + 1;
    static int START_INDEX_ENEMY_HQS = START_INDEX_FRIENDLY_HQS + MAX_HQS;
    static int START_INDEX_WELLS = START_INDEX_ENEMY_HQS + MAX_HQS;

    // #numRESERVERD_SPOTS(=1) + MAX_HQS*2 + MAX_WELLS <= 64 !!!

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static final Random rng = new Random(1234);

    public Robot(RobotController rc) {
        this.rc = rc;
        this.ownType = rc.getType();
        this.friendly = rc.getTeam();
        this.enemy = friendly.opponent();
        // TODO: navigate to the closest spot around HQ instead of spot where built
        try {
            this.built_by = getHQ();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Set the right pathfinding module for each bot
        switch (rc.getType()) {
            case HEADQUARTERS:
                // Doesn't need pathfinding..
                break;
            case LAUNCHER:
                // Has higher than 20 range..
                pathfinding = new BugPathing(rc);
            default:
                pathfinding = new CombinedPDFS20Bug(rc);
        }

        hq_messages = new HashSet<>();
        well_messages = new HashSet<>();
    }

    /**
     * Runs the game loop for the rest of the game and does not return.
     */
    public void runGameLoop() {
        /* This code runs during the entire lifespan of the robot, which is why it is in an infinite
         * loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
         * loop, we call Clock.yield(), signifying that we've done everything we want to do.
         */

        //noinspection InfiniteLoopStatement
        while (true) {
            try {
                // Execute 1 round of actions for this robot.
                this._run();

            } catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You should
                // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                System.out.println(ownType + " GameAction-Exception: " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println("--- BEGIN ERROR ---");
                System.out.println(ownType + " " + e.getClass() + " " + e.getMessage());
                e.printStackTrace();
            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
            // End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
        }

        // Execution should never reach here. (unless intentional) Self-destruction imminent!
    }

    /**
     * Run 1 round of this robot, including the actions beformed before and after by this super class.
     *
     * @throws GameActionException if an illegal game action is performed.
     */
    private void _run() throws GameActionException {
        int turn = rc.getRoundNum();
        sendCommunicationBuffer();
        this.run();

        // Check if we went over bytecode limit last turn, i.e. could not complete a turn
        if (turn != rc.getRoundNum()) {
            rc.setIndicatorDot(rc.getLocation(), 123, 234, 10);
            System.out.println("WENT OVER BYTECODE LIMIT!!! turn " + turn);
        }
    }

    /**
     * Run 1 round of this robot.
     *
     * @throws GameActionException if an illegal game action is performed.
     */
    abstract void run() throws GameActionException;

    // Move towards a Maplocation object
    public void move_towards(MapLocation goal_location) throws GameActionException {
        move_towards(pathfinding.getDirection(goal_location));
    }

    // Move towards a Direction
    public void move_towards(Direction direction) throws GameActionException {
        if (rc.isMovementReady()) {
            if (rc.canMove(direction)) {
                rc.move(direction);
            } else {
                rc.setIndicatorString("oopsy doopsy, i cannot move " + direction.toString() + " there :(");
            }
        }
    }

    // Get touching HQ location
    private MapLocation getHQ() throws GameActionException {
        RobotInfo[] friendlies = rc.senseNearbyRobots(8, friendly);
        MapLocation HQ = null;
        for (RobotInfo robot : friendlies) {
            if (robot.type == RobotType.HEADQUARTERS) {
                HQ = robot.getLocation();
                break;
            }
        }
        return HQ;
    }

    // Scan for interesting structures and store them
    // TODO: scan for islands
    public void scan() throws GameActionException {
        if (Clock.getBytecodesLeft() < 100) return; // Check if we are nearly out of bytecode

        // Scan for wells and store them
        WellInfo[] wells = rc.senseNearbyWells();
        for (WellInfo well : wells) {
            int well_code = encode_well(well);
            if (Clock.getBytecodesLeft() < 200) return; // Check if we are nearly out of bytecode
            store_well_info(well_code);
        }

        RobotInfo[] hqs = rc.senseNearbyRobots(-1, enemy);
        for (RobotInfo hq : hqs) {
            if (Clock.getBytecodesLeft() < 200) return; // Check if we are nearly out of bytecode
            if (hq.type == RobotType.HEADQUARTERS) {
                int hq_code = encode_HQ_location(hq.getLocation());
                store_hq_info(hq_code);
            }
        }
    }

    // Convert well info into binary, then into decimal and return
    // first 2 bits correspond to resource
    // the next 7 bits correspond to x
    // the next 7 correspond to y
    public int encode_well(WellInfo wellinfo) {
        int well_code = 0;
        ResourceType type = wellinfo.getResourceType();

        // encode type as first two bits
        switch (type) {
            case ADAMANTIUM:
                well_code += 1 << 14;
                break;
            case ELIXIR:
                well_code += 2 << 14;
                break;
            case MANA:
                well_code += 3 << 14;
                break;
        }

        MapLocation loc = wellinfo.getMapLocation();
        // encode x as next 7 bits
        well_code += loc.x << 7;
        // encode y as last 7 bits
        well_code += loc.y;

        return well_code;
    }

    // Get well location from the decimal code.
    public MapLocation decode_well_location(Integer wellcode) {
        int x = (wellcode & 0b0011111110000000) >> 7;
        int y = wellcode & 0b0000000001111111;

        return new MapLocation(x, y);
    }

    // Get well resource type from decimal code.
    public ResourceType decode_well_resourceType(Integer wellcode) {
        int int_code = (wellcode & 0b1100000000000000) >> 14;

        ResourceType type;
        switch (int_code) {
            case 1:
                type = ResourceType.ADAMANTIUM;
                break;
            case 2:
                type = ResourceType.ELIXIR;
                break;
            case 3:
                type = ResourceType.MANA;
                break;
            default:
                type = ResourceType.NO_RESOURCE;
                break;
        }

        return type;
    }

    // Checks if wellcode is duplicate, and if not stores it.
    public void store_well_info(Integer wellcode) throws GameActionException {
        for (int i = 0; i < MAX_WELLS; i++) {
            int read = rc.readSharedArray(i);
            if (read == wellcode) {
                return;
            }
        }

        // The entry does not exist yet, so save it
        well_messages.add(wellcode);
    }

    public MapLocation get_nearest_well(ResourceType type) throws GameActionException {
        MapLocation closest = null;
        int min_distance = Integer.MAX_VALUE;
        for (int i = START_INDEX_WELLS; i < START_INDEX_WELLS + MAX_WELLS; i++) {
            int info = rc.readSharedArray(i);
            if (info == 0) {
                return closest;
            }

            if (decode_well_resourceType(info) == type) {
                MapLocation loc = decode_well_location(info);
                int distance = loc.distanceSquaredTo(rc.getLocation());
                if (distance < min_distance) {
                    min_distance = distance;
                    closest = loc;
                }
            }
        }
        return closest;
    }

    // Encode hqinfo into integer, first 6 bits are x, second 6 bits are y
    int encode_HQ_location(MapLocation loc) {
        int hq_code = 0;

        hq_code += loc.x << 6;
        hq_code += loc.y;

        return hq_code;
    }

    // decode location of hq from integer num.
    public MapLocation decode_hq_location(Integer hq_code) {
        int x = (hq_code & 0b0000111111000000) >> 6;
        int y = (hq_code & 0b0000000000111111);

        return new MapLocation(x, y);
    }

    public MapLocation[] getFriendlyHQLocations() throws GameActionException {
        ArrayList<MapLocation> friendlyHQs = new ArrayList<>();
        for (int i = START_INDEX_FRIENDLY_HQS; i < START_INDEX_FRIENDLY_HQS + MAX_HQS; i++) {
            int read = rc.readSharedArray(i);
            if (read != 0) {
                friendlyHQs.add(decode_hq_location(read));
            }
        }
        return friendlyHQs.toArray(new MapLocation[]{});
    }

    // Checks if hq_code is duplicate, and if not stores it.
    public void store_hq_info(Integer hq_code) throws GameActionException {
        for (int i = MAX_WELLS; i < MAX_WELLS + MAX_HQS; i++) {
            int read = rc.readSharedArray(i);
            if (read == hq_code) {
                return;
            }
        }

        // The entry does not exist yet, so save it
        hq_messages.add(hq_code);

    }

    // Sends all outstanding messages, if it is possible
    private void sendCommunicationBuffer() throws GameActionException {
        if (Clock.getBytecodesLeft() < 400) return; // Check if we are nearly out of bytecode

        if (rc.canWriteSharedArray(0, 0)) {
            // We can send messages!

            if (!hq_messages.isEmpty()) {
                // Remove an hq if we know it already
                for (int i = START_INDEX_ENEMY_HQS; i < START_INDEX_ENEMY_HQS + MAX_HQS; i++) {
                    int read = rc.readSharedArray(i);
                    hq_messages.remove(read);
                }
                // Store enemy HQs
                for (Integer m : hq_messages) {
                    for (int i = START_INDEX_ENEMY_HQS; i < START_INDEX_ENEMY_HQS + MAX_HQS; i++) {
                        if (Clock.getBytecodesLeft() < 200) return; // Check if we are nearly out of bytecode

                        int read = rc.readSharedArray(i);
                        if (read == 0) { // we found an empty spot!
                            System.out.println("Writing " + m + " for HQ from " + decode_hq_location(m) + " to index " + i);
                            rc.writeSharedArray(i, m);
                            break;
                        }
                    }
                }
                hq_messages.clear();
            }

            if (Clock.getBytecodesLeft() < 400) return; // Check if we are nearly out of bytecode

            if (!well_messages.isEmpty()) {
                // Remove an HQ if we know it already
                for (int i = START_INDEX_WELLS; i < START_INDEX_WELLS + MAX_WELLS; i++) {
                    int read = rc.readSharedArray(i);
                    well_messages.remove(read);
                }
                // Store enemy HQs
                for (Integer m : well_messages) {
                    for (int i = START_INDEX_WELLS; i < START_INDEX_WELLS + MAX_WELLS; i++) {
                        if (Clock.getBytecodesLeft() < 200) return; // Check if we are nearly out of bytecode

                        int read = rc.readSharedArray(i);
                        if (read == 0) { // we found an empty spot!
                            System.out.println("Writing " + m + " for " + decode_well_resourceType(m) + " well from " + decode_well_location(m) + " to index " + i);
                            rc.writeSharedArray(i, m);
                            break;
                        }
                    }
                }
                well_messages.clear();
            }
        }
    }

    // Save a boolean to the shared array
    void commSaveBool(Constants.Communication_bools type, boolean b) throws GameActionException {
        int index = type.ordinal();
        int shared = rc.readSharedArray(RESERVED_SPOT_BOOLS);
        int updated = shared;

        if (b) {
            // set bit at position index to 1
            updated |= 1 << index;
        } else {
            // set bit at position index to 0
            updated &= ~(1 << index);
        }

        // write back, if changed
        if (shared != updated) {
            rc.writeSharedArray(RESERVED_SPOT_BOOLS, updated);
        }
    }


    // Read a boolean from the shared array
    boolean commReadBool(Constants.Communication_bools type) throws GameActionException {
        int index = type.ordinal();
        int shared = rc.readSharedArray(RESERVED_SPOT_BOOLS);
        return 1 == ((shared >> index) & 1);
    }

    // TODO: bitwise
    ResourceType decode_HQ_resource_assignment(int HQ_id) throws GameActionException {
        String hq_data = String.format("%16s", Integer.toBinaryString(rc.readSharedArray(START_INDEX_ROLE_ASSIGNMENT))).replace(' ', '0');
        hq_data = hq_data.substring(4 * HQ_id, 4 * HQ_id + 2);
        ResourceType type = ResourceType.ADAMANTIUM;
        switch (hq_data) {
            case "01":
                type = ResourceType.ADAMANTIUM;
                break;
            case "10":
                type = ResourceType.ELIXIR;
                break;
            case "11":
                type = ResourceType.MANA;
                break;
        }
        return type;
    }

    int get_HQ_id(MapLocation hq_location) throws GameActionException {
        int HQ_id = -1;
        for (int i = START_INDEX_FRIENDLY_HQS; i < START_INDEX_ENEMY_HQS; i++) {
            MapLocation current_hq_loc = decode_hq_location(rc.readSharedArray(i));
            if (current_hq_loc.equals(hq_location)) {
                HQ_id = i - START_INDEX_FRIENDLY_HQS;
            }
        }
        return HQ_id;
    }

    // Each HQ gets 4 bits.
    // First two bits are to assign carriers
    // TODO: use second two bits to assign launchers
    // TODO: bitwise
    void assign_carrier(ResourceType type, int HQ_id) throws GameActionException {
        String assignment = "";
        switch (type) {
            case NO_RESOURCE:
                assignment = "00";
                break;
            case ADAMANTIUM:
                assignment = "01";
                break;
            case ELIXIR:
                assignment = "10";
                break;
            case MANA:
                assignment = "11";
                break;
        }
        //Adding bits for launcher assignment.
        assignment += "00";

        int stored = rc.readSharedArray(START_INDEX_ROLE_ASSIGNMENT);
        String current = String.format("%16s", Integer.toBinaryString(stored)).replace(' ', '0');
        StringBuffer buf = new StringBuffer(current);
        buf.replace(4 * HQ_id, 4 * HQ_id + 4, assignment);
        current = buf.toString();

        rc.writeSharedArray(START_INDEX_ROLE_ASSIGNMENT, Integer.valueOf(current, 2));
    }

    public MapLocation get_nearest_enemy_HQ() throws GameActionException {
        MapLocation closest_hq = null;
        int min_dist = Integer.MAX_VALUE;
        for (int i = START_INDEX_ENEMY_HQS; i < START_INDEX_ENEMY_HQS + MAX_HQS; i++) {
            int hq_code = rc.readSharedArray(i);
            if (hq_code == 0) {
                return closest_hq;
            }
            MapLocation hq_loc = decode_hq_location(hq_code);
            int distance = hq_loc.distanceSquaredTo(rc.getLocation());
            if (distance < min_dist) {
                closest_hq = hq_loc;
                min_dist = distance;
            }
        }
        return closest_hq;
    }

}
