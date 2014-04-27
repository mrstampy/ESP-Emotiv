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

import java.io.Serializable;

public class EmotivFrameInterpreter implements Serializable {
	private static final long serialVersionUID = -8341751531911086446L;
	
	private final byte[] frame;

	public EmotivFrameInterpreter(byte[] frame) {
		assert frame != null && frame.length == 32;

		this.frame = frame;
	}

	public Sensor getSensor() {
		return Sensor.getChannel(frame);
	}

	public Integer getGyroX() {
		return 0xFF & frame[29];// - 102;
	}

	public Integer getGyroY() {
		return 0xFF & frame[30];// - 104;
	}

	public int getQuality() {
		if(isBattery()) return getBatteryLevel();
		
		return Sensor.QUALITY.apply(frame);
	}
	
	public int getValue() {
		if(isBattery()) return getBatteryLevel();
		
		return getSensor().apply(frame);
	}

	public boolean isBattery() {
		return getSensor() == Sensor.BATTERY;
	}

	public byte[] getFrame() {
		return frame;
	}

	/*
	 * @return [0, 100] the percentage level of the battery, zero if no data
	 *         available.
	 */
	private int getBatteryLevel() {
		int battery = 0xFF & frame[0];

		if (battery >= 248) return 100;

		switch (battery) {
		case 247:
			return 99;
		case 246:
			return 97;
		case 245:
			return 93;
		case 244:
			return 89;
		case 243:
			return 85;
		case 242:
			return 82;
		case 241:
			return 77;
		case 240:
			return 72;
		case 239:
			return 66;
		case 238:
			return 62;
		case 237:
			return 55;
		case 236:
			return 46;
		case 235:
			return 32;
		case 234:
			return 20;
		case 233:
			return 12;
		case 232:
			return 6;
		case 231:
			return 4;
		case 230:
			return 3;
		case 229:
			return 2;
		case 228:
		case 227:
		case 226:
			return 1;
		default:
			return -1;
		}
	}

}
