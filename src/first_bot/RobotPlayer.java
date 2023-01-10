package first_bot;

import battlecode.common.*;
import first_bot.bots.*;


/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public strictfp class RobotPlayer {
    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc The RobotController object. You use it to perform actions from this robot, and to get
     *           information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        // Depending on the type of the bot, create a robot of that type
        final Robot robot;
        switch (rc.getType()) {
            case HEADQUARTERS:
                robot = new HQ(rc);
                break;
            case CARRIER:
                robot = new Carrier(rc);
                break;
            case LAUNCHER:
                robot = new Launcher(rc);
                break;
            case BOOSTER:
                robot = new Booster(rc);
                break;
            case DESTABILIZER:
                robot = new Destabilizer(rc);
                break;
            case AMPLIFIER:
                robot = new Amplifier(rc);
                break;
            default:
                robot = null;
                break;
        }

        robot.runGameLoop();
    }
}
