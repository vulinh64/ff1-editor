package com.ff1.editor.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Transmitter;

public final class MidiPreviewService implements AutoCloseable {

  private static final int END_OF_TRACK = 47;

  private Sequencer sequencer;
  private Transmitter sequencerTransmitter;
  private MidiDevice outputDevice;
  private Receiver outputReceiver;
  private MetaEventListener endListener;
  private Path currentPath;
  private String outputName = "not connected";

  public synchronized PlayResult play(Path path, Runnable onFinished) throws Exception {
    Path audioPath = path.toAbsolutePath().normalize();
    if (!Files.isRegularFile(audioPath)) {
      throw new IOException("Audio file does not exist: " + audioPath);
    }
    if (isPlaying(audioPath)) {
      return PlayResult.ALREADY_PLAYING;
    }

    try {
      playWithCurrentSequencer(audioPath, onFinished);
    } catch (Exception _) {
      closeSequencer();
      playWithCurrentSequencer(audioPath, onFinished);
    }
    return PlayResult.STARTED;
  }

  private boolean isPlaying(Path audioPath) {
    return sequencer != null
        && sequencer.isOpen()
        && sequencer.isRunning()
        && audioPath.equals(currentPath);
  }

  @SuppressWarnings("resource")
  private void playWithCurrentSequencer(Path audioPath, Runnable onFinished) throws Exception {
    Sequencer active = openSequencer();
    stopPlayback();
    MetaEventListener listener =
        meta -> {
          if (meta.getType() == END_OF_TRACK) {
            closeSequencer();
            if (onFinished != null) {
              onFinished.run();
            }
          }
        };
    active.setLoopCount(0);
    active.setSequence(MidiSystem.getSequence(audioPath.toFile()));
    active.setTickPosition(0);
    active.addMetaEventListener(listener);
    endListener = listener;
    currentPath = audioPath;
    active.start();
  }

  private Sequencer openSequencer() throws Exception {
    if (sequencer != null && sequencer.isOpen()) {
      return sequencer;
    }
    sequencer = MidiSystem.getSequencer(false);
    sequencer.open();
    connectOutput(sequencer);
    return sequencer;
  }

  private void connectOutput(Sequencer active) throws Exception {
    OutputConnection output = openPreferredOutput();
    boolean connected = false;
    try {
      sequencerTransmitter = active.getTransmitter();
      sequencerTransmitter.setReceiver(output.receiver());
      outputDevice = output.device();
      outputReceiver = output.receiver();
      outputName = output.name();
      connected = true;
    } finally {
      if (!connected) {
        output.close();
      }
    }
  }

  private static OutputConnection openPreferredOutput() throws Exception {
    OutputConnection virtualMidiSynth = openNamedOutput("virtualmidisynth");
    if (virtualMidiSynth != null) {
      return virtualMidiSynth;
    }
    OutputConnection coolSoftMapper = openNamedOutput("coolsoft midimapper");
    if (coolSoftMapper != null) {
      return coolSoftMapper;
    }
    Receiver receiver = MidiSystem.getReceiver();
    return new OutputConnection(null, receiver, "System default MIDI receiver");
  }

  private static OutputConnection openNamedOutput(String needle) throws Exception {
    for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
      if (!info.getName().toLowerCase(Locale.ROOT).contains(needle)) {
        continue;
      }
      MidiDevice device = MidiSystem.getMidiDevice(info);
      if (device.getMaxReceivers() == 0) {
        continue;
      }
      try {
        device.open();
        return new OutputConnection(device, device.getReceiver(), info.getName());
      } catch (Exception _) {
        if (device.isOpen()) {
          device.close();
        }
      }
    }
    return null;
  }

  public synchronized boolean stop() {
    if (sequencer == null) {
      return false;
    }
    closeSequencer();
    return true;
  }

  private synchronized void stopPlayback() {
    try {
      if (endListener != null) {
        sequencer.removeMetaEventListener(endListener);
      }
      sequencer.stop();
    } finally {
      endListener = null;
      currentPath = null;
    }
  }

  private synchronized void closeSequencer() {
    if (sequencer == null) {
      return;
    }
    try {
      stopPlayback();
    } finally {
      if (sequencerTransmitter != null) {
        sequencerTransmitter.close();
      }
      if (outputReceiver != null) {
        outputReceiver.close();
      }
      if (outputDevice != null && outputDevice.isOpen()) {
        outputDevice.close();
      }
      sequencer.close();
      sequencer = null;
      sequencerTransmitter = null;
      outputReceiver = null;
      outputDevice = null;
      outputName = "not connected";
    }
  }

  public synchronized String outputName() {
    return outputName;
  }

  public enum PlayResult {
    STARTED,
    ALREADY_PLAYING
  }

  private record OutputConnection(MidiDevice device, Receiver receiver, String name) {

    private void close() {
      receiver.close();
      if (device != null && device.isOpen()) {
        device.close();
      }
    }
  }

  @Override
  public void close() {
    stop();
  }
}
