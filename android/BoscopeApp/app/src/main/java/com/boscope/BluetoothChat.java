
package com.boscope;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.gesture.Gesture;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.DrawableRes;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.Series;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.BatchUpdateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import android.graphics.Matrix;


public class BluetoothChat extends Activity {
    // Debugging
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;

    private int numGraphPoints = 0;
    private Spinner timeSpinner;
    private Spinner voltageSpinner;
    private Spinner triggerSpinner;
    public boolean isOscopePaused = false;

    public static final int DKGRAY = -12303292;
    public static final int GREEN = -3355444;
    public int sampNum;
    public GraphView graphView;
    public LineGraphSeries<DataPoint> currentSeries;
    public DataPoint[]  totalSeries;
    public DataPoint[] dataSeries;
    public static final int displayBufferSize = 256;
    public int packnum = 0;

    //Debug timing vars
    protected long startTime;
    protected long endTime;

    public int updateCounter;
    public int lastUpdate;

    // spinner
    LinkedHashMap<String, String> voltageMsg;
    ArrayList<String> voltageMsgArray;
    ArrayList<String> timingMsgArray;
    HashMap<String, String> timingMsg;
    HashMap<String, Double> timingVal;
    HashMap<String, Double> voltageVal;
    private VerticalSeekBar triggerSlider;

    private ArrayList<Double> frequencyBuffer;
    private ArrayList<Double> peakBuffer;
    public boolean autoranging;
    public boolean autorangingTime;
    public boolean autorangingVolt;
    public int currentVoltageScale;
    public int currentTimingScale;
    private boolean firstRead;
    private double currentPeak2peak;
    // TODO: naming confusion
    private double currentTimeScale;
    private double currentNumericalVoltage;
    private double voltageSccaleFactor;
    private boolean shifted;
    private double largestPoint;

    private ScaleGestureDetector SGD;
    GestureDetector gestureDetector;
    GestureListener glistener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the window layout
        setContentView(R.layout.graphs);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        initDropDowns();

        graphView = (GraphView) findViewById(R.id.graph1);
        GridLabelRenderer renderer = graphView.getGridLabelRenderer();
        renderer.setHorizontalLabelsVisible(false);
        renderer.setVerticalLabelsVisible(false);
        renderer.setNumHorizontalLabels(21);
        renderer.setNumVerticalLabels(11);
        renderer.getStyles().gridColor = DKGRAY;


        graphView.getViewport().setMaxX(10);
        graphView.getViewport().setMinX(0);

        DataPoint data[] = generateDataPointdata(500, 0.01);
        currentSeries = new LineGraphSeries<DataPoint>();
        currentSeries.resetData(generateDataPointdata(120, 0.1));
        graphView.addSeries(currentSeries);

        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setMaxX(256);
        graphView.getViewport().setYAxisBoundsManual(true);
        graphView.getViewport().setMaxY(1024);
        graphView.getViewport().setMinY(0);

        sampNum = 0;
        dataSeries = new DataPoint[displayBufferSize];
        totalSeries = new DataPoint[displayBufferSize*2];

        // TODO: fix this mess
        voltageMsg = new LinkedHashMap<String, String>();
        voltageMsg.put("10V/div", "Va");
        voltageMsg.put("5V/div", "Vb");
        voltageMsg.put("2V/div", "Vc");
        voltageMsg.put("1V/div", "Vd");
        voltageMsg.put("0.5V/div", "Ve");
        voltageMsg.put("0.2V/div", "Vf");
        voltageMsg.put("0.1V/div", "Vg");
        voltageMsg.put("0.05V/div", "Vh");

        voltageMsgArray = new ArrayList<String>();
        voltageMsgArray.add(0,"10V/div");
        voltageMsgArray.add(1, "5V/div");
        voltageMsgArray.add(2, "2V/div");
        voltageMsgArray.add(3, "1V/div");
        voltageMsgArray.add(4, "0.5V/div");
        voltageMsgArray.add(5, "0.2V/div");
        voltageMsgArray.add(6, "0.1V/div");
        voltageMsgArray.add(7, "0.05V/div");


