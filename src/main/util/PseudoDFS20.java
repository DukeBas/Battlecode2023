package main.util;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

/**
 * Simple pathfinding going straight to target, ignoring terrain and other robots.
 */
public class PseudoDFS20 implements Pathfinding {
    RobotController rc;
    int mapWidth;
    int mapHeight;
    boolean[] test;

    public PseudoDFS20(RobotController rc){
        this.rc = rc;
        this.mapWidth = rc.getMapWidth();
        this.mapHeight = rc.getMapWidth();
    }

    @Override
    public Direction getDirection(final MapLocation target) throws GameActionException {
        MapLocation ownLocation = rc.getLocation();

        // DFS FOR R^2 of 20

        // TODO: do not save seen, only save previous path?
        // TODO: add stack to be able to pop


        rc.setIndicatorDot(target, 255, 255, 255);

        test = new boolean[121];

        test[offsetToArrIndex(0, 0)] = true;

        // Start from own location and try two DFS', one right inclined and one left
        int max_depth = 8;
        MapLocation head = ownLocation;
        MapLocation offset = new MapLocation(0, 0);
        MapLocation first = ownLocation;

        // right inclined
        for (int i = max_depth; --i > 0; ) {
            Direction dirToTarget = head.directionTo(target);

            // Check if we found the goal
            if (head.distanceSquaredTo(target) <= 2) {
//                System.out.println("Goal found in " + (max_depth - i) + " steps");
                return ownLocation.directionTo(first);
            }

            boolean head_same = true;

            for (int j = 7; --j >= 0; ) {
                MapLocation next_possible_head = head.add(dirToTarget);

                if (!rc.canSenseLocation(next_possible_head)) continue; // location out of bounds

                MapLocation current_offset = offset.add(dirToTarget);

                if (!test[offsetToArrIndex(current_offset.x, current_offset.y)]) {
//                    System.out.println("Trying " + next_possible_head.toString());
                    // set as seen so we don't revisit
                    test[offsetToArrIndex(current_offset.x, current_offset.y)] = true;

                    if (rc.sensePassability(next_possible_head) && !rc.isLocationOccupied(next_possible_head)) {
//                        System.out.println("OLD HEAD IS " + head.toString() + " and next HEAD is " + next_possible_head.toString());
                        head = next_possible_head;
                        offset = offset.add(dirToTarget);
                        head_same = false;

                        if (ownLocation.equals(first)){
                            // Set first step
                            first = head;
                        }

//                        rc.setIndicatorDot(head, 100, 150, 23);
                        break;
                    }
                }

                dirToTarget = dirToTarget.rotateRight();
            }

            // Check if we are stuck, just quit if we are
            if (head_same) {
//                System.out.println("stuck right");
                break;
            }
        }

        //todo check bytecode used/left
        //Todo use mapinfo for currents?

        // reset seen around start
        int width = rc.getMapWidth();
        int height = rc.getMapHeight();
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a != 0 || b != 0) {
                    int new_x = ownLocation.x + a;
                    int new_y = ownLocation.y + b;
                    if (new_x >= 0 && new_x < width && new_y >= 0 && new_y < height) {
                        test[offsetToArrIndex(a, b)] = false;
                    }
                }
            }
        }


        // left inclined
        head = ownLocation;
        offset = new MapLocation(0, 0);
        for (int i = max_depth; --i > 0; ) {
            Direction dirToTarget = head.directionTo(target);

            // Check if we found the goal
            if (head.distanceSquaredTo(target) <= 2) {
//                System.out.println("Goal found in " + (max_depth - i) + " steps");
                return ownLocation.directionTo(first);
            }

            boolean head_same = true;

            for (int j = 7; --j >= 0; ) {
                MapLocation next_possible_head = head.add(dirToTarget);

                if (!rc.canSenseLocation(next_possible_head)) continue; // location out of bounds

                MapLocation current_offset = offset.add(dirToTarget);

                if (!test[offsetToArrIndex(current_offset.x, current_offset.y)]) {
//                    System.out.println("Trying " + next_possible_head.toString());
                    // set as seen so we don't revisit
                    test[offsetToArrIndex(current_offset.x, current_offset.y)] = true;

                    if (rc.sensePassability(next_possible_head) && !rc.isLocationOccupied(next_possible_head)) {
//                        System.out.println("OLD HEAD IS " + head.toString() + " and next HEAD is " + next_possible_head.toString());
                        head = next_possible_head;
                        head_same = false;
                        offset = offset.add(dirToTarget);

                        if (ownLocation.equals(first)){
                            // Set first step
                            first = head;
                        }

//                        rc.setIndicatorDot(head, 200, 50, 230);
                        break;
                    }
                }

                dirToTarget = dirToTarget.rotateLeft();
            }

            // Check if we are stuck, just quit if we are
            if (head_same) {
//                System.out.println("stuck left");
                return Direction.CENTER;
            }
        }

        // Goal was not reached :(
        return Direction.CENTER;
    }

    private int offsetToArrIndex(int x, int y) {
        return (x + 5) * 11 + y + 5;
    }
}
