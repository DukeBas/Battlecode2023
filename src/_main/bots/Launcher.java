package _main.bots;

import battlecode.common.*;

public class Launcher extends Robot {

    MapLocation target_location = null;
    int HQ_id;
    int turnsInCombat = 0;

    boolean[] disabledEnemyHQs = new boolean[]{false, false, false, false};
    MapLocation guardingEnemyHQLocation = null;


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


        MapLocation nearest_known_HQ = get_nearest_enemy_HQ(disabledEnemyHQs);

        if (nearest_known_HQ != null) {
            target_location = nearest_known_HQ;
        } else {
            // Is there a global target?
            int read = rc.readSharedArray(START_INDEX_ATTACK_TARGET);
            if (read != 0) {
                target_location = decode_hq_location(read);
            }
        }

        // Guard enemy HQ if necessary
        if (guardingEnemyHQLocation != null) {
            rc.setIndicatorDot(rc.getLocation(), 0, 0, 0);
            // Stay near that HQ!
            if (target_location != guardingEnemyHQLocation) {
                target_location = guardingEnemyHQLocation;
            }
        } else {
            // Should we guard the enemy HQ?
            MapLocation ownLocation = rc.getLocation();
            if (target_location != null && rc.canSenseLocation(target_location)) {
                RobotInfo robot_at_loc = rc.senseRobotAtLocation(target_location);
                if (robot_at_loc != null && robot_at_loc.getType() == RobotType.HEADQUARTERS && robot_at_loc.getTeam() == enemy) {

                    // There's an enemy HQ!! Should we guard it?
                    if (ownLocation.distanceSquaredTo(target_location) <= 9) {// check to make sure we can see all tiles around it
                        if (rc.senseNearbyRobots(target_location, 2, friendly).length < 2) {
//                            System.out.println("IMMA GUARD " + target_location + " " + rc.senseNearbyRobots(target_location, 2, friendly).length);
                            // Not enough guards, we should guard it
                            guardingEnemyHQLocation = target_location;
                        } else {
                            // Enough guards, mark as disabled
                            // Find ID of HQ
                            for (int i = START_INDEX_ENEMY_HQS; i < START_INDEX_ENEMY_HQS + MAX_HQS; i++) {
                                if (target_location.equals(decode_hq_location(rc.readSharedArray(i)))) {
                                    // Set it as disabled, reset target
//                                    System.out.println(target_location + " IS DISABLED");
                                    disabledEnemyHQs[i - START_INDEX_ENEMY_HQS] = true;
                                    target_location = get_nearest_enemy_HQ(disabledEnemyHQs);
                                    break;
                                }
                            }
                        }
                    }
                }
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
            int num_enemy_hqs = 0;
            for (RobotInfo r : enemies) {
                RobotType type = r.getType();
                switch (type) {
                    case LAUNCHER:
                        num_seen_enemy_combatants++;
                        break;
                    case HEADQUARTERS:
                        num_enemy_hqs++;
                }
            }
            RobotInfo[] attackable_enemies = rc.senseNearbyRobots(RobotType.LAUNCHER.actionRadiusSquared, enemy);
            int num_attackable_enemy_combatants = 0;
            int num_enemy_HQs_in_attack_range = 0;
            for (RobotInfo r : attackable_enemies) {
                RobotType type = r.getType();
                switch (type) {
                    case LAUNCHER:
                        num_attackable_enemy_combatants++;
                        break;
                    case HEADQUARTERS:
                        num_enemy_HQs_in_attack_range++;
                }
            }


            if (num_seen_enemy_combatants > 0) {// There's enemy combatants nearby!
                turnsInCombat++;

                // Did we just join combat and are we damaged?
                if (rc.getHealth() <= 10 && turnsInCombat < 3) {
                    rc.setIndicatorString("Got hit hard, taking a step back");
                    Direction dir = combatPathing.tryDirection(
                            rc.getLocation().directionTo(enemies[0].getLocation()).opposite());
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                    }
                }


                if (num_attackable_enemy_combatants > 1 || num_close_friendly_combatants < num_seen_enemy_combatants) {
                    // We are outnumbered! Retreat!
                    // TODO: retreat properly instead of from the first enemy of array
                    rc.setIndicatorString("I'm outta here. " + num_seen_enemy_combatants + " enemies and " + num_seen_friendly_combatants + " friendlies");
                    Direction dir = combatPathing.tryDirection(
                            rc.getLocation().directionTo(enemies[0].getLocation()).opposite());
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                    }

                } else if (num_seen_friendly_combatants - 2 > num_seen_enemy_combatants && turnsInCombat >= 3) {
                    // Move to enemy when ready
                    if (target_location != null) {
                        rc.setIndicatorString(num_seen_enemy_combatants + "Heading towards target at " + target_location);
                        move_towards(target_location);
                    } else {
                        move_towards(getGroupingLocation());
                    }
                }

                // If there is one enemy is vision (but not attacking range), move in //TODO: just enough to strike first
                if (num_attackable_enemy_combatants == 0 && num_seen_enemy_combatants == 1
                        && turnsInCombat >= 3
                        && rc.getHealth() > 6) {
                    Direction dir = combatPathing.tryDirection(
                            rc.getLocation().directionTo(enemies[0].getLocation()));
                    rc.setIndicatorString("trying to attack! Going to " + dir);
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                        turnsInCombat = 0; // reset
                    }
                }


            } else {
                turnsInCombat = 0;
                if (enemies.length - num_enemy_hqs > 0) {
                    // Non combatants nearby, chaaaaseee!
                    rc.setIndicatorString("I'M SEEING JUICE");
                    move_towards(enemies[0].getLocation());
                } else {
                    // No enemies nearby, do normal movement
                    // Move to enemy when ready
                    if (target_location != null) {
                        rc.setIndicatorString("Heading towards target at " + target_location);
                        move_towards(target_location);
                    } else {
                        move_towards(getGroupingLocation());
                    }
                }
            }

            // Try to attack again now that we've moved
            if (rc.isActionReady()) {
                attack();
            }
        }

        scan();
    }


    // Get location used for squads to wait at
    // TODO: account for blocked grouping location..
    MapLocation getGroupingLocation() {
        return new MapLocation(
                rc.getMapWidth() / 2,
                rc.getMapHeight() / 2
        );
//        return new MapLocation(
//                built_by.x < rc.getMapWidth() / 2 ? built_by.x + 2 : built_by.x - 2,
//                built_by.y < rc.getMapHeight() / 2 ? built_by.y + 2 : built_by.y - 2
//        );
    }
}
