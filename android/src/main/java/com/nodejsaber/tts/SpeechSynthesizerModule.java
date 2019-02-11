package com.nodejsaber.tts;

import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import android.content.Context;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech.Engine;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;

import com.facebook.common.logging.FLog;

import com.facebook.react.common.ReactConstants;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReactContextBaseJavaModule;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.GuardedAsyncTask;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter;


class SpeechSynthesizerModule extends ReactContextBaseJavaModule {
    private Context context;
    private static TextToSpeech tts;
    private Map<String, Promise> ttsPromises = new HashMap<String, Promise>();

    public SpeechSynthesizerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.context = reactContext;
        this.init();
    }

    /**
     * @return the name of this module. This will be the name used to {@code require()} this module
     * from javascript.
     */
    @Override
    public String getName() {
        return "SpeechSynthesizer";
    }

    /**
     * Intialize the TTS module
     */
     public void init(){
        tts = new TextToSpeech(getReactApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.ERROR){
                    FLog.e(ReactConstants.TAG,"Not able to initialized the TTS object");
                }
            }
        });
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onDone(String utteranceId) {
                WritableMap map = Arguments.createMap();
                map.putString("utteranceId", utteranceId);
                getReactApplicationContext().getJSModule(RCTDeviceEventEmitter.class)
                    .emit("FinishSpeechUtterance", map);
                Promise promise = ttsPromises.get(utteranceId);
                promise.resolve(utteranceId);
            }

            @Override
            public void onError(String utteranceId) {
                WritableMap map = Arguments.createMap();
                map.putString("utteranceId", utteranceId);
                getReactApplicationContext().getJSModule(RCTDeviceEventEmitter.class)
                    .emit("ErrorSpeechUtterance", map);
                Promise promise = ttsPromises.get(utteranceId);
                promise.reject(utteranceId);
            }

            @Override
            public void onStart(String utteranceId) {
                WritableMap map = Arguments.createMap();
                map.putString("utteranceId", utteranceId);
                getReactApplicationContext().getJSModule(RCTDeviceEventEmitter.class)
                    .emit("StartSpeechUtterance", map);
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

                    Set<Voice> voices = tts.getVoices();
                    Locale[] locales = Locale.getAvailableLocales();
                    WritableArray data = Arguments.createArray();
                    for (Voice voice : voices) {
                        Locale locale = voice.getLocale();
                        String res = locale.getLanguage();
                        String c = locale.getCountry();

                        if(c != null && c.length() > 0){
                            res = res + "-" + c;
                            String v = locale.getVariant();
                            if(v != null && v.length() > 0){
                                res = res + "-" + v;
                            }
                        }
                        data.pushString(res);
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

                try {
                    if (voice != null && voice.length() > 0) {
                        String[] parts = voice.split("-");
                        String lang = parts[0];
                        String country = "";
                        String variant = "";
                        if(parts.length > 0){
                            country = parts[1];
                            if(parts.length > 1){
                                variant = parts[2];
                            }
                        }
                        tts.setLanguage(new Locale(lang, country, variant));
                    } else {
                        //Setting up default voice
                        tts.setLanguage(new Locale("en"));
                    }
                    //Set the rate if provided by the user
                    if(rate != null){
                        tts.setPitch(rate);
                    }

                    int speakResult = 0;
                    String speechUUID = UUID.randomUUID().toString();
                    if(Build.VERSION.SDK_INT >= 21) {
                        Bundle bundle = new Bundle();
                        bundle.putCharSequence(Engine.KEY_PARAM_UTTERANCE_ID, "");
                        ttsPromises.put(speechUUID, promise);
                        speakResult = tts.speak(text, queueMethod, bundle, speechUUID);
                    } else {
                        HashMap<String, String> map = new HashMap<String, String>();
                        map.put(Engine.KEY_PARAM_UTTERANCE_ID, speechUUID);
                        ttsPromises.put(speechUUID, promise);
                        speakResult = tts.speak(text, queueMethod, map);
                    }

                    if(speakResult < 0) {
                        throw new Exception("Speak failed, make sure that TTS service is installed on you device");
                    }
                } catch (Exception e) {
                    promise.reject(e.getMessage());
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