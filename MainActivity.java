//Your package here
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "MQTT";

    private int qos = 0;
    private MqttAsyncClient mqttClient;
    private MemoryPersistence persistence = new MemoryPersistence();
    private final static String MQTT_BROKER = "your serverhere:port";
    private String clientId;
    private TextView lightStatusTV, temperatureTextView, humidityTextView;
    private Button ledOn, ledOff;
    private SensorManager sensorManager;
    private Sensor gyroscopeSensor;
    private static final float GYRO_THRESHOLD = 1.0f;
    private long lastGyroUpdate;
    private static final int GYRO_UPDATE_INTERVAL = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lightStatusTV = findViewById(R.id.lightStatusTV);
        ledOff = findViewById(R.id.lightOffButton);
        ledOn = findViewById(R.id.lightOnButton);
        temperatureTextView = findViewById(R.id.temperatureTextView);
        humidityTextView = findViewById(R.id.humidityTextView);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        try {
            mqttConnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }

        if (gyroscopeSensor != null) {
            sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Log.e(TAG, "Gyroscope sensor not available.");
        }

        ledOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    MqttMessage message = new MqttMessage("TURN_ON_LCD".getBytes());
                    mqttClient.publish("lcd/control", message);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });
        ledOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    MqttMessage message = new MqttMessage("TURN_OFF_LCD".getBytes());
                    mqttClient.publish("lcd/control", message);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            mqttConnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        try {
            mqttClient.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    private MqttCallback mqttCallback = new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            Log.d(TAG, String.format("messageArrived from topic %s. Message = %s ", topic, payload));
            if (topic.equals("lcd/status")) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (payload.equals("LED_ON")) {
                            lightStatusTV.setText("LED is ON");
                        } else if
                        (payload.equals("LED_OFF")) {
                            lightStatusTV.setText("LED is OFF");
                        }
                    }
                });
            }
            if (topic.equals("sensehat/temperature")) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        temperatureTextView.setText("Temperature: " + payload + "°C");
                    }
                });
            } else if (topic.equals("sensehat/humidity")) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        humidityTextView.setText("Humidity: " + payload + "%");
                    }
                });
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
        }
    };

    private void mqttConnect() throws MqttException {
        // set mqtt options
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        clientId = MqttAsyncClient.generateClientId();

        // connect
        mqttClient = new MqttAsyncClient(MQTT_BROKER, clientId, persistence);
        mqttClient.connect(options, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.d(TAG, "onSuccess: Connected");
                // set the callback
                mqttClient.setCallback(mqttCallback);
                // subscribe
                try {
                    mqttClient.subscribe("lcd/status", qos);
                    mqttClient.subscribe("sensehat/temperature", qos);
                    mqttClient.subscribe("sensehat/humidity", qos);
                    // Request the LED state
                    mqttClient.publish("lcd/request_state", new MqttMessage("REQUEST_STATE".getBytes()));
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.e(TAG, "onFailure: " + exception.getMessage(), exception);
            }
        });
    }
//GYRO DATA
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            Log.d(TAG, String.format("Gyroscope values: x = %f, y = %f, z = %f", x, y, z));

            long currentTime = System.currentTimeMillis();
            if ((currentTime - lastGyroUpdate) > GYRO_UPDATE_INTERVAL) {
                lastGyroUpdate = currentTime;
                if (Math.abs(x) > GYRO_THRESHOLD || Math.abs(y) > GYRO_THRESHOLD || Math.abs(z) > GYRO_THRESHOLD) {
                    try {
                        MqttMessage message = new MqttMessage("GYRO_MOVEMENT".getBytes());
                        mqttClient.publish("mobile/gyro", message);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
