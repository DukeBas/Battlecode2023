package old_v5.bots;

import battlecode.common.*;

import java.util.Arrays;

import static first_bot.util.Constants.directions;

public class Carrier extends Robot {

    static int MAX_RESOURCES = 40;
    ResourceType resource;
    int HQ_id;
    MapLocation target_well = null;
    MapLocation current_hq;

    public Carrier(RobotController rc) throws GameActionException {
        super(rc);
        current_hq = built_by;
        HQ_id = get_HQ_id(built_by);
        resource = decode_HQ_resource_assignment(HQ_id);
        assign_carrier(ResourceType.NO_RESOURCE, HQ_id);
    }

    /**
     * This code is run once per turn (assuming we do not go over bytecode limits.)
     *
     * @throws GameActionException if an illegal game action is performed.
     */
    @Override
    void run() throws GameActionException {
        rc.setIndicatorString("I am a " + resource.toString() + " miner");

        MapLocation ownLocation = rc.getLocation();

        // If we are about to die, attack an enemy!!
        RobotInfo[] enemies_in_16R2 = rc.senseNearbyRobots(16, enemy); // Launcher attack radius
        int max_possible_incoming_damage = 0;
        for (RobotInfo r : enemies_in_16R2) {
            switch (r.getType()) {
                case LAUNCHER:
                    max_possible_incoming_damage += 6;
                    break;
                case DESTABILIZER:
                    max_possible_incoming_damage += 5;
                    break;
                default:
                    break;
            }
        }

        if (max_possible_incoming_damage >= rc.getHealth()) {
            // We can die now! Try to save ourselves by attacking if we can!
            if (getCarrierDamage(get_resource_count()) >= 1) {
                attack();
            }
        }

        // If early game, when you can kill an enemy, spend exactly that amount of resources to do it
        if (rc.getRoundNum() < 50 && rc.isActionReady()) {
            RobotInfo[] attackable = rc.senseNearbyRobots(RobotType.CARRIER.actionRadiusSquared, enemy);
            int max_damage = getCarrierDamage(get_resource_count());
            for (RobotInfo r : attackable) {
                if (r.getHealth() < max_damage) {
                    MapLocation loc = r.getLocation();
                    if (rc.canAttack(loc)) {
                        rc.attack(loc);
                    }
                }
            }
        }

        // Run away from enemy launchers!
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, enemy);
        int num_launchers = 0;
        for (RobotInfo r : enemies) {
            if (r.getType() == RobotType.LAUNCHER) {
                num_launchers++;
            }
        }
        if (num_launchers > 0) {
            MapLocation enemy = enemies[0].getLocation();
            Direction dir = combatPathing.tryDirection(ownLocation.directionTo(enemy).opposite());
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
            reassign_hq(dir.opposite());
        }

        if (rc.canTakeAnchor(built_by, Anchor.STANDARD)) {
            rc.takeAnchor(built_by, Anchor.STANDARD);
        }

        if (rc.getAnchor() != null) {
            // Have anchor, look for islands
            anchor_routine();
        } else if (get_resource_count() == MAX_RESOURCES) {
            // Resources full, head to HQ and deposit
            hq_routine();
        } else if (target_well != null) {
            // Head to well and collect resources
            well_routine();
        } else {
            // Assign target well
            target_well = get_nearest_well(resource);
            if (target_well != null) {
                well_routine();
            } else {
                if (rc.isMovementReady()) {
                    // Cant find well, move randomly
                    Direction dir = directions[rng.nextInt(directions.length)];
                    move_towards(dir);
                }
            }
        }

        if (max_possible_incoming_damage >= rc.getHealth()) {
            // We can die now! Try to save ourselves by attacking if we can!
            if (getCarrierDamage(get_resource_count()) >= 1) {
                attack();
            }
        }

