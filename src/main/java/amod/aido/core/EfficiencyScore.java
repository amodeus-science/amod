/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido.core;

/* package */ class EfficiencyScore extends LinComScore {
    /* package */ EfficiencyScore(ScoreParameters scoreParameters) {
        super(scoreParameters.alpha34);
    }
}
