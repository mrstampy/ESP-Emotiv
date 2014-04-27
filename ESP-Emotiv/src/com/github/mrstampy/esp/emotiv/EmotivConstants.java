/*
 * ESP-Emotiv Copyright (C) 2014 Burton Alexander
 * emokit Copyright (C) Samuel Halliday 2012
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
package com.github.mrstampy.esp.emotiv;

public interface EmotivConstants {

	// emokit Copyright (C) Samuel Halliday 2012
	public static final short EMOTIV_VENDOR = (short) 0x8609;
	public static final short EMOTIV_DEVICE = (short) 0x1;
	
	/**
	 * This is probably wrong...need the hardware to confirm
	 * 
	 */
	public static final int EMOTIV_ENDPOINT_1 = 0x1;
}
