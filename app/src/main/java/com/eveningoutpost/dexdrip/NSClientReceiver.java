package com.eveningoutpost.dexdrip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.UtilityModels.Intents;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.UUID;


/**
 * Created by jamorham on 23/02/2016.
 */
public class NSClientReceiver extends BroadcastReceiver {

    private static final String TAG = "jamorham nsreceiver";
    private static final boolean debug = true;
    private static SharedPreferences prefs;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "NSRECEIVER onReceiver: " + intent.getAction());

        // check source
        if (prefs == null) prefs = PreferenceManager.getDefaultSharedPreferences(context);

        final Bundle bundle = intent.getExtras();
        //  BundleScrubber.scrub(bundle);
        final String action = intent.getAction();


        if ((bundle != null) && (debug)) {
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                Log.d(TAG, String.format("%s %s (%s)", key,
                        value.toString(), value.getClass().getName()));
            }
        }

        switch (action) {
            case Intents.ACTION_NEW_SGV:


                String sgv_json = bundle.getString("sgv", "");
                if (sgv_json.length() > 0) {
                    HashMap<String, Object> sgv_map = JoH.JsonStringtoMap(sgv_json);
                    //  if (prefs.getString("dex_collection_method", "").equals("Follower")) {
                    BgReading.bgReadingInsertFromJson(toBgReadingJSON(sgv_map));
                    //  } else {
                    //      Log.i(TAG, "Received nightscout SGV intent but we are not a follower");
                    //  }
                }

                break;

            case Intents.ACTION_NEW_TREATMENT:
                String treatment_json = bundle.getString("treatment", "");
                if (treatment_json.length() > 0) {
                    try {
                        Log.i(TAG, "Processing treatment from NS");
                        Treatments.pushTreatmentFromJson(toTreatmentJSON(JoH.JsonStringtoMap(treatment_json)));
                    } catch (Exception e) {
                        Log.e(TAG, "Got exception processing treatment from NS client " + e.toString());
                    }
                }
                break;

            default:
                Log.e(TAG, "Unknown action! " + action);
                break;
        }

    }

    private String toBgReadingJSON(HashMap<String, Object> sgv_map) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("uuid", UUID.randomUUID().toString());

            jsonObject.put("timestamp", sgv_map.get("mills"));
            jsonObject.put("calculated_value", sgv_map.get("mgdl"));

            double myslope = (double) sgv_map.get("unfiltered") / (double) sgv_map.get("mgdl");
            double filtered_calculated_value = (double) sgv_map.get("filtered") / myslope;

            jsonObject.put("filtered_calculated_value", filtered_calculated_value);
            //jsonObject.put("calibration_flag", calibration_flag);
            jsonObject.put("filtered_data", sgv_map.get("filtered"));
            //jsonObject.put("raw_calculated", raw_calculated);
            jsonObject.put("raw_data", sgv_map.get("unfiltered"));
            Double slope = BgReading.slopefromName(sgv_map.get("direction").toString());
            jsonObject.put("calculated_value_slope", slope);
            if (BgReading.isSlopeNameInvalid(sgv_map.get("direction").toString())) {
                jsonObject.put("hide_slope", true);
            }
            jsonObject.put("noise", sgv_map.get("noise"));
            return jsonObject.toString();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return "";
        }
    }


    private String toTreatmentJSON(HashMap<String, Object> trt_map) {
        JSONObject jsonObject = new JSONObject();
        try {
            // jsonObject.put("uuid", UUID.fromString(trt_map.get("_id").toString()).toString());

            jsonObject.put("timestamp", trt_map.get("mills"));
            jsonObject.put("eventType", trt_map.get("eventType"));
            jsonObject.put("enteredBy", trt_map.get("enteredBy"));
            if (trt_map.containsKey("carbs")) {
                jsonObject.put("carbs", trt_map.get("carbs"));
            }
            if (trt_map.containsKey("insulin")) {
                jsonObject.put("insulin", trt_map.get("insulin"));
            }
            if (trt_map.containsKey("notes")) {
                jsonObject.put("notes", trt_map.get("notes"));
            }
            //double mgdl = trt_map.get("mgdl"); // bg reading

            jsonObject.put("created_at", trt_map.get("created_at"));
            return jsonObject.toString();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return "";
        }
    }
}


