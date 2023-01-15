package _main.bots;

import battlecode.common.*;

public class Launcher extends Robot {

    MapLocation Hq_target = null;
    Boolean squad = false;
    int HQ_id;

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

        // Only move if there is nothing to attack
        if (rc.isActionReady()) {
            if (get_number_of_launchers() > 1 || squad) {
                squad = true;
                if (Hq_target == null) {
                    Hq_target = get_nearest_enemy_HQ();
                }
                if (Hq_target != null) {
                    rc.setIndicatorString("Heading towards enemy HQ at " + Hq_target);
                    move_towards(Hq_target);
                }
            }

            if (rc.isMovementReady()) {
                rc.setIndicatorString("Could find target, very sad");
                move_towards(getGroupingLocation());
            }

            // Try to attack again now that we've moved
            if (rc.isActionReady()) {
                attack();
            }
        }

        scan();
    }

    public int get_number_of_launchers() throws GameActionException {
        int count = 0;
        // Get launchers at grouping position
        RobotInfo[] robots = rc.senseNearbyRobots(getGroupingLocation(), 2, friendly);
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
                RobotType type = r.getType();
                if (type == RobotType.HEADQUARTERS) continue;

                if ((type == toAttack.type && r.getHealth() < lowestHP) ||
                        (type == RobotType.LAUNCHER && toAttack.type != RobotType.LAUNCHER) ||
                        toAttack.getType() == RobotType.HEADQUARTERS) {
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

    // Get location used for squads to wait at
    MapLocation getGroupingLocation(){
        return new MapLocation(
                built_by.x < rc.getMapWidth() / 2 ? built_by.x + 2 : built_by.x - 2,
                built_by.y < rc.getMapHeight() / 2 ? built_by.y + 2 : built_by.y - 2
        );
    }
}
