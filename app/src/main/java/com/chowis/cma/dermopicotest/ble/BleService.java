/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.chowis.cma.dermopicotest.ble;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import java.util.List;
import java.util.UUID;

import timber.log.Timber;

import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.BOND_BONDING;
import static android.bluetooth.BluetoothDevice.BOND_NONE;

// Service for managing connection and data communication with a GATT server hosted on a
// given Bluetooth LE device.
public class BleService extends Service {
    private BluetoothManager bluetoothManager = null;
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothGatt bluetoothGatt = null;
    private BluetoothGattCharacteristic keyCharacteristic = null;

    private String bleAddress;
    private String connectionState = STATE_DISCONNECTED;
    private boolean hasPendingWriteCharacteristic = false;
    private boolean batteryReadRequestSent = false;
    private boolean batteryNotificationRequestSent = false;
    private boolean moistureNotificationRequestSent = false;
    private boolean restartInitSequence = false;
    private int batteryLevel = 0;
    private int initSequenceRetryCount = 0;
    private BleMode previousBleMode = BleMode.MODE_OFF;
    private static final int MAX_RETRY_WRITE = 50;
    private static final int MAX_RETRY_INIT_SEQUENCE = 5;
    private static final String STATE_DISCONNECTED = "STATE_DISCONNECTED";
    private static final String STATE_CONNECTING = "STATE_CONNECTING";
    private static final String STATE_CONNECTED = "STATE_CONNECTED";

    public final static String ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED";
    public final static String ACTION_CHARACTERISTIC_WRITE_COMPLETE = "ACTION_CHARACTERISTIC_WRITE_COMPLETE";
    public final static String ACTION_CHARACTERISTIC_WRITE_ERROR = "ACTION_CHARACTERISTIC_WRITE_ERROR";
    public final static String ACTION_BATTERY_LEVEL_CHANGED = "ACTION_BATTERY_LEVEL_CHANGED";
    public final static String ACTION_MOISTURE_DATA_AVAILABLE = "ACTION_MOISTURE_DATA_AVAILABLE";
    public final static String ACTION_OTHER_DATA_AVAILABLE = "ACTION_OTHER_DATA_AVAILABLE";
    public final static String BATTERY_DATA = "BATTERY_DATA";
    public final static String MOISTURE_DATA = "MOISTURE_DATA";
    public final static String OTHER_DATA = "OTHER_DATA";

