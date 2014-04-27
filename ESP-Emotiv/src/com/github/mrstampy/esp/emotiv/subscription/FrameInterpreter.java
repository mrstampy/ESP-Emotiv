package com.github.mrstampy.esp.emotiv.subscription;

public interface FrameInterpreter {

	public abstract Sensor getSensor();

	public abstract Integer getGyroX();

	public abstract Integer getGyroY();

	public abstract int getQuality();

	public abstract int getValue();

	public abstract boolean isBattery();

	public abstract byte[] getFrame();

}