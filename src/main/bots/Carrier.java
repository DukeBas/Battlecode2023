package main.bots;

import battlecode.common.*;

import static first_bot.util.Constants.directions;

public class Carrier extends Robot{

    static int MAX_RESOURCES = 40;
    ResourceType resource;
    int HQ_id;
    MapLocation target_well = null;

    public Carrier(RobotController rc) throws GameActionException{
        super(rc);
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
                // Cant find well, move randomly
                Direction dir = directions[rng.nextInt(directions.length)];
                move_towards(dir);
            }
        }
        int bytecodeLeftBefore = Clock.getBytecodesLeft();
        scan();
        System.out.println("Code used: " + (bytecodeLeftBefore - Clock.getBytecodesLeft()) + " bytecode");
    }

    public void well_routine() throws GameActionException {
        if (rc.canCollectResource(target_well, 1)) {
            rc.collectResource(target_well, -1);
        } else {
            move_towards(target_well);
        }
    }

    public void anchor_routine() throws GameActionException {
        // If I have an anchor singularly focus on getting it to the first island I see
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
            move_towards(island);
            if (rc.canPlaceAnchor()) {
                rc.setIndicatorString("Huzzah, placed anchor!");
                rc.placeAnchor();
            }
        } else {
            // Cant find well, move randomly
            Direction dir = directions[rng.nextInt(directions.length)];
            move_towards(dir);
        }
    }

    public void hq_routine() throws GameActionException{
        if (get_resource_count() == MAX_RESOURCES) {
            // Resources full, Pathfind to HQ
            if (rc.canTransferResource(built_by, ResourceType.ADAMANTIUM, 1)) {
                rc.transferResource(built_by, ResourceType.ADAMANTIUM, rc.getResourceAmount(ResourceType.ADAMANTIUM));
            } else if (rc.canTransferResource(built_by, ResourceType.MANA, 1)) {
                rc.transferResource(built_by, ResourceType.MANA, rc.getResourceAmount(ResourceType.MANA));
            } else if (rc.canTransferResource(built_by, ResourceType.ELIXIR, 1)) {
                rc.transferResource(built_by, ResourceType.ELIXIR, rc.getResourceAmount(ResourceType.ELIXIR));
            } else {
                move_towards(built_by);
            }
            target_well = null;
        }
    }

    public int get_resource_count() {
        return rc.getResourceAmount(ResourceType.ADAMANTIUM) + rc.getResourceAmount(ResourceType.MANA) + rc.getResourceAmount(ResourceType.ELIXIR);
    }

    @Override
    public void scan() throws GameActionException {

        // Scan for wells and store them
        WellInfo[] wells = rc.senseNearbyWells();
        for (WellInfo well : wells) {
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
            if (hq.type == RobotType.HEADQUARTERS) {
                int hq_code = encode_HQ_location(hq.getLocation());
                store_hq_info(hq_code);
            }
        }
    }
}

