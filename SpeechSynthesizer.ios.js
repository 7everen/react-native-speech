import { NativeModules } from 'react-native';

const NativeSpeechSynthesizer = NativeModules.SpeechSynthesizer;


class SpeechSynthesizer {
  static supportedVoices() {
    return new Promise(((resolve, reject) => {
      NativeSpeechSynthesizer.speechVoices((error, locales) => {
        if (error) {
          return reject(error);
        }

        resolve(locales);
      });
    }));
  }

  static isSpeaking() {
    return new Promise(((resolve, reject) => {
      NativeSpeechSynthesizer.speaking((error, speaking) => {
        if (error) {
          return reject(error);
        }

        if (speaking === 1) {
          resolve(true);
        } else {
          resolve(false);
        }
      });
    }));
  }

  static isPaused() {
    return new Promise(((resolve, reject) => {
      NativeSpeechSynthesizer.paused((error, paused) => {
        if (error) {
          return reject(error);
        }

        if (paused === 1) {
          resolve(true);
        } else {
          resolve(false);
        }
      });
    }));
  }

  static resume() {
    return NativeSpeechSynthesizer.continueSpeakingAtBoundary();
  }

  static pause() {
    return NativeSpeechSynthesizer.pauseSpeakingAtBoundary();
  }

  static stop() {
    return NativeSpeechSynthesizer.stopSpeakingAtBoundary();
  }

  static speak(options) {
    return new Promise(((resolve, reject) => {
      NativeSpeechSynthesizer.speakUtterance(options, (error, success) => {
        if (error) {
          return reject(error);
        }

        resolve(true);
      });
    }));
  }

  static speakWithFinish(options) {
    return new Promise(((resolve, reject) => {
      NativeSpeechSynthesizer.speakUtteranceWithFinish(options, (error, success) => {
        if (error) {
          return reject(error);
        }
        resolve(true);
      });
    }));
  }
}

module.exports = SpeechSynthesizer;
