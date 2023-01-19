from pathlib import Path

# CODE GENERATED IS SLOWER THAN ARRAY ACCESS :(


def main():
    # output_file = Path(__file__).parent / f"Pathfinding{vision_radius}.java"
    output_file = Path(__file__).parent / "BFS20_1.java"
    max_x = 60
    max_y = 60
    out = "    "

    for x in range(1,max_x+1):
        for y in range(1,max_y+1):
            out += f"""    int seen_x{x}_y{y};\n"""

    out += f"""
    // Gets whether a location has been seen, if not, sets it as seen
    // !! ASSUMES ROUND VARIABLE IS SET PROPERLY BEFORE FUNCTION IS CALLED
    private boolean getSetSeen(MapLocation loc) {{
        switch (loc.x) {{"""

    for x in range(1,max_x+1):
        out += f"""
            case {x}:
                return y{x}func(loc.y);"""
            

    out += f"""
        }}
        // WE SHOULD NEVER GET HERE
        return true;
    }}
    """

    for x in range (1,max_x+1):
        out += f"""
    private boolean y{x}func(int y) {{
        switch (y) {{
            """

        for y in range(1, max_y+1):
            out += f"""case {y}:
                if (seen_x{x}_y{y} < round){{
                    seen_x{x}_y{y} = round;
                    return true;
                }}
                return false;
            """

        out += f"""
            }}

            // WE SHOULD NEVER GET HERE
            return true;
        }}
        """
        
    with output_file.open("w+", encoding="utf-8") as file:
        file.write(out.strip() + "\n")

if __name__ == "__main__":
    main()