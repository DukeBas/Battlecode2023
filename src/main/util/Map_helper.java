package main.util;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Map_helper {
    RobotController rc;

    public Map_helper(RobotController rc){
        this.rc = rc;
    }

    // Returns the location which would be the same by rotational symmetry
    public MapLocation rotationalSymmetricLocation(MapLocation original){

        return new MapLocation(rc.getMapWidth() - original.x - 1, rc.getMapHeight() - original.y - 1);
    }
}
