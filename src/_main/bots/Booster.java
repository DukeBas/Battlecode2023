package _main.bots;

import battlecode.common.*;

import java.util.HashSet;

import _main.util.Constants;

public class Booster extends Robot{
    public Booster(RobotController rc) {
        super(rc);
    }

    /**
     * This code is run once per turn (assuming we do not go over bytecode limits.)
     *
     * @throws GameActionException if an illegal game action is performed.
     */
    @Override
    void run() throws GameActionException {
        scan();

        // Run away from enemy launchers!
        RobotInfo[] enemies = rc.senseNearbyRobots(RobotType.LAUNCHER.actionRadiusSquared, enemy);
        int enemy_launcher = 0;
        for (RobotInfo r : enemies) {
            if (r.getType() == RobotType.LAUNCHER) {
                enemy_launcher++;
            }
        }

        if (enemy_launcher > 0) {
            MapLocation enemy = enemies[0].getLocation();
            Direction dir = combatPathing.tryDirection(rc.getLocation().directionTo(enemy).opposite());
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
        } else {
            // Move towards the most friendly launchers
            move_towards_launchers();
        }

        if (rc.canBoost()) {
            rc.boost();
        }
    }

    public void move_towards_launchers() throws GameActionException {
        HashSet<Direction> launcher_directions = new HashSet<Direction>();
        RobotInfo[] friendlies = rc.senseNearbyRobots(-1, enemy);
        for (RobotInfo r : friendlies) {
            launcher_directions.add(rc.getLocation().directionTo(r.getLocation()));
        }
        
        // Try all directions, choose one with maximal directions
        Direction best = Direction.CENTER;
        int max_launchers = Integer.MIN_VALUE;
        for (Direction l_loc : launcher_directions) {
            int count = rc.senseNearbyRobots(rc.getLocation().add(l_loc), RobotType.LAUNCHER.actionRadiusSquared, friendly).length;
            if (count > max_launchers) {
                best = l_loc;
                max_launchers = count;
            }
        }

        if (rc.canMove(best)) {
            rc.move(best);
        }
    }
}
