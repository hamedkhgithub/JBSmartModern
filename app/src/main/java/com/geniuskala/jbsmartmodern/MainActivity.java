package com.geniuskala.jbsmartmodern;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    private static final int REQ_BT = 5001;

    private BluetoothAdapter adapter;
    private final List<BluetoothDevice> devices = new ArrayList<>();
    private final Set<String> addresses = new HashSet<>();
    private ArrayAdapter<String> spinnerAdapter;

    private Spinner deviceSpinner;
    private TextView statusText;

    // We deliberately access iBluz by reflection so small API differences
    // between old/new SDK releases do not stop this app from compiling.
    private Object bluzDevice;
    private Object bluzManager;

    private int lastR = 255;
    private int lastG = 255;
    private int lastB = 255;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        adapter = BluetoothAdapter.getDefaultAdapter();
        statusText = findViewById(R.id.statusText);
        deviceSpinner = findViewById(R.id.deviceSpinner);

        spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, new ArrayList<>());
        deviceSpinner.setAdapter(spinnerAdapter);

        findViewById(R.id.scanButton).setOnClickListener(v -> ensurePermissionsAndScan());
        findViewById(R.id.connectButton).setOnClickListener(v -> connectSelected());

        findViewById(R.id.onButton).setOnClickListener(v -> sendLight(true));
        findViewById(R.id.offButton).setOnClickListener(v -> sendLight(false));

        findViewById(R.id.redButton).setOnClickListener(v -> sendColor(255, 0, 0));
        findViewById(R.id.greenButton).setOnClickListener(v -> sendColor(0, 255, 0));
        findViewById(R.id.blueButton).setOnClickListener(v -> sendColor(0, 0, 255));
        findViewById(R.id.whiteButton).setOnClickListener(v -> sendColor(255, 255, 255));
        findViewById(R.id.purpleButton).setOnClickListener(v -> sendColor(180, 0, 255));
        findViewById(R.id.cyanButton).setOnClickListener(v -> sendColor(0, 255, 255));

        registerReceiver(receiver, makeFilter());

        if (adapter == null) {
            setStatus("این گوشی Bluetooth ندارد.");
        }
    }

    private IntentFilter makeFilter() {
        IntentFilter f = new IntentFilter();
        f.addAction(BluetoothDevice.ACTION_FOUND);
        f.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        return f;
    }

    private boolean hasBtPermissions() {
        if (Build.VERSION.SDK_INT >= 31) {
            return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void ensurePermissionsAndScan() {
        if (adapter == null) return;

        if (!hasBtPermissions()) {
            if (Build.VERSION.SDK_INT >= 31) {
                requestPermissions(new String[] {
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                }, REQ_BT);
            } else {
                requestPermissions(new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, REQ_BT);
            }
            return;
        }

        if (!adapter.isEnabled()) {
            try {
                startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            } catch (Exception e) {
                toast("Bluetooth را روشن کن.");
            }
            return;
        }

        scan();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_BT && hasBtPermissions()) {
            scan();
        } else {
            toast("برای یافتن لامپ، مجوز Bluetooth لازم است.");
        }
    }

    @SuppressWarnings("MissingPermission")
    private void scan() {
        devices.clear();
        addresses.clear();
        spinnerAdapter.clear();

        // Add already-paired devices first.
        try {
            for (BluetoothDevice d : adapter.getBondedDevices()) addDevice(d);
        } catch (Exception ignored) {}

        setStatus("در حال جست‌وجو…");
        if (adapter.isDiscovering()) adapter.cancelDiscovery();
        adapter.startDiscovery();
    }

    @SuppressWarnings("MissingPermission")
    private void addDevice(BluetoothDevice d) {
        if (d == null || d.getAddress() == null || addresses.contains(d.getAddress())) return;
        addresses.add(d.getAddress());
        devices.add(d);

        String name;
        try { name = d.getName(); } catch (Exception e) { name = null; }
        if (name == null || name.trim().isEmpty()) name = "Bluetooth device";

        spinnerAdapter.add(name + "  •  " + d.getAddress());
        spinnerAdapter.notifyDataSetChanged();
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String a = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(a)) {
                BluetoothDevice d;
                if (Build.VERSION.SDK_INT >= 33) {
                    d = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                } else {
                    d = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                }
                addDevice(d);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(a)) {
                setStatus("جست‌وجو تمام شد؛ دستگاه لامپ را انتخاب کن.");
            }
        }
    };

    @SuppressWarnings("MissingPermission")
    private void connectSelected() {
        int pos = deviceSpinner.getSelectedItemPosition();
        if (pos < 0 || pos >= devices.size()) {
            toast("اول دستگاه لامپ را انتخاب کن.");
            return;
        }

        BluetoothDevice target = devices.get(pos);
        setStatus("در حال اتصال به " + safeName(target) + " …");

        new Thread(() -> {
            try {
                Class<?> factory = Class.forName("com.actions.ibluz.factory.BluzDeviceFactory");
                Method getDevice = factory.getMethod("getDevice", Context.class);
                bluzDevice = getDevice.invoke(null, this);

                // Find a compatible connect(BluetoothDevice) method.
                Method connect = findMethod(bluzDevice.getClass(), "connect", BluetoothDevice.class);
                if (connect == null) {
                    // Some versions expose retry(BluetoothDevice) as the public connector.
                    connect = findMethod(bluzDevice.getClass(), "retry", BluetoothDevice.class);
                }
                if (connect == null) {
                    throw new IllegalStateException("iBluz connect method not found");
                }

                connect.invoke(bluzDevice, target);

                // Give the legacy SDK a little time, then create BluzManager.
                handler.postDelayed(() -> initManager(), 1800);

            } catch (Throwable e) {
                showError("اتصال iBluz ناموفق بود: " + rootMessage(e));
            }
        }).start();
    }

    private void initManager() {
        try {
            Class<?> iDeviceClass = Class.forName("com.actions.ibluz.factory.IBluzDevice");
            Class<?> readyClass = Class.forName(
                    "com.actions.ibluz.manager.BluzManagerData$OnManagerReadyListener");
            Class<?> managerClass = Class.forName("com.actions.ibluz.manager.BluzManager");

            Object readyProxy = Proxy.newProxyInstance(
                    readyClass.getClassLoader(),
                    new Class<?>[]{readyClass},
                    (proxy, method, args) -> {
                        if ("onReady".equals(method.getName())) {
                            handler.post(() -> setStatus("متصل شد؛ کنترل نور آماده است."));
                        }
                        return null;
                    });

            Constructor<?> ctor = managerClass.getConstructor(
                    Context.class, iDeviceClass, readyClass);
            bluzManager = ctor.newInstance(this, bluzDevice, readyProxy);

            // Even if the ready callback is delayed, allow commands shortly after.
            handler.postDelayed(() -> {
                if (bluzManager != null) setStatus("اتصال برقرار است؛ یک رنگ را امتحان کن.");
            }, 1200);

        } catch (Throwable e) {
            showError("BluzManager ساخته نشد: " + rootMessage(e));
        }
    }

    private void sendColor(int r, int g, int b) {
        lastR = clamp(r);
        lastG = clamp(g);
        lastB = clamp(b);
        sendRgbPower(lastR, lastG, lastB, true);
    }

    private void sendLight(boolean on) {
        sendRgbPower(lastR, lastG, lastB, on);
    }

    private void sendRgbPower(int r, int g, int b, boolean on) {
        if (bluzManager == null) {
            toast("ابتدا به لامپ وصل شو.");
            return;
        }

        try {
            Class<?> managerClass = Class.forName("com.actions.ibluz.manager.BluzManager");
            Method buildKey = managerClass.getMethod("buildKey", int.class, int.class);
            int key = (Integer) buildKey.invoke(null, 4, 131);

            // Recovered from original JBsmart Bulb APK:
            // packed color = (R << 16) + (B << 8) + G
            int packedColor = (clamp(r) << 16) | (clamp(b) << 8) | clamp(g);

            // Original app uses 0x50 / 0x51 as the two light states.
            // Its decompiled UI maps 0x51 to one side of the ON/OFF toggle.
            int state = on ? 0x51 : 0x50;

            Method send = bluzManager.getClass().getMethod(
                    "sendCustomCommand",
                    int.class, int.class, int.class, byte[].class);

            send.invoke(bluzManager, key, packedColor, state, new byte[0]);
            setStatus((on ? "روشن" : "خاموش") + " • RGB " + r + "," + g + "," + b);

        } catch (Throwable e) {
            showError("ارسال فرمان ناموفق بود: " + rootMessage(e));
        }
    }

    private Method findMethod(Class<?> c, String name, Class<?> arg) {
        for (Method m : c.getMethods()) {
            Class<?>[] p = m.getParameterTypes();
            if (m.getName().equals(name) && p.length == 1 && p[0].isAssignableFrom(arg)) {
                return m;
            }
            if (m.getName().equals(name) && p.length == 1 && arg.isAssignableFrom(p[0])) {
                return m;
            }
        }
        return null;
    }

    private int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    @SuppressWarnings("MissingPermission")
    private String safeName(BluetoothDevice d) {
        try {
            String n = d.getName();
            return n == null ? d.getAddress() : n;
        } catch (Exception e) {
            return d.getAddress();
        }
    }

    private void setStatus(String s) {
        runOnUiThread(() -> statusText.setText(s));
    }

    private void toast(String s) {
        runOnUiThread(() -> Toast.makeText(this, s, Toast.LENGTH_LONG).show());
    }

    private void showError(String s) {
        setStatus(s);
        toast(s);
    }

    private String rootMessage(Throwable t) {
        Throwable x = t;
        while (x.getCause() != null) x = x.getCause();
        String m = x.getMessage();
        return x.getClass().getSimpleName() + (m == null ? "" : ": " + m);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(receiver); } catch (Exception ignored) {}
        try {
            if (adapter != null && hasBtPermissions() && adapter.isDiscovering()) {
                adapter.cancelDiscovery();
            }
        } catch (Exception ignored) {}
    }
}
