package first_bot.bots;

import battlecode.common.*;

import java.util.Arrays;
import java.util.Comparator;

import static first_bot.util.Constants.directions;

public class Launcher extends Robot{
    public Launcher(RobotController rc) {
        super(rc);
    }

    /**
     * This code is run once per turn (assuming we do not go over bytecode limits.)
     *
     * @throws GameActionException if an illegal game action is performed.
     */
    @Override
    void run() throws GameActionException {
        // Try to attack someone
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);

        // Sort enemies by hp
        Arrays.sort(enemies, Comparator.comparingInt(o -> o.health));

        if (enemies.length > 0) {
             MapLocation toAttack = enemies[0].location;


            if (rc.canAttack(toAttack)) {
                rc.setIndicatorString("Attacking");
                rc.attack(toAttack);
            }
        }

        // Also try to move randomly.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
    }
}
