package _main.bots;

import _main.util.SimplePathing;
import battlecode.common.*;

public class Launcher extends Robot {

    MapLocation Hq_target = null;
    Boolean squad = false;
    int HQ_id;
    int turnsInCombat = 0;
    SimplePathing combatPathing;

    public Launcher(RobotController rc) throws GameActionException {
        super(rc);
        HQ_id = get_HQ_id(built_by);
        combatPathing = new SimplePathing(rc);
    }

    /**
     * This code is run once per turn (assuming we do not go over bytecode limits.)
     *
     * @throws GameActionException if an illegal game action is performed.
     */
    @Override
    void run() throws GameActionException {
        attack();

        // Check if we are in a squad
        if (get_number_of_launchers() > 1 || squad) {
            squad = true;
            if (Hq_target == null) {
                Hq_target = get_nearest_enemy_HQ();
            }
        }

        /*
        Movement plan:
        - If there are more enemy combatants than friendly ones, retreat // TODO what to do with enemy destabilizers?
        - If it is equal, don't move
        - If we have a lot more units, do normal movement
         - If there are no enemies nearby
            -> Move towards group location if not in a squad, else move towards goal if we have one
         */
        if (rc.isMovementReady()) {
            RobotInfo[] friendlies = rc.senseNearbyRobots(-1, friendly);
            int num_seen_friendly_combatants = 1; // we ourselves are one
            for (RobotInfo r : friendlies) {
                RobotType type = r.getType();
                if (type == RobotType.LAUNCHER) num_seen_friendly_combatants++;
            }
            RobotInfo[] close_friendlies = rc.senseNearbyRobots(7, friendly);
            int num_close_friendly_combatants = 1; // we ourselves are one
            for (RobotInfo r : close_friendlies) {
                RobotType type = r.getType();
                if (type == RobotType.LAUNCHER) num_close_friendly_combatants++;
            }
            RobotInfo[] enemies = rc.senseNearbyRobots(-1, enemy);
            int num_seen_enemy_combatants = 0;
            for (RobotInfo r : enemies) {
                RobotType type = r.getType();
                if (type == RobotType.LAUNCHER) num_seen_enemy_combatants++;
            }
            RobotInfo[] attackable_enemies = rc.senseNearbyRobots(RobotType.LAUNCHER.actionRadiusSquared, enemy);
            int num_attackable_enemy_combatants = 0;
            for (RobotInfo r : attackable_enemies) {
                RobotType type = r.getType();
                if (type == RobotType.LAUNCHER) num_attackable_enemy_combatants++;
            }

            if (num_seen_enemy_combatants > 0) {
                // There's enemies nearby!
                turnsInCombat++;
                if (num_attackable_enemy_combatants > 1 || num_close_friendly_combatants < num_seen_enemy_combatants) {
                    // We are outnumbered! Retreat!
                    // TODO: retreat properly instead of from the first enemy of array
                    rc.setIndicatorString("I'm outta here. " + num_seen_enemy_combatants + " enemies and " + num_seen_friendly_combatants + " friendlies");
                    Direction dir = combatPathing.tryDirection(
                            rc.getLocation().directionTo(enemies[0].getLocation()).opposite());
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                    }

                } else if (num_seen_friendly_combatants - 2 > num_seen_enemy_combatants) {
                    // Move to enemy when ready
                    if (squad && Hq_target != null) {
                        rc.setIndicatorString("Heading towards enemy HQ at " + Hq_target);
                        move_towards(Hq_target);
                    } else {
                        move_towards(getGroupingLocation());
                    }
                }

                // If there is one enemy is vision (but not attacking range), move in //TODO: just enough to strike first
                if (num_attackable_enemy_combatants == 0 && num_seen_enemy_combatants == 1){
                    Direction dir = combatPathing.tryDirection(
                            rc.getLocation().directionTo(enemies[0].getLocation()));
                    rc.setIndicatorString("I'm outta here, so low. Going to " + dir);
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                    }
                }


            } else {
                // No enemies nearby, do normal movement
                turnsInCombat = 0;
                // Move to enemy when ready
                if (squad && Hq_target != null) {
                    rc.setIndicatorString("Heading towards enemy HQ at " + Hq_target);
                    move_towards(Hq_target);
                } else {
                    move_towards(getGroupingLocation());
                }
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


    // Get location used for squads to wait at
    // TODO: account for blocked grouping location..
    MapLocation getGroupingLocation() {
        return new MapLocation(
                built_by.x < rc.getMapWidth() / 2 ? built_by.x + 2 : built_by.x - 2,
                built_by.y < rc.getMapHeight() / 2 ? built_by.y + 2 : built_by.y - 2
        );
    }
}
