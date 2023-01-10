package first_bot.bots;

import battlecode.common.*;
import first_bot.util.Constants;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import first_bot.util.SimplePathing;

import static first_bot.util.Constants.directions;

public class Carrier extends Robot{

    static int MAX_RESOURCES = 40;

    public Carrier(RobotController rc) {
        super(rc);
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
        // Try to gather from squares around us.
        MapLocation me = rc.getLocation();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation wellLocation = new MapLocation(me.x + dx, me.y + dy);
                if (rc.canCollectResource(wellLocation, -1)) {
                    // Do not attempt to collect resources if full
                    if (get_resource_count() >= MAX_RESOURCES) {
                        rc.collectResource(wellLocation, -1);
                        rc.setIndicatorString("Collecting, now have, AD:" +
                                rc.getResourceAmount(ResourceType.ADAMANTIUM) +
                                " MN: " + rc.getResourceAmount(ResourceType.MANA) +
                                " EX: " + rc.getResourceAmount(ResourceType.ELIXIR));
                    }
                }
            }
        }
        
        if (get_resource_count() == MAX_RESOURCES) {
            // Resources full, Pathfind to HQ
            move_towards(built_by);
        } else {
            // Resources not full, Pathfind to well
            pathfind_to_nearest_well();
        }
    }

    public int get_resource_count() {
        return rc.getResourceAmount(ResourceType.ADAMANTIUM) + rc.getResourceAmount(ResourceType.MANA) + rc.getResourceAmount(ResourceType.ELIXIR);
    }

    public void pathfind_to_nearest_well() throws GameActionException{
        // Find closest well
        MapLocation goal_Location = null;
        WellInfo[] wells = rc.senseNearbyWells();
            int min_dist = Integer.MIN_VALUE;
            if (wells.length != 0) {
                for (WellInfo well : wells) {
                    int dist_to_well = well.getMapLocation().distanceSquaredTo(rc.getLocation());
                    if (dist_to_well <= min_dist) {
                        min_dist = dist_to_well;
                        goal_Location = well.getMapLocation();
                    }
                    if (goal_Location != null) {
                        move_towards(goal_Location);
                    }
                }
                // do navigation
            } else {
                // No nearby wells
                rc.setIndicatorString("I dont see any wells :(");
                // Also try to move randomly.
                // TODO: FIX ME PLEASE :)
                Direction dir = directions[rng.nextInt(directions.length)];
                move_towards(dir);
            }
    }
}
