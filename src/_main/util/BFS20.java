package _main.util;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import static _main.util.Constants.directions;

public class BFS20 implements Pathfinding {
    RobotController rc;

    MapLocation[] stack = new MapLocation[69]; // amount of tiles for 20R^2
    int start_stack_counter; // where in the stack should we look for the next location
    int end_stack_counter; // where in the stack should we add new items

    // Array to keep track of which locations (based on offset of robot location) we have seen
    boolean[][] seen; // 5x5 offsets

    public BFS20(RobotController rc) {
        this.rc = rc;
    }

    // Function for when taking two steps is not possible
    @Override
    public Direction getDirection(MapLocation target) throws GameActionException {
        // Initialize stack and seen
        start_stack_counter = 1;
        end_stack_counter = 0;
//        stack = new MapLocation[69]; // If we overwrite things properly, we do not need to reset the stack
        MapLocation ownLocation = rc.getLocation();
        stack[start_stack_counter] = ownLocation;
//        seen = new boolean[121];
//        seen[offsetToArrIndex(0,0)] = true;
        seen = new boolean[11][11];
        seen[5][5] = true;


        for (int i = 67; --i >= 0; ) {
            // Pop from stack
            MapLocation current = stack[start_stack_counter];
            start_stack_counter++;




//            // Add each direction to stack if it's new
//            // TODO: unroll
//            for (Direction d : directions) {
                MapLocation loc = current.add(Direction.EAST);
//                int dx = loc.x - ownLocation.x + 5;
//                int dy = loc.y - ownLocation.y + 5;
//
//                if (!seen[dx][dy]){
//                    // It's new! Save it and add it to stack
//                    seen[dx][dy] = true;
//                }
//                Direction d1 = Direction.NORTHWEST;
//                MapLocation loc1 = current.add(d1);
//                int dx1 = loc1.x - ownLocation.x + 5;
//                int dy1 = loc1.y - ownLocation.y + 5;

//                if (!seen[dx1][dy1]){
//                    // It's new! Save it and add it to stack
//                    seen[dx1][dy1] = true;
//                }
//            }

            // All so bad :(

            stack[start_stack_counter] = loc;
        }

        return null;
    }

    // Todo: function for two or three steps

    private int offsetToArrIndex(int x, int y) {
        return (x + 5) * 11 + y + 5;
    }

}
