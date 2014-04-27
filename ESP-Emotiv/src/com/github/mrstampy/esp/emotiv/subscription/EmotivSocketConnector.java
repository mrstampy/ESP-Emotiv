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
package com.github.mrstampy.esp.emotiv.subscription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import com.github.mrstampy.esp.multiconnectionsocket.AbstractSocketConnector;
import com.github.mrstampy.esp.multiconnectionsocket.event.AbstractMultiConnectionEvent;

/**
 * {@link AbstractSocketConnector} implementation for the Emotiv.
 * 
 * @author burton
 * 
 */
public class EmotivSocketConnector extends AbstractSocketConnector<Sensor> {

	private List<EmotivEventListener> listeners = Collections.synchronizedList(new ArrayList<EmotivEventListener>());
	//@formatter:off
	private EnumSet<Sensor> subscribables = 
			EnumSet.of(
					Sensor.AF3, 
					Sensor.AF4, 
					Sensor.BATTERY, 
					Sensor.F3, 
					Sensor.F4, 
					Sensor.F7, 
					Sensor.F8, 
					Sensor.FC5, 
					Sensor.FC6, 
					Sensor.O1, 
					Sensor.O2, 
					Sensor.P7, 
					Sensor.P8, 
					Sensor.T7, 
					Sensor.T8);
	//@formatter:on

	public EmotivSocketConnector(String socketBroadcasterHost) {
		super(socketBroadcasterHost);
	}

	public void addListener(EmotivEventListener l) {
		if (l != null && !listeners.contains(l)) listeners.add(l);
	}

	public void removeListener(EmotivEventListener l) {
		if (l != null) listeners.remove(l);
	}

	public void clearListeners() {
		listeners.clear();
	}

	public boolean subscribe(Sensor... sensors) {
		assert noQualitySensor(sensors);

		return subscribe(new EmotivSubscriptionRequest(sensors));
	}

	private boolean noQualitySensor(Sensor[] sensors) {
		for (Sensor sensor : sensors) {
			if (!subscribables.contains(sensor)) return false;
		}

		return true;
	}

	@Override
	public boolean subscribeAll() {
		return subscribe(subscribables.toArray(new Sensor[] {}));
	}

	@Override
	protected void processEvent(AbstractMultiConnectionEvent<Sensor> message) {
		EmotivEvent event = (EmotivEvent) message;

		for (EmotivEventListener l : listeners) {
			l.emotivEventPerformed(event);
		}
	}

}
