package fusionrts;

import ai.core.AI;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import rts.units.UnitTypeTable;
import ai.RandomBiasedAI;

public class FusionRTSWithPH extends FusionRTS {

    public FusionRTSWithPH(UnitTypeTable utt) {
        super(100, -1, 100, 10, 0.3f, 0.0f, 0.4f, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3(), true, true, false, false);
        this.PH_enabled = true;
    }

    public FusionRTSWithPH(int available_time, int max_playouts, int lookahead, int max_depth,
                           float e_l, float discout_l, float e_g, float discout_g,
                           float e_0, float discout_0, AI policy, EvaluationFunction a_ef,
                           boolean fensa, boolean TR_flag, boolean AWLM_flag) {
        super(available_time, max_playouts, lookahead, max_depth, e_l, discout_l, e_g, discout_g, e_0, discout_0, policy, a_ef, fensa, true, TR_flag, AWLM_flag);
        this.PH_enabled = true;
    }
}