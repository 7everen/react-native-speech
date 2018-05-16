import { NativeModules } from 'react-native'

const NativeSpeechSynthesizer = NativeModules.SpeechSynthesizer;

/**
 * High-level docs for the SpeechSynthesizer Android API can be written here.
 */

class SpeechSynthesizer {
  static test() {
    return NativeSpeechSynthesizer.reactNativeSpeech();
  }

  static supportedVoices() {
    return NativeSpeechSynthesizer.supportedVoices();
  }

  static isSpeaking() {
    return NativeSpeechSynthesizer.isSpeaking();
  }

  static isPaused() {
    return NativeSpeechSynthesizer.isPaused();
  }

  static resume() {
    return NativeSpeechSynthesizer.resume();
  }

  static pause() {
    return NativeSpeechSynthesizer.pause();
  }

  static stop() {
    return NativeSpeechSynthesizer.stop();
  }

  static speak(options) {
    return NativeSpeechSynthesizer.speak(options);
  }

  static speakWithFinish(options) {
    return NativeSpeechSynthesizer.speakWithFinish(options);
  }
};

module.exports = SpeechSynthesizer;