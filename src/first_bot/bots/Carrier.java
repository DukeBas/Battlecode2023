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
        if (get_resource_count() == MAX_RESOURCES) {
            // Resources full, Pathfind to HQ
            move_towards(built_by);
        } else {
            // Resources not full, Pathfind to well
            MapLocation nearest_well = get_nearest_well();
            if (nearest_well == null) {
                // Cant find well, move randomly
                Direction dir = directions[rng.nextInt(directions.length)];
                move_towards(dir);
            } else {
                if (rc.canCollectResource(nearest_well, -1)) {
                    rc.collectResource(nearest_well, -1);
                } else {
                    move_towards(nearest_well);
                }
            }
        }
    }

    public int get_resource_count() {
        return rc.getResourceAmount(ResourceType.ADAMANTIUM) + rc.getResourceAmount(ResourceType.MANA) + rc.getResourceAmount(ResourceType.ELIXIR);
    }

    public MapLocation get_nearest_well() throws GameActionException {
        // Find closest well
        MapLocation nearest_well = null;
        WellInfo[] wells = rc.senseNearbyWells();
            int min_dist = Integer.MIN_VALUE;
            if (wells.length != 0) {
                for (WellInfo well : wells) {
                    int dist_to_well = well.getMapLocation().distanceSquaredTo(rc.getLocation());
                    if (dist_to_well <= min_dist) {
                        min_dist = dist_to_well;
                        nearest_well = well.getMapLocation();
                    }
                }
            } else {
                rc.setIndicatorString("I dont see any wells :(");
            }
        return nearest_well;
    }
}
