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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.usb.UsbClaimException;
import javax.usb.UsbConfiguration;
import javax.usb.UsbDevice;
import javax.usb.UsbDeviceDescriptor;
import javax.usb.UsbDisconnectedException;
import javax.usb.UsbEndpoint;
import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import javax.usb.UsbInterface;
import javax.usb.UsbPipe;
import javax.usb.event.UsbPipeDataEvent;
import javax.usb.event.UsbPipeErrorEvent;
import javax.usb.event.UsbPipeListener;

import javolution.util.FastList;

import org.apache.mina.core.service.IoHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Scheduler;
import rx.Scheduler.Recurse;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import com.github.mrstampy.esp.emotiv.subscription.EmotivEvent;
import com.github.mrstampy.esp.emotiv.subscription.EmotivEventListener;
import com.github.mrstampy.esp.emotiv.subscription.EmotivFrameInterpreter;
import com.github.mrstampy.esp.multiconnectionsocket.AbstractMultiConnectionSocket;
import com.github.mrstampy.esp.multiconnectionsocket.MultiConnectionSocketException;

/**
 * Emotiv implementation of the {@link AbstractMultiConnectionSocket}.
 * 
 * @author burton
 * 
 */
public class MultiConnectEmotivSocket extends AbstractMultiConnectionSocket<byte[]> implements EmotivConstants {
	private static final Logger log = LoggerFactory.getLogger(MultiConnectEmotivSocket.class);
	private static final int MAX_NUM_OUTSTANDING = 10;

	private UsbInterface usbInterface;
	private UsbPipe emotivPipe;
	private UsbDevice emotiv;

	private volatile boolean connected;

	private List<EmotivEventListener> listeners = new FastList<EmotivEventListener>();

	private EmotivSubscriptionHandlerAdapter subscriptionHandlerAdapter;

	private AtomicInteger numOutstanding = new AtomicInteger();

	private Scheduler scheduler = Schedulers.executor(Executors.newScheduledThreadPool(5));

	// wat is dis?
	private static final List<byte[]> supportedConsumer = new ArrayList<byte[]>();
	// private static final List<byte[]> supportedResearch = new ArrayList<byte[]>();
	private volatile boolean research = false;

	private Cipher cipher;

	// emokit Copyright (C) Samuel Halliday 2012
	static {
		supportedConsumer.add(new byte[] { 33, -1, 31, -1, 30, 0, 0, 0 });
		supportedConsumer.add(new byte[] { 32, -1, 31, -1, 30, 0, 0, 0 });
		supportedConsumer.add(new byte[] { -32, -1, 31, -1, 0, 0, 0, 0 }); // unconfirmed
	}

	public MultiConnectEmotivSocket() throws IOException {
		this(false);
	}