        scan();
    }

    public void well_routine() throws GameActionException {
        if (rc.canCollectResource(target_well, 1)) {
            rc.collectResource(target_well, -1);
        } else {
            if (rc.isMovementReady()) {
                move_towards(target_well);
            }
        }
    }

    public void anchor_routine() throws GameActionException {
        // If I have an anchor singularly focus on getting it to the first island I see
        // TODO: improve it. Do not place an anchor if there's a lot of enemies around..
        int[] islands = rc.senseNearbyIslands();
        MapLocation island = null;
        int min_dist = Integer.MAX_VALUE;
        for (int id : islands) {
            // Only add possible island if it is unclaimed
            if (rc.senseTeamOccupyingIsland(id) == Team.NEUTRAL) {
                MapLocation[] thisIslandLocs = rc.senseNearbyIslandLocations(id);
                for (MapLocation island_loc : thisIslandLocs) {
                    int distance = island_loc.distanceSquaredTo(rc.getLocation());
                    if (distance < min_dist) {
                        min_dist = distance;
                        island = island_loc;
                    }
                }
            }
        }
        if (island != null) {
            if (rc.isMovementReady()) {
                move_towards(island);
            }
            if (rc.canPlaceAnchor()) {
                rc.setIndicatorString("Huzzah, placed anchor!");
                rc.placeAnchor();
            }
        } else {
            if (rc.isMovementReady()) {
                // Cant find well, move randomly
                Direction dir = directions[rng.nextInt(directions.length)];
                move_towards(dir);
            }
        }
    }

    public void hq_routine() throws GameActionException {
        if (get_resource_count() == MAX_RESOURCES) {
            // Resources full, pathfind to HQ
            if (rc.canTransferResource(built_by, ResourceType.ADAMANTIUM, 1)) {
                rc.transferResource(built_by, ResourceType.ADAMANTIUM, rc.getResourceAmount(ResourceType.ADAMANTIUM));
            } else if (rc.canTransferResource(built_by, ResourceType.MANA, 1)) {
                rc.transferResource(built_by, ResourceType.MANA, rc.getResourceAmount(ResourceType.MANA));
            } else if (rc.canTransferResource(built_by, ResourceType.ELIXIR, 1)) {
                rc.transferResource(built_by, ResourceType.ELIXIR, rc.getResourceAmount(ResourceType.ELIXIR));
            } else {
                if (rc.isMovementReady()) {
                    move_towards(built_by);
                }
            }
            target_well = null;
        }
    }

    public int get_resource_count() {
        return rc.getResourceAmount(ResourceType.ADAMANTIUM) + rc.getResourceAmount(ResourceType.MANA) + rc.getResourceAmount(ResourceType.ELIXIR);
    }

    // TODO: DO BETTER THAN THIS BAINDAID
    @Override
    public void move_towards(MapLocation loc) throws GameActionException {
        MapLocation before = rc.getLocation();
        super.move_towards(loc);
        MapLocation after = rc.getLocation();
        Direction dirTaken = before.directionTo(after);

        Direction next = combatPathing.getDirection(loc);
        MapLocation next_loc = after.add(next);
        // Try to make a guess for a good next step
        if (!next_loc.equals(before) && rc.canMove(next)) {
            rc.move(next);
        }
        // Otherwise try taking the same direction as before..
        if (rc.canMove(dirTaken)){
            rc.move(dirTaken);
        }
        // If all else fails; see which tiles get us close to goal but not closer to start, go there
        Direction best = Direction.CENTER;
        int closest_dist = Integer.MAX_VALUE;
        for (Direction d : directions) {
            MapLocation l = after.add(d);
            if (rc.canSenseLocation(l) && rc.canMove(d) && !before.equals(l)){
                int dist = loc.distanceSquaredTo(l);
                if (dist < closest_dist) {
                    closest_dist = dist;
                    best = d;
                }
            }
        }
        if (best != Direction.CENTER && rc.canMove(best)){
            rc.move(best);
        }
    }

    @Override
    public void scan() throws GameActionException {
        if (Clock.getBytecodesLeft() < 100) return; // Check if we are nearly out of bytecode

        // Scan for wells and store them
        WellInfo[] wells = rc.senseNearbyWells();
//        rc.setIndicatorString(Arrays.toString(wells));
        for (WellInfo well : wells) {
            if (Clock.getBytecodesLeft() < 100) return; // Check if we are nearly out of bytecode

            if (well.getResourceType() == resource && target_well == null) { // Go to the well of our type
                target_well = well.getMapLocation();
            }

            if (well.getResourceType() == resource && target_well != null) {
                if (well.getMapLocation().distanceSquaredTo(rc.getLocation()) <= target_well.distanceSquaredTo(rc.getLocation())) {
                    target_well = well.getMapLocation();
                }
            }
            int well_code = encode_well(well);
            store_well_info(well_code);
        }

        RobotInfo[] hqs = rc.senseNearbyRobots(-1, enemy);
        for (RobotInfo hq : hqs) {
            if (Clock.getBytecodesLeft() < 100) return; // Check if we are nearly out of bytecode
            if (hq.type == RobotType.HEADQUARTERS) {
                int hq_code = encode_HQ_location(hq.getLocation());
                store_hq_info(hq_code);
            }
        }
    }

    public void reassign_hq(Direction enemy_direction) throws GameActionException {
        if (rc.getLocation().directionTo(built_by) == enemy_direction) {
            MapLocation closest_hq = null;
            int min_dist = Integer.MAX_VALUE;
            for (int i = START_INDEX_ENEMY_HQS; i < START_INDEX_ENEMY_HQS + MAX_HQS; i++) {
                int hq_code = rc.readSharedArray(i);
                if (hq_code == 0) {
                    break;
                }
                MapLocation hq_loc = decode_hq_location(hq_code);
                int distance = hq_loc.distanceSquaredTo(rc.getLocation());
                if (distance < min_dist && rc.getLocation().directionTo(hq_loc) != enemy_direction) {
                    closest_hq = hq_loc;
                    min_dist = distance;
                }
            }
            if (closest_hq != null) {
                current_hq = closest_hq;
            } else {
                rc.setIndicatorString("tried to defect uwu but failed");
            }
        }
    }

    public int getCarrierDamage(int resource_count) {
        return (int) Math.floor(resource_count / 5.0);
    }
}

