package org.eclipse.iofog.utils.shell;

import java.io.IOException;

public final class BashFogCommands {
    private BashFogCommands() {}

    //TODO: this is only debian commands. create redhat commands later
    public static String GET_IOFOG_PACKAGE_INSTALLED_VERSION = "apt-cache policy iofog | grep Installed | awk '{print $2}'";
    public static String GET_IOFOG_PACKAGE_CANDIDATE_VERSION = "apt-cache policy iofog | grep Candidate | awk '{print $2}'";

    public static String getFogInstalledVersion() throws IOException, InterruptedException {
        return BashUtil.executeShellCommand(GET_IOFOG_PACKAGE_INSTALLED_VERSION);
    }

    public static String  getFogCandidateVersion() throws IOException, InterruptedException {
        return BashUtil.executeShellCommand(GET_IOFOG_PACKAGE_CANDIDATE_VERSION);
    }

}
