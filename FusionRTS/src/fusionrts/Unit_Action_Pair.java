/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package fusionrts;

import java.util.List;
import rts.UnitAction;
import rts.units.Unit;

/**
 *
 * @author 39347
 */
public class Unit_Action_Pair {
    public Unit u;
    public UnitAction action;   
    
    public Unit_Action_Pair ( Unit unit , UnitAction unit_action ){
        u = unit;
        action = unit_action;
    }
}
