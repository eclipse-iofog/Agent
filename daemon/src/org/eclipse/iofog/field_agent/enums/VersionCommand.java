package org.eclipse.iofog.field_agent.enums;

import org.eclipse.iofog.field_agent.exceptions.UnknownVersionCommandException;
import javax.json.JsonObject;
import java.util.Arrays;
import java.util.Optional;

import static org.eclipse.iofog.utils.Constants.SNAP_COMMON;

public enum VersionCommand {

	UPGRADE("upgrade") {
		@Override
		public String getScript() {
			return UPGRADE_VERSION_SCRIPT;
		}
	}, ROLLBACK("rollback") {
		@Override
		public String getScript() {
			return ROLLBACK_VERSION_SCRIPT;
		}
	};

	private String commandStr;

	public static String CHANGE_VERSION_SCRIPTS_DIR = SNAP_COMMON + "/usr/share/iofog/";
	public static String UPGRADE_VERSION_SCRIPT = CHANGE_VERSION_SCRIPTS_DIR + "upgrade.sh";
	public static String ROLLBACK_VERSION_SCRIPT = CHANGE_VERSION_SCRIPTS_DIR + "rollback.sh";

	VersionCommand(String commandStr) {
		this.commandStr = commandStr;
	}

	public String getCommandStr() {
		return this.commandStr;
	}

	public static VersionCommand parseCommandString(String commandStr) throws UnknownVersionCommandException {
		Optional<VersionCommand> versionCommandOptional =  Arrays.stream(VersionCommand.values())
				.filter(versionCommand -> versionCommand.getCommandStr().equals(commandStr))
				.findFirst();
		if (versionCommandOptional.isPresent()) {
			return versionCommandOptional.get();
		} else {
			throw new UnknownVersionCommandException();
		}
	}

	public static VersionCommand parseJson(JsonObject versionData) throws UnknownVersionCommandException {
		String versionCommandStr = versionData.getString("versionCommand");
		return parseCommandString(versionCommandStr);
	}

	public abstract String getScript();
}
