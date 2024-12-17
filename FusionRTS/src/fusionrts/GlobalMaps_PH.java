/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package fusionrts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 *
 * @author 39347
 * 
 * Main class implementing the progressive history enanchment
 */
public class GlobalMaps_PH {
    
    // New pointer to the global structures used to implement the Progressive History technique
    List<ExtendedUnitActionTableEntry> UnitActionTable = new ArrayList<>();
    // Link for each (unit type,x and y coordinates and player)  the corresponding 
    // entry in Global_unitActionTable
    HashMap<NoID_Key,Integer> TypeXYMap = new LinkedHashMap<>();
    // Store also the maximum index of the list
    int last_idx;
    
    // Initialize structures based on specific player and state
    /*public GlobalMaps_PH(int a_player, GameState gs ) throws Exception {
        PlayerActionGenerator MoveGenerator = new PlayerActionGenerator(gs, a_player);
        
        int idx = 0;
        for (Pair<Unit, List<UnitAction>> choice : MoveGenerator.getChoices()) {
            // m_a represents the actual unit selected
            // m_b stores all the possible actions to be performed by m_a
            ExtendedUnitActionTableEntry ae = new ExtendedUnitActionTableEntry();
            ae.u = choice.m_a; 
            ae.nactions = choice.m_b.size(); 
            ae.actions = new ArrayList<>(choice.m_b);
            ae.accum_evaluation = new ArrayList<>();
            ae.visit_count = new ArrayList<>();
            for (int i = 0; i < ae.nactions; i++) {
                ae.accum_evaluation.add(0.0);
                ae.visit_count.add(0);
            }
            UnitActionTable.add(ae);
            // Generate new key
            NoID_Key new_key = new NoID_Key(ae.u);
            
            // Add a new entry in the hash map
            TypeXYMap.put(new_key,idx);
            idx++;
            }
        last_idx = idx;
    }*/
    public GlobalMaps_PH() {
        // Nothing to do
        UnitActionTable = new ArrayList<>();
        TypeXYMap = new LinkedHashMap<>();
    }
    
    public void update(List<Unit_Action_Pair> unit_action_list, double evaluation){
        for( Unit_Action_Pair pair:unit_action_list){
            
            NoID_Key new_key = new NoID_Key(pair.u); 

            if(TypeXYMap.containsKey(new_key)){
                // Retrieve index of the unit in the list and actual entry
                int idxlist = TypeXYMap.get(new_key);
                ExtendedUnitActionTableEntry ae = UnitActionTable.get(idxlist);

                // Retrieve index of particular action
                int indexOfAction = ae.actions.indexOf(pair.action);

                if ( indexOfAction != -1 ){
                    // Update counter
                    List<Integer> visit_count = ae.visit_count;
                    List<Double> acc_evaluation = ae.accum_evaluation;
                    
                    // reset counter if the game went too far
                    //    if (visit_count.get(indexOfAction) > 5000 ){
                    //        visit_count.set(indexOfAction, 1);
                    //        acc_evaluation.set(indexOfAction, evaluation);
                    //    }
                    //    else{
                            visit_count.set(indexOfAction, visit_count.get(indexOfAction)+1);
                            acc_evaluation.set(indexOfAction, acc_evaluation.get(indexOfAction)+evaluation);
                    //    }
                }
                else{
                    // We are adding a new possible move for a particular unit
                    ae.actions.add(pair.action);
                    ae.nactions++;
                    ae.accum_evaluation.add(evaluation);
                    ae.visit_count.add(1);
                } 
            }
            else{
                // We are adding a new entry to the list
                ExtendedUnitActionTableEntry ae = new ExtendedUnitActionTableEntry();
                ae.u = pair.u; 
                ae.nactions = 1; // At the moment we are simply keeping track of 1 action
                ae.actions = new ArrayList<>();
                ae.actions.add(pair.action);
                ae.accum_evaluation = new ArrayList<>();
                ae.accum_evaluation.add(evaluation);
                ae.visit_count = new ArrayList<>();
                ae.visit_count.add(1);

                UnitActionTable.add(ae);
                TypeXYMap.put(new_key, last_idx);

                // Update index counter
                last_idx++;
            }
        }
    }
    
    public double get_statistic(List<Unit_Action_Pair> unit_action_list){
        // Funcion used ot evaluate sa/na
        double mean_evaluation = 0;
        double mean_visits = 0;
        
        for( Unit_Action_Pair pair:unit_action_list){
            NoID_Key new_key = new NoID_Key(pair.u); 

            if(TypeXYMap.containsKey(new_key)){
                // Retrieve index of the unit in the list and actual entry
                int idxlist = TypeXYMap.get(new_key);
                ExtendedUnitActionTableEntry ae = UnitActionTable.get(idxlist);

                // Retrieve index of particular action
                int indexOfAction = ae.actions.indexOf(pair.action);

                if ( indexOfAction != -1 ){
                    // Update counter
                    List<Integer> visit_count = ae.visit_count;
                    List<Double> acc_evaluation = ae.accum_evaluation;
                    mean_visits += visit_count.get(indexOfAction);
                    mean_evaluation += acc_evaluation.get(indexOfAction);
                }
                else{
                    // This should never happen, it means we choose a path with an
                    // action that has not been inserted in global list
                    System.err.println("GlobalMaps_PH: This should not have happened...");
                } 
            }
            else{
                // This should never happen, it means we choose a path with a
                // unit that has not been inserted in global list
                System.err.println("GlobalMaps_PH: This should not have happened...");
            }
        }
        double statistic = mean_evaluation/mean_visits;
        //System.out.println("Evaluation = " + mean_evaluation + "Visits = " + mean_visits);
        
        return statistic;
    }
}
