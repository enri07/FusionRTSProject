/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package fusionrts;

import java.util.Objects;
import rts.units.Unit;
import rts.units.UnitType;

/**
 *
 * @author 39347
 * 
 * This class will be used to define keys for the global structure used to 
 * implement the progressive history enanchment.
 */

// Define the custom key class
public class NoID_Key {
    private final UnitType unitType; // Unit type as a class
    private final int x;            // X coordinate
    private final int y;            // Y coordinate
    private final int player;       // Owner of the unit
    private final int resources;
    private final int hitpoints;

    public NoID_Key(Unit u) {
        this.unitType = u.getType();
        this.x = u.getX();
        this.y = u.getY();
        this.player = u.getPlayer();
        this.resources = u.getResources();
        this.hitpoints = u.getHitPoints();
    }

    // Override equals() for logical equality
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NoID_Key Key = (NoID_Key) o;
        return x == Key.x &&
               y == Key.y &&
               player == Key.player &&
               resources == Key.resources &&
               hitpoints == Key.hitpoints &&
               Objects.equals(unitType, Key.unitType);
    }

    // Override hashCode() for correct hashing
    @Override
    public int hashCode() {
        return Objects.hash(unitType, x, y, player, resources, hitpoints);
    }

    @Override
    public String toString() {
        return "UnitKey{unitType=" + unitType.getClass().getName() + ", x=" + x + ", y=" + y + "}";
    }
}
