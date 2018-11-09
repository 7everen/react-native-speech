
package com.nodejsaber.tts;

import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.GuardedAsyncTask;
import com.facebook.react.common.ReactConstants;
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter;

import java.util.Map;
import java.util.Locale;
import java.util.HashMap;
import java.util.UUID;

import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.content.Context;
import android.annotation.TargetApi;
import android.os.Bundle;
import android.speech.tts.TextToSpeech.Engine;

class SpeechSynthesizerModule extends ReactContextBaseJavaModule {

    private static TextToSpeech tts;
	private boolean IS_READY;
    private Context context;
    private Map<String, Promise> ttsPromises = new HashMap<String, Promise>();

    public SpeechSynthesizerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.context = reactContext;
        this.init();
    }

    @Override
    public String getName() {
        return "SpeechSynthesizer";
    }

  
     public void init(){
        tts = new TextToSpeech(getReactApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                
                if(status == TextToSpeech.SUCCESS){
					IS_READY = true;
                } else {
                    IS_READY = false;
                    FLog.e(ReactConstants.TAG,"Not able to initialized the TTS object");
                }
            }
        });
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                WritableMap map = Arguments.createMap();
                map.putString("utteranceId", utteranceId);
                getReactApplicationContext().getJSModule(RCTDeviceEventEmitter.class)
                    .emit("StartSpeechUtterance", map);
            }

            @Override
            public void onDone(String utteranceId) {
                WritableMap map = Arguments.createMap();
                map.putString("utteranceId", utteranceId);
                getReactApplicationContext().getJSModule(RCTDeviceEventEmitter.class)
                    .emit("FinishSpeechUtterance", map);
                Promise promise = ttsPromises.get(utteranceId);
                if (promise != null) {
                    promise.resolve(utteranceId);
                    ttsPromises.remove(utteranceId);
                }
            }


            @Override
            public void onError(String utteranceId) {
                WritableMap map = Arguments.createMap();
                map.putString("utteranceId", utteranceId);
                getReactApplicationContext().getJSModule(RCTDeviceEventEmitter.class)
                    .emit("ErrorSpeechUtterance", map);
                Promise promise = ttsPromises.get(utteranceId);
                if (promise != null) {
                    promise.reject(utteranceId);
                    ttsPromises.remove(utteranceId);
                } 

            }

            @Override
			public void onStop(String utteranceId, boolean interrupted) {
                WritableMap map = Arguments.createMap();
                map.putString("utteranceId", utteranceId);
                getReactApplicationContext().getJSModule(RCTDeviceEventEmitter.class)
                .emit("StopSpeechUtterance", map);
                Promise promise = ttsPromises.get(utteranceId);
                if (promise != null) {
                    promise.resolve(utteranceId);
                    ttsPromises.remove(utteranceId);
                }
			}



        });
    }

    @ReactMethod
    public void supportedVoices(final Promise promise) {
        new GuardedAsyncTask<Void, Void>(getReactApplicationContext()) {
            @Override
            protected void doInBackgroundGuarded(Void... params) {
                try{
                    if(tts == null){
                        init();
                    }
                    Locale[] locales = Locale.getAvailableLocales();
                    WritableArray data = Arguments.createArray();
                    for (Locale locale : locales) {
                        int res = tts.isLanguageAvailable(locale);
                        if(res == TextToSpeech.LANG_COUNTRY_AVAILABLE){
                            data.pushString(locale.getLanguage());
                        }
                    }
                    promise.resolve(data);
                } catch (Exception e) {
                    promise.reject(e.getMessage());
                }
            }
        }.execute();
    }

    @ReactMethod
    public void isSpeaking(final Promise promise) {
        new GuardedAsyncTask<Void,Void>(getReactApplicationContext()){
            @Override
            protected  void doInBackgroundGuarded(Void... params){
                try {
                    if (tts.isSpeaking()) {
                        promise.resolve(true);
                    } else {
                        promise.resolve(false);
                    }
                } catch (Exception e){
                    promise.reject(e.getMessage());
                }
            }
        }.execute();
    }

    @ReactMethod
    public void isPaused(final Promise promise) {
        promise.resolve(false);
    }

    @ReactMethod
    public void resume(final Promise promise) {
        FLog.e(ReactConstants.TAG, "resume function doesn\'t exists on android !");
        promise.resolve(false);
    }

    @ReactMethod
    public void pause(final Promise promise) {
        FLog.e(ReactConstants.TAG, "pause function doesn\'t exists on android !");
        promise.resolve(false);
    }

    @ReactMethod
    public void stop(final Promise promise) {
        new GuardedAsyncTask<Void,Void>(getReactApplicationContext()){
            @Override
            protected void doInBackgroundGuarded(Void... params){ 
                try {
                    tts.stop();
                    promise.resolve(true);
                } catch (Exception e){
                    promise.reject(e.getMessage());
                }
            }
        }.execute();
    }

    private void _speak(final ReadableMap args, final Promise promise) {
        new GuardedAsyncTask<Void, Void>(getReactApplicationContext()) {
            @Override
            protected void doInBackgroundGuarded(Void... params) {
                if(tts == null){
                    init();
                }
                String text = args.hasKey("text") ? args.getString("text") : null;
                String voice = args.hasKey("voice") ? args.getString("voice") : null;
                Boolean forceStop = args.hasKey("forceStop") ?  args.getBoolean("forceStop") : true;
                Float rate = args.hasKey("rate") ? (float)  args.getDouble("rate") : null;
                int queueMethod = TextToSpeech.QUEUE_FLUSH;

                if(tts.isSpeaking()){
                    //Force to stop and start new speech
                    if(forceStop != null && forceStop){
                        tts.stop();
                    } else {
                        queueMethod = TextToSpeech.QUEUE_ADD;
                    }
                }

                if(args.getString("text") == null || text == ""){
                    promise.reject("Text cannot be blank");
                }

                String speechUUID = UUID.randomUUID().toString();
                ttsPromises.put(speechUUID, promise);

                try {
                    if (voice != null && voice != "") {
                        tts.setLanguage(new Locale(voice));
                    } else {
                        //Setting up default voice
                        tts.setLanguage(new Locale("en"));
                    }
                    //Set the rate if provided by the user
                    if(rate != null){
                        tts.setPitch(rate);
                    }

                    int speakResult = 0;
                    if(Build.VERSION.SDK_INT >= 21) {
                        Bundle bundle = new Bundle();
                        bundle.putCharSequence(Engine.KEY_PARAM_UTTERANCE_ID, "");
                        speakResult = tts.speak(text, queueMethod, bundle, speechUUID);
                    } else {
                        HashMap<String, String> map = new HashMap<String, String>();
                        map.put(Engine.KEY_PARAM_UTTERANCE_ID, speechUUID);
                        speakResult = tts.speak(text, queueMethod, map);
                    }

                    if(speakResult < 0) {
                        throw new Exception("Speak failed, make sure that TTS service is installed on you device");
                    }
                } catch (Exception e) {
                    Promise promise = ttsPromises.get(speechUUID);
                    if (promise != null) {
                        promise.reject(e.getMessage());
                        ttsPromises.remove(speechUUID);
                    } 
                }
            }
        }.execute();
    }
    

    @ReactMethod
    public void speak(final ReadableMap args, final Promise promise) {
       _speak(args, promise);
    }
    

    @ReactMethod
    public void speakWithFinish(final ReadableMap args, final Promise promise) {
        _speak(args, promise);
    }
}