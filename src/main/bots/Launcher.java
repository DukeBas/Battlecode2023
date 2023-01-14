package main.bots;

import battlecode.common.*;

import java.util.Arrays;
import java.util.Comparator;

import static first_bot.util.Constants.directions;

public class Launcher extends Robot {

    MapLocation Hq_target = null;
    Boolean squad = false;
    int HQ_id = -1;

    public Launcher(RobotController rc) throws GameActionException {
        super(rc);
        HQ_id = get_HQ_id(built_by);
    }

    /**
     * This code is run once per turn (assuming we do not go over bytecode limits.)
     *
     * @throws GameActionException if an illegal game action is performed.
     */
    @Override
    void run() throws GameActionException {
        attack();

        if (get_number_of_launchers() > 1 || squad) {
            squad = true;
            if (Hq_target == null) {
                Hq_target = get_nearest_enemy_HQ();
            }
            if (Hq_target != null) {
                move_towards(Hq_target);
            }
        }
        scan();
    }

    public int get_number_of_launchers() throws GameActionException {
        int count = 0;
        RobotInfo[] robots = rc.senseNearbyRobots(5, friendly);
        for (RobotInfo robot : robots) {
            if (robot.getType() == RobotType.LAUNCHER) {
                count++;
            }
        }
        return count;
    }

    public void attack() throws GameActionException {
        // Try to attack someone
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);

//        // Sort enemies by hp
//        Arrays.sort(enemies, Comparator.comparingInt(o -> o.health));

        if (enemies.length > 0) {
            RobotInfo toAttack = enemies[0];
            int lowestHP = Integer.MAX_VALUE; // needs separate value as initial target might be HQ

            for (RobotInfo r : enemies) {
                if (r.getType() == RobotType.HEADQUARTERS) continue;

                if (r.getHealth() < lowestHP){
                    lowestHP = r.getHealth();
                    toAttack = r;
                }
            }

            MapLocation loc = toAttack.getLocation();
            if (rc.canAttack(loc)) {
                rc.setIndicatorString("Attacking");
                rc.attack(loc);
            }
        }
    }
}
