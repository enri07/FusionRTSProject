/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tournaments;

import ai.core.AI;

import java.io.File;
import java.io.Writer;
import java.util.List;

import rts.PhysicalGameState;
import rts.units.UnitTypeTable;

/**
 *
 * @author santi
 * @author douglasrizzo
 */
public class RoundRobinTournament extends Tournament{

    public RoundRobinTournament(List<AI> AIs) {
        super(AIs);
    }

    public void runTournament(int playOnlyGamesInvolvingThisAI,
                              List<String> maps,
                              int iterations,
                              int maxGameLength,
                              int timeBudget,
                              int iterationsBudget,
                              long preAnalysisBudgetFirstTimeInAMap,
                              long preAnalysisBudgetRestOfTimes,
                              boolean fullObservability,
                              boolean selfMatches,
                              boolean timeoutCheck,
                              boolean runGC,
                              boolean preAnalysis,
                              UnitTypeTable utt,
                              String traceOutputfolder,
                              Writer out,
                              Writer progress,
                              String folderForReadWriteFolders) throws Exception {
        if (progress != null) {
            progress.write(getClass().getName()+": Starting tournament\n");
        }
        out.write(getClass().getName()+"\n");
        out.write("AIs\n");
        for (AI ai : AIs) {
            out.write(splitter + ai.toString() + "\n");
        }
        out.write("maps\n");
        for (String map : maps) {
            out.write(splitter + map + "\n");
        }
        out.write("iterations" + splitter + iterations + "\n");
        out.write("maxGameLength" + splitter + maxGameLength + "\n");
        out.write("timeBudget" + splitter + timeBudget + "\n");
        out.write("iterationsBudget" + splitter + iterationsBudget + "\n");
        out.write("pregameAnalysisBudget" + splitter + preAnalysisBudgetFirstTimeInAMap + splitter + preAnalysisBudgetRestOfTimes + "\n");
        out.write("preAnalysis" + splitter + preAnalysis + "\n");
        out.write("fullObservability" + splitter + fullObservability + "\n");
        out.write("timeoutCheck" + splitter + timeoutCheck + "\n");
        out.write("runGC" + splitter + runGC + "\n");
        out.write("iteration" + splitter + "map" + splitter + "ai1" + splitter + "ai2" + splitter
                + "time" + splitter + "winner" + splitter + "crashed"
                + splitter + "timedout" + "\n");
        out.flush();
        
        // create all the read/write folders:
        String readWriteFolders[] = new String[AIs.size()];
        for(int i = 0;i<AIs.size();i++) {
            readWriteFolders[i] = folderForReadWriteFolders + "/AI" + i + "readWriteFolder";
            File f = new File(readWriteFolders[i]);
            f.mkdir();
        }

        boolean firstPreAnalysis[][] = new boolean[AIs.size()][maps.size()];
        for (int i = 0; i < AIs.size(); i++) {
            for (int j = 0; j < maps.size(); j++) {
                firstPreAnalysis[i][j] = true;
            }
        }
        
        for (int iteration = 0; iteration < iterations; iteration++) {
            for (int map_idx = 0; map_idx < maps.size(); map_idx++) {
                PhysicalGameState pgs = PhysicalGameState.load(maps.get(map_idx), utt);
                for (int ai1_idx = 0; ai1_idx < AIs.size(); ai1_idx++) {
                    for (int ai2_idx = 0; ai2_idx < AIs.size(); ai2_idx++) {
                        if (!selfMatches && ai1_idx == ai2_idx) continue;
                        if (playOnlyGamesInvolvingThisAI != -1) {
                            if (ai1_idx != playOnlyGamesInvolvingThisAI &&
                                    ai2_idx != playOnlyGamesInvolvingThisAI) continue;
                        }
                        playSingleGame(maxGameLength, timeBudget, iterationsBudget,
                                preAnalysisBudgetFirstTimeInAMap, preAnalysisBudgetRestOfTimes, fullObservability,
                                timeoutCheck, runGC, preAnalysis, utt, traceOutputfolder, out, progress,
                                readWriteFolders, firstPreAnalysis, iteration, map_idx, pgs, ai1_idx, ai2_idx);
                    }
                }
            }
        }

        printEndSummary(maps,iterations, out, progress);
    }
}
