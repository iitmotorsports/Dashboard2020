package sae.iit.saedashboard;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Mirror {
    private final MirrorPinValueCallback onPinSet;
    private boolean enabled = false;
    private final TeensyStream stream;
    private final MirrorPinValueCallback onUpdate;
    private final MirrorModeCallback enabledCallback;
    private final MirrorModeCallback disabledCallback;
    private final Timer updateTimer = new Timer();
    private int requestedPin = 0;
    private int index = 0;
    private int maxPin = 58;
    private boolean update = true;
    private TimerTask updateTask;
    //    private final List<Long> pinList = new ArrayList<>();
    private final List<Integer> blacklist = new ArrayList<>();

    public interface MirrorModeCallback {
        void run();
    }

    public interface MirrorPinValueCallback {
        void run(int pin, Long value);
    }

    public interface MirrorPinCallback {
        void run(int pin);
    }

//    private void setListValue(int pin, long value) {
//        for (int i = pinList.size(); i <= pin; i++) {
//            pinList.add(null);
//        }
//        pinList.set(pin, value);
//    }

    private void end() {
        if (updateTask != null)
            updateTask.cancel();
        updateTimer.purge();
        enabled = false;
//        pinList.clear();
        blacklist.clear();
        index = 0;
        disabledCallback.run();
    }

    private void start() {
        updateTask = new TimerTask() {
            @Override
            public void run() {
                if (update) {
                    while (blacklist.contains(index)) {
                        index = (index + 1) % maxPin;
                    }
                    updatePin(index);
                    index = (index + 1) % maxPin;
                }
            }
        };
        updateTimer.schedule(updateTask, 50, 20);
        enabledCallback.run();
        enabled = true;
    }

    public void updatePin(int pin) {
        if (pin == TeensyStream.COMMAND.ENTER_MIRROR_SET[0])
            return;
        requestedPin = pin;
        stream.write(new byte[]{(byte) pin}); // Todo: check that casting pins > 127 works fine
    }

    public void setPin(int pin, int value) {
        boolean _update = update;
        update = false;
        if (pin == TeensyStream.COMMAND.ENTER_MIRROR_SET[0])
            return;
        stream.write(TeensyStream.COMMAND.ENTER_MIRROR_SET);
        byte[] bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
        stream.write(new byte[]{(byte) pin});
        stream.write(bytes);
        onPinSet.run(pin, (long) value);
        update = _update;
    }

    private void receiveValue(long value) {
//        setListValue(requestedPin, value);
        onUpdate.run(requestedPin, value);
    }

    public Mirror(TeensyStream stream, MirrorModeCallback enabledCallback, MirrorModeCallback disabledCallback, MirrorPinValueCallback onUpdate, MirrorPinCallback onFail, MirrorPinValueCallback onPinSet) {
        this.stream = stream;
        this.enabledCallback = enabledCallback;
        this.disabledCallback = disabledCallback;
        this.onUpdate = onUpdate;
        this.onPinSet = onPinSet;

        long enabledID = stream.requestMsgID("[Mirror]", "[ LOG ] Mirror Mode Enabled");
        long disabledID = stream.requestMsgID("[Mirror]", "[ LOG ] Mirror Mode Disabled");
        long requestID = stream.requestMsgID("[Mirror]", "[ LOG ] Requested pin");
        long noPinID = stream.requestMsgID("[Pins]", "[DEBUG] No pin defined");
        stream.setCallback(disabledID, num -> end(), TeensyStream.UPDATE.ON_RECEIVE);
        stream.setCallback(enabledID, num -> start(), TeensyStream.UPDATE.ON_RECEIVE);
        stream.setCallback(requestID, this::receiveValue, TeensyStream.UPDATE.ON_RECEIVE);
        stream.setCallback(noPinID, num -> {
            blacklist.add((int) num);
            onFail.run((int) num);
        }, TeensyStream.UPDATE.ON_RECEIVE);
    }

//    public List<Long> getPinList() {
//        return pinList;
//    }

    public void toggle() {
        stream.write(TeensyStream.COMMAND.TOGGLE_MIRROR_MODE);
    }

    public void enable() {
        if (!enabled)
            toggle();
    }

    public void enableUpdate(boolean state) {
        this.update = state;
    }

    public void disable() {
        if (enabled)
            toggle();
    }

    public void setMaxPin(int value) {
        this.maxPin = value + 1;
    }

}
