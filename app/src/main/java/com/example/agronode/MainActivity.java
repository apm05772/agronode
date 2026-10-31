package com.example.agronode;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Displays live ESP32 measurements or simulated measurements in demo mode. */
public class MainActivity extends Activity {

    private Button btnConnect;
    private Button btnDemo;

    private TextView txtStatus;
    private TextView txtTemperature;
    private TextView txtAirHumidity;
    private TextView txtSoilHumidity;
    private TextView txtRiskLevel;
    private TextView txtRiskMessage;
    private TextView txtLastUpdate;

    private MiniGraphView tempGraph;
    private MiniGraphView humidityGraph;

    private final ArrayList<Float> tempValues = new ArrayList<>();
    private final ArrayList<Float> airValues = new ArrayList<>();
    private final ArrayList<Float> soilValues = new ArrayList<>();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile BluetoothSocket activeSocket;

    private boolean demoRunning = false;
    private float demoTemp = 22;
    private float demoAir = 60;
    private float demoSoil = 45;

    private static final UUID SERIAL_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnConnect = findViewById(R.id.btnConnect);
        btnDemo = findViewById(R.id.btnDemo);

        txtStatus = findViewById(R.id.txtStatus);
        txtTemperature = findViewById(R.id.txtTemperature);
        txtAirHumidity = findViewById(R.id.txtAirHumidity);
        txtSoilHumidity = findViewById(R.id.txtSoilHumidity);
        txtRiskLevel = findViewById(R.id.txtRiskLevel);
        txtRiskMessage = findViewById(R.id.txtRiskMessage);
        txtLastUpdate = findViewById(R.id.txtLastUpdate);

        tempGraph = findViewById(R.id.tempGraph);
        humidityGraph = findViewById(R.id.humidityGraph);

        tempGraph.setLineColors(Color.rgb(233, 131, 53), Color.TRANSPARENT);
        humidityGraph.setLineColors(Color.rgb(54, 120, 184), Color.rgb(86, 141, 69));

