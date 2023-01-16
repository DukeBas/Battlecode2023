package old_v5.bots;

import battlecode.common.*;
import old_v5.util.Constants;

public class Amplifier extends Robot{
    public Amplifier(RobotController rc) {
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
