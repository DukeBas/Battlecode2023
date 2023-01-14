package main.bots;

import battlecode.common.*;
import main.util.Constants;

public class Destabilizer extends Robot{
    public Destabilizer(RobotController rc) {
        super(rc);
    }

    /**
     * This code is run once per turn (assuming we do not go over bytecode limits.)
     *
     * @throws GameActionException if an illegal game action is performed.
     */
    @Override
    void run() throws GameActionException {
        scan();
    }
}