	public MultiConnectEmotivSocket(boolean broadcasting) throws IOException {
		super(broadcasting);
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

	public String getSerial() throws IOException, UsbDisconnectedException, UsbException {
		if (!isConnected()) return null;

		String serial = emotiv.getSerialNumberString();
		if (!serial.startsWith("SN") || serial.length() != 16) throw new IOException("Bad serial: " + serial);

		return serial;
	}

	@Override
	public boolean isConnected() {
		return connected;
	}

	@Override
	protected void startImpl() throws MultiConnectionSocketException {
		try {
			emotivStart();
		} catch (Exception e) {
			stop();
			throw new MultiConnectionSocketException(e);
		}
	}

	private void emotivStart() throws Exception {
		numOutstanding.set(0);
		initDevice();
		usbInterface.claim();
		emotivPipe.open();
		connected = true;
		startReadThread();
	}

	@Override
	protected void stopImpl() {
		try {
			emotivStop();
		} catch (Exception e) {
			log.error("Problem closing Emotiv", e);
		}
	}

	private void emotivStop() throws UsbException, UsbClaimException {
		connected = false;
		if (emotivPipe != null && emotivPipe.isOpen()) {
			emotivPipe.abortAllSubmissions();
			emotivPipe.close();
		}
		if (usbInterface != null && usbInterface.isClaimed()) usbInterface.release();
	}

	@Override
	protected IoHandler getHandlerAdapter() {
		subscriptionHandlerAdapter = new EmotivSubscriptionHandlerAdapter(this);
		return subscriptionHandlerAdapter;
	}

	@Override
	protected void parseMessage(byte[] message) {
		try {
			byte[] decrypted = cipher.doFinal(message);
			process(new EmotivEvent(new EmotivFrameInterpreter(decrypted)));
		} catch (Exception e) {
			log.error("Could not add sample", e);
		}
	}

	private void process(EmotivEvent event) {
		notifyListeners(event);
		if (canBroadcast()) subscriptionHandlerAdapter.sendMultiConnectionEvent(event);
	}

	private void notifyListeners(EmotivEvent event) {
		if (listeners.isEmpty()) return;

		for (EmotivEventListener l : listeners) {
			l.emotivEventPerformed(event);
		}
	}

	private void startReadThread() {
		scheduler.scheduleRecursive(new Action1<Scheduler.Recurse>() {

			@Override
			public void call(Recurse t1) {
				if (!isConnected()) return;

				addBytesToPipe();

				t1.schedule();
			}
		});
	}

	private void addBytesToPipe() {
		try {
			byte[] b = new byte[32];
			emotivPipe.asyncSubmit(b);

			int num = numOutstanding.incrementAndGet();
			while (isConnected() && num > MAX_NUM_OUTSTANDING) {
				Thread.sleep(1);
				num = numOutstanding.get();
			}
		} catch (Exception e) {
			log.error("Problem reading from the Nia", e);
		}
	}

	private void initDevice() throws Exception {
		UsbHub hub = UsbHostManager.getUsbServices().getRootUsbHub();

		emotiv = findDevice(hub, EMOTIV_VENDOR, EMOTIV_DEVICE);
		if (emotiv == null) {
			String msg = "No Emotiv found. Ensure the device is connected.";
			log.error(msg);
			throw new RuntimeException(msg);
		}

		featureReport();
		initCipher();

		UsbConfiguration config = emotiv.getActiveUsbConfiguration();
		usbInterface = config.getUsbInterface((byte) EMOTIV_DEVICE);

		// TODO find the correct value for EMOTIV_ENDPOINT_1
		UsbEndpoint ue = usbInterface.getUsbEndpoint((byte) EMOTIV_ENDPOINT_1);
		emotivPipe = ue.getUsbPipe();

		emotivPipe.addUsbPipeListener(new UsbPipeListener() {

			@Override
			public void errorEventOccurred(UsbPipeErrorEvent event) {
				log.error("Unexpected exception reading Emotiv", event.getUsbException());
			}

			@Override
			public void dataEventOccurred(UsbPipeDataEvent event) {
				Observable.from(event).subscribe(new Action1<UsbPipeDataEvent>() {

					@Override
					public void call(UsbPipeDataEvent t1) {
						if (isConnected()) {
							numOutstanding.decrementAndGet();
							publishMessage(t1.getData());
						}
					}
				});
			}
		});
	}

	// emokit Copyright (C) Samuel Halliday 2012
	private void initCipher() throws Exception {
		cipher = Cipher.getInstance("AES/ECB/NoPadding");
		SecretKeySpec key = getKey();
		cipher.init(Cipher.DECRYPT_MODE, key);
	}

	// emokit Copyright (C) Samuel Halliday 2012
	private SecretKeySpec getKey() throws IOException, UsbDisconnectedException, UsbException {
		if (!isConnected()) return null;

		String serial = getSerial();

		byte[] raw = serial.getBytes();
		assert raw.length == 16;
		byte[] bytes = new byte[16];

		bytes[0] = raw[15];
		bytes[1] = 0;
		bytes[2] = raw[14];
		bytes[3] = research ? (byte) 'H' : (byte) 'T';
		bytes[4] = research ? raw[15] : raw[13];
		bytes[5] = research ? (byte) 0 : 16;
		bytes[6] = research ? raw[14] : raw[12];
		bytes[7] = research ? (byte) 'T' : (byte) 'B';
		bytes[8] = research ? raw[13] : raw[15];
		bytes[9] = research ? (byte) 16 : 0;
		bytes[10] = research ? raw[12] : raw[14];
		bytes[11] = research ? (byte) 'B' : (byte) 'H';
		bytes[12] = raw[13];
		bytes[13] = 0;
		bytes[14] = raw[12];
		bytes[15] = 'P';

		return new SecretKeySpec(bytes, "AES");
	}

	// emokit Copyright (C) Samuel Halliday 2012
	private void featureReport() {
		// HIDDevice dev = info.open();
		// try {
		// byte[] report = new byte[9];
		// int size = dev.getFeatureReport(report);
		// byte[] result = Arrays.copyOf(report, size);
		//
		// for (byte[] check : supportedConsumer) {
		// if (Arrays.equals(check, result)) {
		// return dev;
		// }
		// }
		//
		// for (byte[] check : supportedResearch) {
		// if (Arrays.equals(check, result)) {
		// research = true;
		// return dev;
		// }
		// }
		//
		// dev.close();
		// } catch (Exception e) {
		// dev.close();
		// }
	}

	@SuppressWarnings("unchecked")
	private UsbDevice findDevice(UsbHub hub, short vendorId, short productId) {
		for (UsbDevice device : (List<UsbDevice>) hub.getAttachedUsbDevices()) {
			UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
			if (desc.idVendor() == vendorId && desc.idProduct() == productId) return device;
			if (device.isUsbHub()) {
				device = findDevice((UsbHub) device, vendorId, productId);
				if (device != null) return device;
			}
		}

		return null;
	}

}
