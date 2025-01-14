 /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import ai.core.AI;
import ai.mcts.naivemcts.NaiveMCTS;
import fusionrts.FusionRTS;
import fusionrts.FusionRTSWithAllEnhancements;
import gui.PhysicalGameStatePanel;

import javax.swing.JFrame;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;
import tests.MapGenerator;

 /**
 *
 * @author santi
 */
public class GameVisualSimulationTest {
    public static void main(String[] args) throws Exception {
        UnitTypeTable utt = new UnitTypeTable();
        MapGenerator mapGenerator = new MapGenerator(utt);
        //PhysicalGameState pgs = mapGenerator.basesWorkers8x8Obstacle();
        PhysicalGameState pgs = PhysicalGameState.load("../../microrts/maps/16x16/basesWorkers16x16.xml", utt);

        GameState gs = new GameState(pgs, utt);
        int MAXCYCLES = 5000;
        int PERIOD = 20;
        boolean gameover = false;
        
        // Here you can enable the different enanchment by using the respective boolean flags:
        //  - Progressive History;
        //  - Tree Reuse;
        //  - AWLM heuristic.
        //AI ai1 = new FusionRTS(utt,false,false,false);
        AI ai1 = new FusionRTSWithAllEnhancements(utt);
        AI ai2 = new NaiveMCTS(utt);
        //AI ai2 = new LightRush(utt, new BFSPathFinding());

        JFrame w = PhysicalGameStatePanel.newVisualizer(gs,640,640,false,PhysicalGameStatePanel.COLORSCHEME_BLACK);
//        JFrame w = PhysicalGameStatePanel.newVisualizer(gs,640,640,false,PhysicalGameStatePanel.COLORSCHEME_WHITE);

        long nextTimeToUpdate = System.currentTimeMillis() + PERIOD;
        do{
            if (System.currentTimeMillis()>=nextTimeToUpdate) {
                PlayerAction pa1 = ai1.getAction(0, gs);
                PlayerAction pa2 = ai2.getAction(1, gs);
                gs.issueSafe(pa1);
                gs.issueSafe(pa2);

                // simulate:
                gameover = gs.cycle();
                w.repaint();
                nextTimeToUpdate+=PERIOD;
            } else {
                try {
                    Thread.sleep(1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }while(!gameover && gs.getTime()<MAXCYCLES);
        ai1.gameOver(gs.winner());
        ai2.gameOver(gs.winner());
        
        System.out.println("Game Over");
    }    
}
