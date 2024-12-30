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
 * This class will be used to define the keys for the global map structure used to
 * implement the progressive history enhancement
 */

// Define the custom key class
public class NoIDKey {
    private final UnitType unitType; // Unit type as a class
    private final int x;             // X coordinate of the unit
    private final int y;             // Y coordinate of the unit
    private final int player;        // Owner of the unit
    private final int resources;     // Resources that the unit has taken
    private final int hitpoints;     // hitpoints of the unit

    public NoIDKey(Unit u) {
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
        NoIDKey Key = (NoIDKey) o;
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
        return "NoIDKey{" +
                "unitType= " + unitType.name +
                ", x= " + x +
                ", y= " + y +
                ", player= " + player +
                ", resources= " + resources +
                ", hitpoints= " + hitpoints +
                "}";
    }
}
