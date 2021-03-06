package com.marginallyclever.robotOverlord.entity.scene;

import javax.vecmath.Matrix4d;

public abstract interface Moveable {
	// get the movable thing's current pose
	public abstract Matrix4d getPoseWorld();
	
	// force move to a given pose
	public abstract void setPoseWorld(Matrix4d m);

	/**
	 * Ask this entity "can you move to newWorldPose?"
	 * @param newWorldPose the desired world pose of the subject.
	 * @return true if it can.
	 */
	public abstract boolean canYouMoveTo(Matrix4d newWorldPose);
	
	// TODO if canYouMoveTo says no, get reasons why?
}
