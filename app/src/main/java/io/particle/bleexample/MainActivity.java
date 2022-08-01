package io.particle.bleexample;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.protobuf.InvalidProtocolBufferException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import io.particle.controlrequestchannel.ControlRequestChannel;
import io.particle.controlrequestchannel.ControlRequestChannelCallback;
import io.particle.controlrequestchannel.Stream;
import io.particle.firmwareprotos.ctrl.wifi.WifiNew;

public class MainActivity extends AppCompatActivity {
    private String logs;

    // Device setup code. By default, 6 characters of the serial number
    private final String setupCode = "HMCS78";
    // Mobile secret available on the QR sticker on the device
    private final String mobileSecret = "U6RWB9YCSHKV5V9";
    // UUIDs defined in the firmware
    private final UUID serviceUUID = UUID.fromString("6e400021-b5a3-f393-e0a9-e50e24dcca9e");
    private final UUID txCharUUID = UUID.fromString("6e400022-b5a3-f393-e0a9-e50e24dcca9e");
    private final UUID rxCharUUID = UUID.fromString("6e400023-b5a3-f393-e0a9-e50e24dcca9e");
    private final UUID versionCharUUID = UUID.fromString("6e400024-b5a3-f393-e0a9-e50e24dcca9e");

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private ScanCallback scanCallback;
    private ArrayList<BluetoothDevice> foundDevices;
    private BluetoothGattCallback bluetoothGattCallback;

    private BluetoothGattCharacteristic txCharacteristic;
    private BluetoothGattCharacteristic rxCharacteristic;
    private BluetoothGattCharacteristic versionCharacteristic;

    private Stream stream;
    private ControlRequestChannel controlRequestChannel;
    private ControlRequestChannelCallback controlRequestChannelCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        foundDevices = new ArrayList<BluetoothDevice>();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.logs = "";
        this.log("Press Connect to start");
        MainActivity self = this;

        stream = new Stream() {
            @RequiresApi(api = 33)
            @SuppressLint("MissingPermission")
            public void write(byte[] data) {
                if (self.bluetoothGatt != null && self.txCharacteristic != null) {
                    self.bluetoothGatt.writeCharacteristic(self.txCharacteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                }
            }
        };
        controlRequestChannelCallback = new ControlRequestChannelCallback() {
            @Override
            public void onOpen() {
                super.onOpen();

                self.onRequestChannelOpen();
            }
        };
        try {
            controlRequestChannel = new ControlRequestChannel(stream, mobileSecret, controlRequestChannelCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }

        scanCallback = new ScanCallback() {
            @RequiresApi(api = Build.VERSION_CODES.R)
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);

                self.addDevice(result);
            }
        };

        bluetoothGattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);

                self.bluetoothGatt = gatt;
                runOnUiThread(() -> self.onConnectionStateChange(gatt, status, newState));
            }

            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);

                runOnUiThread(() -> self.onServicesDiscovered(gatt, status));
            }

            @Override
            public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);

                runOnUiThread(() -> self.onCharacteristicRead(gatt, characteristic, status));
            }

            @Override
            public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);

                stream.data(characteristic.getValue());
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!bluetoothAdapter.isEnabled()) {
            this.log("BLE not enabled");
            Intent enableBLEIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                this.log("Not enough permissions to use BLE");
                return;
            }
            startActivityForResult(enableBLEIntent, 1);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) {
            if (resultCode != Activity.RESULT_OK) {
                this.log("Failed to request the BLE permissions");
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 2) {
            if (!Arrays.asList(grantResults).contains(PackageManager.PERMISSION_DENIED)) {
                this.ensurePermissions();
            } else {
                this.startBleScan();
            }
        }
    }

    @SuppressLint("MissingPermission")
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        String deviceAddress = gatt.getDevice().getAddress();
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                this.log("Connected to " + deviceAddress + "!");
                gatt.discoverServices();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                this.log("Disconnected from " + deviceAddress + "!");
            }
        } else {
            this.log("Error " + status + " occurred for " + deviceAddress);
            gatt.close();
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        this.log("Finished discovering services");

        if (gatt.getServices().isEmpty()) {
            this.log("No services found!");
            return;
        }

        gatt.getServices().forEach(bluetoothGattService -> {
            this.log(bluetoothGattService.getUuid() + "\n  Has " + bluetoothGattService.getCharacteristics().size() + " characteristics");

            if (bluetoothGattService.getUuid().equals(serviceUUID)) {
                this.log("Found the communication service!");
                if (this.ensureCharacteristics(bluetoothGattService)) {
                    this.log("Checking the protocol version...");
                    if (!gatt.readCharacteristic(this.versionCharacteristic)) {
                        this.log("Failed to read the version");
                    }
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Integer version = characteristic.getIntValue(FORMAT_UINT8, 0);

            if (version != 2) {
                this.log("Protocol version " + version + " is not supported");
                return;
            }

            if (!gatt.setCharacteristicNotification(this.rxCharacteristic, true)) {
                this.log("Failed to subscribe to incoming data");
                return;
            }

            this.openControlChannel();
        } else if (status == BluetoothGatt.GATT_READ_NOT_PERMITTED) {
            this.log("Read not permitted for " + characteristic.getUuid());
        } else {
            this.log("Characteristic read failed for " + characteristic.getUuid() + ": " + status);
        }
    }

    public void onRequestChannelOpen() {
        this.log("Request channel opened! Scanning for WiFi networks...");
        this.scanWifiNetworks();
    }

    public boolean ensureCharacteristics(BluetoothGattService service) {
        this.txCharacteristic = service.getCharacteristic(this.txCharUUID);
        this.rxCharacteristic = service.getCharacteristic(this.rxCharUUID);
        this.versionCharacteristic = service.getCharacteristic(this.versionCharUUID);

        if (this.txCharacteristic == null ||
                (((this.txCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) == 0) &&
                ((this.txCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == 0))) {
            this.log("The write characteristic was not found or is not writable");
            return false;
        }

        if (this.rxCharacteristic == null ||
                ((this.rxCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0)) {
            this.log("The read characteristic was not found or does not notify");
            return false;
        }

        if (this.versionCharacteristic == null ||
                ((this.versionCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) == 0)) {
            this.log("The write characteristic was not found or is not readable");
            return false;
        }

        this.log("All necessary characteristics were found!");
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    public void onClick(View view) {
        this.log("\nLooking for " + this.setupCode + "...");

        this.startBleScan();
    }

    private boolean hasPermission(String permissionType) {
        return ContextCompat.checkSelfPermission(this, permissionType) == PackageManager.PERMISSION_GRANTED;
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private boolean ensurePermissions() {
        boolean isPermissionGranted =
                this.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
                this.hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                this.hasPermission(Manifest.permission.BLUETOOTH_CONNECT);

        if (isPermissionGranted) {
            return true;
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT
                    },
                    2
            );
            return false;
        }
    }

    private void log(String line) {
        this.logs += line + "\n";
        TextView textView = findViewById(R.id.textView);
        textView.setText(this.logs);
        ScrollView scrollView = findViewById(R.id.scrollView);
        scrollView.fullScroll(View.FOCUS_DOWN);
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.S)
    private void startBleScan() {
        if (!this.ensurePermissions()) {
            return;
        }

        if (this.bluetoothLeScanner == null) {
            this.log("BLE scanner unavailable");
            return;
        }

        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(serviceUUID))
                .build();

        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                .setReportDelay(0L)
                .build();

        this.bluetoothLeScanner.startScan(
                Arrays.asList(filter),
                scanSettings,
                scanCallback
        );
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.R)
    private void addDevice(ScanResult result) {
        BluetoothDevice device = result.getDevice();

        byte[] manufacturerDataBytes = result.getScanRecord().getManufacturerSpecificData(1634);
        String manufacturerData = new String(manufacturerDataBytes, StandardCharsets.UTF_8);
        String deviceName = result.getScanRecord().getDeviceName();
        if (!deviceName.endsWith(this.setupCode) && !manufacturerData.endsWith(this.setupCode)) {
            this.log("Found " + deviceName + " but it's not matching the setup code");
            return;
        }

        if (!this.foundDevices.contains(device)) {
            this.foundDevices.add(device);
            this.log("Found " + deviceName + " [" + device.getAddress() + "]");

            // We're connecting to the first found device, no need to scan further
            this.bluetoothLeScanner.stopScan(this.scanCallback);
            this.connect(device);
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.R)
    private void connect(BluetoothDevice device) {
        this.log("Connecting to " + device.getAlias() + "...");
        device.connectGatt(getApplicationContext(), false, this.bluetoothGattCallback);
    }

    private void openControlChannel() {
        this.log("Ready to open the control channel");
        this.controlRequestChannel.open();
    }

    private void scanWifiNetworks() {
        WifiNew.ScanNetworksRequest request = WifiNew.ScanNetworksRequest.newBuilder().build();
        // TODO: Send the request to the device and store the reply
        byte[] replyData = new byte[0];
        WifiNew.ScanNetworksReply reply = null;
        try {
            reply = WifiNew.ScanNetworksReply.parseFrom(replyData);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        this.log("Found " + reply.getNetworksCount() + " networks");
        for (WifiNew.ScanNetworksReply.Network network: reply.getNetworksList()) {
            this.log("  " + network.getSsid() + " [" + network.getBssid() + "] " + network.getRssi() + "dB");
        }
    }
}