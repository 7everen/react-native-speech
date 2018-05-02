import { NativeModules } from 'react-native'

const NativeSpeechSynthesizer = NativeModules.SpeechSynthesizer;

/**
 * High-level docs for the SpeechSynthesizer Android API can be written here.
 */

export default {
  test () {
    return NativeSpeechSynthesizer.reactNativeSpeech();
  },

  supportedVoices() {
    return NativeSpeechSynthesizer.supportedVoices();
  },

  isSpeaking() {
    return NativeSpeechSynthesizer.isSpeaking();
  },

  isPaused() {
    return NativeSpeechSynthesizer.isPaused();
  },

  resume() {
    return NativeSpeechSynthesizer.resume();
  },

  pause() {
    return NativeSpeechSynthesizer.pause();
  },

  stop() {
    return NativeSpeechSynthesizer.stop();
  },

  speak(options) {
    return NativeSpeechSynthesizer.speak(options);
  }
};