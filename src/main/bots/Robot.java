package main.bots;

import battlecode.common.*;
import battlecode.world.Well;
import main.util.Pathfinding;
import main.util.SimplePathing;

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


    static int turnCount = 0; // Number of turns this bot has been alive
    static int MAX_WELLS = 16;
    static int MAX_HQS = 4; // accounts only for one team, so max HQ 4 means 8 HQs in total on the map

    // Sets that hold messages that we could not send before
    HashSet<Integer> hq_messages;
    HashSet<Integer> well_messages;


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
            System.out.println(e);
        }
        pathfinding = new SimplePathing(rc);

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
        turnCount++;
        scan();
        this.run();
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
        if (rc.canMove(direction)) {
            rc.move(direction);
        } else {
            rc.setIndicatorString("oopsy doopsy, i cannot move " + direction.toString() + " there :(");
        }
    }

    // Get HQ location
    private MapLocation getHQ() throws GameActionException {
        RobotInfo[] friendlies = rc.senseNearbyRobots(2, friendly);
        MapLocation HQ = null;
        for (RobotInfo robot : friendlies) {
            if (robot.type == RobotType.HEADQUARTERS) {
                HQ = robot.getLocation();
            }
        }
        return HQ;
    }

    // Scan for interesting structures and store them
    // TODO: scan for islands
    private void scan() throws GameActionException {

        // Scan for wells and store them
        WellInfo[] wells = rc.senseNearbyWells();
        for (WellInfo well : wells) {
            int well_code = encode_well(well);
            store_well_info(well_code);
        }

        RobotInfo[] hqs = rc.senseNearbyRobots(rc.getLocation(), -1, enemy);
        for (RobotInfo hq : hqs) {
            if (hq.type == RobotType.HEADQUARTERS) {
                int hq_code = encode_hq(hq);
                store_hq_info(hq_code);
            }
        }
    }

    // Convert well info into binary, then into decimal and return
    // first 2 bits correspond to resource
    // the next 7 bits correspond to x
    // the next 7 correspond to y
    private int encode_well(WellInfo wellinfo) {
        String resource_code = "";
        ResourceType type = wellinfo.getResourceType();
        if (type == ResourceType.ADAMANTIUM) {
            resource_code = String.format("%2s", Integer.toBinaryString(1)).replace(' ', '0');
        } else if (type == ResourceType.ELIXIR) {
            resource_code = String.format("%2s", Integer.toBinaryString(2)).replace(' ', '0');
        } else if (type == ResourceType.MANA) {
            resource_code = String.format("%2s", Integer.toBinaryString(3)).replace(' ', '0');
        }

        MapLocation loc = wellinfo.getMapLocation();
        String location_code = "";
        location_code = location_code + String.format("%7s", Integer.toBinaryString(loc.x)).replace(' ', '0');
        location_code = location_code + String.format("%7s", Integer.toBinaryString(loc.y)).replace(' ', '0');
        return Integer.parseInt(resource_code + location_code, 2);
    }

    // Get well location from the decimal code.
    public MapLocation decode_well_location(Integer wellcode) {
        String code_binary = String.format("%16s", Integer.toBinaryString(wellcode)).replace(' ', '0');
        int x = Integer.parseInt(code_binary.substring(2, 9), 2);
        int y = Integer.parseInt(code_binary.substring(9, 16), 2);
        return new MapLocation(x, y);
    }

    // Get well resource type from decimal code.
    public ResourceType decode_well_resourceType(Integer wellcode) {
        String code_binary = String.format("%16s", Integer.toBinaryString(wellcode)).replace(' ', '0');
        int int_code = Integer.parseInt(code_binary.substring(0, 2), 2);

        ResourceType type = null;
        if (int_code == 1) {
            type = ResourceType.ADAMANTIUM;
        } else if (int_code == 2) {
            type = ResourceType.ELIXIR;
        } else if (int_code == 3) {
            type = ResourceType.MANA;
        }
        return type;
    }

    // Checks if wellcode is duplicate, and if not stores it.
    private void store_well_info(Integer wellcode) throws GameActionException {
        for (int i = 0; i < MAX_WELLS; i++) {
            int read = rc.readSharedArray(i);
            if (read == wellcode) {
                return;
            }
        }

        // The entry does not exist yet, so save it
        well_messages.add(wellcode);
    }

    // Encode hqinfo into integer, first 8 bits are x, second 8 bits are y
    private int encode_hq(RobotInfo hq) {
        MapLocation loc = hq.getLocation();
        String location_code = "";
        location_code = location_code + String.format("%8s", Integer.toBinaryString(loc.x)).replace(' ', '0');
        location_code = location_code + String.format("%8s", Integer.toBinaryString(loc.y)).replace(' ', '0');
        return Integer.parseInt(location_code, 2);
    }

    // decode location of hq from integer num.
    public MapLocation decode_hq_location(Integer hq_code) {
        String code_binary = String.format("%16s", Integer.toBinaryString(hq_code)).replace(' ', '0');
        int x = Integer.parseInt(code_binary.substring(0, 8), 2);
        int y = Integer.parseInt(code_binary.substring(8, 16), 2);
        return new MapLocation(x, y);
    }

    // Checks if hq_code is duplicate, and if not stores it.
    private void store_hq_info(Integer hq_code) throws GameActionException {
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
        if (rc.canWriteSharedArray(0, 0)) {
            // We can send messages!

            if (!hq_messages.isEmpty()){
                // Remove an hq if we know it already
                for (int i = MAX_WELLS; i < MAX_WELLS + MAX_HQS; i++) {
                    int read = rc.readSharedArray(i);
                    hq_messages.remove(read);
                }
                // Store enemy HQs
                for (Integer m : hq_messages) {
                    for (int i = MAX_WELLS; i < MAX_WELLS + MAX_HQS; i++) {
                        int read = rc.readSharedArray(i);
                        if (read == 0){ // we found an empty spot!
                            rc.writeSharedArray(i, m);
                        }
                    }
                }
            }

            if (!well_messages.isEmpty()){
                // Remove an hq if we know it already
                for (int i = 0; i < MAX_WELLS; i++) {
                    int read = rc.readSharedArray(i);
                    well_messages.remove(read);
                }
                // Store enemy HQs
                for (Integer m : well_messages) {
                    for (int i = 0; i < MAX_WELLS; i++) {
                        int read = rc.readSharedArray(i);
                        if (read == 0){ // we found an empty spot!
                            rc.writeSharedArray(i, m);
                        }
                    }
                }
            }
        }
    }
}
