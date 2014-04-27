/*
 * ESP-Emotiv Copyright (C) 2014 Burton Alexander
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 * 
 */
package com.github.mrstampy.esp.emotiv.subscription;

import com.github.mrstampy.esp.multiconnectionsocket.event.AbstractMultiConnectionEvent;

public class EmotivEvent extends AbstractMultiConnectionEvent<Sensor> implements FrameInterpreter {
	private static final long serialVersionUID = 3880422949695371519L;
	
	private final FrameInterpreter interpreter;

	public EmotivEvent(EmotivFrameInterpreter interpreter) {
		super(interpreter.getSensor());
		this.interpreter = interpreter;
	}

	private FrameInterpreter getInterpreter() {
		return interpreter;
	}

	@Override
	public Sensor getSensor() {
		return getInterpreter().getSensor();
	}

	@Override
	public Integer getGyroX() {
		return getInterpreter().getGyroX();
	}

	@Override
	public Integer getGyroY() {
		return getInterpreter().getGyroY();
	}

	@Override
	public int getQuality() {
		return getInterpreter().getQuality();
	}

	@Override
	public int getValue() {
		return getInterpreter().getValue();
	}

	@Override
	public boolean isBattery() {
		return getInterpreter().isBattery();
	}

	@Override
	public byte[] getFrame() {
		return getInterpreter().getFrame();
	}

}