        btnConnect.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
            } else {
                connectToEsp32();
            }
        });
        btnDemo.setOnClickListener(v -> toggleDemoMode());
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == 1) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                connectToEsp32();
            } else {
                showConnectionError("Se necesita permiso Bluetooth para conectar.");
            }
        }
    }

    private void connectToEsp32() {
        stopDemoMode();
        btnConnect.setEnabled(false);
        btnDemo.setEnabled(false);
        btnConnect.setText("Conectando...");
        setStatus("●  Buscando AgroNode...", R.drawable.bg_status_neutral, Color.WHITE);

        new Thread(() -> {
            try {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

                if (adapter == null) {
                    showConnectionError("Este celular no tiene Bluetooth.");
                    return;
                }

                if (!adapter.isEnabled()) {
                    showConnectionError("Bluetooth está apagado.");
                    return;
                }

                BluetoothDevice agroNode = findPairedDevice(adapter);

                if (agroNode == null) {
                    showConnectionError("No se encontró AgroNode entre los dispositivos vinculados.");
                    return;
                }

                setStatus("●  Conectando al nodo...", R.drawable.bg_status_neutral, Color.WHITE);

                BluetoothSocket socket =
                        agroNode.createRfcommSocketToServiceRecord(SERIAL_UUID);
                activeSocket = socket;
                socket.connect();

                runOnUiThread(() -> {
                    setStatus("●  Nodo conectado", R.drawable.bg_status_connected,
                            getColor(R.color.green_primary));
                    btnConnect.setText("Conectado");
                });

                try (BufferedReader reader =
                        new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        parseSensorLine(line);
                    }
                }

                showConnectionError("La conexión terminó.");

            } catch (SecurityException e) {
                showConnectionError("Permiso Bluetooth denegado.");
            } catch (Exception e) {
                showConnectionError("No se pudo conectar al ESP32.");
            } finally {
                BluetoothSocket socket = activeSocket;
                activeSocket = null;
                if (socket != null) {
                    try { socket.close(); } catch (IOException ignored) { }
                }
                runOnUiThread(() -> {
                    btnConnect.setEnabled(true);
                    btnDemo.setEnabled(true);
                    btnConnect.setText("Conectar");
                });
            }
        }).start();
    }

    private BluetoothDevice findPairedDevice(BluetoothAdapter adapter) {
        Set<BluetoothDevice> devices = adapter.getBondedDevices();

        for (BluetoothDevice device : devices) {
            try {
                String name = device.getName();

                if (name != null && name.equalsIgnoreCase("AgroNode")) {
                    return device;
                }
            } catch (SecurityException ignored) {
                return null;
            }
        }

        return null;
    }

    // Invalid frames are ignored instead of silently creating zero-valued readings.
    private void parseSensorLine(String line) {
        try {
            SensorReading reading = SensorReading.parse(line);
            updateReadings(reading.temperature, reading.airHumidity, reading.soilHumidity);
        } catch (IllegalArgumentException invalidFrame) {
            Log.w("AgroNode", "Ignoring invalid telemetry frame", invalidFrame);
        }
    }

    private void updateReadings(float temperature, float airHumidity, float soilHumidity) {
        runOnUiThread(() -> {
            txtTemperature.setText(formatNumber(temperature));
            txtAirHumidity.setText(formatNumber(airHumidity));
            txtSoilHumidity.setText(formatNumber(soilHumidity));

            updateRisk(temperature, airHumidity, soilHumidity);

            addLimited(tempValues, temperature);
            addLimited(airValues, airHumidity);
            addLimited(soilValues, soilHumidity);

            tempGraph.setGraphData(tempValues, null, 0, 50);
            humidityGraph.setGraphData(airValues, soilValues, 0, 100);

            String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    .format(new Date());
            txtLastUpdate.setText("Última actualización: " + time);
        });
    }

    private String formatNumber(float value) {
        return String.format(Locale.getDefault(), "%.1f", value);
    }

    private void updateRisk(float temperature, float airHumidity, float soilHumidity) {
        int score = 0;

        // Reglas sencillas para demostrar el prototipo.
        if (temperature >= 18 && temperature <= 30) {
            score++;
        }

        if (airHumidity >= 60) {
            score++;
        }

        if (soilHumidity < 35 || soilHumidity > 80) {
            score++;
        }

        if (score == 3) {
            txtRiskLevel.setText("RIESGO ALTO");
            txtRiskLevel.setTextColor(getColor(R.color.risk_high));
            txtRiskLevel.setBackgroundResource(R.drawable.bg_risk_high);
            txtRiskMessage.setText("Las condiciones favorecen la aparición de plagas. Revisa hojas y tallos.");
        } else if (score == 2) {
            txtRiskLevel.setText("RIESGO MEDIO");
            txtRiskLevel.setTextColor(getColor(R.color.risk_medium));
            txtRiskLevel.setBackgroundResource(R.drawable.bg_risk_medium);
            txtRiskMessage.setText("Conviene mantener el cultivo bajo observación durante las próximas horas.");
        } else {
            txtRiskLevel.setText("RIESGO BAJO");
            txtRiskLevel.setTextColor(getColor(R.color.risk_low));
            txtRiskLevel.setBackgroundResource(R.drawable.bg_risk_low);
            txtRiskMessage.setText("Las lecturas actuales no muestran condiciones de riesgo elevado.");
        }
    }

    private void addLimited(ArrayList<Float> list, float value) {
        list.add(value);

        if (list.size() > 30) {
            list.remove(0);
        }
    }

    private void setStatus(String text, int backgroundResource, int textColor) {
        runOnUiThread(() -> {
            txtStatus.setText(text);
            txtStatus.setBackgroundResource(backgroundResource);
            txtStatus.setTextColor(textColor);
        });
    }

    private void showConnectionError(String message) {
        runOnUiThread(() -> {
            setStatus("●  " + message, R.drawable.bg_status_error,
                    getColor(R.color.risk_high));
            btnConnect.setEnabled(true);
            btnConnect.setText("Conectar");
        });
    }

    private void toggleDemoMode() {
        if (demoRunning) {
            stopDemoMode();
            setStatus("●  Modo demo detenido", R.drawable.bg_status_neutral, Color.WHITE);
        } else {
            demoRunning = true;
            btnDemo.setText("Detener demo");
            btnConnect.setEnabled(false);
            setStatus("●  Modo demo activo", R.drawable.bg_status_demo,
                    getColor(R.color.risk_medium));
            runDemoLoop();
        }
    }

    private void stopDemoMode() {
        demoRunning = false;
        btnDemo.setText("Modo demo");
        btnConnect.setEnabled(true);
    }

    private void runDemoLoop() {
        handler.postDelayed(() -> {
            if (!demoRunning) {
                return;
            }

            demoTemp += (float) ((Math.random() * 2) - 1);
            demoAir += (float) ((Math.random() * 4) - 2);
            demoSoil += (float) ((Math.random() * 4) - 2);

            demoTemp = clamp(demoTemp, 15, 35);
            demoAir = clamp(demoAir, 30, 95);
            demoSoil = clamp(demoSoil, 10, 95);

            updateReadings(round1(demoTemp), round1(demoAir), round1(demoSoil));
            runDemoLoop();
        }, 2000);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    protected void onDestroy() {
        demoRunning = false;
        handler.removeCallbacksAndMessages(null);
        BluetoothSocket socket = activeSocket;
        if (socket != null) {
            try { socket.close(); } catch (IOException ignored) { }
        }
        super.onDestroy();
    }

    private float round1(float value) {
        return Math.round(value * 10) / 10f;
    }
}
