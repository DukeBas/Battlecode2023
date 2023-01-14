package main.bots;

import battlecode.common.*;
import main.util.Constants;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import main.util.SimplePathing;

import static first_bot.util.Constants.directions;

public class Carrier extends Robot{

    static int MAX_RESOURCES = 40;
    ResourceType resource = null;
    int HQ_id = -1;
    MapLocation target_well = null;

    public Carrier(RobotController rc) throws GameActionException{
        super(rc);
        HQ_id = get_HQ_id(built_by);
        resource = decode_HQ_resource_assignment(HQ_id);
        System.out.println("I AM CARRIER WITH GOAL " + resource.toString());
        assign_carrier(ResourceType.NO_RESOURCE, HQ_id);
    }

    /**
     * This code is run once per turn (assuming we do not go over bytecode limits.)
     *
     * @throws GameActionException if an illegal game action is performed.
     */
    @Override
    void run() throws GameActionException {
        if (rc.getAnchor() != null) {
            // If I have an anchor singularly focus on getting it to the first island I see
            int[] islands = rc.senseNearbyIslands();
            Set<MapLocation> islandLocs = new HashSet<>();
            for (int id : islands) {
                // Only add possible island if it is unclaimed
                if (rc.senseTeamOccupyingIsland(id) == Team.NEUTRAL) {
                    MapLocation[] thisIslandLocs = rc.senseNearbyIslandLocations(id);
                    islandLocs.addAll(Arrays.asList(thisIslandLocs));
                }
            }
            if (islandLocs.size() > 0) {
                MapLocation islandLocation = islandLocs.iterator().next();
                rc.setIndicatorString("Moving my anchor towards " + islandLocation);
                while (!rc.getLocation().equals(islandLocation)) {
                    Direction dir = rc.getLocation().directionTo(islandLocation);
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                    }
                }
                if (rc.canPlaceAnchor()) {
                    rc.setIndicatorString("Huzzah, placed anchor!");
                    rc.placeAnchor();
                }
            }
        }

        if (rc.canTakeAnchor(built_by, Anchor.STANDARD)) {
            rc.takeAnchor(built_by, Anchor.STANDARD);
        } else if (get_resource_count() == MAX_RESOURCES) {
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
        } else {
            // Resources not full, Pathfind to well
            if (target_well == null) {
                target_well = get_nearest_well(resource);
            }
            if (target_well == null || rc.getAnchor() != null) {
                // Cant find well, move randomly
                Direction dir = directions[rng.nextInt(directions.length)];
                move_towards(dir);
            } else {
                if (rc.canCollectResource(target_well, 1)) {
                    rc.collectResource(target_well, -1);
                } else {
                    move_towards(target_well);
                }
            }
        }
        scan();
    }

    public int get_resource_count() {
        return rc.getResourceAmount(ResourceType.ADAMANTIUM) + rc.getResourceAmount(ResourceType.MANA) + rc.getResourceAmount(ResourceType.ELIXIR);
    }

    private void scan() throws GameActionException {

        // Scan for wells and store them
        WellInfo[] wells = rc.senseNearbyWells();
        for (WellInfo well : wells) {
            if (well.getResourceType() == resource) {

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

