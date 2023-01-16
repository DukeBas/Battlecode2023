package old_v5.util;

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

    public MapLocation verticalSymmetricLocation(MapLocation hq) {
        return new MapLocation(hq.x, rc.getMapHeight() - hq.y - 1);
    }

    public MapLocation horizontalSymmetricLocation(MapLocation hq) {
        return new MapLocation(rc.getMapWidth() - hq.x - 1, hq.y);
    }
}