        firstRead = true;
        //voltageMsg.put("0.02V/div", "V0612");
        //voltageMsg.put("0.01V/div", "V0614");
        //voltageMsg.put("0.005V/div", "V0616");

        timingMsg = new HashMap<String, String>();
        timingMsg.put("20\u03BCs/div", "T000010");
        timingMsg.put("50\u03BCs/div", "T000190");
        timingMsg.put("100\u03BCs/div", "T000380");
        timingMsg.put("200\u03BCs/div", "T000760");
        timingMsg.put("500\u03BCs/div", "T001900");
        timingMsg.put("1ms/div", "T003800");
        timingMsg.put("2ms/div", "T007600");
        timingMsg.put("5ms/div", "T019000");
        timingMsg.put("10ms/div", "T038000");

        timingMsgArray = new ArrayList<String>();
        timingMsgArray.add(0,"20\u03BCs/div");
        timingMsgArray.add(1, "50\u03BCs/div");
        timingMsgArray.add(2, "100\u03BCs/div");
        timingMsgArray.add(3, "200\u03BCs/div");
        timingMsgArray.add(4, "1ms/div");
        timingMsgArray.add(5, "2ms/div");
        timingMsgArray.add(6, "5ms/div");
        timingMsgArray.add(7, "10ms/div");

        timingVal = new HashMap<String, Double>();
        timingVal.put("20\u03BCs/div", new Double(.00002));
        timingVal.put("50\u03BCs/div", new Double(.00005));
        timingVal.put("100\u03BCs/div", new Double(.0001));
        timingVal.put("200\u03BCs/div", new Double(.0002));
        timingVal.put("500\u03BCs/div", new Double(.0005));
        timingVal.put("1ms/div", new Double(.001));
        timingVal.put("2ms/div", new Double(.002));
        timingVal.put("5ms/div", new Double(.005));
        timingVal.put("10ms/div", new Double(.01));

        voltageVal = new HashMap<String, Double>();
        voltageVal.put("10V/div", new Double(10.0));
        voltageVal.put("5V/div", new Double(5.0));
        voltageVal.put("2V/div", new Double(2.0));
        voltageVal.put("1V/div", new Double(1.0));
        voltageVal.put("0.5V/div", new Double(0.5));
        voltageVal.put("0.2V/div", new Double(0.2));
        voltageVal.put("0.1V/div", new Double(0.1));
        voltageVal.put("0.05V/div", new Double(0.05));

        //clearDataIndicator();
        clearConnectedIndicator();
        updateCounter = 0;
        lastUpdate = 0;
        frequencyBuffer = new ArrayList<Double>();
        peakBuffer = new ArrayList<Double>();
        currentPeak2peak = 0;
        currentTimeScale = 0;
        currentNumericalVoltage = 0;
        voltageSccaleFactor = 1;
        shifted = false;
        largestPoint = 0;

        SGD = new ScaleGestureDetector(this, new ScaleListener());
        gestureDetector = new GestureDetector(this, new GestureListener());

