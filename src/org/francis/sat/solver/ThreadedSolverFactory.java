package org.francis.sat.solver;

import com.ericsson.otp.erlang.OtpErlangPid;
import com.ericsson.otp.erlang.OtpNode;

public interface ThreadedSolverFactory {

    public abstract void createAndRunSolversLocal(OtpErlangPid listeningProcess,
            OtpNode node, int networkSize, int worksharingThreshold);
}