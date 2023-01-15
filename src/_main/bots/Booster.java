package _main.bots;

import battlecode.common.*;
import _main.util.Constants;

public class Booster extends Robot{
    public Booster(RobotController rc) {
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
