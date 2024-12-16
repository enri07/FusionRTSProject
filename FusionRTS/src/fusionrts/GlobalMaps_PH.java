/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package fusionrts;

import ai.mcts.naivemcts.UnitActionTableEntry;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import rts.GameState;
import rts.PlayerActionGenerator;
import rts.UnitAction;
import rts.units.Unit;
import util.Pair;

/**
 *
 * @author 39347
 * 
 * Main class implementing the progressive history enanchment
 */
public class GlobalMaps_PH {
    
    // New pointer to the global structures used to implement the Progressive History technique
    List<UnitActionTableEntry> UnitActionTable;
    // Link for each (unit type,x coordinate and y coordinate)  the corresponding 
    // entry in Global_unitActionTable
    HashMap<TypeXY_Key,Integer> TypeXYMap = new LinkedHashMap<>();
    
    // Initialize structures based on specific player and state
    public GlobalMaps_PH(int a_player, GameState gs ) throws Exception {
        PlayerActionGenerator MoveGenerator = new PlayerActionGenerator(gs, a_player);
        
        int idx = 0;
        for (Pair<Unit, List<UnitAction>> choice : MoveGenerator.getChoices()) {
            // m_a represents the actual unit selected
            // m_b stores all the possible actions to be performed by m_a
            UnitActionTableEntry ae = new UnitActionTableEntry();
            ae.u = choice.m_a; 
            ae.nactions = choice.m_b.size(); 
            ae.actions = choice.m_b;
            ae.accum_evaluation = new double[ae.nactions];
            ae.visit_count = new int[ae.nactions];
                for (int i = 0; i < ae.nactions; i++) {
                    ae.accum_evaluation[i] = 0;
                    ae.visit_count[i] = 0;
                }
            UnitActionTable.add(ae);
            // Generate new key
            TypeXY_Key new_key = new TypeXY_Key(ae.u.getType(),ae.u.getX(),ae.u.getY());
            
            // Add a new entry in the hash map
            TypeXYMap.put(new_key,idx);
            idx++;
            }
    }
    
}
