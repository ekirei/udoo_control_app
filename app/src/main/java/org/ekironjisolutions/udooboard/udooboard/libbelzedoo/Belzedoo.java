package org.ekironjisolutions.udooboard.udooboard.libbelzedoo;// -*- mode: java; c-basic-offset: 2; -*-
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

import android.os.AsyncTask;
import java.io.IOException;
import java.util.Arrays;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Belzedoo{
    private OutputStream outputStream;
    private InputStream inputStream;
    private Connection mConnection;

    public final int MIN_SKETCH_VERSION = 1;

//    public Belzedoo(OutputStream outputStream, InputStream inputStream, UdooConnectionInterface udooConnection) {
//        this.outputStream = outputStream;
//        this.inputStream = inputStream;
//        this.udooConnection = udooConnection;
//    }


    public Belzedoo(Connection mConnection) {
        this.mConnection = mConnection;
        this.outputStream = mConnection.getOutputStream();
        this.inputStream = mConnection.getInputStream();
    }

    public static final int HIGH = 1;
    public static final int LOW = 0;
    public static final int OUTPUT = 1;
    public static final int INPUT = 0;

    public void hi() {
        JSONObject json = new JSONObject();
        try {
            json.put("method", "hi");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject response = sendJson(json);

        try {
            boolean success = ((Boolean) response.get("success")).booleanValue();
            int version = (Integer) response.get("version");

            if (success) {
                if (version < MIN_SKETCH_VERSION) {
                    throw new RuntimeException("Arduino sketch too old.");
                }
                return;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        throw new RuntimeException("Invalid response from ADK");
    }

    public void digitalWrite(String pin, String value){
        int lowhigh = HIGH;
        if (value.toUpperCase().charAt(0) == 'L') {
            lowhigh = LOW;
        }

        JSONObject json = new JSONObject();
        try {
            json.put("method", "digitalWrite");
            json.put("pin", pinNameToInt(pin));
            json.put("value", lowhigh);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendJson(json);
    }

    public void analogWrite(String pin, int value) {
        JSONObject json = new JSONObject();
        try {
            json.put("method", "analogWrite");
            json.put("pin", pinNameToInt(pin));
            json.put("value", value);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendJson(json);
    }

    public void pinMode(String pin, String value) {
        int inout = OUTPUT;
        if (value.toUpperCase().charAt(0) == 'I') {
            inout = INPUT;
        }

        JSONObject json = new JSONObject();
        try {
            json.put("method", "pinMode");
            json.put("pin", pinNameToInt(pin));
            json.put("value", inout);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendJson(json);
    }

    public void delay(int value) {
        JSONObject json = new JSONObject();
        try {
            json.put("method", "delay");
            json.put("value", value);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendJson(json);
    }

    public int digitalRead(String pin) throws Exception {
        JSONObject json = new JSONObject();
        try {
            json.put("method", "digitalRead");
            json.put("pin", pinNameToInt(pin));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject response = sendJson(json);

        try {
            boolean success = ((Boolean) response.get("success")).booleanValue();
            int value = (Integer) response.get("value");

            if (success) {
                return value;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        throw new Exception("Invalid response from ADK");
    }

    public int analogRead(String pin) throws Exception {
        JSONObject json = new JSONObject();
        try {
            json.put("method", "analogRead");
            json.put("pin", pinNameToInt(pin));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject response = sendJson(json);

        try {
            boolean success = ((Boolean) response.get("success")).booleanValue();
            int value = (Integer) response.get("value");

            if (success) {
                return value;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        throw new Exception("Invalid response from ADK");
    }

    int map(int value, int fromLow, int fromHigh, int toLow, int toHigh) throws Exception {
        JSONObject json = new JSONObject();
        try {
            json.put("method", "map");
            json.put("value", value);
            json.put("fromLow", fromLow);
            json.put("fromHigh", fromHigh);
            json.put("toLow", toLow);
            json.put("toHigh", toHigh);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject response = sendJson(json);

        try {
            boolean success = ((Boolean) response.get("success")).booleanValue();

            if (success) {
                return (Integer) response.get("value");
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        throw new Exception("Invalid response from ADK");
    }

    public JSONObject sensor(String pin, String sensorName) throws Exception {
        JSONObject json = new JSONObject();
        try {
            json.put("sensor", sensorName);
            json.put("pin", pinNameToInt(pin));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject response = sendJson(json);

        try {
            boolean success = ((Boolean) response.get("success")).booleanValue();

            if (success) {
                return response;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        throw new Exception("Invalid response from ADK");
    }


    private JSONObject sendJson(JSONObject json){
        try {
            return new JsonWriterTask().execute(json).get(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Logger.getLogger(Belzedoo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(Belzedoo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TimeoutException ex) {
            Logger.getLogger(Belzedoo.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    private class JsonWriterTask extends AsyncTask<JSONObject, Void, JSONObject>    {
        protected JSONObject doInBackground(JSONObject... args)  {
            try {

                outputStream = mConnection.getOutputStream();
                inputStream = mConnection.getInputStream();

                if(outputStream == null)
                    return null;

                // add random id msg
                int msgKey = (int) Math.random()*35000;
                args[0].put("id",msgKey);

                String temp = args[0].toString();

                outputStream.write(temp.getBytes());
                outputStream.flush();

                String readResponse = this.read().trim();
                Log.d("REQUEST", temp);
                Log.d("RESPONSE", readResponse);
                return new JSONObject(readResponse);

            } catch (IOException e) {
                mConnection.disconnect();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) { }
                mConnection.reconnect();
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        private String read(){
            byte[] buffer = new byte[256];
            byte[] response;
            String message = "";
            int mByteRead = -1;

            try {
                if (inputStream instanceof FileInputStream) {
                    mByteRead = inputStream.read(buffer, 0, buffer.length);
                    if (mByteRead != -1) {
                        response = Arrays.copyOfRange(buffer, 0, mByteRead);
                        message = new String(response);
                    } else {
                        message = new String();
                    }
                } else {
                    do {
                        mByteRead = inputStream.read(buffer, 0, buffer.length);
                        message += new String(Arrays.copyOfRange(buffer, 0, mByteRead));
                    } while (!message.contains("\n"));
                }

            } catch (IOException e) {
                Log.e("ARDUINO IO Exception", e.getMessage());
                message = null;
            }

            return message;
        }
    }

    private int pinNameToInt(String name)    {
        name = name.trim().toUpperCase();

        if (name.equals("A0")) {
            return 54;
        } else if (name.equals("A1")) {
            return 55;
        } else if (name.equals("A2")) {
            return 56;
        } else if (name.equals("A3")) {
            return 57;
        } else if (name.equals("A4")) {
            return 58;
        } else if (name.equals("A5")) {
            return 59;
        } else if (name.equals("A6")) {
            return 60;
        } else if (name.equals("A7")) {
            return 61;
        } else {
            return Integer.parseInt(name);
        }
    }


    public boolean areStreamsAvailable(){
        return mConnection.areStreamsAvailable();
    }

}
