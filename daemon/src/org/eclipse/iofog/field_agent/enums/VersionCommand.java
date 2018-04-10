package org.eclipse.iofog.field_agent.enums;

import org.eclipse.iofog.field_agent.exceptions.UnknownVersionCommandException;
import javax.json.JsonObject;
import java.util.Arrays;
import java.util.Optional;

import static org.eclipse.iofog.utils.Constants.SNAP_COMMON;

public enum VersionCommand {

	UPGRADE {
		@Override
		public String getScript() {
			return UPGRADE_VERSION_SCRIPT;
		}

		@Override
		public String toString() {
			return "upgrade";
		}
	}, ROLLBACK {
		@Override
		public String getScript() {
			return ROLLBACK_VERSION_SCRIPT;
		}

		@Override
		public String toString() {
			return "rollback";
		}
	};

	public final static String CHANGE_VERSION_SCRIPTS_DIR = SNAP_COMMON + "/usr/share/iofog/";
	public final static String UPGRADE_VERSION_SCRIPT = CHANGE_VERSION_SCRIPTS_DIR + "upgrade.sh";
	public final static String ROLLBACK_VERSION_SCRIPT = CHANGE_VERSION_SCRIPTS_DIR + "rollback.sh";

	public static VersionCommand parseCommandString(String commandStr) throws UnknownVersionCommandException {
		return Optional.of(Arrays.stream(VersionCommand.values())
				.filter(versionCommand -> versionCommand.toString().equals(commandStr))
				.findFirst()
				.orElseThrow(UnknownVersionCommandException::new))
				.get();
	}

	public static VersionCommand parseJson(JsonObject versionData) throws UnknownVersionCommandException {
		String versionCommandStr = versionData.getString("versionCommand");
		return parseCommandString(versionCommandStr);
	}

	public abstract String getScript();
}
