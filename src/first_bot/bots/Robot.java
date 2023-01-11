package first_bot.bots;

import battlecode.common.*;
import battlecode.world.Well;
import first_bot.util.Pathfinding;
import first_bot.util.SimplePathing;

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
    // Number of turns this bot has been alive
    static int turnCount = 0;
    static int MAX_WELLS = 5;

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static final Random rng = new Random(6147);

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
                System.out.println(ownType + " GameAction-Exception");
                e.printStackTrace();

                rc.setIndicatorString("G-E: " + e);
            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println(ownType + " Generic-Exception");
                e.printStackTrace();

                rc.setIndicatorString(e.getStackTrace()[0].getMethodName() + " : " + e);
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
        RobotInfo friendlies[] = rc.senseNearbyRobots(2, friendly);
        MapLocation HQ = null;
        for (RobotInfo robot : friendlies) {
            if (robot.type == RobotType.HEADQUARTERS) {
                HQ = robot.getLocation();
            }
        }
        return HQ;
    }

    // Scan for wells and store info
    // TODO: scan for other shit
    private void scan() throws GameActionException{
        WellInfo[] wells = rc.senseNearbyWells();
        for (WellInfo well : wells) {
            int well_code = encode_well(well);
            // store_wellinfo(well_code);
            int x = rc.readSharedArray(4);
            rc.setIndicatorString(String.valueOf(x));
        }
    }

    private int encode_well(WellInfo wellinfo) {
        int code = 0;
        ResourceType type = wellinfo.getResourceType();
        if (type == ResourceType.ADAMANTIUM) {
            code = 1;
        } else if (type == ResourceType.ELIXIR) {
            code = 2;
        } else if (type == ResourceType.MANA) {
            code = 3;
        }

        MapLocation loc = wellinfo.getMapLocation();
        code = ((int) Math.pow(2,2)) + loc.x;
        code = ((int) Math.pow(2,3)) + loc.y;
        return code;
    }

    private MapLocation decode_well_location (Integer wellcode) {
        
    }

    private ResourceType decode_well_resourceType (Integer wellcode) {

    }

    // private void store_wellinfo(Integer wellcode) {
    //     boolean duplicate = false;
    //     for (int i = 0; i < MAX_WELLS; i++) {
    //         int read = rc.readSharedArray(i);
    //         if (read == wellcode) {
    //             if ()
    //         }
    //     }
    // }
}