    public boolean isConnected() {
        return connectionState.equals(STATE_CONNECTED);
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    private void restartInitSequence() {
        if (initSequenceRetryCount == MAX_RETRY_INIT_SEQUENCE) {
            Timber.e("Restart init sequence failed after %s retries.", MAX_RETRY_INIT_SEQUENCE);
            restartInitSequence = false;
        } else {
            initSequenceRetryCount++;
            restartInitSequence = true;
            Timber.d("Restarting init sequence: initSequenceRetryCount=%s", initSequenceRetryCount);
            disconnect();
        }
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Timber.d("this=%s, status=%s, newState=%s", this, status, newState);

            final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(bleAddress);
            if (device == null) {
                Timber.e("Device not found. Unable to connect.");
            }

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    int bondState = device.getBondState();
                    // Take action depending on the bond state
                    if (bondState == BOND_NONE || bondState == BOND_BONDED) {

                        restartInitSequence = false;
                        keyCharacteristic = null;
                        batteryReadRequestSent = false;
                        batteryNotificationRequestSent = false;
                        moistureNotificationRequestSent = false;
                        previousBleMode = BleMode.MODE_OFF;

                        Timber.d("Connected to GATT server.");
                        connectionState = STATE_CONNECTED;
                        broadcastUpdate(ACTION_GATT_CONNECTED);

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }

                        if (bluetoothGatt.discoverServices()) {
                            Timber.d("Started asynchronous service discovery. Waiting for onServicesDiscovered().");
                        } else {
                            Timber.e("Failed to start asynchronous service discovery.");
                            restartInitSequence();
                        }

                    } else if (bondState == BOND_BONDING) {
                        // Bonding process in progress, let it complete
                        Timber.d("Waiting for bonding to complete.");
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    // We successfully disconnected on our own request including
                    // our request to restart the init sequence.
                    Timber.d("Disconnected from GATT server.");
                    bluetoothGatt.close();
                    bluetoothGatt = null;
                    connectionState = STATE_DISCONNECTED;
                    broadcastUpdate(ACTION_GATT_DISCONNECTED);

                    // when init sequence fails, disconnect and then reconnect to restart it all.
                    // initSequence: GATT connect, discover services, enable battery and moisture
                    // note: no restart when there is a pending characteristic write (for now)
                    // since it still fails based on testing (doesn't change the odds)
                    if (restartInitSequence) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }
                        connectGatt("");
                    }
                } else {
                    // We're CONNECTING or DISCONNECTING, ignore for now
                    Timber.d("We're CONNECTING or DISCONNECTING, ignore for now.");
                }
            } else if (status == 8) { // BluetoothGatt.GATT_CONN_TIMEOUT) {
                Timber.e("The connection timed out and the device disconnected itself.");
                bluetoothGatt.close();
                bluetoothGatt = null;
                connectionState = STATE_DISCONNECTED;
                broadcastUpdate(ACTION_GATT_DISCONNECTED);
            } else {
                // An error happened...figure out what happened!
                Timber.e("An error happened...figure out what happened!");
                bluetoothGatt.close();
                bluetoothGatt = null;
                connectionState = STATE_DISCONNECTED;
                broadcastUpdate(ACTION_GATT_DISCONNECTED);

                // reconnect if has not yet successfully connected
                if (connectionState.equals(STATE_CONNECTING)) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                    connectGatt("");
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Timber.d("status=%s", status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Timber.d("Successfully discovered services.");

                UUID keyServiceUuid = GattAttribute.KEY_SERVICE.getUUID();
                BluetoothGattService keyService = bluetoothGatt.getService(keyServiceUuid);
                if (keyService == null) {
                    Timber.e("Key service not found.");
                    restartInitSequence();
                    return;
                }

                final UUID keyCharacteristicUuid = GattAttribute.KEY_CHARACTERISTIC.getUUID();
                BluetoothGattCharacteristic keyCharacteristic = keyService.getCharacteristic(keyCharacteristicUuid);
                if (keyCharacteristic == null) {
                    Timber.e("Key characteristic not found.");
                    restartInitSequence();
                    return;
                } else {
                    Timber.d("Successfully found key characteristic.");
                    BleService.this.keyCharacteristic = keyCharacteristic;
                }

                // Send battery notification request only after services are discovered.
                if (!batteryReadRequestSent) {
                    boolean sendSuccess = sendBatteryReadRequest();

                    if (sendSuccess) {
                        batteryReadRequestSent = true;
                    } else {
                        Timber.e("Failed to send battery notification request.");
                        restartInitSequence();
                    }
                }
            } else {
                Timber.e("discoverServices() failed: Unable to discover GATT services.");
                restartInitSequence();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Timber.d("characteristic=%s, status=%s", GattAttribute.getUuidName(characteristic), status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Send battery notification request only after battery read request is sent.
                if (batteryReadRequestSent) {
                    Timber.d("Successfully received battery level from the device.");
                    broadcastDataAvailable(characteristic);
                    if (!batteryNotificationRequestSent) {
                        boolean sendSuccess = sendBatteryNotificationRequest(true);

                        if (sendSuccess) {
                            batteryNotificationRequestSent = true;
                        } else {
                            Timber.e("Failed to send battery notification request.");
                            restartInitSequence();
                        }
                    } else {
                        Timber.e("Unexpected execution block.");
                    }
                } else {
                    Timber.e("Unexpected execution block.");
                }
            } else {
                Timber.e("readCharacteristic() failed: Unable to receive a value.");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Timber.d("characteristic=%s", GattAttribute.getUuidName(characteristic));
            // Used to automatically receive notifications of changes to battery level and moisture value
            broadcastDataAvailable(characteristic);
        }


        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Timber.d("characteristic=%s, status=%s", GattAttribute.getUuidName(characteristic), status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_CHARACTERISTIC_WRITE_COMPLETE);
            } else {
                Timber.e("writeCharacteristic() failed.");
                broadcastUpdate(ACTION_CHARACTERISTIC_WRITE_ERROR);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            // this callback is called for writeDescriptor() for battery and moisture
            Timber.d("status=%s", status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (batteryNotificationRequestSent) {
                    if (!moistureNotificationRequestSent) {
                        Timber.d("Device successfully turned on battery notification.");
                        // At this point battery notification has been turned on.
                        // Now it's time to send moisture notification request.
                        boolean sendSuccess = sendMoistureNotificationRequest(true);

                        if (sendSuccess) {
                            moistureNotificationRequestSent = true;
                        } else {
                            Timber.e("Failed to send moisture notification request.");
                            restartInitSequence();
                        }
                    } else {
                        Timber.d("Device successfully turned on moisture notification.");
                        Timber.d("Init sequence successfully completed.");
                        // Init sequence is completed at this point.
                        // We have connected GATT, discovered services, received the initial
                        // battery level and finally turned on battery and moisture notifications.
                        if (hasPendingWriteCharacteristic) {
                            // Previous writeCharacteristic() failed so we retry.
                            writeCharacteristicWithRetry();
                        }
                    }
                } else {
                    Timber.e("Unexpected execution block.");
                }
            } else {
                Timber.e("writeDescriptor() failed: Unable to turn on a notification.");
                restartInitSequence();
            }
        }
    };

    private void broadcastUpdate(final String action) {
        Timber.d("action=%s", action);
        if (action.equals(ACTION_GATT_CONNECTED) || action.equals(ACTION_GATT_DISCONNECTED))
            batteryLevel = 0;
        Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastDataAvailable(final BluetoothGattCharacteristic characteristic) {
        Timber.d("characteristic=%s", GattAttribute.getUuidName(characteristic));
        String characteristicStr = characteristic.getUuid().toString();
        Intent intent = new Intent();

        if (GattAttribute.BATTERY_CHARACTERISTIC.getUuidString().equals(characteristicStr)) {
            int batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            Timber.d("batteryLevel=%s", batteryLevel);
            this.batteryLevel = batteryLevel;
            intent.setAction(ACTION_BATTERY_LEVEL_CHANGED);

        } else if (GattAttribute.MOISTURE_CHARACTERISTIC.getUuidString().equals(characteristicStr)) {
            final byte[] rawData = characteristic.getValue();
            if (rawData != null && rawData.length > 0) {
                byte[] adjustedData = new byte[rawData.length];
                for (int i = 0; i < rawData.length; i++) {
                    if (rawData[i] == 0x30 ||
                            rawData[i] == 0x31 ||
                            rawData[i] == 0x32 ||
                            rawData[i] == 0x33 ||
                            rawData[i] == 0x34 ||
                            rawData[i] == 0x35 ||
                            rawData[i] == 0x36 ||
                            rawData[i] == 0x37 ||
                            rawData[i] == 0x38 ||
                            rawData[i] == 0x39) {
                        adjustedData[i] = rawData[i];
                    }
                }

                String strData = new String(adjustedData).trim();
                int moistureData = 0;

                try {
                    moistureData = Integer.parseInt(strData);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }

                Timber.d("moistureData=%s", moistureData);
                intent.setAction(ACTION_MOISTURE_DATA_AVAILABLE);
                intent.putExtra(MOISTURE_DATA, moistureData);
            }

        } else {
            Timber.e("Unexpected execution block.");
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data) {
                    stringBuilder.append(String.format("%02X ", byteChar));
                }

                intent.setAction(ACTION_OTHER_DATA_AVAILABLE);
                intent.putExtra(OTHER_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
        }
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public BleService getService() {
            Timber.d(" ");
            return BleService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Timber.d(" ");
        return localBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Timber.d(" ");
        super.onRebind(intent);
    }


    @Override
    public boolean onUnbind(Intent intent) {
        Timber.d(" ");
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        //close();
        return super.onUnbind(intent);
    }

    private final IBinder localBinder = new LocalBinder();

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        Timber.d("this=%s, address=%s, connectionState=%s", this, address, connectionState);

        if (address == null || address.equals("")) {
            Timber.e("Unspecified address.");
            return false;
        }

        // For API level 18 and above, get a reference to BluetoothAdapter through BluetoothManager.
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                Timber.e("Unable to initialize BluetoothManager.");
                return false;
            }
        }

        if (bluetoothAdapter == null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter == null) {
                Timber.e("Unable to obtain a BluetoothAdapter.");
                return false;
            } else {
                if (!bluetoothAdapter.isEnabled()) {
                    Timber.e("BluetoothAdapter is disabled.");
                    return false;
                }
            }
        }

        if (address.equals(bleAddress)) {
            if (connectionState.equals(STATE_CONNECTED) || connectionState.equals(STATE_CONNECTING)) {
                Timber.d("State is connected or connecting. No need to connect again.");
                return true;
            }
            // Previously connected device.  Try to reconnect.
            else if (bluetoothGatt != null) {
                Timber.d("Trying to use an existing BluetoothGatt for connection.");
                if (bluetoothGatt.connect()) {
                    connectionState = STATE_CONNECTING;
                    return true;
                } else {
                    Timber.e("Reuse of existing BluetoothGatt for connection failed.");
                    return false;
                }
            }
        }

        return connectGatt(address);
    }

    private boolean connectGatt(String address) {
        Timber.d("bluetoothGatt=%s", bluetoothGatt);

        if (address.isEmpty())
            // Has previously connected to this device in this app session.
            address = bleAddress;

        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Timber.e("Device not found. Unable to connect.");
            return false;
        }

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        // Additionally, when disconnect is received we close bluetoothGatt and set it to null.
        // This is appears to be the best practice. See this detailed series on BLE topic:
        // https://medium.com/@martijn.van.welie/making-android-ble-work-part-2-47a3cdaade07
        Timber.d("Trying to create a new connection.");
        bluetoothGatt = device.connectGatt(this, true, bluetoothGattCallback);
        if (bluetoothGatt != null) {
            connectionState = STATE_CONNECTING;
            bleAddress = address;
        }
        Timber.d("bluetoothGatt=%s, connectionState=%s", bluetoothGatt, connectionState);
        return (bluetoothGatt != null);
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        Timber.d("this=%s", this);
        if (bluetoothGatt == null) {
            Timber.w("BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        Timber.d("this=%s", this);
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }

    /**
     * Enables or disables notification on a given characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enable         If true, enable notification.  False otherwise.
     */
    @SuppressLint("NewApi")
    private boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enable) {
        Timber.d("characteristic=%s, enabled=%s", GattAttribute.getUuidName(characteristic), enable);
        if (bluetoothGatt == null) {
            Timber.e("Not initialized.");
            return false;
        }

        // bluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER);

        // Step 1. To enable notifications on Android, you normally have to locally
        // enable the notification for the particular characteristic you are interested in.
        if (bluetoothGatt.setCharacteristicNotification(characteristic, enable)) {
            Timber.d("Successfully set characteristic notification status.");
        } else {
            Timber.e("Failed to set characteristic notification status.");
            return false;
        }

        // Step 2: Once that’s done, you also have to enable notifications on the peer device
        // by writing to the device’s client characteristic configuration descriptor (CCCD).
        final UUID configUuid = GattAttribute.CCC_DESCRIPTOR.getUUID();
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(configUuid);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        if (bluetoothGatt.writeDescriptor(descriptor)) {
            Timber.d("Successfully initiated writeDescriptor(). Waiting for onDescriptorWrite().");
            return true;
        } else {
            Timber.e("Failed to initiate writeDescriptor().");
            return false;
        }
    }

    public boolean isChangeModeNeeded(BleMode incomingBleMode) {
        Timber.d("incomingBleMode="+ incomingBleMode.name());
        boolean isChangeModeNeeded;
        if (previousBleMode == incomingBleMode) {
            if (previousBleMode == BleMode.MOISTURE)
                isChangeModeNeeded = true; // in moisture tab and take moisture again -> allow to set BleMode.MOISTURE again
            else
                isChangeModeNeeded = false;
        } else {
            if (previousBleMode == BleMode.MOISTURE && incomingBleMode == BleMode.MODE_OFF) // from moisture tab to another tab with result mode (not camera mode)
                isChangeModeNeeded = false;
            else
                isChangeModeNeeded = true;
        }
        Timber.d("previousBleMode=%s, incomingBleMode=%s, isChangeModeNeeded=%s", previousBleMode.name(), incomingBleMode.name(), isChangeModeNeeded);
        return isChangeModeNeeded;
    }

    public void writeCharacteristic(BleMode bleMode) {
        Timber.d("characteristic=%s, bleMode=%s", (keyCharacteristic != null) ? GattAttribute.getUuidName(keyCharacteristic) : null, bleMode.name());

        if (bluetoothGatt == null) {
            Timber.e("Not initialized.");
            return;
        }

        if (keyCharacteristic == null) {
            Timber.e("Init sequence did not complete.");
            return;
        }

        previousBleMode = bleMode;

        byte[] byteArray = new byte[1];
        byteArray[0] = (byte) bleMode.getByte();

        keyCharacteristic.setValue(byteArray);
        hasPendingWriteCharacteristic = true;

        writeCharacteristicWithRetry();
    }

    private void writeCharacteristicWithRetry() {
        Timber.d("characteristic=%s", (keyCharacteristic != null) ? GattAttribute.getUuidName(keyCharacteristic) : null);

        if (bluetoothGatt == null) {
            Timber.e("Not initialized.");
            return;
        }

        boolean writeSuccess = false;
        int retryCount = 0;
        while (!writeSuccess) {
            Timber.d("retryCount=%s", retryCount);
            writeSuccess = writeCharacteristic();
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
            retryCount++;
            if (retryCount == MAX_RETRY_WRITE)
                break;
        }

        if (writeSuccess) {
            hasPendingWriteCharacteristic = false;
        } else {
            Timber.e("Failed writeCharacteristic() after %s retries.", MAX_RETRY_WRITE);
            restartInitSequence();
        }
    }

    private boolean writeCharacteristic() {
        Timber.d("characteristic=%s", (keyCharacteristic != null) ? GattAttribute.getUuidName(keyCharacteristic) : null);

        // An option of creating the key characteristic from scratch
        // instead of taking it from service discovery response.
        // But this did not work.
//        BluetoothGattCharacteristic rxCharacteristic = new BluetoothGattCharacteristic(GattAttribute.KEY_CHARACTERISTIC),
//                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
//                BluetoothGattCharacteristic.PERMISSION_WRITE);
//        rxCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
//        rxCharacteristic.setValue(characteristic.getValue());

        //if (bluetoothGatt.writeCharacteristic(rxCharacteristic)) {
        if (bluetoothGatt.writeCharacteristic(keyCharacteristic)) {
            Timber.d("Successfully initiated writeCharacteristic(). Waiting for onCharacteristicWrite().");
            return true;
        } else {
            Timber.e("Failed to initiate writeCharacteristic().");
            return false;
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        Timber.d(" ");
        if (bluetoothGatt == null) {
            Timber.e("Not initialized.");
            return null;
        }

        return bluetoothGatt.getServices();
    }

    private boolean sendBatteryReadRequest() {
        Timber.d(" ");
        if (bluetoothGatt == null) {
            Timber.e("Not initialized.");
            return false;
        }

        UUID batteryServiceUuid = GattAttribute.BATTERY_SERVICE.getUUID();
        BluetoothGattService batteryService = bluetoothGatt.getService(batteryServiceUuid);
        if (batteryService == null) {
            Timber.e("Battery service not found.");
            return false;
        }

        final UUID batteryCharacteristicUuid = GattAttribute.BATTERY_CHARACTERISTIC.getUUID();
        BluetoothGattCharacteristic batteryCharacteristic = batteryService.getCharacteristic(batteryCharacteristicUuid);
        if (batteryCharacteristic == null) {
            Timber.e("Battery characteristic not found.");
            return false;
        }

        if (bluetoothGatt.readCharacteristic(batteryCharacteristic)) {
            Timber.d("Successfully initiated readCharacteristic(). Waiting for onCharacteristicRead().");
            return true;
        } else {
            Timber.e("Failed to initiate readCharacteristic().");
            return false;
        }
    }

    private boolean sendBatteryNotificationRequest(boolean enable) {
        Timber.d(" ");
        if (bluetoothGatt == null) {
            Timber.e("Not initialized.");
            return false;
        }

        UUID batteryServiceUuid = GattAttribute.BATTERY_SERVICE.getUUID();
        BluetoothGattService batteryService = bluetoothGatt.getService(batteryServiceUuid);
        if (batteryService == null) {
            Timber.e("Battery service not found.");
            return false;
        }

        final UUID batteryCharacteristicUuid = GattAttribute.BATTERY_CHARACTERISTIC.getUUID();
        BluetoothGattCharacteristic batteryCharacteristic = batteryService.getCharacteristic(batteryCharacteristicUuid);
        if (batteryCharacteristic == null) {
            Timber.e("Battery characteristic not found.");
            return false;
        }

        return setCharacteristicNotification(batteryCharacteristic, enable);
    }

    private boolean sendMoistureNotificationRequest(boolean enable) {
        Timber.d(" ");
        if (bluetoothGatt == null) {
            Timber.e("Not initialized.");
            return false;
        }

        UUID moistureServiceUuid = GattAttribute.MOISTURE_SERVICE.getUUID();
        BluetoothGattService moistureService = bluetoothGatt.getService(moistureServiceUuid);
        if (moistureService == null) {
            Timber.e("Moisture service not found.");
            return false;
        }

        final UUID moistureCharacteristicUuid = GattAttribute.MOISTURE_CHARACTERISTIC.getUUID();
        BluetoothGattCharacteristic moistureCharacteristic = moistureService.getCharacteristic(moistureCharacteristicUuid);
        if (moistureCharacteristic == null) {
            Timber.e("Moisture characteristic not found.");
            return false;
        }

        return setCharacteristicNotification(moistureCharacteristic, enable);
    }
}