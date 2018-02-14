package org.eclipse.iofog.utils.enums;

import org.eclipse.iofog.utils.exceptions.UnknownVersionCommandException;
import javax.json.JsonObject;


public enum VersionCommand {
    UPGRADE, ROLLBACK;

    public static VersionCommand parseCommandString(String commandStr) throws UnknownVersionCommandException {
        VersionCommand currentCommand = null;
        switch (commandStr) {
            case "upgrade":
                currentCommand = UPGRADE;
                break;
            case "rollback":
                currentCommand = ROLLBACK;
                break;
            default:
                throw new UnknownVersionCommandException();
        }

        return currentCommand;
    }

    public static VersionCommand parseJson(JsonObject versionData) throws UnknownVersionCommandException {
        String versionCommandStr = versionData.getString("versionCommand");
        return parseCommandString(versionCommandStr);
    }
}
