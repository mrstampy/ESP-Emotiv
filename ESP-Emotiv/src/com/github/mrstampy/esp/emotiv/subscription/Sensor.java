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

public enum Sensor {
	//@formatter:off
  QUALITY(99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112),
  F3(10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7),
  FC5(28, 29, 30, 31, 16, 17, 18, 19, 20, 21, 22, 23, 8, 9),
  AF3(46, 47, 32, 33, 34, 35, 36, 37, 38, 39, 24, 25, 26, 27),
  F7(48, 49, 50, 51, 52, 53, 54, 55, 40, 41, 42, 43, 44, 45),
  T7(66, 67, 68, 69, 70, 71, 56, 57, 58, 59, 60, 61, 62, 63),
  P7(84, 85, 86, 87, 72, 73, 74, 75, 76, 77, 78, 79, 64, 65),
  O1(102, 103, 88, 89, 90, 91, 92, 93, 94, 95, 80, 81, 82, 83),
  O2(140, 141, 142, 143, 128, 129, 130, 131, 132, 133, 134, 135, 120, 121),
  P8(158, 159, 144, 145, 146, 147, 148, 149, 150, 151, 136, 137, 138, 139),
  T8(160, 161, 162, 163, 164, 165, 166, 167, 152, 153, 154, 155, 156, 157),
  F8(178, 179, 180, 181, 182, 183, 168, 169, 170, 171, 172, 173, 174, 175),
  AF4(196, 197, 198, 199, 184, 185, 186, 187, 188, 189, 190, 191, 176, 177),
  FC6(214, 215, 200, 201, 202, 203, 204, 205, 206, 207, 192, 193, 194, 195),
  F4(216, 217, 218, 219, 220, 221, 222, 223, 208, 209, 210, 211, 212, 213),
  BATTERY();
	//@formatter:on

	private final int[] bits;

	Sensor(int... bits) {
		this.bits = bits;
	}

	public int apply(byte[] frame) {
		if(bits == null) return -1;
		
		int level = 0;
		for (int i = bits.length - 1; i >= 0; --i) {
			level <<= 1;
			int b = (bits[i] >> 3) + 1;
			int o = bits[i] % 8;
			level |= ((0xFF & frame[b]) >>> o) & 1;
		}
		return level;
	}

	public static Sensor getChannel(byte[] frame) {
		byte counter = frame[0];
		
		if(counter < 0) return BATTERY;
		
		if (64 <= counter && counter <= 75) {
			counter = (byte) (counter - 64);
		}
		// TODO: https://github.com/fommil/emokit-java/issues/3
		// else if (76 <= counter) {
		// counter = (byte) ((counter - 76) % 4 + 15);
		// }
		switch (counter) {
		case 0:
			return F3;
		case 1:
			return FC5;
		case 2:
			return AF3;
		case 3:
			return F7;
		case 4:
			return T7;
		case 5:
			return P7;
		case 6:
			return O1;
		case 7:
			return O2;
		case 8:
			return P8;
		case 9:
			return T8;
		case 10:
			return F8;
		case 11:
			return AF4;
		case 12:
			return FC6;
		case 13:
			return F4;
		case 14:
			return F8;
		case 15:
			return AF4;
		default:
			return null;
		}
	}

}
