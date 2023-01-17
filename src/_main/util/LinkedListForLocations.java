package _main.util;

import battlecode.common.MapLocation;

public class LinkedListForLocations {
    // First item has null pointer for previous
    // Last item has null pointer for next

    Item start = null;
    Item end = null;

    // Adds an item to the end of the linked list
    public void add(MapLocation loc) {
        // Empty list? Add it at start
        if (start == null) {
            start = new Item(loc, null, null);
            end = start;
        } else {
            // Else append it to the end and move end pointer
            Item item = new Item(loc, end, null);
            end.next = item;
            end = item;
        }
    }

    // Returns whether the list is not empty
    public boolean isNotEmpty() {
        return start != null;
    }

    // Returns first from the list, but does not remove it
    public MapLocation peek() {
        if (isNotEmpty()) {
            return start.stored;
        }
        return null;
    }

    // Removes first from the list and returns it
    public MapLocation pop() {
        if (isNotEmpty()) {
            MapLocation loc = start.stored;

            // make the next one the start, if there is one
            if (start.next != null) {
                start = start.next;
            } else {
                start = null;
            }

            return loc;
        }
        return null;
    }

    // Removes last from the list and returns it
    public MapLocation getLast() {
        if (isNotEmpty()) {
            MapLocation loc = end.stored;

            // make the prev one the end, if there is one
            if (end.prev != null) {
                end = end.prev;
            } else {
                end = null;
            }

            return loc;
        }
        return null;
    }

    // Data class
    private static class Item {
        public MapLocation stored;
        public Item prev;
        public Item next;

        public Item(MapLocation loc, Item previous, Item next) {
            this.stored = loc;
            this.prev = previous;
            this.next = next;
        }
    }
}
