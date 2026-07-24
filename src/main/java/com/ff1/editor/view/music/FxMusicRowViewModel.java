package com.ff1.editor.view.music;

import com.ff1.editor.data.AudioResource;
import java.nio.file.Path;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

final class FxMusicRowViewModel {

  private final AudioResource resource;
  private final StringProperty replacement = new SimpleStringProperty("");
  private final BooleanProperty playing = new SimpleBooleanProperty(false);

  FxMusicRowViewModel(AudioResource resource) {
    this.resource = resource;
  }

  AudioResource resource() {
    return resource;
  }

  String entryName() {
    return resource.entryName();
  }

  String label() {
    return resource.label();
  }

  StringProperty replacementProperty() {
    return replacement;
  }

  BooleanProperty playingProperty() {
    return playing;
  }

  void playing(boolean playing) {
    this.playing.set(playing);
  }

  void replacement(Path path) {
    replacement.set(path == null ? "" : path.toString());
  }

  boolean changed() {
    return !replacement.get().isBlank();
  }

  Path replacementPath() {
    return Path.of(replacement.get().trim());
  }
}
