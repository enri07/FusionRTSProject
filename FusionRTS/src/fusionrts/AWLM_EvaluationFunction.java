package fusionrts;

import ai.evaluation.EvaluationFunction;
import java.util.LinkedList;
import java.util.List;
import rts.GameState;
import rts.PhysicalGameState;
import rts.units.*;

/**
 *
 * @author Enrico Calandrini, Carmine Vitiello, Davide Mele
 * 
 * Against Worker in Large Maps Evaluation Function. This evaluation function 
 * uses the same base evaluation as SimpleSqrtEvaluationFunction3. In addition,
 * it stores the minimum (critical) distance between the players bases. If this distance 
 * is greater then 9 (usually on a 12x12 map and larger) it applys a discouraging 
 * factor on the development of new workers. This factor is proportional to the number of
 * workers already availables. In this way, bases will store more resources instead of
 * continuously deploying workers and this will lead to the creation of barracks and
 * fighting units. Conversely, if this critical distance is lower then 9 then the 
 * same evaluation of the standard SimpleSqrtEvaluationFunction3 is used.
 * 
 */
public class AWLM_EvaluationFunction extends EvaluationFunction {    
    public static float RESOURCE = 20;
    public static float RESOURCE_IN_WORKER = 10;
    public static float UNIT_BONUS_MULTIPLIER = 40.0f;
    
    
    public float evaluate(int maxplayer, int minplayer, GameState gs) {
        float s1 = base_score(maxplayer,gs);
        float s2 = base_score(minplayer,gs);
        if (s1 + s2 == 0) return 0.5f;
        return  (2*s1 / (s1 + s2))-1;
    }
    
    public float base_score(int player, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        float score = gs.getPlayer(player).getResources()*RESOURCE;
        boolean anyunit = false;
        int counter_worker = 0;
        List<Unit> my_base_list = new LinkedList<>();
        List<Unit> opponent_base_list = new LinkedList<>();
        double crit_distance = 40;
        for(Unit u:pgs.getUnits()) {
            if (u.getPlayer()==player) {
                anyunit = true;
                if ( ("Worker".equals(u.getType().name) && crit_distance > 9 ) ){
                    counter_worker++;
                    score += u.getResources() * RESOURCE_IN_WORKER / counter_worker;
                    score += UNIT_BONUS_MULTIPLIER * u.getCost()*Math.sqrt( u.getHitPoints()/u.getMaxHitPoints() ) / counter_worker;
                }
                else{
                    if ("Base".equals(u.getType().name) ){
                        my_base_list.add(u);
                        for( Unit opponent_base:opponent_base_list ){
                            double new_dist = Math.sqrt( Math.abs(opponent_base.getX()-u.getX()) + 
                                Math.abs(opponent_base.getY()-u.getY()));
                            if( new_dist < crit_distance ){
                                crit_distance = new_dist;
                            }
                        }
                    }
                    score += u.getResources() * RESOURCE_IN_WORKER;
                    score += UNIT_BONUS_MULTIPLIER * u.getCost()*Math.sqrt( u.getHitPoints()/u.getMaxHitPoints() );
                }
            }
            else{
                if( "Base".equals(u.getType().name) ){
                    opponent_base_list.add(u);
                    for(Unit my_base:my_base_list){
                        double x_diff = Math.abs(my_base.getX()-u.getX());
                        double y_diff = Math.abs(my_base.getY()-u.getY());
                        double new_dist = Math.sqrt( Math.pow(x_diff,2) + Math.pow(y_diff,2) );
                        if( new_dist < crit_distance ){
                            crit_distance = new_dist;
                        }   
                    }
                }
            }
        }
        //score -= 100/crit_distance;
        
        if (!anyunit) return 0;
        return score;
    }    
    
    public float upperBound(GameState gs) {
        return 1.0f;
    }
}
