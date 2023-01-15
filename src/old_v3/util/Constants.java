package old_v3.util;

import battlecode.common.Direction;

public class Constants {
    /** Array containing all the possible movement directions. */
    public static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    /*
     Enumeration of all booleans for use in communication
     Currently at most 16 are allowed!
     */
    public enum Communication_bools {
        SYM_ROTATIONAL,
        SYM_HORIZONTAL,
        SYM_VERTICAL,
    }
}
