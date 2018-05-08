package org.eclipse.iofog.process_manager;

/**
 * Created by Stolbunov D on 15.02.2018.
 */
public enum ElementState {

	CREATED, RUNNING, PAUSED, RESTARTING, STUCK_IN_RESTART, REMOVING, EXITED, DEAD, STOPPED, ATTACH, COMMIT, COPY, CREATE,
	DESTROY, DETACH, DIE, EXEC_CREATE, EXEC_DETACH, EXEC_START, EXPORT, HEALTH_STATUS, KILL, OOM, PAUSE,
	RENAME, RESIZE, RESTART, START, STOP, TOP, UNPAUSE, UPDATE, DELETE, IMPORT, LOAD, PULL, PUSH, SAVE, TAG, UNTAG;

	public static ElementState fromText(String value){
		return valueOf(value.toUpperCase());
	}
}