        //glistener = new GestureListener();
        View graph = (View) findViewById(R.id.graph1);
        graph.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
                //SGD.onTouchEvent(event);
                //return false;
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");
        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    public void sendStringMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService == null || mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);
            Log.e(TAG, "Message Sent");
        }
    }

    private final void setStatus(int resId) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(resId);
    }

    private final void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(subTitle);
    }

    private void setConnectedIndicator() {
//        TextView conn_status = (TextView) findViewById(R.id.conn_status);
//        conn_status.setText("Connected");
//        conn_status.setTextColor(Color.GREEN);
    }

    private void clearConnectedIndicator() {
//        TextView conn_status = (TextView) findViewById(R.id.conn_status);
//        conn_status.setText("No Connection");
//        conn_status.setTextColor(Color.GRAY);
    }

    /*private void setDataIndicator() {
        TextView data_status = (TextView) findViewById(R.id.data_status);
        data_status.setText("Data");
        data_status.setTextColor(Color.GREEN);
    }

    private void clearDataIndicator() {
        TextView data_status = (TextView) findViewById(R.id.data_status);
        data_status.setText("No Data");
        data_status.setTextColor(Color.GRAY);
    }*/

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            setConnectedIndicator();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            clearConnectedIndicator();
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    break;
                case MESSAGE_READ:
                    if(firstRead) {
                        sendInitialState();
                        firstRead = false;
                    }
                    updateCounter++;
                    //setDataIndicator();
                    double[] newSet = (double[]) msg.obj;

                    /*int frequency = (Integer) msg.arg1;
                    frequencyBuffer.add(frequency);
                    if(frequencyBuffer.size() > 9) {
                        StringBuilder values = new StringBuilder();
                        for( Integer f : frequencyBuffer) {
                            values.append(f + ", ");
                        }
                        //Log.e(TAG, "Frequencies: " + values);
                        frequencyBuffer.clear();
                    }*/
                    // TODO: add some subroutines
                    double max = 0;
                    double min = 1024;

                    for(int i = 0; i < newSet.length; i++) {
                        if(newSet[i] > max)
                            max = newSet[i];
                        else if(newSet[i] < min)
                            min = newSet[i];
                    }

                    double midPoint = max - ((max - min) / 2);
                    int edgePasses = 0;

                    for(int i = 1; i < newSet.length; i++) {
                        if((newSet[i-1] < midPoint && newSet[i] > midPoint)
                            || (newSet[i-1] > midPoint && newSet[i] < midPoint))
                            edgePasses++;
                    }
                    int periods = edgePasses / 2;
                    // TODO: make global
                    double freq = 1 / (currentTimeScale*20 / periods);
                    TextView freqLabel = (TextView) findViewById(R.id.frequency);
                    //freqLabel.setText("f: " + freq + "Hz");
                    frequencyBuffer.add(freq);
                    if(frequencyBuffer.size() > 9) {
                        double total = 0;
                        for( Double f : frequencyBuffer) {
                            total += f;
                        }
                        double avg = total / 10;
                        String frequencyPrint = formatFrequency(avg);
                        freqLabel.setText("f = " + frequencyPrint);
                        //Log.e(TAG, "Frequencies: " + values);
                        frequencyBuffer.clear();
                    }

                    if(autoranging) {
                        if(0 < currentVoltageScale && currentVoltageScale < voltageMsgArray.size() - 1) {
                            int voltState = checkVoltRange(max, min);
                            switch (voltState) {
                                case -1:
                                    currentVoltageScale--;
                                    sendStringMessage(voltageMsg.get(voltageMsgArray.get(currentVoltageScale)));
                                    break;
                                case 0:
                                    // keep currentVoltageScale
                                    voltageSccaleFactor = 1.32;
                                    voltageSpinner.setSelection(currentVoltageScale);
                                    autoranging = false;
                                    break;
                                case 1:
                                    currentVoltageScale++;
                                    sendStringMessage(voltageMsg.get(voltageMsgArray.get(currentVoltageScale)));
                                    break;
                            }
                        } else {
                            Log.e(TAG, "No remaining steps: " + voltageMsgArray.get(currentVoltageScale));
                            // TODO: make volt specific
                            autoranging = false;
                        }
                    }

                    if(autorangingTime) {
                        int state = checkTimeRange(periods);
                        if(0 < currentTimingScale && currentTimingScale < timingMsgArray.size() - 1) {
                            switch (state) {
                                case -1:
                                    sendStringMessage(timingMsg.get(timingMsgArray.get(currentTimingScale)));
                                    break;
                                case 0:
                                    // keep currentVoltageScale
                                    timeSpinner.setSelection(currentTimingScale);
                                    autorangingTime = false;
                                    break;
                                case 1:
                                    sendStringMessage(timingMsg.get(timingMsgArray.get(currentTimingScale)));
                                    break;
                            }
                        } else {
                            Log.e(TAG, "No remaining steps: " + timingMsgArray.get(currentTimingScale));
                            autorangingTime = false;
                        }
                    }



                    // set currentVoltageScale to 1.3, one lower to 1.2, one bigger to 1.4

                    // if currentVoltageScale is one less than ideal voltage scale: 1.2
                    // if currentVoltageScale is one more than ideal voltage scale: 1.4
                    // if currentVoltageScale is equal to ideal voltage scale: 1.3

                    /*switch(currentVoltageScale) {
                        case 0: // 10V/div
                            scaleFactor = 1.2;
                            break;
                        case 1: // 5
                            scaleFactor = 1.3;
                            break;
                        case 2: // 2        1.2
                            scaleFactor = 1.4;
                            break;
                        case 3: // 1        1.3
                            scaleFactor = 1.3;
                            break;
                        case 4: // 0.5      1.4
                            scaleFactor = 1.4;
                            break;
                        case 5: // 0.2
                            scaleFactor = 1.30;
                            break;
                        case 6: // 0.1
                            scaleFactor = 1.3;
                            break;
                        case 7: // 0.05
                            scaleFactor = 1.3;
                            break;
                    }*/
                    // TODO; upward shifts necessary also?
                    double verticalShift = 0;
                    if(max*voltageSccaleFactor > 1024) {
                        largestPoint = max;
                        verticalShift = max*voltageSccaleFactor - 1024;
                        shifted = true;
                    } else {
                        shifted = false;
                    }

                    double newMax = 0, newMin = 1024;
                    for(int i = 0; i < newSet.length; i++) {
                        double val = (newSet[i]* voltageSccaleFactor) - verticalShift;
                        if(val > newMax)
                            newMax = val;
                        if(val < newMin)
                            newMin = val;
                        newSet[i] = val;
                    }
                    currentPeak2peak = ((newMax - newMin) / 100) * currentNumericalVoltage;
                    TextView p2pLabel = (TextView) findViewById(R.id.p2p);
                    peakBuffer.add(currentPeak2peak);
                    if(peakBuffer.size() > 4) {
                        double total = 0;
                        for( Double p : peakBuffer) {
                            total += p;
                        }
                        double avg = total / 10;
                        currentPeak2peak = avg;
                        p2pLabel.setText("p2p = " + String.format("%.2f", currentPeak2peak) + "V");
                        peakBuffer.clear();
                    }

                    dataSeries = new DataPoint[displayBufferSize];
                    for(int i = 0; i < newSet.length; i++) {
                        dataSeries[i] = new DataPoint(i, newSet[i]);
                    }
                    if(!isOscopePaused) {
                        currentSeries.resetData(dataSeries);
                    }

                    if(updateCounter == 1000) {
                        updateCounter = 0;
                    }
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private void sendInitialState() {
        int start = 3;
        timeSpinner.setSelection(start);
        voltageSpinner.setSelection(start);
        sendStringMessage(timingMsg.get(timeSpinner.getItemAtPosition(start).toString()));
        sendStringMessage(voltageMsg.get(voltageSpinner.getItemAtPosition(start).toString()));
        currentVoltageScale = start;
        currentTimingScale = start;
        currentTimeScale = timingVal.get(timeSpinner.getItemAtPosition(start).toString());
        currentNumericalVoltage = voltageVal.get(voltageSpinner.getItemAtPosition(start).toString());
        String trigger = "G" + String.format("%04d", triggerSlider.getProgress());
        sendStringMessage(trigger);
        //autoranging = true;
    }

    private String formatFrequency(double avg) {
        StringBuilder formatted = new StringBuilder();
        if(avg > 1000) {
            avg /= 1000;
            formatted.append(String.format("%.2f", avg));
            formatted.append(" KHz");
        } else if (avg > 1000000) {
            avg /= 1000000;
            formatted.append(String.format("%.2f", avg));
            formatted.append(" MHz");
        } else {
            formatted.append(String.format("%.2f", avg));
            formatted.append(" Hz");
        }
        return formatted.toString();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent = null;
        switch (item.getItemId()) {
            case R.id.secure_connect_scan:
                // Launch the DeviceListActivity to see devices and do scan
                serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            case R.id.pause:
                isOscopePaused = !isOscopePaused;
                if(item.getTitle().equals("Running")) {
                    item.setTitle("Paused");
                    item.setIcon(R.drawable.ic_action_play);
                }
                else if(item.getTitle().equals("Paused")) {
                    item.setTitle("Running");
                    item.setIcon(R.drawable.ic_action_pause);
                }
                return true;
        }
        return false;
    }

    private DataPoint[] generateDataPointdata(int n, double step) {
        DataPoint[] series = new DataPoint[256];
        double x_min = 0,  y_min = 512;
        //double x_max = 10, y_max = 800;
        for(int i=0; i<256; i++) {
            series[i] = new DataPoint(x_min++, y_min);
        }
        return series;
    }

    private double getRandom(double high, double low) {
        return Math.random() * (high - low) + low;
    }

    private int checkVoltRange(double max, double min) {
        // Ideally 60% of y-axis is covered by data points
        // 0.6 * 1024 = 614
        // 0.2 * 1024 = 205
        int peak2peak = (int)(max - min);
        int upperBound = 600;
        int lowerBound = 300;
        if(lowerBound < peak2peak && peak2peak < upperBound) {
            Log.e(TAG, "Scale good: " + voltageMsgArray.get(currentVoltageScale) + ", Peak2peak: " + peak2peak);
            return 0;
        }
        if(peak2peak < lowerBound) {
            Log.e(TAG, "Zooming in: " + voltageMsgArray.get(currentVoltageScale) + ", Peak2peak: " + peak2peak);
            //currentVoltageScale++;
            return 1;
        }
        if(peak2peak > upperBound) {
            Log.e(TAG, "Zooming out: " + voltageMsgArray.get(currentVoltageScale) + ", Peak2peak: " + peak2peak);
            //currentVoltageScale--;
            return -1;
        }
        return 0;
    }

    private int checkTimeRange(int periods) {
        int upperBound = 6;
        int lowerBound = 3;
        if(lowerBound < periods && periods < upperBound) {
            Log.e(TAG, "Scale good: " + timingMsgArray.get(currentTimingScale) + ", Periods: " + periods);
            return 0;
        }
        if(periods < lowerBound) {
            Log.e(TAG, "Zooming in: " + timingMsgArray.get(currentTimingScale) + ", Periods: " + periods);
            currentTimingScale++;
            return 1;
        }
        if(periods > upperBound) {
            Log.e(TAG, "Zooming out: " + timingMsgArray.get(currentTimingScale) + ", Periods: " + periods);
            currentTimingScale--;
            return -1;
        }
        return 0;
    }

    private void initDropDowns() {
        triggerSlider = (VerticalSeekBar) findViewById(R.id.trigger_slider);
        timeSpinner = (Spinner) findViewById(R.id.timeAxis);
        voltageSpinner = (Spinner) findViewById(R.id.voltageAxis);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(this,
                R.array.time_divisions, android.R.layout.simple_spinner_item);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this,
                R.array.voltage_divisions, android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        timeSpinner.setAdapter(adapter1);
        voltageSpinner.setAdapter(adapter2);

        timeSpinner.setOnItemSelectedListener( new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                // stuff
                String selection = parent.getItemAtPosition(pos).toString();
                Log.d(TAG, "Selected: " + selection + ". Message: " + timingMsg.get(selection));
                //selection
                // changeTimeScale(selection);
                Log.e(TAG, "Sent " + selection + " " + timingMsg.get(selection));
                sendStringMessage(timingMsg.get(selection));
                currentTimeScale = timingVal.get(selection);
                currentTimingScale = pos;
                Log.e(TAG, "Scale: " + currentTimeScale);
            }

            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        voltageSpinner.setOnItemSelectedListener( new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                // stuff
                String selection = parent.getItemAtPosition(pos).toString();
                //selection.
                Log.d(TAG, "Selected: " + selection + ". Message: " + voltageMsg.get(selection));
                sendStringMessage(voltageMsg.get(selection));
                if(pos == currentVoltageScale - 1) {
                    voltageSccaleFactor = 1.2;
                }
                if(pos == currentVoltageScale + 1) {
                    voltageSccaleFactor = 1.4;
                }
                currentVoltageScale = pos;
                currentNumericalVoltage = voltageVal.get(selection);
            }

            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        triggerSlider.setMax(1024);
        triggerSlider.setProgress(512);

        Bitmap bmpOriginal = BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_action);
        Bitmap bmpResult = Bitmap.createBitmap(bmpOriginal.getWidth(), bmpOriginal.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas tempCanvas = new Canvas(bmpResult);
        tempCanvas.rotate(90, bmpOriginal.getWidth()/2, bmpOriginal.getHeight()/2);
        tempCanvas.drawBitmap(bmpOriginal, 0, 0, null);
        triggerSlider.setThumb(new BitmapDrawable(getResources(), bmpResult));
        triggerSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                double shiftValue = voltageSccaleFactor*largestPoint - 1024;
                if(shifted) {
                    progress -= shiftValue;
                }
                progress /= voltageSccaleFactor;
                String message = "G" + String.format("%04d", progress);
                Log.e(TAG, "Selected: " + progress + ". Message: " + message);
                sendStringMessage(message);
            }
        });

        Button screenCap = (Button) findViewById(R.id.button_screenCap);
        screenCap.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bitmap;
                View graph = findViewById(R.id.graph1);
                graph.setDrawingCacheEnabled(true);
                bitmap = Bitmap.createBitmap(graph.getDrawingCache());
                graph.setDrawingCacheEnabled(false);
                String path = Environment.getExternalStorageDirectory().toString();
                Log.e(TAG, "Exporting to: " + path);
                OutputStream out = null;
                File file = new File(path, "OscilloscopeGraph.jpg");
                try {
                    out = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
                    out.flush();
                    out.close();
                } catch(IOException e) {
                    Log.e(TAG, "I/O Error", e);
                    e.getMessage();
                }

            }
        });

        Button auto = (Button) findViewById(R.id.button_auto);
        auto.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!autoranging) {
                    autoranging = true;
                    currentVoltageScale = 4;
                }
                if(!autorangingTime) {
                    autorangingTime = true;
                    currentTimingScale = 4;
                }
            }
        });
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
        }
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            Log.e(TAG, "Pinch Detected!");
            return false;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
        // event when double tap occurs
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            /*double location = e.getX();
            Log.e(TAG, "Double Tap! " + e.getX());
            if(location > 550) {
                zoomVoltageIn();
            } else if(location < 550) {
                zoomVoltageOut();
            }*/
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.e(TAG, "Scrolled!");
            return true;
        }

        @Override
        public void onLongPress(MotionEvent ev) {
            Log.e(TAG, "Long Pressed!");
        }
    }

    private void zoomVoltageIn() {
        voltageSpinner.setSelection(++currentVoltageScale);
        Log.e(TAG, "Zooming to: " + currentVoltageScale);
        sendStringMessage(voltageMsg.get(voltageSpinner.getItemAtPosition(currentVoltageScale).toString()));
        currentNumericalVoltage = voltageVal.get(voltageSpinner.getItemAtPosition(currentVoltageScale).toString());
    }

    private void zoomVoltageOut() {
        voltageSpinner.setSelection(--currentVoltageScale);
        Log.e(TAG, "Zooming to: " + currentVoltageScale);
        sendStringMessage(voltageMsg.get(voltageSpinner.getItemAtPosition(currentVoltageScale).toString()));
        currentNumericalVoltage = voltageVal.get(voltageSpinner.getItemAtPosition(currentVoltageScale).toString());
    }

}
