/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package fusionrts;

import java.util.Objects;
import rts.units.UnitType;

/**
 *
 * @author 39347
 * 
 * This class will be used to define keys for the global structure used to 
 * implement the progressive history enanchment.
 */

// Define the custom key class
public class TypeXY_Key {
    private final UnitType unitType; // Unit type as a class
    private final int x;            // X coordinate
    private final int y;            // Y coordinate

    public TypeXY_Key(UnitType Type, int x, int y) {
        this.unitType = Type;
        this.x = x;
        this.y = y;
    }

    // Override equals() for logical equality
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeXY_Key Key = (TypeXY_Key) o;
        return x == Key.x &&
               y == Key.y &&
               Objects.equals(unitType, Key.unitType);
    }

    // Override hashCode() for correct hashing
    @Override
    public int hashCode() {
        return Objects.hash(unitType, x, y);
    }

    @Override
    public String toString() {
        return "UnitKey{unitType=" + unitType.getClass().getName() + ", x=" + x + ", y=" + y + "}";
    }
}
