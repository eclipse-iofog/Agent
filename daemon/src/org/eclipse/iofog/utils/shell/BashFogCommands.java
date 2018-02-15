package org.eclipse.iofog.utils.shell;

import java.io.IOException;

public final class BashFogCommands {
    private BashFogCommands() {}

    public static String GET_IOFOG_PACKAGE_INSTALLED_VERSION = "apt-cache policy iofog | grep Installed | awk '{print $2}'";
    public static String GET_IOFOG_PACKAGE_CANDIDATE_VERSION = "apt-cache policy iofog | grep Candidate | awk '{print $2}'";

    public static Double getFogInstalledVersion() throws IOException, InterruptedException {
        String verStr = BashUtil.executeShellCommand(GET_IOFOG_PACKAGE_INSTALLED_VERSION);
        return new Double(verStr);
    }

    public static Double getFogCandidateVersion() throws IOException, InterruptedException {
        String verStr = BashUtil.executeShellCommand(GET_IOFOG_PACKAGE_CANDIDATE_VERSION);
        return new Double(verStr);
    }

}
