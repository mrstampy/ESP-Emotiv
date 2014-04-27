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

import com.github.mrstampy.esp.multiconnectionsocket.subscription.MultiConnectionSubscriptionRequest;

public class EmotivSubscriptionRequest implements MultiConnectionSubscriptionRequest<Sensor> {

	private static final long serialVersionUID = 6054403295795982525L;
	
	private final Sensor[] sensors;
	
	public EmotivSubscriptionRequest(Sensor...sensors) {
		assert sensors != null && sensors.length > 0;
		
		this.sensors = sensors;
	}

	@Override
	public Sensor[] getEventTypes() {
		return sensors;
	}

	@Override
	public boolean containsEventType(Sensor type) {
		for(Sensor s : sensors) {
			if(s == type) return true;
		}
		
		return false;
	}

}
